package dev.anvilcraft.gtouming.doge_plus.logic;

import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.Map;

import static dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager.*;

/**
 * 单个维度的逻辑门网络状态
 */
final class LevelNetworks {

    private final ServerLevel level;
    private final LogicGateOutputData persistentData;

    /** 位置 -> 所属网络 */
    final Long2ObjectOpenHashMap<Network> byGate = new Long2ObjectOpenHashMap<>();

    /** 区块 -> 网络集合 */
    private final Long2ObjectOpenHashMap<ObjectOpenHashSet<Network>> byChunk = new Long2ObjectOpenHashMap<>();

    /** 需要重建拓扑的种子点 */
    final LongOpenHashSet topologySeeds = new LongOpenHashSet();

    /** 需要重算信号的网络 */
    private final ObjectOpenHashSet<Network> dirtySignals = new ObjectOpenHashSet<>();

    /** 振荡检测：记录当前更新周期内各非门输出翻转次数（位置 -> 次数） */
    private final Long2IntOpenHashMap toggleCounts = new Long2IntOpenHashMap();

    /** 记录每个非门最近一次翻转的游戏刻（用于跨刻累计连续振荡） */
    private final Long2LongOpenHashMap toggleTicks = new Long2LongOpenHashMap();

    /** 本批次内已判定振荡并破坏的方块（避免重复破坏） */
    private final LongOpenHashSet destroyed = new LongOpenHashSet();

    /** 非门在连续游戏刻内翻转次数达到该值视为振荡，破坏方块 */
    private static final int OSCILLATION_LIMIT = 16;

    /** 防止写回触发重入 */
    boolean applyingTopology = false;

    /** 防止更新循环重入 */
    private boolean processingUpdates = false;

    LevelNetworks(ServerLevel level) {
        this.level = level;
        this.persistentData = LogicGateOutputData.get(level);
    }

    // ==================== 更新调度 ====================

    void requestTopologyUpdate(long pos) {
        topologySeeds.add(pos);
        runUpdates();
    }

    void requestSignalUpdate(Network network) {
        dirtySignals.add(network);
        runUpdates();
    }

    void tick() {
        // 清理超过一个游戏刻未翻转的计数条目，防止残留导致误判与内存增长。
        // 连续翻转（同一刻或相邻刻）会保留计数，跨刻累计到阈值判定振荡。
        long now = level.getGameTime();
        toggleTicks.long2LongEntrySet().removeIf(e -> now - e.getLongValue() > 1);
        toggleCounts.keySet().removeIf(pos -> !toggleTicks.containsKey(pos));
        runUpdates();
    }

    /**
     * 批量处理更新，直到稳定或达到收敛上限
     */
    private void runUpdates() {
        if (processingUpdates) return;
        processingUpdates = true;

        try {
            int pass = 0;
            while ((!topologySeeds.isEmpty() || !dirtySignals.isEmpty())
                    && pass++ < MAX_SETTLING_PASSES) {

                if (!topologySeeds.isEmpty()) {
                    LongOpenHashSet seeds = new LongOpenHashSet(topologySeeds);
                    topologySeeds.clear();
                    rebuildFromSeeds(seeds);
                }

                if (!dirtySignals.isEmpty()) {
                    ObjectOpenHashSet<Network> dirty = new ObjectOpenHashSet<>(dirtySignals);
                    dirtySignals.clear();
                    for (Network network : dirty) {
                        if (network.valid && !network.overflow) {
                            recompute(network);
                        }
                    }
                }
            }
        } finally {
            processingUpdates = false;
            destroyed.clear();
        }
    }

    // ==================== 拓扑重建 ====================

    /**
     * 从种子点重建受影响的网络
     */
    private void rebuildFromSeeds(LongOpenHashSet changedPositions) {
        ObjectOpenHashSet<Network> affected = new ObjectOpenHashSet<>();
        LongOpenHashSet rebuildSeeds = new LongOpenHashSet(changedPositions);

        // 收集所有受影响的旧网络
        for (LongIterator it = changedPositions.iterator(); it.hasNext();) {
            long pos = it.nextLong();
            Network oldNetwork = byGate.get(pos);
            if (oldNetwork != null) {
                affected.add(oldNetwork);
            }

            // 检查邻居是否属于其他网络（可能被桥接）
            BlockPos blockPos = BlockPos.of(pos);
            for (Direction dir : Direction.values()) {
                long neighbor = blockPos.relative(dir).asLong();
                Network neighborNetwork = byGate.get(neighbor);
                if (neighborNetwork != null && neighborNetwork != oldNetwork) {
                    affected.add(neighborNetwork);
                }
            }
        }

        // 失效所有受影响的网络
        for (Network network : affected) {
            invalidate(network, rebuildSeeds);
        }

        // 从种子重建新网络
        applyingTopology = true;
        try {
            for (LongIterator it = rebuildSeeds.iterator(); it.hasNext();) {
                long seed = it.nextLong();
                if (byGate.containsKey(seed)) continue;
                if (isLogicGate(level,BlockPos.of(seed))) {
                    buildNetwork(seed);
                }
            }
        } finally {
            applyingTopology = false;
        }
    }

    /**
     * BFS 构建连通网络
     */
    private void buildNetwork(long seed) {
        Long2ObjectLinkedOpenHashMap<GateNode> nodes = new Long2ObjectLinkedOpenHashMap<>();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        LongOpenHashSet queued = new LongOpenHashSet();

        queue.enqueue(seed);
        queued.add(seed);
        boolean overflow = false;

        while (!queue.isEmpty()) {
            long packedPos = queue.dequeueLong();
            BlockPos pos = BlockPos.of(packedPos);
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof ILogicGate)) continue;

            LogicGateOutputData data = LogicGateOutputData.get(level);
            if (data == null) continue;

            // 获取该位置的门配置
            nodes.put(packedPos, new GateNode(data, pos));

            // 检查规模限制
            if (nodes.size() >= MAX_NETWORK_SIZE) {
                overflow = !queue.isEmpty();
                if (overflow) break;
            }

            // 添加邻居：所有方向的邻居逻辑门
            for (Direction dir : Direction.values()) {
                long neighbor = pos.relative(dir).asLong();
                if (queued.add(neighbor) && isLogicGate(level,BlockPos.of(neighbor))) {
                    queue.enqueue(neighbor);
                }
            }
        }

        // 创建网络
        Network network = new Network(nodes, overflow);
        registerNetwork(network);

        if (!overflow) {
            // 初始化信号
            recompute(network);
        }
    }

    /**
     * 注册网络到索引
     */
    private void registerNetwork(Network network) {
        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
            long pos = it.nextLong();
            byGate.put(pos, network);

            long chunkPos = ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4);
            byChunk.computeIfAbsent(chunkPos, k -> new ObjectOpenHashSet<>()).add(network);
        }
    }

    /**
     * 失效网络
     */
    private void invalidate(Network network, LongOpenHashSet rebuildSeeds) {
        if (!network.valid) return;
        network.valid = false;

        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
            long pos = it.nextLong();
            if (byGate.get(pos) == network) {
                byGate.remove(pos);
                rebuildSeeds.add(pos);
            }
        }

        // 清理区块索引
        for (LongIterator it = network.chunks.iterator(); it.hasNext();) {
            long chunkPos = it.nextLong();
            ObjectOpenHashSet<Network> networks = byChunk.get(chunkPos);
            if (networks != null) {
                networks.remove(network);
                if (networks.isEmpty()) byChunk.remove(chunkPos);
            }
        }
    }

    // ==================== 信号计算 ====================

    /**
     * 重算网络信号
     */
    private void recompute(Network network) {
        if (!network.valid || network.overflow) return;

        // 收集所有门的输入
        Long2ObjectOpenHashMap<Map<Direction, Integer>> inputs = collectInputs(network);

        // 计算每个门的输出
        boolean changed = false;
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            long pos = entry.getLongKey();
            GateNode node = entry.getValue();
            BlockPos blockPos = BlockPos.of(pos);
            BlockState state = level.getBlockState(blockPos);

            if (!(state.getBlock() instanceof ILogicGate gate)) continue;

            // 获取该门所有方向的输入
            Map<Direction, Integer> inputMap = inputs.getOrDefault(pos, Map.of());

            // 计算各方向输出
            for (Direction outputDir : Direction.values()) {
                LogicGateType gateType = gate.doge_plus$getGateType(level, blockPos, outputDir);
                int newSignal = gateType.calculate(outputDir, inputMap);
                int oldSignal = node.getOutput(outputDir);
                if (oldSignal != newSignal) {
                    node.setOutput(outputDir, newSignal);
                    changed = true;
                    // 振荡检测：非门输出翻转计数。
                    // 计数窗口为「连续游戏刻」而非单次批次：弱信号环路（红石粉即时反馈）
                    // 在同一刻内翻转多次累计；强信号环路（中继器延迟反馈）每刻翻转一次，
                    // 通过相邻刻连续翻转跨刻累计。翻转中断（间隔 >1 刻）则重置。
                    if (gateType == LogicGateType.NOT_GATE && !destroyed.contains(pos)) {
                        long now = level.getGameTime();
                        long last = toggleTicks.get(pos);
                        if (now - last > 1) {
                            toggleCounts.put(pos, 1);
                        } else {
                            toggleCounts.addTo(pos, 1);
                        }
                        toggleTicks.put(pos, now);
                        if (toggleCounts.get(pos) >= OSCILLATION_LIMIT) {
                            destroyed.add(pos);
                            breakOscillatingGate(pos);
                        }
                    }
                }
            }
        }

        if (changed) {
            // 同步到持久化存储
            syncToData(network);
            // 通知邻居
            notifyNeighbors(network);
            // 重新调度信号更新：单轮计算中下游门用的是上游门的旧输出，
            // 需多轮收敛门链（如 信号→非门→非门）到稳定值，否则下游门保持错误输出。
            requestSignalUpdate(network);
        }
    }

    /**
     * 破坏被判定为振荡的非门方块。
     *
     * <p>方块被破坏后其镶嵌数据（含门配置）会随之清除（见 {@code onRemove}），
     * 本方法额外触发拓扑更新，使网络重新收敛到无振荡的稳定状态。</p>
     */
    private void breakOscillatingGate(long pos) {
        BlockPos blockPos = BlockPos.of(pos);
        level.destroyBlock(blockPos, true);
        BlockInlayManager.remove(level, blockPos);
        LogicGateNetworkManager.topologyChanged(level, blockPos);
    }

    /**
     * 收集网络中所有门的输入信号
     * <p>只把标记为 {@link LogicGateType#INPUT} 的方向放入 map（值可为 0），
     * 使 {@link LogicGateType#calculate} 能区分「输入面存在但信号为 0」与「无输入面」。</p>
     */
    private Long2ObjectOpenHashMap<Map<Direction, Integer>> collectInputs(Network network) {
        Long2ObjectOpenHashMap<Map<Direction, Integer>> result = new Long2ObjectOpenHashMap<>();

        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
            long pos = it.nextLong();
            BlockPos blockPos = BlockPos.of(pos);
            Map<Direction, Integer> inputs = new EnumMap<>(Direction.class);

            // 仅收集「输入面」（INPUT 门标记的方向）的信号，非输入面不查询邻居。
            // 这样输入信号映射只含输入面，门的计算逻辑无需再过滤非输入面。
            BlockInlays inlays = BlockInlayManager.get(level, blockPos);
            for (Direction dir : Direction.values()) {
                if (inlays.getGateType(dir) != LogicGateType.INPUT) continue;
                BlockPos neighborPos = blockPos.relative(dir);
                long neighbor = neighborPos.asLong();

                // 如果邻居在网络中，从网络读取输出
                int signal;
                if (network.nodes.containsKey(neighbor)) {
                    GateNode neighborNode = network.nodes.get(neighbor);
                    signal = neighborNode.getOutput(dir.getOpposite());
                } else if (LogicGateNetworkManager.isLogicGate(level, neighborPos)) {
                    // 跨网络的逻辑门：直接读其网络输出（该门朝本门方向的面）。
                    signal = LogicGateNetworkManager.getSignal(level, neighborPos, dir.getOpposite());
                } else {
                    // 外部输入：从世界读取红石信号。
                    // vanilla 约定：getSignal/getDirectSignal 的 direction 参数是
                    // 「调用者 → 被查询方块」的方向，即本门指向邻居的 dir（而非 dir.getOpposite()）。
                    // 中继器等方向敏感信号源按此约定输出，传反则读不到（红石粉不分方向所以不暴露）。
                    // 取弱信号与强信号的最大值：红石粉只提供弱信号（getSignal），
                    // 而中继器/比较器等只输出强信号（getDirectSignal），两者都需支持。
                    int weak = level.getSignal(neighborPos, dir);
                    int strong = level.getDirectSignal(neighborPos, dir);
                    signal = Math.max(weak, strong);
                }
                inputs.put(dir, signal);
            }

            result.put(pos, inputs);
        }

        return result;
    }

    /**
     * 同步到持久化存储
     */
    private void syncToData(Network network) {
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            BlockPos pos = BlockPos.of(entry.getLongKey());
            GateNode node = entry.getValue();
            for (Direction dir : Direction.values()) {
                int signal = node.getOutput(dir);
                if (signal > 0) {
                    persistentData.setSignal(pos, dir, signal);
                }
            }
        }
        persistentData.setDirty();
    }

    /**
     * 通知邻居方块信号变化。
     *
     * <p>必须对<b>门自身位置</b>调用 {@code updateNeighborsAt}：vanilla 语义是
     * 「pos 处的方块变化 → 触发 pos 周围所有邻居检测 pos」。若对邻居位置调用，
     * 实际触发的是邻居的邻居，红石粉等目标方块不会重算。</p>
     */
    private void notifyNeighbors(Network network) {
        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
            long pos = it.nextLong();
            BlockPos blockPos = BlockPos.of(pos);
            BlockState state = level.getBlockState(blockPos);
            Block block = state.getBlock();
            // 触发门周围所有方块（含红石粉）的 neighborChanged，使它们检测到门新输出
            level.updateNeighborsAt(blockPos, block);
        }
    }

    // ==================== 区块管理 ====================

    @Nullable
    Network getOrBuildNetwork(long packedPos) {
        Network network = byGate.get(packedPos);
        if (network == null && isLogicGate(level,BlockPos.of(packedPos))) {
            requestTopologyUpdate(packedPos);
            network = byGate.get(packedPos);
        }
        return network;
    }

    void chunkUnloaded(long chunkPos) {
        // 清除该区块的种子
        removeChunkPositions(topologySeeds, chunkPos);

        ObjectOpenHashSet<Network> affected = byChunk.remove(chunkPos);
        if (affected == null) return;

        LongOpenHashSet rebuildSeeds = new LongOpenHashSet();
        for (Network network : affected) {
            invalidate(network, rebuildSeeds);
        }
        topologySeeds.addAll(rebuildSeeds);
    }

    private static void removeChunkPositions(LongOpenHashSet positions, long chunkPos) {
        for (LongIterator it = positions.iterator(); it.hasNext();) {
            long pos = it.nextLong();
            if (ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4) == chunkPos) {
                it.remove();
            }
        }
    }
}