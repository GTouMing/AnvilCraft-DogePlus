package dev.anvilcraft.gtouming.doge_plus.transfer;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.LogisticsCarrierBlock;
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
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * 单个维度的物品传输网状态（与 {@code logic/LevelNetworks} 同构）。
 *
 * <p>节点 = 至少有一个面带「存入」或「取出」属性的方块；连接 = 「本方某面 ↔ 正邻格的互补面」
 * （存入的对面是取出），方向是「本方存入面 → 正邻格的取出面」。一张弱连通块即一张传输网。</p>
 *
 * <p>「存入 / 取出」是面属性 {@link FaceMode} 的两个取值：物流载体由轮盘编程写入，镶嵌载体由材料
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
 * <p>7 游戏刻冷却由<b>整张网共用</b>：一轮里所有「存入」面同时响应，一轮真的搬动了物品才进入冷却，
 * 没搬动则下一 tick 立即重试。同一轮里多个「存入」面从同一个「取出」面取货时，按各面<b>物流量为权重</b>
 * 分配该取出面取出的总量（总量 = min(该取出面物品数, 各竞争面物流量之和)，第 i 面得 总量·w_i/Σw）；
 * 均分后份额为 0 的面顺延尝试它的下一个取出面。记录与冷却都只在内存里，区块重载时会随拓扑重建一并重算。</p>
 *
 * <p><b>链路过滤</b>：链路里朝相邻载体、本身不收货的中间「存入」面，其过滤同样是闸门——物品要从源容器
 * 端到端走到目标容器，必须依次通过沿途<b>每一道「存入」面</b>的过滤（逐级 AND），而不只是目标面那一道。
 * 沿途过滤在 {@link #sourcesOf} 里随反向 BFS 累积进每条 {@link SourceRoute}；因此<i>设置过滤</i>也会让按需
 * 货源缓存过期，需要重建网络（见 {@code SetLogisticsFilterPacket}）。</p>
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

    /**
     * 远程门信道 → 成员位置（仅物流载体的远程面）。
     *
     * <p>远程门不存储物品，只提供「远程查找取出」：某个「存入」节点能从同信道的远端「取出」面取货。
     * 查找时由 {@link #sourcesOf} 沿反向边 BFS、在遇到远程成员时跳到同信道其它成员。</p>
     */
    private final Long2ObjectOpenHashMap<LongOpenHashSet> channelMembers = new Long2ObjectOpenHashMap<>();

    /** 位置 → 其远程信道键（登记时记下，失效时据此精确移除）。 */
    private final Long2ObjectOpenHashMap<long[]> nodeChannels = new Long2ObjectOpenHashMap<>();

    private static final long[] NO_CHANNELS = new long[0];

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
            TransferNode node = network.nodes.get(packed);
            if (node != null) this.addRemoteMembership(packed, node.remoteChannels());
        }
    }

    /** 把该位置登记为其远程信道的成员。 */
    private void addRemoteMembership(long packed, long[] channels) {
        if (channels.length == 0) return;
        this.nodeChannels.put(packed, channels);
        for (long channel : channels) {
            this.channelMembers.computeIfAbsent(channel, key -> new LongOpenHashSet()).add(packed);
        }
    }

    /** 把该位置从其所有远程信道中移除。 */
    private void removeRemoteMembership(long packed) {
        long[] channels = this.nodeChannels.remove(packed);
        if (channels == null) return;
        for (long channel : channels) {
            LongOpenHashSet members = this.channelMembers.get(channel);
            if (members == null) continue;
            members.remove(packed);
            if (members.isEmpty()) this.channelMembers.remove(channel);
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
            this.removeRemoteMembership(packed);
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
     * 按需算出某个「存入」节点能到达的全部有效「取出」路线（顶点图里该节点的出边），先近后远。
     *
     * <p>从该节点沿<b>反向边</b> BFS：我方「取出」面指向上一格、上一格用「存入」面接住我方——
     * 正是正向传播「本方存入面 → 正邻格的取出面」的逆边。BFS 先到者跳数更少，因此结果天然按距离排序。</p>
     *
     * <p>BFS 同时把沿途经过的「存入」面的过滤累积进 {@link SourceRoute#chainFilters()}（不含起点服务面自身），
     * 让搬运时能对整条链路的每一道「存入」面逐级校验过滤。</p>
     *
     * <p>结果缓存在 {@code network.sources} 里（含「算出为空」的负缓存），
     * 拓扑 / 端点 / 过滤变化时由 {@link #recomputeRecords} 或重建整体作废。</p>
     */
    private ObjectArrayList<SourceRoute> sourcesOf(TransferNetwork network, long packed) {
        ObjectArrayList<SourceRoute> found = new ObjectArrayList<>();
        ArrayDeque<RouteStep> queue = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet();
        queue.addLast(new RouteStep(packed, new ObjectArrayList<>()));
        visited.add(packed);

        while (!queue.isEmpty()) {
            RouteStep step = queue.pollFirst();
            long currentPacked = step.packed();
            TransferNode node = this.nodeAt(network, currentPacked);
            if (node == null) continue;
            Direction face = this.firstValidExtract(currentPacked, node);
            if (face != null) {
                found.add(new SourceRoute(new TransferSource(currentPacked, face), step.chain()));
            }

            BlockPos pos = BlockPos.of(currentPacked);
            for (Direction dir : Direction.values()) {
                if (!node.isExtract(dir)) continue;
                long upstreamPacked = pos.relative(dir).asLong();
                TransferNode upstream = network.nodes.get(upstreamPacked);
                if (upstream == null || !upstream.isInsert(dir.getOpposite())) continue;
                if (!visited.add(upstreamPacked)) continue;
                // 到达 upstream 经过的是它朝我方的「存入」面：把该面过滤并入链路（逐级 AND）。
                ObjectArrayList<ItemStack> chain = new ObjectArrayList<>(step.chain());
                ItemStack filter = this.faceFilter(upstreamPacked, dir.getOpposite());
                if (!filter.isEmpty()) chain.add(filter);
                queue.addLast(new RouteStep(upstreamPacked, chain));
            }
            // 远程跳：本方有远程面时，跳到同信道的其它成员（远端「取出」货源由此可达）。
            // 跨信道不经过任何「存入」面，链路过滤不变。
            for (long channel : node.remoteChannels()) {
                LongOpenHashSet members = this.channelMembers.get(channel);
                if (members == null) continue;
                for (LongIterator mit = members.iterator(); mit.hasNext(); ) {
                    long memberPacked = mit.nextLong();
                    if (!visited.add(memberPacked)) continue;
                    queue.addLast(new RouteStep(memberPacked, step.chain()));
                }
            }
        }
        return found;
    }

    /** 该位置某面的过滤物品（未设置时为空栈）。 */
    private ItemStack faceFilter(long packed, Direction dir) {
        BlockPos pos = BlockPos.of(packed);
        if (!this.level.hasChunkAt(pos)) return ItemStack.EMPTY;
        return BlockInlayManager.get(this.level, pos).getFilter(dir);
    }

    /** 反向 BFS 的一步：当前节点 + 从服务面走到这里沿途累积的「存入」面过滤。 */
    private record RouteStep(long packed, ObjectArrayList<ItemStack> chain) {
    }

    /** 取网络内该位置的节点；远程成员可能在别的网络里，回退到现读该位置。 */
    @Nullable
    private TransferNode nodeAt(TransferNetwork network, long packed) {
        TransferNode node = network.nodes.get(packed);
        if (node != null) return node;
        return this.readNode(BlockPos.of(packed));
    }

    /** 该节点的第一个「有效取出」面（面朝容器确实存在）；没有则返回 {@code null}。 */
    @Nullable
    private Direction firstValidExtract(long packed, TransferNode node) {
        BlockPos pos = BlockPos.of(packed);
        for (Direction dir : Direction.values()) {
            if (!node.isExtract(dir)) continue;
            if (this.containerAt(pos, dir) != null) return dir;
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

        // 取快照再遍历：搬运的副作用（物品入容器触发的方块更新）会重入 runUpdates 改写
        // this.networks，直接遍历会被就地增删从而作废 fastutil 迭代器（NPE）。
        ObjectOpenHashSet<TransferNetwork> snapshot = new ObjectOpenHashSet<>(this.networks);
        for (TransferNetwork network : snapshot) {
            if (!network.valid) continue;
            // 网络级共享冷却：整张网所有「存入」面共用一个冷却，未到就整网不动。
            if (now < network.readyAt) continue;
            // 一轮真的搬动了物品才重置冷却；没搬动则不冷却、下一 tick 立即重试（与「成功才冷却」一致）。
            if (this.runNetworkRound(network)) {
                network.readyAt = now + TRANSFER_COOLDOWN;
            }
        }
    }

    /**
     * 一张网的一轮搬运：先为每个「存入」面认领一个取出源（先近后远），再按「物流量权重」把每个
     * 取出源能给的量分给竞争该源的全部「存入」面。
     *
     * <p>分配口径：设某源共有 {@code k} 个竞争面、物流量权重为 {@code w_1..w_k}，则该源取出的总量为
     * {@code pool = min(该源物品总数, Σw)}，第 i 个面分得 {@code pool * w_i / Σw}（除不尽的余数按顺序
     * 补齐、不超各自物流量）。货源被抢空时各面按权重缩水；某面份额为 0 就顺延下一个取出面
     * （不动点迭代，直到每个源的每个竞争面都能拿到 ≥ 1）。</p>
     *
     * @return 本轮是否真的搬动了物品（决定是否进入网络冷却）
     */
    private boolean runNetworkRound(TransferNetwork network) {
        // 计数封顶：够大即可（≥ 所有竞争面物流量之和），把容器扫描成本封住。
        int cap = Math.max(1, network.servicePositions.size()) * AnvilCraftDogePlus.CONFIG.logisticsThroughputMax;

        // ---- 收集本轮所有「存入」面及其（先近后远的）货源 ----
        List<RoundFace> faces = new ArrayList<>();
        for (int i = 0; i < network.servicePositions.size(); i++) {
            long packed = network.servicePositions.getLong(i);
            Direction face = Direction.values()[network.serviceFaces.getByte(i)];
            BlockPos pos = BlockPos.of(packed);
            IItemHandler target = this.containerAt(pos, face);
            if (target == null) continue;

            // 候选货源按需缓存：没算过就现算一次（空结果也缓存成负缓存），端点 / 拓扑变化时整体作废。
            // 带远程面的节点例外：它的货源可能落在其它网络的同信道成员上，缓存无法可靠失效，故每 tick 现算。
            TransferNode serviceNode = network.nodes.get(packed);
            boolean remote = serviceNode != null && serviceNode.isRemote();
            ObjectArrayList<SourceRoute> sources;
            if (remote) {
                sources = this.sourcesOf(network, packed);
            } else {
                sources = network.sources.get(packed);
                if (sources == null) {
                    sources = this.sourcesOf(network, packed);
                    network.sources.put(packed, sources);
                }
            }
            if (sources.isEmpty()) continue;

            // service 面本身就是一个「存入」面：物流量与过滤都按该面现场读。
            BlockInlays data = BlockInlayManager.get(this.level, pos);
            int limit = Math.clamp(data.getThroughput(face), 1, AnvilCraftDogePlus.CONFIG.logisticsThroughputMax);
            faces.add(new RoundFace(target, limit, data.getFilter(face), sources));
        }
        if (faces.isEmpty()) return false;

        // 本轮内源容器 / 物品总数的快照（搬运前同轮不变）。
        Object2ObjectOpenHashMap<TransferSource, IItemHandler> handlers = new Object2ObjectOpenHashMap<>();
        Object2ObjectOpenHashMap<TransferSource, Integer> totals = new Object2ObjectOpenHashMap<>();

        // ---- 认领（不动点）：份额为 0 的面顺延，直到每个源的每个竞争面都拿得到 ≥ 1 ----
        boolean progress = true;
        while (progress) {
            progress = false;
            Object2ObjectOpenHashMap<TransferSource, List<RoundFace>> groups = new Object2ObjectOpenHashMap<>();
            for (RoundFace rf : faces) {
                // 跳过没有可搬运物品（并沿链路过滤逐级放行）的取出源。
                while (rf.pointer < rf.sources.size()) {
                    SourceRoute route = rf.sources.get(rf.pointer);
                    IItemHandler handler = this.sourceHandler(handlers, route.source());
                    if (handler != null && hasMatching(handler, rf.filter, route.chainFilters())) break;
                    rf.pointer++;
                    progress = true;
                }
                if (rf.pointer >= rf.sources.size()) continue;
                groups.computeIfAbsent(rf.sources.get(rf.pointer).source(), key -> new ArrayList<>()).add(rf);
            }
            for (Object2ObjectMap.Entry<TransferSource, List<RoundFace>> entry : groups.object2ObjectEntrySet()) {
                List<RoundFace> group = entry.getValue();
                IItemHandler handler = this.sourceHandler(handlers, entry.getKey());
                long sum = 0;
                for (RoundFace rf : group) sum += rf.limit;
                int pool = (int) Math.min(this.sourceTotal(totals, entry.getKey(), handler, cap), sum);
                // 任一竞争面份额 < 1（pool * w_i < Σw）就让它顺延下一个取出源。
                for (RoundFace rf : group) {
                    if ((long) pool * rf.limit < sum) {
                        rf.pointer++;
                        progress = true;
                    }
                }
            }
        }

        // ---- 分配并搬运：同源按物流量权重分「该源能给的量」 ----
        Object2ObjectOpenHashMap<TransferSource, List<RoundFace>> finalGroups = new Object2ObjectOpenHashMap<>();
        for (RoundFace rf : faces) {
            if (rf.pointer < rf.sources.size()) {
                finalGroups.computeIfAbsent(rf.sources.get(rf.pointer).source(), key -> new ArrayList<>()).add(rf);
            }
        }

        boolean moved = false;
        for (Object2ObjectMap.Entry<TransferSource, List<RoundFace>> entry : finalGroups.object2ObjectEntrySet()) {
            IItemHandler handler = this.sourceHandler(handlers, entry.getKey());
            if (handler == null) continue;
            List<RoundFace> group = entry.getValue();
            long sum = 0;
            for (RoundFace rf : group) sum += rf.limit;
            int pool = (int) Math.min(this.sourceTotal(totals, entry.getKey(), handler, cap), sum);

            // 按权重 pool * w_i / Σw 取整；余数按顺序补齐（不超各自物流量），尽量不剩。
            int[] shares = new int[group.size()];
            int assigned = 0;
            for (int i = 0; i < group.size(); i++) {
                shares[i] = (int) ((long) pool * group.get(i).limit / sum);
                assigned += shares[i];
            }
            int remainder = pool - assigned;
            for (int i = 0; i < group.size() && remainder > 0; i++) {
                int add = Math.min(group.get(i).limit - shares[i], remainder);
                shares[i] += add;
                remainder -= add;
            }

            for (int i = 0; i < group.size(); i++) {
                if (shares[i] <= 0) continue;
                RoundFace rf = group.get(i);
                // 服务面自身的过滤 + 沿途每一道「存入」面的过滤，逐级校验。
                ObjectArrayList<ItemStack> chain = rf.sources.get(rf.pointer).chainFilters();
                if (moveOneStack(handler, rf.target, shares[i], rf.filter, chain)) moved = true;
            }
        }
        return moved;
    }

    /** 取出源的物品处理器（本轮内缓存非空结果；源不存在则返回 {@code null}）。 */
    @Nullable
    private IItemHandler sourceHandler(
            Object2ObjectOpenHashMap<TransferSource, IItemHandler> cache, TransferSource source) {
        IItemHandler cached = cache.get(source);
        if (cached != null) return cached;
        IItemHandler handler = this.handlerAt(source.containerPos(), source.containerSide());
        if (handler != null) cache.put(source, handler);
        return handler;
    }

    /** 取出源的物品总数（本轮内缓存，封顶 {@code cap}）。 */
    private int sourceTotal(
            Object2ObjectOpenHashMap<TransferSource, Integer> cache, TransferSource source,
            @Nullable IItemHandler handler, int cap) {
        if (handler == null) return 0;
        Integer cached = cache.get(source);
        if (cached != null) return cached;
        int total = countItems(handler, cap);
        cache.put(source, total);
        return total;
    }

    /** 源容器物品总数，封顶 {@code cap}。 */
    private static int countItems(IItemHandler handler, int cap) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            total += stack.getCount();
            if (total >= cap) return cap;
        }
        return total;
    }

    /** 源容器里是否存在能通过「服务面过滤 + 链路过滤」的物品。 */
    private static boolean hasMatching(IItemHandler handler, ItemStack serviceFilter, List<ItemStack> chain) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            if (passesAll(stack, serviceFilter, chain)) return true;
        }
        return false;
    }

    /** 物品是否通过服务面过滤与沿途每一道「存入」面的过滤（逐级 AND）。 */
    private static boolean passesAll(ItemStack item, ItemStack serviceFilter, List<ItemStack> chain) {
        if (!serviceFilter.isEmpty() && !FilterItem.filter(serviceFilter, item)) return false;
        for (int i = 0; i < chain.size(); i++) {
            ItemStack filter = chain.get(i);
            if (!filter.isEmpty() && !FilterItem.filter(filter, item)) return false;
        }
        return true;
    }

    /** 本轮里一个参与搬运的「存入」面：目标容器、物流量（权重）、过滤与货源游标。 */
    private static final class RoundFace {
        final IItemHandler target;
        final int limit;
        final ItemStack filter;
        final ObjectArrayList<SourceRoute> sources;
        int pointer;

        RoundFace(IItemHandler target, int limit, ItemStack filter, ObjectArrayList<SourceRoute> sources) {
            this.target = target;
            this.limit = limit;
            this.filter = filter;
            this.sources = sources;
        }
    }

    /**
     * 从源容器搬出至多 {@code limit} 个物品，塞进目标容器；物品需通过 {@code serviceFilter} 与该路线的
     * {@code chainFilters}（沿途每一道「存入」面的过滤，逐级 AND）才放行。
     *
     * <p>先模拟取出、再模拟插入，只有目标确实能收下的部分才真正取走，避免物品被取出后塞不回去。</p>
     *
     * <p>插入走 {@link ItemHandlerUtil#insertItem}：对前置的 {@code PollableFilteredItemStackHandler} 用
     * 非轮询入口，避免搬运触发轮询副作用。个别容器（如储物抽屉）的模拟与实际容量可能不一致，故实际
     * 塞不下的余量回灌源容器，不静默丢弃。</p>
     *
     * <p>{@code limit} 是该「存入」面的物流量；过滤判定交给 {@link FilterItem#filter}，因此普通物品、
     * 内嵌 {@code FilterContent} 的前置过滤器物品都成立。一叠物品同质，按整叠判定即可。</p>
     *
     * @return 是否真的搬动了物品
     */
    private static boolean moveOneStack(
            IItemHandler source, IItemHandler target, int limit,
            ItemStack serviceFilter, List<ItemStack> chainFilters) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack available = source.extractItem(slot, limit, true);
            if (available.isEmpty()) continue;
            if (!passesAll(available, serviceFilter, chainFilters)) continue;
            ItemStack leftover = ItemHandlerUtil.insertItem(target, available, true);
            int accepted = available.getCount() - leftover.getCount();
            if (accepted <= 0) continue;
            ItemStack taken = source.extractItem(slot, accepted, false);
            if (taken.isEmpty()) continue;
            // 模拟与实际可能不一致（抽屉类容器尤其如此）：实际塞不下的余量回灌源容器，不静默丢弃。
            ItemStack actualLeftover = ItemHandlerUtil.insertItem(target, taken, false);
            if (!actualLeftover.isEmpty()) {
                source.insertItem(slot, actualLeftover, false);
            }
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

    /** 该面是否算「存入」：物流载体编程的搬运角色，或镶嵌材料自带的方向性属性。 */
    private static boolean isInsertFace(BlockInlays data, Direction dir) {
        return data.getFace(dir) == FaceMode.INSERT;
    }

    /** 该面是否算「取出」。 */
    private static boolean isExtractFace(BlockInlays data, Direction dir) {
        return data.getFace(dir) == FaceMode.EXTRACT;
    }

    /** 读取该位置的传输节点；六个面都没有传输属性（含远程）时返回 {@code null}。 */
    @Nullable
    private TransferNode readNode(BlockPos pos) {
        // 未加载时不强制加载区块：等它加载后由 chunkLoaded 重新播种。
        if (!this.level.hasChunkAt(pos)) return null;
        BlockInlays data = BlockInlayManager.get(this.level, pos);
        // 记录必须属于当前方块：方块已被移除、记录还在时不能把空气当成幽灵传输节点。
        if (!BlockInlayManager.isConsistent(this.level.getBlockState(pos).getBlock(), data)) return null;
        int insertMask = 0;
        int extractMask = 0;
        for (Direction dir : Direction.values()) {
            if (isInsertFace(data, dir)) insertMask |= 1 << dir.ordinal();
            if (isExtractFace(data, dir)) extractMask |= 1 << dir.ordinal();
        }
        long[] remoteChannels = this.remoteChannels(pos, data);
        if (insertMask == 0 && extractMask == 0 && remoteChannels.length == 0) return null;
        return new TransferNode(pos.asLong(), insertMask, extractMask, remoteChannels);
    }

    /** 该位置的物品远程信道（仅物流载体的远程面参与；去重），无则返回 {@link #NO_CHANNELS}。 */
    private long[] remoteChannels(BlockPos pos, BlockInlays data) {
        // 未加载时不强制加载区块：等它加载后由 chunkLoaded 重新播种。
        if (!this.level.hasChunkAt(pos)) return NO_CHANNELS;
        if (!(this.level.getBlockState(pos).getBlock() instanceof LogisticsCarrierBlock)) return NO_CHANNELS;
        long[] channels = new long[Direction.values().length];
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (data.getFace(dir) != FaceMode.REMOTE) continue;
            long channel = data.getChannel(dir).key();
            boolean seen = false;
            for (int i = 0; i < count; i++) {
                if (channels[i] == channel) {
                    seen = true;
                    break;
                }
            }
            if (!seen) channels[count++] = channel;
        }
        return count == 0 ? NO_CHANNELS : java.util.Arrays.copyOf(channels, count);
    }

    private boolean hasTransferFace(BlockPos pos) {
        // 未加载时不强制加载区块：等它加载后由 chunkLoaded 重新播种。
        if (!this.level.hasChunkAt(pos)) return false;
        BlockInlays data = BlockInlayManager.get(this.level, pos);
        if (!BlockInlayManager.isConsistent(this.level.getBlockState(pos).getBlock(), data)) return false;
        for (Direction dir : Direction.values()) {
            if (isInsertFace(data, dir) || isExtractFace(data, dir)) {
                return true;
            }
        }
        return this.remoteChannels(pos, data).length > 0;
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
