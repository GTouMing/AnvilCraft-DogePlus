package dev.anvilcraft.gtouming.doge_plus.transfer;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.item.FilterItem;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;

/**
 * 单个维度的物品传输网状态（与 {@code logic/LevelNetworks} 同构）。
 *
 * <p>节点 = 至少有一个面带「存入」或「取出」属性的方块；连接 = 「本方某面 ↔ 正邻格的互补面」
 * （存入的对面是取出），方向是「本方存入面 → 正邻格的取出面」。一张弱连通块即一张传输网。</p>
 *
 * <p>「存入 / 取出」是面属性 {@link FaceMode} 的两个取值：管道载体由轮盘编程写入，镶嵌载体由材料
 * 自带的 {@code InlayProperty.INSERT / EXTRACT} 经 {@code BlockInlays#faceModeOf} 推导——两者最终都落在
 * {@code BlockInlays.faces} 这一张表里，这里读表即可。</p>
 *
 * <p>结构沿用逻辑门网络那一套：按维度缓存、{@code byNode} 索引、拓扑种子重建、区块卸载精确失效。
 * 「货源」则照前置流体管网的思路做成一张<b>顶点图 + 按需缓存</b>：网里只维护「哪些取出有效」
 * 这个集合（面朝容器确实存在，按差集增删），某个「存入」能到达的全部有效取出（先近后远）在真正
 * 搬运时用一次反向 BFS 现算并缓存，集合一变缓存整体作废。于是没被搬运过的节点不占缓存，
 * 端点的出现 / 消失也只做一次集合求差，不会随取出数量线性膨胀。</p>
 *
 * <p>搬运时逐个尝试缓存的取出，最近的那个取不到货就顺次改用次近的。</p>
 *
 * <p>7 游戏刻冷却只记在「存入」面上：「取出」本身没有冷却，因此多个「存入」向同一个「取出」
 * 取货时会各自立即响应，互不占位。记录与冷却都只在内存里，区块重载时会随拓扑重建一并重算。</p>
 */
final class LevelTransferNetworks {

    /** 单次更新允许的最大批次数（拓扑 + 记录），剩余脏数据交给下一 tick。 */
    private static final int MAX_SETTLING_PASSES = 16;

    /** 每个「存入」面两次搬运之间的游戏刻。 */
    private static final int TRANSFER_COOLDOWN = 7;

    private final ServerLevel level;

    /** 位置 → 所属网络。 */
    private final Long2ObjectOpenHashMap<TransferNetwork> byNode = new Long2ObjectOpenHashMap<>();

    /** 区块 → 网络集合。 */
    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<TransferNetwork>> byChunk = new Long2ObjectOpenHashMap<>();

    /** 已建网络（每 tick 只遍历它们）。 */
    private final ObjectOpenHashSet<TransferNetwork> networks = new ObjectOpenHashSet<>();

    /** 需要重建拓扑的种子点。 */
    private final LongOpenHashSet topologySeeds = new LongOpenHashSet();

    /** 拓扑没变、只需重算货源记录的网络（端点容器出现 / 消失）。 */
    private final ObjectOpenHashSet<TransferNetwork> dirtyRecords = new ObjectOpenHashSet<>();

    /** 位置 → 六个面的「可再次搬运」游戏刻，位下标同 {@link Direction#ordinal()}。 */
    private final Long2ObjectOpenHashMap<long[]> cooldowns = new Long2ObjectOpenHashMap<>();

    private boolean processingUpdates = false;

    LevelTransferNetworks(ServerLevel level) {
        this.level = level;
    }

    // ==================== 更新调度 ====================

    void requestTopologyUpdate(long packedPos) {
        this.topologySeeds.add(packedPos);
        this.runUpdates();
    }

    /**
     * 某个位置上的端点容器（方块容器；矿车 / 船 / Doge 节点这类实体容器）出现或消失。
     *
     * <p>拓扑没变，只需重算记录，所以只把「确实有面朝该位置的节点」所在网络标脏，不重建网络。
     * 这个过滤很关键：矿车沿管线跑动时会不断跨格，若不看有没有面朝着它，每跨一格都要白扫整张网。</p>
     */
    void containerChanged(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            long packed = pos.relative(dir).asLong();
            TransferNetwork network = this.byNode.get(packed);
            if (network == null) continue;
            TransferNode node = network.nodes.get(packed);
            Direction facing = dir.getOpposite();
            if (node == null || (!node.isInsert(facing) && !node.isExtract(facing))) continue;
            this.dirtyRecords.add(network);
        }
        this.runUpdates();
    }

    private void runUpdates() {
        if (this.processingUpdates) return;
        this.processingUpdates = true;
        try {
            int pass = 0;
            while ((!this.topologySeeds.isEmpty() || !this.dirtyRecords.isEmpty())
                    && pass++ < MAX_SETTLING_PASSES) {
                if (!this.topologySeeds.isEmpty()) {
                    LongOpenHashSet seeds = new LongOpenHashSet(this.topologySeeds);
                    this.topologySeeds.clear();
                    this.rebuildFromSeeds(seeds);
                }
                if (!this.dirtyRecords.isEmpty()) {
                    ObjectOpenHashSet<TransferNetwork> dirty = new ObjectOpenHashSet<>(this.dirtyRecords);
                    this.dirtyRecords.clear();
                    for (TransferNetwork network : dirty) {
                        if (network.valid) this.recomputeRecords(network);
                    }
                }
            }
        } finally {
            this.processingUpdates = false;
        }
    }

    // ==================== 拓扑重建 ====================

    private void rebuildFromSeeds(LongOpenHashSet changedPositions) {
        ObjectOpenHashSet<TransferNetwork> affected = new ObjectOpenHashSet<>();
        LongOpenHashSet rebuildSeeds = new LongOpenHashSet(changedPositions);

        for (LongIterator it = changedPositions.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            TransferNetwork old = this.byNode.get(packed);
            if (old != null) affected.add(old);
            // 新放置的节点可能把多张网络桥接起来，因此还要收集它相邻的网络。
            BlockPos pos = BlockPos.of(packed);
            for (Direction dir : Direction.values()) {
                TransferNetwork neighbor = this.byNode.get(pos.relative(dir).asLong());
                if (neighbor != null && neighbor != old) affected.add(neighbor);
            }
        }

        for (TransferNetwork network : affected) {
            this.invalidate(network, rebuildSeeds, Long.MIN_VALUE);
        }

        for (LongIterator it = rebuildSeeds.iterator(); it.hasNext(); ) {
            long seed = it.nextLong();
            if (this.byNode.containsKey(seed)) continue;
            if (this.hasTransferFace(BlockPos.of(seed))) this.buildNetwork(seed);
        }
    }

    /** 从种子做弱连通 BFS：出边（本方取出 → 邻格存入）与入边（本方存入 → 邻格取出）都算连接。 */
    private void buildNetwork(long seed) {
        Long2ObjectLinkedOpenHashMap<TransferNode> nodes = new Long2ObjectLinkedOpenHashMap<>();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet queued = new LongOpenHashSet();
        queue.enqueue(seed);
        queued.add(seed);

        while (!queue.isEmpty()) {
            long packed = queue.dequeueLong();
            BlockPos pos = BlockPos.of(packed);
            TransferNode node = this.readNode(pos);
            if (node == null) continue;
            nodes.put(packed, node);

            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = pos.relative(dir);
                long neighborPacked = neighborPos.asLong();
                if (queued.contains(neighborPacked)) continue;
                TransferNode neighbor = this.readNode(neighborPos);
                if (neighbor == null) continue;
                boolean forward = node.isExtract(dir) && neighbor.isInsert(dir.getOpposite());
                boolean backward = node.isInsert(dir) && neighbor.isExtract(dir.getOpposite());
                if (!forward && !backward) continue;
                queued.add(neighborPacked);
                queue.enqueue(neighborPacked);
            }
        }

        TransferNetwork network = new TransferNetwork(nodes);
        this.registerNetwork(network);
        this.recomputeRecords(network);
    }

    private void registerNetwork(TransferNetwork network) {
        this.networks.add(network);
        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            this.byNode.put(packed, network);
            long chunkPos = ChunkPos.asLong(BlockPos.getX(packed) >> 4, BlockPos.getZ(packed) >> 4);
            if (network.chunks.add(chunkPos)) {
                // 同一区块可能有大量节点，反向索引只登记一次网络对象。
                this.byChunk.computeIfAbsent(chunkPos, key -> new ObjectOpenHashSet<>()).add(network);
            }
        }
    }

    private void invalidate(TransferNetwork network, LongOpenHashSet rebuildSeeds, long excludedChunk) {
        if (!network.valid) return;
        network.valid = false;
        this.networks.remove(network);
        this.dirtyRecords.remove(network);

        for (LongIterator it = network.chunks.iterator(); it.hasNext(); ) {
            long chunkPos = it.nextLong();
            ObjectOpenHashSet<TransferNetwork> chunkNetworks = this.byChunk.get(chunkPos);
            if (chunkNetworks != null) {
                chunkNetworks.remove(network);
                if (chunkNetworks.isEmpty()) this.byChunk.remove(chunkPos);
            }
        }

        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            // 身份检查避免误删已经被其他重建过程重新归属的新网络映射。
            if (this.byNode.get(packed) == network) {
                this.byNode.remove(packed);
            }
            // 正在卸载的区块不再排进重建种子：它的方块已经读不到了，等它重新加载时再由
            // chunkLoaded 重新播种（否则会把已卸载的位置当成节点重新建进网络里）。
            long nodeChunk = ChunkPos.asLong(BlockPos.getX(packed) >> 4, BlockPos.getZ(packed) >> 4);
            if (nodeChunk != excludedChunk) {
                rebuildSeeds.add(packed);
            }
        }

        network.sources.clear();
        network.extracts.clear();
        network.servicePositions.clear();
        network.serviceFaces.clear();
    }

    // ==================== 货源记录 ====================

    /**
     * 维护一张网的端点状态（顶点图：有效「取出」集合 + 需要搬运的「存入」面清单）。
     *
     * <p>与前置流体管网同一套路子：这里<b>不</b>预先给每个「存入」算出货源（那会随取出数量
     * 线性膨胀），只维护「哪些取出有效」这个集合（面朝容器确实存在），按差集增删；
     * 真正用到的候选货源由 {@link #sourcesOf} 从该「存入」按需 BFS 得到并缓存，集合一变缓存整体作废。
     * 于是没被搬运过的节点不占缓存，端点的出现 / 消失也只做一次集合求差。</p>
     */
    private void recomputeRecords(TransferNetwork network) {
        ObjectOpenHashSet<TransferSource> current = this.collectValidExtracts(network);
        if (!current.equals(network.extracts)) {
            // 有效取出变了：按需缓存里的候选货源不再可信，整体作废（含「算出为空」的负缓存）。
            network.extracts.clear();
            network.extracts.addAll(current);
            network.sources.clear();
        }
        this.rebuildServices(network);
    }

    /** 该网当前全部「有效取出」：面朝容器确实存在的取出面。 */
    private ObjectOpenHashSet<TransferSource> collectValidExtracts(TransferNetwork network) {
        ObjectOpenHashSet<TransferSource> current = new ObjectOpenHashSet<>();
        for (Long2ObjectMap.Entry<TransferNode> entry : network.nodes.long2ObjectEntrySet()) {
            long packed = entry.getLongKey();
            TransferNode node = entry.getValue();
            BlockPos pos = BlockPos.of(packed);
            for (Direction dir : Direction.values()) {
                if (!node.isExtract(dir)) continue;
                if (this.containerAt(pos, dir) == null) continue;
                current.add(new TransferSource(packed, dir));
            }
        }
        return current;
    }

    /**
     * 按需算出某个「存入」节点能到达的全部有效「取出」（顶点图里该节点的出边），先近后远。
     *
     * <p>从该节点沿<b>反向边</b> BFS：我方「取出」面指向上一格、上一格用「存入」面接住我方——
     * 正是正向传播「本方存入面 → 正邻格的取出面」的逆边。BFS 先到者跳数更少，因此结果天然按距离排序。</p>
     *
     * <p>结果缓存在 {@code network.sources} 里（含「算出为空」的负缓存），
     * 拓扑 / 端点变化时由 {@link #recomputeRecords} 整体作废。</p>
     */
    private ObjectArrayList<TransferSource> sourcesOf(TransferNetwork network, long packed) {
        ObjectArrayList<TransferSource> found = new ObjectArrayList<>();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet visited = new LongOpenHashSet();
        queue.enqueue(packed);
        visited.add(packed);

        while (!queue.isEmpty()) {
            long currentPacked = queue.dequeueLong();
            TransferNode node = network.nodes.get(currentPacked);
            if (node == null) continue;
            Direction face = this.firstValidExtract(network, currentPacked, node);
            if (face != null) found.add(new TransferSource(currentPacked, face));

            BlockPos pos = BlockPos.of(currentPacked);
            for (Direction dir : Direction.values()) {
                if (!node.isExtract(dir)) continue;
                long upstreamPacked = pos.relative(dir).asLong();
                TransferNode upstream = network.nodes.get(upstreamPacked);
                if (upstream == null || !upstream.isInsert(dir.getOpposite())) continue;
                if (!visited.add(upstreamPacked)) continue;
                queue.enqueue(upstreamPacked);
            }
        }
        return found;
    }

    /** 该节点的第一个「有效取出」面（已记在 {@link TransferNetwork#extracts} 里）；没有则返回 {@code null}。 */
    @Nullable
    private Direction firstValidExtract(TransferNetwork network, long packed, TransferNode node) {
        for (Direction dir : Direction.values()) {
            if (!node.isExtract(dir)) continue;
            if (network.extracts.contains(new TransferSource(packed, dir))) return dir;
        }
        return null;
    }

    /** 重算需要搬运的「存入」面清单：面朝容器确实存在的「存入」面各一条。 */
    private void rebuildServices(TransferNetwork network) {
        network.servicePositions.clear();
        network.serviceFaces.clear();
        for (Long2ObjectMap.Entry<TransferNode> entry : network.nodes.long2ObjectEntrySet()) {
            TransferNode node = entry.getValue();
            if (!node.hasInsert()) continue;
            BlockPos pos = BlockPos.of(entry.getLongKey());
            for (Direction dir : Direction.values()) {
                if (!node.isInsert(dir)) continue;
                if (this.containerAt(pos, dir) == null) continue;
                network.servicePositions.add(entry.getLongKey());
                network.serviceFaces.add((byte) dir.ordinal());
            }
        }
    }

    // ==================== 每 tick 搬运 ====================

    void tick() {
        this.runUpdates();
        long now = this.level.getGameTime();
        this.cooldowns.keySet().removeIf(packed -> !this.byNode.containsKey(packed));

        for (TransferNetwork network : this.networks) {
            if (!network.valid) continue;
            for (int i = 0; i < network.servicePositions.size(); i++) {
                long packed = network.servicePositions.getLong(i);
                Direction face = Direction.values()[network.serviceFaces.getByte(i)];
                // 7 游戏刻冷却只记在「存入」面上；「取出」没有冷却，可被多个「存入」同时取用。
                if (this.onCooldown(packed, face, now)) continue;

                // 候选货源按需缓存：没算过就现算一次（空结果也缓存成负缓存），端点 / 拓扑变化时整体作废。
                ObjectArrayList<TransferSource> sources = network.sources.get(packed);
                if (sources == null) {
                    sources = this.sourcesOf(network, packed);
                    network.sources.put(packed, sources);
                }
                if (sources.isEmpty()) continue;
                BlockPos pos = BlockPos.of(packed);
                IItemHandler target = this.containerAt(pos, face);
                if (target == null) continue;
                // service 面本身就是一个「存入」面：物流量与过滤都按该面现场读。
                BlockInlays data = BlockInlayManager.get(this.level, pos);
                int limit = Math.clamp(data.getThroughput(face), 1, AnvilCraftDogePlus.CONFIG.pipeThroughputMax);
                ItemStack filter = data.getFilter(face);

                // 按记录顺序（先近后远）逐个尝试：最近的取出取不到货就顺次改用次近的，
                // 任一取出成功供货即结束本次搬运，该「存入」面随后进入冷却。
                for (int s = 0; s < sources.size(); s++) {
                    TransferSource source = sources.get(s);
                    // 货源容器是 «源节点朝 sourceFace 一格» 那个方块，不是再往外一格。
                    IItemHandler sourceHandler = this.handlerAt(source.containerPos(), source.containerSide());
                    if (sourceHandler == null) continue;
                    if (!moveOneStack(sourceHandler, target, limit, filter)) continue;
                    this.markTransfer(packed, face, now);
                    break;
                }
            }
        }
    }

    private boolean onCooldown(long packed, Direction face, long now) {
        long[] ready = this.cooldowns.get(packed);
        return ready != null && now < ready[face.ordinal()];
    }

    private void markTransfer(long packed, Direction face, long now) {
        long[] ready = this.cooldowns.get(packed);
        if (ready == null) {
            ready = new long[Direction.values().length];
            this.cooldowns.put(packed, ready);
        }
        ready[face.ordinal()] = now + TRANSFER_COOLDOWN;
    }

    /**
     * 从源容器搬出至多 {@code limit} 个物品，塞进目标容器；{@code filter} 非空时只搬能通过它的物品。
     *
     * <p>先模拟取出、再模拟插入，只有目标确实能收下的部分才真正取走，避免物品被取出后塞不回去。</p>
     *
     * <p>{@code limit} 是该「存入」面的物流量；{@code filter} 是该面的过滤物品，判定交给
     * {@link FilterItem#filter}，因此普通物品、内嵌 {@code FilterContent} 的前置过滤器物品都成立。
     * 一叠物品同质，按整叠判定即可。</p>
     *
     * @return 是否真的搬动了物品
     */
    private static boolean moveOneStack(IItemHandler source, IItemHandler target, int limit, ItemStack filter) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack available = source.extractItem(slot, limit, true);
            if (available.isEmpty()) continue;
            if (!filter.isEmpty() && !FilterItem.filter(filter, available)) continue;
            ItemStack leftover = ItemHandlerHelper.insertItem(target, available, true);
            int accepted = available.getCount() - leftover.getCount();
            if (accepted <= 0) continue;
            ItemStack taken = source.extractItem(slot, accepted, false);
            if (taken.isEmpty()) continue;
            ItemHandlerHelper.insertItem(target, taken, false);
            return true;
        }
        return false;
    }

    /**
     * 该面朝向的容器；不存在（或所在区块未加载）时返回 {@code null}。
     *
     * <p>查询的是容器朝向本方块的那一面（{@code face.getOpposite()}），与溜槽的取法一致。</p>
     */
    @Nullable
    private IItemHandler containerAt(BlockPos pos, Direction face) {
        return this.handlerAt(pos.relative(face), face.getOpposite());
    }

    /**
     * 指定容器位置与查询侧的能力；所在区块未加载时返回 {@code null}。
     *
     * <p>方块容器优先；方块不是容器时回退到该格的实体容器（箱子 / 漏斗矿车、箱船、Doge 节点等）。
     * {@link ItemHandlerUtil#getSourceItemHandler} 对实体只在非空时才认可，而空容器正是「存入」的目标，
     * 故这里再做一次不看空满的实体查找。</p>
     */
    @Nullable
    private IItemHandler handlerAt(BlockPos containerPos, Direction side) {
        // 未加载时不强制加载区块：等它加载后由 chunkLoaded 重算记录。
        if (!this.level.hasChunkAt(containerPos)) return null;
        IItemHandler handler = ItemHandlerUtil.getSourceItemHandler(containerPos, side, this.level);
        if (handler != null) return handler;
        return this.entityContainerAt(containerPos, side);
    }

    /**
     * 与 {@code pos} 关联的实体容器的物品处理器；没有则返回 {@code null}。
     *
     * <p>只认明确声明支持自动化的实体（{@link Capabilities.ItemHandler#ENTITY_AUTOMATION}），
     * 避免碰到玩家或生物的背包；且不要求容器非空——空容器同样算端点，否则空的「存入」目标
     * （例如空箱子矿车）根本登记不上，而空的「取出」源也要能登记、等它被装进东西后再取。</p>
     *
     * <p>判定盒从方块底面向上多探 0.1 格：矿车停在轨道上、Doge 节点贴在方块顶面，包围盒都跨在
     * 方块上边界附近，这样容器两侧（{@code pos} 与其上一层）的实体都能命中。</p>
     */
    @Nullable
    private IItemHandler entityContainerAt(BlockPos pos, Direction side) {
        AABB box = new AABB(pos).setMaxY(pos.getY() + 1.1);
        for (Entity entity : this.level.getEntitiesOfClass(Entity.class, box, Entity::isAlive)) {
            IItemHandler handler = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side);
            if (handler != null) return handler;
        }
        return null;
    }

    // ==================== 节点读取 ====================

    /** 该面是否算「存入」：管道载体编程的搬运角色，或镶嵌材料自带的方向性属性。 */
    private static boolean isInsertFace(BlockInlays data, Direction dir) {
        return data.getFace(dir) == FaceMode.INSERT;
    }

    /** 该面是否算「取出」。 */
    private static boolean isExtractFace(BlockInlays data, Direction dir) {
        return data.getFace(dir) == FaceMode.EXTRACT;
    }

    /** 读取该位置的传输节点；六个面都没有传输属性时返回 {@code null}。 */
    @Nullable
    private TransferNode readNode(BlockPos pos) {
        BlockInlays data = BlockInlayManager.get(this.level, pos);
        int insertMask = 0;
        int extractMask = 0;
        for (Direction dir : Direction.values()) {
            if (isInsertFace(data, dir)) insertMask |= 1 << dir.ordinal();
            if (isExtractFace(data, dir)) extractMask |= 1 << dir.ordinal();
        }
        if (insertMask == 0 && extractMask == 0) return null;
        return new TransferNode(pos.asLong(), insertMask, extractMask);
    }

    private boolean hasTransferFace(BlockPos pos) {
        BlockInlays data = BlockInlayManager.get(this.level, pos);
        for (Direction dir : Direction.values()) {
            if (isInsertFace(data, dir) || isExtractFace(data, dir)) {
                return true;
            }
        }
        return false;
    }

    // ==================== 区块管理 ====================

    void chunkLoaded(ChunkAccess chunk) {
        // 只扫描区块自己的方块：避免为了找网络而同步加载相邻区块。
        chunk.findBlocks(
                state -> !state.isAir(),
                (pos, state) -> {
                    if (this.hasTransferFace(pos)) this.topologySeeds.add(pos.asLong());
                });
        // 端点的容器可能就在这个区块里，紧邻的网络需要重算记录。
        this.markNetworksAroundChunkDirty(chunk.getPos().toLong());
    }

    void chunkUnloaded(long chunkPos) {
        removeChunkPositions(this.topologySeeds, chunkPos);
        ObjectOpenHashSet<TransferNetwork> affected = this.byChunk.remove(chunkPos);
        if (affected != null) {
            LongOpenHashSet rebuildSeeds = new LongOpenHashSet();
            for (TransferNetwork network : affected) {
                // 跨区块网络必须整体失效；但卸载区块里的节点不再作为重建种子。
                this.invalidate(network, rebuildSeeds, chunkPos);
            }
            this.topologySeeds.addAll(rebuildSeeds);
        }
        // 容器随区块卸载后「有效取出」可能不再有效，相邻网络的记录要重算。
        this.markNetworksAroundChunkDirty(chunkPos);
        this.cooldowns.keySet().removeIf(packed -> !this.byNode.containsKey(packed));
    }

    private void markNetworksAroundChunkDirty(long chunkPos) {
        int centerX = ChunkPos.getX(chunkPos);
        int centerZ = ChunkPos.getZ(chunkPos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ObjectOpenHashSet<TransferNetwork> chunkNetworks =
                        this.byChunk.get(ChunkPos.asLong(centerX + dx, centerZ + dz));
                if (chunkNetworks == null) continue;
                this.dirtyRecords.addAll(chunkNetworks);
            }
        }
    }

    private static void removeChunkPositions(LongOpenHashSet positions, long chunkPos) {
        for (LongIterator it = positions.iterator(); it.hasNext(); ) {
            long packed = it.nextLong();
            if (ChunkPos.asLong(BlockPos.getX(packed) >> 4, BlockPos.getZ(packed) >> 4) == chunkPos) {
                it.remove();
            }
        }
    }
}
