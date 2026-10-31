package dev.anvilcraft.gtouming.doge_plus.logic;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogisticsCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
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

    /** 本轮结算中「输入面显示值发生变化」的节点，用于只刷新这些载体的外观 / HUD。 */
    private final LongOpenHashSet visualDirty = new LongOpenHashSet();

    /** 复用求值输入映射与传播队列，避免每节点 / 每次结算分配。 */
    private final EnumMap<Direction, Integer> scratchInputs = new EnumMap<>(Direction.class);
    private final LongOpenHashSet propagateQueued = new LongOpenHashSet();
    private final LongArrayList propagateQueue = new LongArrayList();

    /** 复用 runUpdates 每轮的种子 / 脏网络集合。 */
    private final LongOpenHashSet passSeeds = new LongOpenHashSet();
    private final ObjectOpenHashSet<Network> passDirty = new ObjectOpenHashSet<>();

    /** 有状态门各输入面上一 tick 的信号（位置 -> 下标同 {@link Direction#ordinal()}），用于逐面上升沿判定 */
    private final Long2ObjectOpenHashMap<int[]> lastInputs = new Long2ObjectOpenHashMap<>();

    /**
     * 延时输入门各面上一 tick <b>自读邻居</b>的信号（位置 -> 下标同 {@link Direction#ordinal()}）。
     *
     * <p>不能与 {@link #lastInputs} 共用一个下标：延时输入门在 {@link #lastInputs} 上的那一格记的是
     * 它自己产生的 1 tick 脉冲（供其它状态门计数），它自读邻居的上升沿判定得另存一份，否则两者互相覆盖。</p>
     */
    private final Long2ObjectOpenHashMap<int[]> lastDelayInputs = new Long2ObjectOpenHashMap<>();

    /** 计数门 1 tick 脉冲的收尾掩码（位置 -> 方向位） */
    private final Long2IntOpenHashMap pulseMasks = new Long2IntOpenHashMap();

    /**
     * 延时输入门 1 tick 输入脉冲（位置 -> 方向位）。
     *
     * <p>延时输入门不是「延时输出」而是「延时输入」：计时归零时它不向面外发红石，而是把当初记录的信号值
     * 作为「本方块该面这一 tick 收到了输入」喂给同方块的其它门（见 {@link #collectNodeInputs}），
     * 使输出 / 非 / 与门与计数 / 锁存 / 延时门的到达事件都能读到这一路输入。</p>
     *
     * <p>因此这个脉冲不写面输出，只在这里存活一个 tick：计时归零的那一 tick 置位，
     * 下一 tick 由 {@link #advanceTimers} 收尾（清掉记录值并让依赖它的门重算）。</p>
     */
    private final Long2IntOpenHashMap delayInputPulses = new Long2IntOpenHashMap();

    /** 正在倒计时的延时门（位置 -> 方向位），每 tick 只推进这些门 */
    private final Long2IntOpenHashMap pendingDelays = new Long2IntOpenHashMap();

    /**
     * 延时门本次计时的登记时刻（位置 -> 游戏刻）。
     *
     * <p>上升沿在世界更新阶段处理、{@code tick()} 在同一刻末尾递减，若登记当刻就递减，
     * 设定 N 只会输出 N-1 tick（N=1 更是来不及被观察到就被清零）。登记当刻跳过递减，
     * 使持续时长正好为设定值。</p>
     */
    private final Long2LongOpenHashMap delayRegisteredAt = new Long2LongOpenHashMap();

    /**
     * 远程门信道 → 参与红石总线的成员位置。
     *
     * <p>红石总线的成员 = 逻辑载体 / 普通载体上的远程面（物流载体的远程面归物品传输网，见
     * {@code LevelTransferNetworks}）。总线信号 = 同信道所有成员「向外发布的信号」的最大值；
     * 每个成员的远程面又把这个总线信号当作输入喂给本方块的门，于是远程面成为一条双向导线。</p>
     */
    private final Long2ObjectOpenHashMap<LongOpenHashSet> channelMembers = new Long2ObjectOpenHashMap<>();

    /** 位置 → 其参与的信道键（登记时记下，失效时据此精确移除，无需重读已被清掉的镶嵌数据）。 */
    private final Long2ObjectOpenHashMap<long[]> nodeChannels = new Long2ObjectOpenHashMap<>();

    private static final long[] NO_CHANNELS = new long[0];

    /** 非门在连续游戏刻内翻转次数达到该值视为振荡，破坏方块 */
    private static final int OSCILLATION_LIMIT = 16;

    /** 不排除任何区块（{@link ChunkPos#asLong} 的结果恒为非负，不会与之冲突）。 */
    private static final long NO_CHUNK = Long.MIN_VALUE;

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
        markSignalDirty(network);
        runUpdates();
    }

    /**
     * 只把网络标脏，不立即结算。
     *
     * <p>区块卸载 / 加载这类回调发生在主线程 tick 区块的过程中，此时同步 {@link #runUpdates()}
     * 会去读方块、强制加载区块甚至等待主线程自己。标脏后交给下一 tick 统一收敛。</p>
     */
    private void markSignalDirty(Network network) {
        // 主输入（外部红石 / 邻居门）变化：必须从全 0 重算，避免旧的反馈电平被当作状态残留。
        network.needsReset = true;
        dirtySignals.add(network);
    }

    /**
     * 结算续算：保持当前中间值继续求不动点（不清零、不当作主输入变化）。
     * <p>用于长门链跨轮 / 跨 tick 传播。</p>
     */
    private void requestContinuation(Network network) {
        dirtySignals.add(network);
        runUpdates();
    }

    /**
     * 让紧邻该位置的网络重采样输入（位置本身不在任何网络里时使用）。
     *
     * <p>红石粉等外部方块变化时，受影响的只是相邻网络的输入，拓扑并没有变。
     * 绝大多数邻居变化周围根本没有门，因此先在无分配的情况下扫一遍就返回。</p>
     *
     * @param except 本次变化来源所属的网络，跳过它：它的输出刚刚收敛，再标脏只会让同一张网
     *               被整张重新结算一遍（自己通知自己导致的二次结算）
     */
    void signalUpdateAdjacent(BlockPos pos, @Nullable Network except) {
        ObjectOpenHashSet<Network> affected = null;
        for (Direction dir : Direction.values()) {
            Network network = byGate.get(pos.relative(dir).asLong());
            if (network == null || !network.valid || network.overflow || network == except) continue;
            if (affected == null) affected = new ObjectOpenHashSet<>();
            affected.add(network);
        }
        if (affected == null) return;
        for (Network network : affected) {
            requestSignalUpdate(network);
            // 同信道对端不相邻，收不到这次邻居变化：显式让它们重采样总线。
            dirtyChannelPeers(network);
        }
    }

    void tick() {
        // 关闭流程里不再推进任何时序 / 拓扑 / 信号：此时读方块会强制加载正在卸载的区块。
        if (level.getServer().isStopped()) return;

        // 清理超过一个游戏刻未翻转的计数条目，防止残留导致误判与内存增长。
        // 连续翻转（同一刻或相邻刻）会保留计数，跨刻累计到阈值判定振荡。
        long now = level.getGameTime();
        toggleTicks.long2LongEntrySet().removeIf(e -> now - e.getLongValue() > 1);
        toggleCounts.keySet().removeIf(pos -> !toggleTicks.containsKey(pos));

        // 有状态门的「到达事件」在网络更新时处理（见 applyGateArrivals）；这里只推进与时间有关的部分。
        lastInputs.keySet().removeIf(pos -> !byGate.containsKey(pos));
        lastDelayInputs.keySet().removeIf(pos -> !byGate.containsKey(pos));
        pulseMasks.keySet().removeIf(pos -> !byGate.containsKey(pos));
        delayInputPulses.keySet().removeIf(pos -> !byGate.containsKey(pos));
        pendingDelays.keySet().removeIf(pos -> !byGate.containsKey(pos));
        delayRegisteredAt.keySet().removeIf(pos -> !byGate.containsKey(pos));
        advanceTimers();

        runUpdates();
    }

    /** 一个输入面的到达事件：该上升沿的信号值。 */
    private record Rise(int value) {
    }

    /**
     * 抹掉某面逻辑门的输出与运行时状态（该面换料 / 移除镶嵌时调用）。
     *
     * <p>换料可能把该面从无状态门换成有状态门。有状态门的输出由自身逻辑（到达事件 / 倒计时）
     * 持有，既不参与组合结算、也不在 {@code resetOutputs} 的范围内，若不显式清除，
     * 旧门的输出会被一直保留，红石读数不会更新（如输出门的 15 换成锁存门后仍是 15）。</p>
     */
    void clearFace(BlockPos pos, Direction face) {
        long packedPos = pos.asLong();
        int bit = 1 << face.ordinal();

        if (persistentData != null) {
            persistentData.setSignal(pos, face, 0);
            persistentData.setDirty();
        }
        if (stateData != null) {
            stateData.clear(pos, face);
        }
        // 该面未跑完的计数脉冲 / 延时倒计时 / 延时输入脉冲一并丢弃，避免它们之后把新门的输出清零
        // 或把陈旧的记录值继续当作输入喂给同方块的门。
        int pulses = pulseMasks.get(packedPos);
        if ((pulses & bit) != 0) {
            if ((pulses & ~bit) == 0) pulseMasks.remove(packedPos);
            else pulseMasks.put(packedPos, pulses & ~bit);
        }
        int inputPulses = delayInputPulses.get(packedPos);
        if ((inputPulses & bit) != 0) {
            if ((inputPulses & ~bit) == 0) delayInputPulses.remove(packedPos);
            else delayInputPulses.put(packedPos, inputPulses & ~bit);
        }
        int delays = pendingDelays.get(packedPos);
        if ((delays & bit) != 0) {
            if ((delays & ~bit) == 0) {
                pendingDelays.remove(packedPos);
                delayRegisteredAt.remove(packedPos);
            } else {
                pendingDelays.put(packedPos, delays & ~bit);
            }
        }
    }

    /**
     * 每 tick 只推进与时间有关的部分：计数门的 1 tick 脉冲收尾、延时输入门的 1 tick 输入脉冲收尾、
     * 延时门的倒计时。
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

        // 延时输入门的 1 tick 输入脉冲收尾：丢弃记录值，让读到这一路输入的门重算回 0。
        // 本刻新置位的脉冲在下面才登记，因此不会被这一步清掉。
        if (!delayInputPulses.isEmpty()) {
            for (Long2IntMap.Entry entry : delayInputPulses.long2IntEntrySet()) {
                long packedPos = entry.getLongKey();
                Network network = byGate.get(packedPos);
                if (network == null) continue;
                BlockPos pos = BlockPos.of(packedPos);
                int mask = entry.getIntValue();
                for (Direction dir : Direction.values()) {
                    if ((mask & (1 << dir.ordinal())) == 0) continue;
                    if (stateData != null) stateData.setCount(pos, dir, 0);
                    dirty.add(network);
                }
            }
            delayInputPulses.clear();
        }

        // 延时门倒计时，归零时停止输出。
        if (!pendingDelays.isEmpty()) {
            long now = level.getGameTime();
            Long2IntOpenHashMap stillPending = new Long2IntOpenHashMap();
            for (Long2IntMap.Entry entry : pendingDelays.long2IntEntrySet()) {
                long packedPos = entry.getLongKey();
                Network network = byGate.get(packedPos);
                if (network == null) continue;
                BlockPos pos = BlockPos.of(packedPos);
                int mask = entry.getIntValue();
                // 本刻刚登记的计时不在本刻递减，保证持续时长正好为设定 tick 数（见 delayRegisteredAt）。
                if (delayRegisteredAt.containsKey(packedPos) && delayRegisteredAt.get(packedPos) == now) {
                    stillPending.put(packedPos, mask);
                    continue;
                }
                BlockInlays inlays = BlockInlayManager.get(level, pos);
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
                        if (stateData != null && inlays.getFace(dir) == FaceMode.DELAY_INPUT_GATE) {
                            // 延时输入门：倒计时归零，把当初记录的信号值当作本方块该面这一 tick 的输入
                            // 喂给同方块的门（记录值留到下一 tick 的收尾才丢弃，故恰好持续 1 tick）。
                            delayInputPulses.put(packedPos, delayInputPulses.get(packedPos) | (1 << dir.ordinal()));
                        } else {
                            network.setOutputSignal(packedPos, dir, 0);
                        }
                        dirty.add(network);
                    }
                }
                if (remainingMask != 0) {
                    stillPending.put(packedPos, remainingMask);
                } else {
                    // 计时结束，登记时刻一并清除，避免长期残留。
                    delayRegisteredAt.remove(packedPos);
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

        // 先把所有有状态门的输入取成快照：同一轮内各门的到达判定必须基于同一份输入，
        // 否则先处理的门写回的输出会被后处理的门读到，结果会取决于遍历顺序。
        Long2ObjectOpenHashMap<Map<Direction, Integer>> inputs = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            if (entry.getValue().statefulMask() == 0) continue;
            long packedPos = entry.getLongKey();
            BlockPos pos = BlockPos.of(packedPos);
            if (!level.hasChunkAt(pos)) continue;
            inputs.put(packedPos, collectNodeInputs(network, pos, BlockInlayManager.get(level, pos)));
        }

        boolean changed = false;
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            long packedPos = entry.getLongKey();
            GateNode node = entry.getValue();
            if (node.statefulMask() == 0) continue;
            BlockPos pos = BlockPos.of(packedPos);
            if (!level.hasChunkAt(pos)) continue;
            BlockInlays inlays = BlockInlayManager.get(level, pos);
            Map<Direction, Integer> inputMap = inputs.getOrDefault(packedPos, Map.of());
            int[] previous = lastInputs.computeIfAbsent(packedPos, key -> new int[Direction.values().length]);
            int[] previousNeighbor = null;

            // 汇总各输入面（含远程面，以及延时输入门自己产生的那 1 tick 脉冲，见 collectNodeInputs）
            // 与上次记录比对得到的到达事件，供计数 / 锁存 / 延时门共用。
            // 延时输入门的那一路也走这里统一比对：脉冲存在的那一 tick 恰好是一次上升沿，
            // 且 previous 随即被更新为脉冲值，同一 tick 内重复结算也不会重复计数。
            List<Rise> rises = new ArrayList<>(2);
            for (Direction dir : Direction.values()) {
                int current = inputMap.getOrDefault(dir, 0);
                if (current > previous[dir.ordinal()]) {
                    rises.add(new Rise(current));
                }
                previous[dir.ordinal()] = current;
            }

            boolean arrived = false;
            for (Direction dir : Direction.values()) {
                if ((node.statefulMask() & (1 << dir.ordinal())) == 0) continue;
                FaceMode type = inlays.getFace(dir);
                int held = node.getOutput(dir);
                int next = held;
                if (type == FaceMode.DELAY_INPUT_GATE) {
                    // 延时输入门：读自己这一面的邻居红石（与输入面同一套读法），只有该面上升沿才计时。
                    // 该面的设定值是延时 tick 数而不是信号上限，故不走输入面的截断读法。
                    // 上一 tick 的邻居读数另存于 lastDelayInputs：previous 上的那一格是它自己的脉冲。
                    if (previousNeighbor == null) {
                        previousNeighbor = lastDelayInputs.computeIfAbsent(
                                packedPos, key -> new int[Direction.values().length]);
                    }
                    int current = neighborSignal(network, pos, dir);
                    if (current > previousNeighbor[dir.ordinal()]) {
                        next = arriveDelayInput(pos, dir, packedPos, current, held);
                        arrived = true;
                    }
                    previousNeighbor[dir.ordinal()] = current;
                } else if (!rises.isEmpty()) {
                    arrived = true;
                    next = switch (type) {
                        case COUNTER_GATE -> arriveCounter(pos, dir, packedPos, rises.size(), held);
                        case LATCH_GATE -> arriveLatch(pos, dir, rises, held);
                        case DELAY_GATE -> arriveDelay(pos, dir, packedPos, rises, held);
                        default -> held;
                    };
                } else {
                    continue;
                }
                if (next != held) {
                    node.setOutput(dir, next);
                }
            }
            // 有到达事件：即使输出不变（如计数门只累计），运行时显示值也可能变化，需要刷新。
            if (arrived) changed = true;
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
        // 锁存门的设定值与当前记录同步：记录值写回镶嵌数据（随后同步给客户端）。
        // 但「可调节的设定值」是载体独有的特性——只有载体会渲染 / 显示这个值；
        // 非载体（如镶嵌了锁存门的石头）写回只会留下无意义的存档写入与客户端同步。
        if (level.getBlockState(pos).getBlock() instanceof InlayCarrierBlock) {
            BlockInlayManager.put(level, pos, BlockInlayManager.get(level, pos).withValue(dir, next));
        }
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
                // 登记当刻不递减（见 advanceTimers），保证持续时长正好为设定 tick 数。
                delayRegisteredAt.put(packedPos, level.getGameTime());
            }
        }
        if (remaining > 0) {
            next = rises.getLast().value();
        }
        stateData.setRemaining(pos, dir, remaining);
        return next;
    }

    /**
     * 延时输入门：记录自己那一面的上升沿信号值并重置倒计时；倒计时归零时由 {@link #advanceTimers}
     * 把记录值当作该面 1 tick 的输入喂给同方块的其它门（不向面外输出红石）。
     *
     * <p>与延时门不同：它<b>只认自己面上的输入</b>（像输入门一样读邻居红石），倒计时期间本方块读不到它这一路；
     * 期间再次收到上升沿会<b>重新记录并重置倒计时</b>，记录值暂存于 {@code LogicGateStateData}
     * 的计数字段（本门不使用计数语义）。</p>
     *
     * @param value 该面本次上升沿读到的信号值
     */
    private int arriveDelayInput(BlockPos pos, Direction dir, long packedPos, int value, int held) {
        if (stateData != null) {
            stateData.setCount(pos, dir, value);
        }
        int remaining = Math.clamp(
                BlockInlayManager.get(level, pos).getValue(dir), 0, AnvilCraftDogePlus.CONFIG.delayMaxTicks);
        if (remaining > 0) {
            pendingDelays.put(packedPos, pendingDelays.get(packedPos) | (1 << dir.ordinal()));
            // 登记当刻不递减（见 advanceTimers）；重触发时一并覆盖登记时刻，倒计时从设定值重新开始。
            delayRegisteredAt.put(packedPos, level.getGameTime());
        }
        if (stateData != null) {
            stateData.setRemaining(pos, dir, remaining);
        }
        return held;
    }

    /**
     * 批量处理更新，直到稳定或达到收敛上限
     */
    private void runUpdates() {
        if (processingUpdates) return;
        // 服务端关闭时 stopServer 会在主线程反复 tick / 卸载区块。此刻结算网络会去读方块，
        // 强制加载正在卸载的区块并等待主线程自己，把「Saving worlds」永久卡住。
        if (level.getServer().isStopped()) return;
        processingUpdates = true;

        try {
            int pass = 0;
            while ((!topologySeeds.isEmpty() || !dirtySignals.isEmpty())
                    && pass++ < MAX_SETTLING_PASSES) {

                if (!topologySeeds.isEmpty()) {
                    passSeeds.clear();
                    passSeeds.addAll(topologySeeds);
                    topologySeeds.clear();
                    rebuildFromSeeds(passSeeds);
                }

                if (!dirtySignals.isEmpty()) {
                    passDirty.clear();
                    passDirty.addAll(dirtySignals);
                    dirtySignals.clear();
                    for (Network network : passDirty) {
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

        // 从种子重建新网络。重建期间外部方块（红石粉等）会被通知并反过来通知相邻的门，
        // 那些「重采样输入」的排期必须照常生效，否则跨外部方块的下一张网收不到更新。
        for (LongIterator it = rebuildSeeds.iterator(); it.hasNext();) {
            long seed = it.nextLong();
            if (byGate.containsKey(seed)) continue;
            BlockPos seedPos = BlockPos.of(seed);
            // 未加载的区块不读：读取会强制加载区块（关闭流程里则是主线程自等待）。
            // 等它加载后由 chunkLoaded 重新播种。
            if (!level.hasChunkAt(seedPos)) continue;
            if (isLogicGate(level, seedPos)) {
                buildNetwork(seed);
            }
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
        boolean hasNot = false;

        while (!queue.isEmpty()) {
            long packedPos = queue.dequeueLong();
            BlockPos pos = BlockPos.of(packedPos);
            // BFS 会摸到跨区块边界的邻居：未加载时当作非节点跳过，绝不强制加载区块。
            if (!level.hasChunkAt(pos)) continue;
            BlockState state = level.getBlockState(pos);

            LogicGateOutputData data = LogicGateOutputData.get(level);
            if (data == null) continue;

            // 记录有状态门的方向：其输出由每 tick 驱动持有，需从纯函数结算中豁免。
            // 数据必属于当前方块（否则视为非门）。
            BlockInlays inlays = logicGateData(level, pos, state);
            if (inlays == null) continue;
            int statefulMask = 0;
            for (Direction dir : Direction.values()) {
                FaceMode mode = inlays.getFace(dir);
                if (mode.isStateful()) statefulMask |= 1 << dir.ordinal();
                if (mode == FaceMode.NOT_GATE) hasNot = true;
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
        Network network = new Network(nodes, overflow, hasStateful, hasNot);
        // 建网时的输出即视为「已通知」的基准：区块加载 / 拓扑重建不应误报一次全量邻居更新。
        network.lastNotified = snapshotOutputs(network);
        registerNetwork(network);
        // 新成员加入总线：同信道对端不相邻，不会被拓扑重建带上，显式让它们重采样。
        dirtyChannelPeers(network);

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

            addRemoteMembership(pos);
        }
    }

    /** 该位置上参与红石总线的远程面信道键（物流载体不参与红石总线，返回空）。 */
    private long[] redstoneChannels(BlockPos pos, BlockInlays inlays) {
        if (!level.hasChunkAt(pos)) return NO_CHANNELS;
        if (level.getBlockState(pos).getBlock() instanceof LogisticsCarrierBlock) return NO_CHANNELS;
        long[] channels = new long[Direction.values().length];
        int count = 0;
        for (Direction dir : Direction.values()) {
            if (inlays.getFace(dir) != FaceMode.REMOTE) continue;
            long channel = inlays.getChannel(dir).key();
            boolean seen = false;
            for (int i = 0; i < count; i++) {
                if (channels[i] == channel) {
                    seen = true;
                    break;
                }
            }
            if (!seen) channels[count++] = channel;
        }
        return count == 0 ? NO_CHANNELS : Arrays.copyOf(channels, count);
    }

    /** 把该位置登记为其远程面信道总线的成员（无远程面则不动）。 */
    private void addRemoteMembership(long packedPos) {
        long[] channels = redstoneChannels(BlockPos.of(packedPos), BlockInlayManager.get(level, BlockPos.of(packedPos)));
        if (channels.length == 0) return;
        nodeChannels.put(packedPos, channels);
        for (long channel : channels) {
            channelMembers.computeIfAbsent(channel, key -> new LongOpenHashSet()).add(packedPos);
        }
    }

    /** 把该位置从其所有信道总线中移除。 */
    private void removeRemoteMembership(long packedPos) {
        long[] channels = nodeChannels.remove(packedPos);
        if (channels == null) return;
        for (long channel : channels) {
            LongOpenHashSet members = channelMembers.get(channel);
            if (members == null) continue;
            members.remove(packedPos);
            if (members.isEmpty()) channelMembers.remove(channel);
        }
    }

    /**
     * 失效网络
     */
    private void invalidate(Network network, LongOpenHashSet rebuildSeeds) {
        invalidate(network, rebuildSeeds, NO_CHUNK);
    }

    /**
     * 失效网络，并把仍在加载状态下的节点排进重建种子。
     *
     * @param excludedChunk 该区块里的节点不再排进重建种子。区块卸载时用：它们的方块已经读不到，
     *                      排进去会立刻触发对未加载区块的读取；等区块重新加载时由 chunkLoaded 重新播种。
     */
    private void invalidate(Network network, LongOpenHashSet rebuildSeeds, long excludedChunk) {
        if (!network.valid) return;
        network.valid = false;

        LongOpenHashSet channels = null;
        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
            long pos = it.nextLong();
            if (byGate.get(pos) == network) {
                byGate.remove(pos);
                if (chunkOf(pos) != excludedChunk) {
                    rebuildSeeds.add(pos);
                }
            }
            long[] nodeChannelList = nodeChannels.get(pos);
            if (nodeChannelList != null) {
                if (channels == null) channels = new LongOpenHashSet();
                for (long channel : nodeChannelList) channels.add(channel);
            }
            removeRemoteMembership(pos);
        }

        // 总线上少了一个成员：同信道对端不相邻，显式让它们重采样。
        if (channels != null) {
            for (long channel : channels) {
                LongOpenHashSet members = channelMembers.get(channel);
                if (members == null) continue;
                for (LongIterator mit = members.iterator(); mit.hasNext();) {
                    Network peer = byGate.get(mit.nextLong());
                    // 只标脏：本方法也会在区块卸载回调里被调用，那里不能同步结算。
                    if (peer != null && peer.valid && !peer.overflow) markSignalDirty(peer);
                }
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
        visualDirty.clear();

        // 网络没有时序元件，门的输出必须是「主输入」的纯函数，故每轮结算先把输出清零，
        // 再迭代求最小不动点。若沿用上一次的输出作为迭代初值，纯反馈环（如 输出→输入→输出→…）
        // 会收敛到「全 15」这个非零不动点，于是没有任何信号源也会自锁持续输出（死锁）。
        // 从 0 出发则无源环必然收敛到 0；非门环会持续翻转，由振荡检测破坏。
        boolean resetThisRound = network.needsReset || network.baseline == null;
        if (resetThisRound) {
            network.baseline = snapshotOutputs(network);
            resetOutputs(network);
            network.needsReset = false;
            // 新一轮结算开始，清空上一轮遗留的未收敛候选。
            network.churningNotGates.clear();
            // 从全 0 起步：整网都要重新求值。
            network.dirtyNodes.clear();
            network.dirtyNodes.addAll(network.nodes.keySet());
        }
        if (!propagate(network)) {
            // 预算用尽仍未静默：保留中间值继续下一轮（极端的长反馈环），
            // 期间不刷新外观 / 通知邻居，避免把中间态暴露出去。
            requestContinuation(network);
            return;
        }

        // 已收敛：此刻的输出才是「真实输出」，据此做振荡判定。
        // 结算途中「清零 → 逐跳恢复」的中间值一律不计入：否则两个非门顺序相接时，
        // 下游非门每轮结算都会经历 0→15→0 的假翻转，累计到阈值被误破坏。
        // 没有非门、且本轮既未清零、传播中也没有非门翻转时，最终输出必与 baseline 相同，无需全节点扫描。
        if ((resetThisRound || !network.churningNotGates.isEmpty()) && network.hasNotGates) {
            countToggledNotGates(network);
        }
        network.churningNotGates.clear();

        // 只处理输出相对「上次已通知」快照真正变化的节点：视觉 / 持久化 / 邻居通知都不再全网络遍历。
        // 输入面 / 远程面的显示变化另由 evaluateNode 记入 visualDirty（见 markInputVisualDirty）。
        LongOpenHashSet changed = collectChangedNodes(network, network.lastNotified);
        refreshCarrierVisuals(network, changed, visualDirty);

        // 只有输出相对「上次已通知」的状态真正变化时才同步 / 通知。
        if (changed != null) {
            syncToData(network, changed);
            notifyNeighbors(network, changed);
            network.lastNotified = snapshotOutputs(network);
            // 同信道对端不相邻：本网输出变化若源于输入面的重采样（总线上可能已变），显式传导过去。
            dirtyChannelPeers(network);
        }
        network.baseline = null;

        // 有状态门：用本轮收敛后的输入与上次记录比对，处理到达事件。
        if (applyGateArrivals(network)) {
            // 门的输出变了，依赖它们的门需要再结算一轮。
            requestSignalUpdate(network);
        }
    }

    /**
     * 把本次待求值的节点推进到静默：逐节点用**当前**输入求值，输出变了就把它在网内的邻居入队。
     *
     * <p>旧实现「先收全部输入再统一求值」意味着一次握手只前进一跳，链长 L 就要 L 轮，
     * 于是被 {@link LogicGateNetworkManager#MAX_SETTLING_PASSES} 截断成「每 tick 十几跳」，
     * 并让单次传播退化成 O(L²)。工作队列下每个节点只在输入变化时被求值，无环网络（门链）
     * 一次调用内即可传到底，代价与网络长度同阶。</p>
     *
     * @return 是否在预算内达到静默
     */
    private boolean propagate(Network network) {
        propagateQueued.clear();
        propagateQueue.clear();
        for (LongIterator it = network.dirtyNodes.iterator(); it.hasNext(); ) {
            long pos = it.nextLong();
            if (propagateQueued.add(pos)) propagateQueue.add(pos);
        }
        network.dirtyNodes.clear();

        long budget = (long) MAX_SETTLING_EVALUATIONS_PER_NODE * network.nodes.size();
        // 下标即队头：出队只推进 head，不移动元素。
        for (int head = 0; head < propagateQueue.size(); head++) {
            if (budget-- <= 0) {
                // 预算用尽：剩余待求值节点留给下一次结算（下一轮或下一游戏刻）继续推进。
                for (int i = head; i < propagateQueue.size(); i++) {
                    network.dirtyNodes.add(propagateQueue.getLong(i));
                }
                return false;
            }

            long pos = propagateQueue.getLong(head);
            propagateQueued.remove(pos);
            if (!evaluateNode(network, pos)) continue;

            // 只有输出真的变了，邻居才需要重新求值。
            BlockPos blockPos = BlockPos.of(pos);
            for (Direction dir : Direction.values()) {
                long neighbor = blockPos.relative(dir).asLong();
                if (network.nodes.containsKey(neighbor) && propagateQueued.add(neighbor)) {
                    propagateQueue.add(neighbor);
                }
            }
        }
        return true;
    }

    /**
     * 求值单个节点的所有无状态输出面。
     *
     * @return 该节点是否有输出发生变化
     */
    private boolean evaluateNode(Network network, long packedPos) {
        GateNode node = network.nodes.get(packedPos);
        if (node == null) return false;

        BlockPos blockPos = BlockPos.of(packedPos);
        // 区块已卸载的节点不再求值（网络会在卸载时失效）：读方块会强制加载区块。
        if (!level.hasChunkAt(blockPos)) return false;
        BlockState state = level.getBlockState(blockPos);

        // 每节点只取一次镶嵌数据；各方向的输入与设定值直接从它读，避免逐方向重复查 SavedData。
        // 数据必须属于当前方块：方块已被移除 / 记录指向别的方块时不能再当作门求值。
        BlockInlays inlays = logicGateData(level, blockPos, state);
        if (inlays == null) return false;
        Map<Direction, Integer> inputMap = collectNodeInputs(network, blockPos, inlays, scratchInputs);
        markInputVisualDirty(packedPos, blockPos, inlays, inputMap);

        boolean changed = false;
        for (Direction outputDir : Direction.values()) {
            FaceMode gateType = inlays.getFace(outputDir);
            // 有状态门（计数 / 锁存 / 延时）的输出由每 tick 驱动写入，不参与纯函数结算；
            // 远程面不经本方法（它只把总线信号喂给本方块的门，自身不向外输出，见 collectNodeInputs）。
            if (gateType.isStateful()) continue;
            int newSignal = gateType.calculate(inputMap, inlays.getValue(outputDir));
            int oldSignal = node.getOutput(outputDir);
            if (oldSignal != newSignal) {
                node.setOutput(outputDir, newSignal);
                changed = true;
                // 结算途中的翻转只记为「未收敛」候选，不代表真实输出变化：
                // 只有网络最终无法收敛（真正的即时反馈环）才会据其判定振荡（见 runUpdates）。
                if (gateType == FaceMode.NOT_GATE) {
                    network.churningNotGates.add(packedPos);
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
        if (network.baseline == null || !network.hasNotGates) return;
        long now = level.getGameTime();
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            long pos = entry.getLongKey();
            if (destroyed.contains(pos)) continue;
            int before = network.baseline.get(pos);
            GateNode node = entry.getValue();
            BlockPos blockPos = BlockPos.of(pos);
            if (!level.hasChunkAt(blockPos)) continue;
            BlockState state = level.getBlockState(blockPos);
            BlockInlays inlays = logicGateData(level, blockPos, state);
            if (inlays == null) continue;
            for (Direction dir : Direction.values()) {
                if (inlays.getFace(dir) != FaceMode.NOT_GATE) continue;
                int previous = (before >> (dir.ordinal() * 4)) & 0xF;
                if (node.getOutput(dir) != previous) {
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

    /** 快照网络内每个节点六个方向的输出（每节点一个 packed int）。 */
    private static Long2IntOpenHashMap snapshotOutputs(Network network) {
        Long2IntOpenHashMap snapshot = new Long2IntOpenHashMap(network.nodes.size());
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            snapshot.put(entry.getLongKey(), entry.getValue().getPacked());
        }
        return snapshot;
    }

    /** 当前输出与快照不同的节点集合；无变化返回 {@code null}，快照缺失时返回全部节点。 */
    @Nullable
    private static LongOpenHashSet collectChangedNodes(Network network, @Nullable Long2IntOpenHashMap snapshot) {
        if (snapshot == null || snapshot.size() != network.nodes.size()) {
            LongOpenHashSet all = new LongOpenHashSet(network.nodes.size());
            for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
                all.add(it.nextLong());
            }
            return all;
        }
        LongOpenHashSet changed = null;
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            long pos = entry.getLongKey();
            if (snapshot.get(pos) != entry.getValue().getPacked()) {
                if (changed == null) changed = new LongOpenHashSet();
                changed.add(pos);
            }
        }
        return changed;
    }

    /** 把所有节点的输出清零，作为最小不动点迭代的起点；有状态门的输出由 tick 驱动持有，跳过。 */
    private static void resetOutputs(Network network) {
        for (Long2ObjectMap.Entry<GateNode> entry : network.nodes.long2ObjectEntrySet()) {
            GateNode node = entry.getValue();
            int statefulMask = node.statefulMask();
            int keep = 0;
            for (Direction dir : Direction.values()) {
                if ((statefulMask & (1 << dir.ordinal())) != 0) {
                    keep |= 0xF << (dir.ordinal() * 4);
                }
            }
            int packed = node.getPacked();
            int cleared = packed & keep;
            if (cleared != packed) node.setPacked(cleared);
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
     * 收集单个节点各输入面的信号。
     *
     * <p>只把标记为 {@link FaceMode#INPUT} 的方向放入 map（值可为 0），
     * 使 {@link FaceMode#calculate} 能区分「输入面存在但信号为 0」与「无输入面」。</p>
     *
     * <p>{@link FaceMode#DELAY_INPUT_GATE} 也是本方块的一路输入，但它不读邻居：
     * 只有它自己的 1 tick 输入脉冲存在时才出现（见 {@link #delayInputPulses}）。</p>
     *
     * @param inlays 该位置已取好的镶嵌数据，避免逐方向重复查 SavedData
     */
    private Map<Direction, Integer> collectNodeInputs(Network network, BlockPos blockPos, BlockInlays inlays) {
        return collectNodeInputs(network, blockPos, inlays, new EnumMap<>(Direction.class));
    }

    private Map<Direction, Integer> collectNodeInputs(
            Network network, BlockPos blockPos, BlockInlays inlays, Map<Direction, Integer> inputs) {
        inputs.clear();

        // 仅收集「输入面」（INPUT 门标记的方向）、「远程面」与「延时输入门」的信号，其余面不查询邻居。
        // 这样输入信号映射只含这三类面，门的计算逻辑无需再过滤。
        boolean logistics = level.getBlockState(blockPos).getBlock() instanceof LogisticsCarrierBlock;
        long packedPos = blockPos.asLong();
        for (Direction dir : Direction.values()) {
            FaceMode face = inlays.getFace(dir);
            if (face == FaceMode.DELAY_INPUT_GATE) {
                // 延时输入门：它这一路输入不来自邻居，而是自己计时归零时产生的那 1 tick 脉冲
                // （值 = 当初记录的信号）。只在脉冲存在的那一 tick 出现，其余时刻缺席，
                // 恰好对应「该面这一 tick 收到了输入」。
                if (stateData != null && (delayInputPulses.get(packedPos) & (1 << dir.ordinal())) != 0) {
                    inputs.put(dir, stateData.getCount(blockPos, dir));
                }
                continue;
            }
            if (face == FaceMode.REMOTE) {
                // 远程面：把同信道「其它成员」发布的信号当作输入喂给本方块的门。
                // 必须排除本方块自己发布的那一份：否则刚在输入面收下的信号会经总线原样喂回自己，
                // 造成自激，并让有状态门把同一个上升沿数两次。同信道对端之间互发则是总线的本意。
                // 物流载体的远程面归物品传输网（见 LevelTransferNetworks），不参与红石总线。
                if (!logistics) inputs.put(dir, busSignal(inlays.getChannel(dir).key(), blockPos));
                continue;
            }
            if (face != FaceMode.INPUT) continue;
            inputs.put(dir, inputFaceSignal(network, blockPos, dir, inlays));
        }

        return inputs;
    }

    /**
     * 输入面（{@link FaceMode#INPUT}）读到的信号：邻居是网内门则取其输出，否则读跨网门 / 世界红石，
     * 最后按该面设定值取小截断。
     *
     * @param network 该面所在网络；为 {@code null} 时跳过「网内邻居」这条快路（仍能正确求值）
     */
    private int inputFaceSignal(@Nullable Network network, BlockPos blockPos, Direction dir, BlockInlays inlays) {
        // 输入门设定值：只传递 [0, 设定值] 内的信号（取小截断）
        return Math.min(neighborSignal(network, blockPos, dir), inlays.getValue(dir));
    }

    /**
     * 该面朝 {@code dir} 方向的邻居读到的原始红石信号（不做设定值截断）。
     *
     * <p>邻居是网内门取其在网内的输出；是跨网门取其网络输出；否则读世界红石弱 / 强信号的最大值。</p>
     *
     * @param network 该面所在网络；为 {@code null} 时跳过「网内邻居」这条快路（仍能正确求值）
     */
    private int neighborSignal(@Nullable Network network, BlockPos blockPos, Direction dir) {
        BlockPos neighborPos = blockPos.relative(dir);
        long neighbor = neighborPos.asLong();

        if (network != null && network.nodes.containsKey(neighbor)) {
            GateNode neighborNode = network.nodes.get(neighbor);
            return neighborNode.getOutput(dir.getOpposite());
        }
        // 已建网的门（跨网络）直接取网内输出；不是门则按外部方块读世界红石。
        Network neighborNetwork = byGate.get(neighbor);
        if (neighborNetwork != null && neighborNetwork.valid && !neighborNetwork.overflow) {
            return neighborNetwork.getOutputSignal(neighbor, dir.getOpposite());
        }
        // 邻居所在区块没加载时不读它：getSignal / getDirectSignal 同样会经 getBlockState 强制加载。
        // 按无信号处理，区块加载后由 markNetworksAroundChunkDirty 让本网重新采样。
        if (!level.hasChunkAt(neighborPos)) return 0;
        // 外部输入：从世界读取红石信号。
        // vanilla 约定：getSignal/getDirectSignal 的 direction 参数是
        // 「调用者 → 被查询方块」的方向，即本门指向邻居的 dir（而非 dir.getOpposite()）。
        // 中继器等方向敏感信号源按此约定输出，传反则读不到（红石粉不分方向所以不暴露）。
        // 取弱信号与强信号的最大值：红石粉只提供弱信号（getSignal），
        // 而中继器/比较器等只输出强信号（getDirectSignal），两者都需支持。
        int weak = level.getSignal(neighborPos, dir);
        int strong = level.getDirectSignal(neighborPos, dir);
        return Math.max(weak, strong);
    }

    // ==================== 远程总线 ====================

    /** 某远程面所在总线的信号（供外观 / HUD 显示）；非远程面或物流载体返回 0。 */
    int remoteBusSignal(BlockPos pos, Direction dir) {
        if (!level.hasChunkAt(pos)) return 0;
        BlockInlays inlays = BlockInlayManager.get(level, pos);
        if (inlays.getFace(dir) != FaceMode.REMOTE) return 0;
        if (level.getBlockState(pos).getBlock() instanceof LogisticsCarrierBlock) return 0;
        return busSignal(inlays.getChannel(dir).key());
    }

    /** 某信道总线的信号（含查询者自己），供外观 / HUD 显示。 */
    private int busSignal(long channel) {
        return busSignal(channel, null);
    }

    /**
     * 某信道总线的信号：同信道各成员「向外发布的信号」的最大值；无成员为 0。
     *
     * @param exclude 要排除的成员（通常是查询者自己）；{@code null} 表示不排除
     */
    private int busSignal(long channel, @Nullable BlockPos exclude) {
        LongOpenHashSet members = channelMembers.get(channel);
        if (members == null || members.isEmpty()) return 0;
        long excluded = exclude == null ? 0L : exclude.asLong();
        int max = 0;
        for (LongIterator it = members.iterator(); it.hasNext();) {
            long member = it.nextLong();
            if (exclude != null && member == excluded) continue;
            int published = publishedSignal(member);
            if (published > max) max = published;
        }
        return max;
    }

    /**
     * 该位置向总线「发布」的信号：其<b>输入面</b>（{@link FaceMode#INPUT}）读到的信号的最大值。
     *
     * <p>远程面转发的是<b>输入</b>而不是输出：它把本方块输入面收到的信号送上总线，让同信道其它成员的
     * 门以之为输入。所以想发信号就给本方块镶一个输入面 + 远程面，而不是靠输出门。远程面自身既不发布
     * 本方块门的输出、也不驱动相邻红石——它只是一根直连「输入侧」的无线导线。</p>
     */
    private int publishedSignal(long packedPos) {
        BlockPos pos = BlockPos.of(packedPos);
        BlockInlays inlays = BlockInlayManager.get(level, pos);
        int max = 0;
        for (Direction dir : Direction.values()) {
            if (inlays.getFace(dir) != FaceMode.INPUT) continue;
            max = Math.max(max, inputFaceSignal(null, pos, dir, inlays));
        }
        return max;
    }

    /**
     * 把与 {@code network} 同信道（远程总线）的其它网络标脏。
     *
     * <p>远程成员彼此<b>不相邻</b>，普通邻居通知（{@code updateNeighborsAt} / {@code neighborChanged}）
     * 和拓扑重建都到不了同信道的对端网络，因此凡是会影响总线取值的变动，都要显式把对端标脏：
     * </p>
     * <ul>
     *   <li>本网络采到外部信号变化（{@link #signalUpdateAdjacent}、{@code neighborChanged}）；</li>
     *   <li>拓扑增删成员（{@link #buildNetwork} 登记后、{@link #invalidate} 移除后）。</li>
     * </ul>
     *
     * <p>标脏是对端做一次重采样，不会反向再标脏本网络（除非对端输出真的变了并走通知路径），
     * 因此不会无限来回；真正的反馈环由 {@link LogicGateNetworkManager#MAX_SETTLING_PASSES} 兜底。</p>
     */
    void dirtyChannelPeers(Network network) {
        if (nodeChannels.isEmpty()) return;
        ObjectOpenHashSet<Network> peers = null;
        for (LongIterator it = network.nodes.keySet().iterator(); it.hasNext();) {
            long[] channels = nodeChannels.get(it.nextLong());
            if (channels == null) continue;
            for (long channel : channels) {
                LongOpenHashSet members = channelMembers.get(channel);
                if (members == null) continue;
                for (LongIterator mit = members.iterator(); mit.hasNext();) {
                    Network peer = byGate.get(mit.nextLong());
                    if (peer == null || peer == network || !peer.valid || peer.overflow) continue;
                    if (peers == null) peers = new ObjectOpenHashSet<>();
                    peers.add(peer);
                }
            }
        }
        if (peers == null) return;
        for (Network peer : peers) {
            requestSignalUpdate(peer);
        }
    }

    /**
     * 同步到持久化存储
     */
    private void syncToData(Network network, LongOpenHashSet changed) {
        for (LongIterator it = changed.iterator(); it.hasNext();) {
            long packed = it.nextLong();
            GateNode node = network.nodes.get(packed);
            if (node == null) continue;
            BlockPos pos = BlockPos.of(packed);
            for (Direction dir : Direction.values()) {
                int signal = node.getOutput(dir);
                if (signal > 0) persistentData.setSignal(pos, dir, signal);
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
    private void notifyNeighbors(Network network, LongOpenHashSet changed) {
        for (LongIterator it = changed.iterator(); it.hasNext();) {
            BlockPos blockPos = BlockPos.of(it.nextLong());
            BlockState state = level.getBlockState(blockPos);
            // 触发门周围所有方块（含红石粉）的 neighborChanged，使它们检测到门新输出
            level.updateNeighborsAt(blockPos, state.getBlock());
        }
    }

    /**
     * 把网络中载体的 powered 外观刷新为当前信号。
     *
     * <p>外观变化不改拓扑，刷新期间抑制拓扑更新，避免信号每次翻转都重建网络。</p>
     */
    private void refreshCarrierVisuals(
            Network network, @Nullable LongOpenHashSet changed, LongOpenHashSet visualDirty) {
        if ((changed == null || changed.isEmpty()) && visualDirty.isEmpty()) return;
        LogicGateNetworkManager.runSuppressedTopologyChange(() -> {
            if (changed != null) {
                for (LongIterator it = changed.iterator(); it.hasNext();) {
                    refreshCarrierVisual(BlockPos.of(it.nextLong()));
                }
            }
            for (LongIterator it = visualDirty.iterator(); it.hasNext();) {
                refreshCarrierVisual(BlockPos.of(it.nextLong()));
            }
        });
        visualDirty.clear();
    }

    private void refreshCarrierVisual(BlockPos blockPos) {
        if (!level.hasChunkAt(blockPos)) return;
        if (level.getBlockState(blockPos).getBlock() instanceof InlayCarrierBlock) {
            InlayCarrierBlock.refreshState(level, blockPos);
        }
    }

    /**
     * 输入面 / 远程面的显示值变化时把节点标脏，使其外观 / HUD 更新，而不必刷新整张网络。
     *
     * <p>输入门设定值为默认 15 时，输入映射里的值就是实际读到的邻居信号，可与载体缓存精确比较；
     * 设定值更小（被截断）或延时输入 / 远程面时显示值另有来源，保守起见直接标脏。</p>
     */
    private void markInputVisualDirty(
            long packedPos, BlockPos blockPos, BlockInlays inlays, Map<Direction, Integer> inputMap) {
        for (Direction dir : Direction.values()) {
            FaceMode face = inlays.getFace(dir);
            if (face == FaceMode.INPUT) {
                if (inlays.getValue(dir) >= BlockInlays.DEFAULT_VALUE) {
                    if (level.getBlockEntity(blockPos) instanceof InlayCarrierBlockEntity carrier
                            && carrier.signal(dir) != inputMap.getOrDefault(dir, 0)) {
                        visualDirty.add(packedPos);
                        return;
                    }
                } else {
                    visualDirty.add(packedPos);
                    return;
                }
            } else if (face == FaceMode.DELAY_INPUT_GATE || face == FaceMode.REMOTE) {
                visualDirty.add(packedPos);
                return;
            }
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
            // 卸载区块内的节点不再作为重建种子；其余节点的重建留给下一 tick。
            invalidate(network, rebuildSeeds, chunkPos);
        }
        // 这里只排期，绝不重算：本回调发生在主线程 tick / 卸载区块的过程中，同步读方块会强制加载
        // 正在卸载的区块并等待主线程自己，形成自等待死锁（存档卡在 Saving worlds）。
        topologySeeds.addAll(rebuildSeeds);
    }

    /**
     * 让以该区块为中心的 3×3 区块内已建网络的输入重新采样（只标脏，不结算）。
     *
     * <p>跨区块的输入面收不到邻居更新，而区块未加载期间读到的外部信号按 0 处理；
     * 区块加载后必须显式重采样一次，否则门会一直停留在 0。</p>
     */
    void markNetworksAroundChunkDirty(long chunkPos) {
        int centerX = ChunkPos.getX(chunkPos);
        int centerZ = ChunkPos.getZ(chunkPos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ObjectOpenHashSet<Network> networks = byChunk.get(ChunkPos.asLong(centerX + dx, centerZ + dz));
                if (networks == null) continue;
                for (Network network : networks) {
                    if (network.valid && !network.overflow) markSignalDirty(network);
                }
            }
        }
    }

    private static long chunkOf(long packedPos) {
        return ChunkPos.asLong(BlockPos.getX(packedPos) >> 4, BlockPos.getZ(packedPos) >> 4);
    }

    private static void removeChunkPositions(LongOpenHashSet positions, long chunkPos) {
        for (LongIterator it = positions.iterator(); it.hasNext();) {
            if (chunkOf(it.nextLong()) == chunkPos) {
                it.remove();
            }
        }
    }
}