package dev.anvilcraft.gtouming.doge_plus.logic;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager.*;

/**
 * 单个维度的逻辑门网络状态
 */
final class LevelNetworks {

    private final ServerLevel level;
    private final LogicGateOutputData persistentData;

    /** 有状态门（计数 / 锁存 / 延时）的持久化状态 */
    @Nullable
    private final LogicGateStateData stateData;

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

    /** 有状态门各输入面上一 tick 的信号（位置 -> 下标同 {@link Direction#ordinal()}），用于逐面上升沿判定 */
    private final Long2ObjectOpenHashMap<int[]> lastInputs = new Long2ObjectOpenHashMap<>();

    /** 计数门 1 tick 脉冲的收尾掩码（位置 -> 方向位） */
    private final Long2IntOpenHashMap pulseMasks = new Long2IntOpenHashMap();

    /** 正在倒计时的延时门（位置 -> 方向位），每 tick 只推进这些门 */
    private final Long2IntOpenHashMap pendingDelays = new Long2IntOpenHashMap();

    /** 非门在连续游戏刻内翻转次数达到该值视为振荡，破坏方块 */
    private static final int OSCILLATION_LIMIT = 16;

    /** 防止写回触发重入 */
    boolean applyingTopology = false;

    /** 防止更新循环重入 */
    private boolean processingUpdates = false;

    LevelNetworks(ServerLevel level) {
        this.level = level;
        this.persistentData = LogicGateOutputData.get(level);
        this.stateData = LogicGateStateData.get(level);
    }

    // ==================== 更新调度 ====================

    void requestTopologyUpdate(long pos) {
        topologySeeds.add(pos);
        runUpdates();
    }

    void requestSignalUpdate(Network network) {
        // 主输入（外部红石 / 邻居门）变化：必须从全 0 重算，避免旧的反馈电平被当作状态残留。
        network.needsReset = true;
        dirtySignals.add(network);
        runUpdates();
    }

    /**
     * 结算续算：保持当前中间值继续求不动点（不清零、不当作主输入变化）。
     * <p>用于长门链跨轮 / 跨 tick 传播。</p>
     */
    private void requestContinuation(Network network) {
        dirtySignals.add(network);
        runUpdates();
    }

    void tick() {
        // 清理超过一个游戏刻未翻转的计数条目，防止残留导致误判与内存增长。
        // 连续翻转（同一刻或相邻刻）会保留计数，跨刻累计到阈值判定振荡。
        long now = level.getGameTime();
        toggleTicks.long2LongEntrySet().removeIf(e -> now - e.getLongValue() > 1);
        toggleCounts.keySet().removeIf(pos -> !toggleTicks.containsKey(pos));

        // 有状态门的「到达事件」在网络更新时处理（见 applyGateArrivals）；这里只推进与时间有关的部分。
        lastInputs.keySet().removeIf(pos -> !byGate.containsKey(pos));
        pulseMasks.keySet().removeIf(pos -> !byGate.containsKey(pos));
        pendingDelays.keySet().removeIf(pos -> !byGate.containsKey(pos));
        advanceTimers();

        runUpdates();
    }

    /** 一个输入面的到达事件：该上升沿的信号值。 */
    private record Rise(int value) {
    }

    /**
     * 每 tick 只推进与时间有关的部分：计数门的 1 tick 脉冲收尾、延时门的倒计时。
     *
     * <p>输入比对不在这里做——它发生在网络更新时（{@link #applyGateArrivals}），
     * 因此同 tick 内的脉冲不会被漏掉，也不必每 tick 扫描整个网络。</p>
     */
    private void advanceTimers() {
        ObjectOpenHashSet<Network> dirty = new ObjectOpenHashSet<>();

        // 计数门脉冲只持续 1 tick。
        if (!pulseMasks.isEmpty()) {
            for (Long2IntMap.Entry entry : pulseMasks.long2IntEntrySet()) {
                long packedPos = entry.getLongKey();
                Network network = byGate.get(packedPos);
                if (network == null) continue;
                int mask = entry.getIntValue();
                for (Direction dir : Direction.values()) {
                    if ((mask & (1 << dir.ordinal())) == 0) continue;
                    network.setOutputSignal(packedPos, dir, 0);
                    dirty.add(network);
                }
            }
            pulseMasks.clear();
        }

        // 延时门倒计时，归零时停止输出。
        if (!pendingDelays.isEmpty()) {
            Long2IntOpenHashMap stillPending = new Long2IntOpenHashMap();
            for (Long2IntMap.Entry entry : pendingDelays.long2IntEntrySet()) {
                long packedPos = entry.getLongKey();
                Network network = byGate.get(packedPos);
                if (network == null) continue;
                BlockPos pos = BlockPos.of(packedPos);
                int mask = entry.getIntValue();
                int remainingMask = 0;
                for (Direction dir : Direction.values()) {
                    if ((mask & (1 << dir.ordinal())) == 0) continue;
                    int remaining = 0;
                    if (stateData != null) {
                        remaining = stateData.getRemaining(pos, dir) - 1;
                    }
                    if (remaining > 0) {
                        stateData.setRemaining(pos, dir, remaining);
                        remainingMask |= 1 << dir.ordinal();
                    } else {
                        stateData.setRemaining(pos, dir, 0);
                        network.setOutputSignal(packedPos, dir, 0);
                        dirty.add(network);
                    }
                }
                if (remainingMask != 0) {
                    stillPending.put(packedPos, remainingMask);
                }
                // 同步"已计时"显示值；外观刷新期间抑制拓扑更新。
                LogicGateNetworkManager.runSuppressedTopologyChange(
                        () -> InlayCarrierBlock.refreshState(level, pos));
            }
            pendingDelays.clear();
            pendingDelays.putAll(stillPending);
        }

        for (Network network : dirty) {
            if (network.valid && !network.overflow) {
                requestSignalUpdate(network);
            }
        }
    }

    /**
     * 网络更新时处理有状态门的「到达事件」：与本网络上次记录的各输入面信号比较，
     * 逐面检测上升沿（当前信号大于上次），有变化才推进计数 / 锁存 / 延时。
     *
     * @return 本轮是否存在到达事件（需要重算网络 / 刷新运行时长显示）
     */
    private boolean applyGateArrivals(Network network) {
        if (stateData == null || !network.hasStatefulGates) return false;

        Long2ObjectOpenHashMap<Map<Direction, Integer>> inputs = collectInputs(network);
        boolean changed = false;

        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            long packedPos = entry.getLongKey();
            GateNode node = entry.getValue();
            if (node.statefulMask() == 0) continue;
            BlockPos pos = BlockPos.of(packedPos);
            Map<Direction, Integer> inputMap = inputs.getOrDefault(packedPos, Map.of());

            // 逐输入面与上次记录比对，得到本轮的到达事件。
            List<Rise> rises = new ArrayList<>(2);
            int[] previous = lastInputs.computeIfAbsent(packedPos, key -> new int[Direction.values().length]);
            for (Direction dir : Direction.values()) {
                int current = inputMap.getOrDefault(dir, 0);
                if (current > previous[dir.ordinal()]) {
                    rises.add(new Rise(current));
                }
                previous[dir.ordinal()] = current;
            }
            if (rises.isEmpty()) continue;
            // 有到达事件：即使输出不变（如计数门只累计），运行时显示值也可能变化，需要刷新。
            changed = true;

            for (Direction dir : Direction.values()) {
                if ((node.statefulMask() & (1 << dir.ordinal())) == 0) continue;
                LogicGateType type = BlockInlayManager.get(level, pos).getGateType(dir);
                int held = node.getOutput(dir);
                int next = switch (type) {
                    case COUNTER_GATE -> arriveCounter(pos, dir, packedPos, rises.size(), held);
                    case LATCH_GATE -> arriveLatch(pos, dir, rises, held);
                    case DELAY_GATE -> arriveDelay(pos, dir, packedPos, rises, held);
                    default -> held;
                };
                if (next != held) {
                    node.setOutput(dir, next);
                }
            }
        }
        return changed;
    }

    /** 计数门：每个到达事件记一次，累计到设定次数时输出 15（下一 tick 由 advanceTimers 收尾）并立即清零。 */
    private int arriveCounter(BlockPos pos, Direction dir, long packedPos, int arrivals, int held) {
        int threshold = Math.clamp(
                BlockInlayManager.get(level, pos).getValue(dir), 1, AnvilCraftDogePlus.CONFIG.counterMaxCount);
        int count = 0;
        if (stateData != null) {
            count = stateData.getCount(pos, dir) + arrivals;
        }
        if (count >= threshold) {
            stateData.setCount(pos, dir, 0);
            pulseMasks.put(packedPos, pulseMasks.get(packedPos) | (1 << dir.ordinal()));
            return 15;
        }
        stateData.setCount(pos, dir, count);
        return held;
    }

    /** 锁存门：每个到达事件在「记录并输出该信号」与「清除并停止输出」之间切换。 */
    private int arriveLatch(BlockPos pos, Direction dir, List<Rise> rises, int held) {
        boolean latched = false;
        if (stateData != null) {
            latched = stateData.isLatched(pos, dir);
        }
        int next = held;
        for (Rise rise : rises) {
            if (latched) {
                latched = false;
                next = 0;
            } else {
                latched = true;
                next = rise.value();
            }
        }
        stateData.setLatched(pos, dir, latched);
        // 锁存门的设定值与当前记录同步：记录值直接写回镶嵌数据（随后同步给客户端）。
        BlockInlayManager.put(level, pos, BlockInlayManager.get(level, pos).withValue(dir, next));
        return next;
    }

    /** 延时门：每个到达事件更新输出值；未在计时则按设定 tick 起计时（倒计时由 advanceTimers 推进）。 */
    private int arriveDelay(BlockPos pos, Direction dir, long packedPos, List<Rise> rises, int held) {
        int remaining = 0;
        if (stateData != null) {
            remaining = stateData.getRemaining(pos, dir);
        }
        int next = held;
        if (remaining == 0) {
            remaining = Math.clamp(
                    BlockInlayManager.get(level, pos).getValue(dir), 0, AnvilCraftDogePlus.CONFIG.delayMaxTicks);
            if (remaining > 0) {
                pendingDelays.put(packedPos, pendingDelays.get(packedPos) | (1 << dir.ordinal()));
            }
        }
        if (remaining > 0) {
            next = rises.getLast().value();
        }
        stateData.setRemaining(pos, dir, remaining);
        return next;
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
            // 未能在预算内收敛的网络：本轮持续翻转的非门记一次振荡事件（真正的即时反馈环）。
            // 收敛前的中间翻转已在 countToggledNotGates 中排除，故这里只剩持续振荡。
            long now = level.getGameTime();
            for (Network network : dirtySignals) {
                for (LongIterator it = network.churningNotGates.iterator(); it.hasNext();) {
                    recordNotGateToggle(it.nextLong(), now);
                }
                network.churningNotGates.clear();
            }
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
        boolean hasStateful = false;

        while (!queue.isEmpty()) {
            long packedPos = queue.dequeueLong();
            BlockPos pos = BlockPos.of(packedPos);
            BlockState state = level.getBlockState(pos);

            if (!(state.getBlock() instanceof ILogicGate)) continue;

            LogicGateOutputData data = LogicGateOutputData.get(level);
            if (data == null) continue;

            // 记录有状态门的方向：其输出由每 tick 驱动持有，需从纯函数结算中豁免。
            BlockInlays inlays = BlockInlayManager.get(level, pos);
            int statefulMask = 0;
            for (Direction dir : Direction.values()) {
                if (inlays.getGateType(dir).isStateful()) statefulMask |= 1 << dir.ordinal();
            }
            if (statefulMask != 0) hasStateful = true;

            // 获取该位置的门配置
            nodes.put(packedPos, new GateNode(data, pos, statefulMask));

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
        Network network = new Network(nodes, overflow, hasStateful);
        registerNetwork(network);

        if (!overflow) {
            // 存档中尚未跑完的延时门：重建网络后继续倒计时。
            registerPendingDelays(nodes);
            // 初始化信号
            recompute(network);
        }
    }

    /** 把存档中仍在倒计时的延时门登记为每 tick 推进对象。 */
    private void registerPendingDelays(Long2ObjectLinkedOpenHashMap<GateNode> nodes) {
        if (stateData == null) return;
        for (Long2ObjectMap.Entry<GateNode> entry : nodes.long2ObjectEntrySet()) {
            int statefulMask = entry.getValue().statefulMask();
            if (statefulMask == 0) continue;
            BlockPos pos = BlockPos.of(entry.getLongKey());
            int pendingMask = 0;
            for (Direction dir : Direction.values()) {
                if ((statefulMask & (1 << dir.ordinal())) != 0 && stateData.getRemaining(pos, dir) > 0) {
                    pendingMask |= 1 << dir.ordinal();
                }
            }
            if (pendingMask != 0) {
                pendingDelays.put(entry.getLongKey(), pendingMask);
            }
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

        // 网络没有时序元件，门的输出必须是「主输入」的纯函数，故每轮结算先把输出清零，
        // 再迭代求最小不动点。若沿用上一次的输出作为迭代初值，纯反馈环（如 输出→输入→输出→…）
        // 会收敛到「全 15」这个非零不动点，于是没有任何信号源也会自锁持续输出（死锁）。
        // 从 0 出发则无源环必然收敛到 0；非门环会持续翻转，由振荡检测破坏。
        if (network.needsReset || network.baseline == null) {
            network.baseline = snapshotOutputs(network);
            resetOutputs(network);
            network.needsReset = false;
            // 新一轮结算开始，清空上一轮遗留的未收敛候选。
            network.churningNotGates.clear();
        }

        boolean passChanged = settlePass(network);

        if (passChanged) {
            // 尚未收敛：保留中间值继续下一轮（长链可能跨 tick 传播），
            // 期间不刷新外观 / 通知邻居，避免把中间态暴露出去。
            requestContinuation(network);
            return;
        }

        // 已收敛：此刻的输出才是「真实输出」，据此做振荡判定。
        // 结算途中「清零 → 逐跳恢复」的中间值一律不计入：否则两个非门顺序相接时，
        // 下游非门每轮结算都会经历 0→15→0 的假翻转，累计到阈值被误破坏。
        countToggledNotGates(network);
        network.churningNotGates.clear();

        // 刷新载体外观（powered 模型）。即使门输出未变，输入门「收到信号」也可能变化。
        refreshCarrierVisuals(network);

        // 只有最终输出相对结算前真正变化时才同步 / 通知。
        // 「清零」只是求最小不动点的手段，不能算作变化，否则每次重算都会通知形成回声。
        if (!outputsEqual(network, network.baseline)) {
            syncToData(network);
            notifyNeighbors(network);
        }
        network.baseline = null;

        // 有状态门：用本轮收敛后的输入与上次记录比对，处理到达事件。
        if (applyGateArrivals(network)) {
            // 门的输出变了，依赖它们的门需要再结算一轮。
            requestSignalUpdate(network);
        }
    }

    /**
     * 单轮信号结算：所有门基于本轮开始时收集到的输入求值（一轮传播一跳）。
     *
     * @return 本轮输出相对上一轮是否发生变化
     */
    private boolean settlePass(Network network) {
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
                // 有状态门（计数 / 锁存 / 延时）的输出由每 tick 驱动写入，不参与纯函数结算。
                if (gateType.isStateful()) continue;
                int newSignal = gateType.calculate(
                        outputDir, inputMap, gate.doge_plus$getValue(level, blockPos, outputDir));
                int oldSignal = node.getOutput(outputDir);
                if (oldSignal != newSignal) {
                    node.setOutput(outputDir, newSignal);
                    changed = true;
                    // 结算途中的翻转只记为「未收敛」候选，不代表真实输出变化：
                    // 只有网络最终无法收敛（真正的即时反馈环）才会据其判定振荡（见 runUpdates）。
                    if (gateType == LogicGateType.NOT_GATE) {
                        network.churningNotGates.add(pos);
                    }
                }
            }
        }
        return changed;
    }

    /**
     * 已收敛的一轮结算结束后，按「真实输出」相对结算前的翻转对非门计数。
     *
     * <p>一次结算只记一次翻转（同一方块多面非门也只记一次），
     * 连续游戏刻内累计到 {@link #OSCILLATION_LIMIT} 才判定为振荡。</p>
     */
    private void countToggledNotGates(Network network) {
        if (network.baseline == null) return;
        long now = level.getGameTime();
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            long pos = entry.getLongKey();
            if (destroyed.contains(pos)) continue;
            int[] before = network.baseline.get(pos);
            if (before == null) continue;
            GateNode node = entry.getValue();
            BlockPos blockPos = BlockPos.of(pos);
            BlockState state = level.getBlockState(blockPos);
            if (!(state.getBlock() instanceof ILogicGate gate)) continue;
            for (Direction dir : Direction.values()) {
                if (gate.doge_plus$getGateType(level, blockPos, dir) != LogicGateType.NOT_GATE) continue;
                if (node.getOutput(dir) != before[dir.ordinal()]) {
                    recordNotGateToggle(pos, now);
                    break;
                }
            }
        }
    }

    /**
     * 记一次非门翻转；连续游戏刻内累计到 {@link #OSCILLATION_LIMIT} 即判定振荡并破坏方块。
     *
     * <p>翻转中断（间隔超过一个游戏刻）会重新计数，因此只有持续振荡才会触发破坏。</p>
     */
    private void recordNotGateToggle(long pos, long now) {
        if (destroyed.contains(pos)) return;
        if (now - toggleTicks.get(pos) > 1) {
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

    /** 快照网络内每个节点六个方向的输出。 */
    private static Long2ObjectOpenHashMap<int[]> snapshotOutputs(Network network) {
        Long2ObjectOpenHashMap<int[]> snapshot = new Long2ObjectOpenHashMap<>(network.nodes.size());
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            int[] values = new int[Direction.values().length];
            for (Direction dir : Direction.values()) {
                values[dir.ordinal()] = entry.getValue().getOutput(dir);
            }
            snapshot.put(entry.getLongKey(), values);
        }
        return snapshot;
    }

    /** 当前输出是否与快照一致。 */
    private static boolean outputsEqual(Network network, Long2ObjectOpenHashMap<int[]> snapshot) {
        if (snapshot.size() != network.nodes.size()) return false;
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            int[] values = snapshot.get(entry.getLongKey());
            if (values == null) return false;
            for (Direction dir : Direction.values()) {
                if (values[dir.ordinal()] != entry.getValue().getOutput(dir)) return false;
            }
        }
        return true;
    }

    /** 把所有节点的输出清零，作为最小不动点迭代的起点；有状态门的输出由 tick 驱动持有，跳过。 */
    private static void resetOutputs(Network network) {
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            GateNode node = entry.getValue();
            int statefulMask = node.statefulMask();
            for (Direction dir : Direction.values()) {
                if ((statefulMask & (1 << dir.ordinal())) != 0) continue;
                if (node.getOutput(dir) != 0) node.setOutput(dir, 0);
            }
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
                // 输入门设定值：只传递 [0, 设定值] 内的信号（取小截断）
                inputs.put(dir, Math.min(signal, inlays.getValue(dir)));
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

    /**
     * 把网络中载体的 powered 外观刷新为当前信号。
     *
     * <p>外观变化不改拓扑，刷新期间抑制拓扑更新，避免信号每次翻转都重建网络。</p>
     */
    private void refreshCarrierVisuals(Network network) {
        LogicGateNetworkManager.runSuppressedTopologyChange(() -> {
            for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
                BlockPos blockPos = BlockPos.of(it.nextLong());
                if (level.getBlockState(blockPos).getBlock() instanceof InlayCarrierBlock) {
                    InlayCarrierBlock.refreshState(level, blockPos);
                }
            }
        });
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