package dev.anvilcraft.gtouming.doge_plus.logic;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;

/**
 * 逻辑门网络 - 连通分量
 */
final class Network {
    /** 网络中的所有门节点 */
    final Long2ObjectLinkedOpenHashMap<GateNode> nodes;

    /** 是否因规模过大被截断 */
    final boolean overflow;

    /** 网络内是否存在有状态门（计数 / 锁存 / 延时），用于每 tick 只推进这些网络。 */
    final boolean hasStatefulGates;

    /** 网络内是否存在非门；没有非门时无需做振荡判定。 */
    final boolean hasNotGates;

    /** 网络内带非门的节点位置；振荡判定只遍历这些节点，不再扫整网。 */
    final LongOpenHashSet notGateNodes;

    /** 网络内带非 NONE 面且「输出由时序状态驱动」的节点；到达事件只遍历这些节点。 */
    final LongOpenHashSet statefulNodes;

    /** 网络内参与红石总线的远程面节点；信道变化只标脏这些节点。 */
    final LongOpenHashSet remoteNodes;

    /**
     * 网络的门间依赖图是否存在环。
     *
     * <p>无环时每个门的输出都是输入的唯一纯函数，从当前输出继续求值即可到达同一不动点，
     * 不必每轮清零重算；有环时必须从全 0 迭代，否则纯反馈环会自锁在非零值。</p>
     */
    final boolean hasFeedback;

    /** 本轮求值中被改写过的节点（含清零、时序门写回）；用于增量对比「已通知」快照。 */
    final LongOpenHashSet touchedNodes = new LongOpenHashSet();

    /** 网络跨越的区块 */
    final LongOpenHashSet chunks = new LongOpenHashSet();

    /** 是否有效 */
    boolean valid = true;

    /**
     * 结算前是否先把所有输出清零，以组合逻辑语义求最小不动点。
     * <p>网络没有时序元件，输出只应由主输入决定；若沿用作初值，纯反馈环会自锁在非零值。</p>
     */
    boolean needsReset = true;

    /** 本轮结算开始前的输出快照（位置 -> 6 面 4-bit 压缩），用于判断收敛后是否真的变化。 */
    Long2IntOpenHashMap baseline;

    /**
     * 上一次已通知邻居的输出快照（位置 -> 6 面 4-bit 压缩）。
     *
     * <p>通知判定必须与它比较，而不是与 {@link #baseline}：{@code baseline} 每轮结算开始时都会重建，
     * 会把「上一轮结算末尾写入的有状态门输出」吸收进去，导致计数 / 锁存 / 延时门的输出变化
     * 永远不通知邻居（红石读不到）。本快照只在真正通知后更新，因此任何写入路径的变化都会被补上通知。</p>
     */
    Long2IntOpenHashMap lastNotified;

    /**
     * 本轮结算（可能跨多轮 / 跨游戏刻）内输出发生过变化的非门位置。
     *
     * <p>结算途中的翻转只是「未收敛」的候选：组合逻辑从全 0 迭代求不动点时中间值不代表真实输出。
     * 只有网络最终仍未收敛（即真正的即时反馈环）才会据此判定振荡。</p>
     */
    final LongOpenHashSet churningNotGates = new LongOpenHashSet();

    /**
     * 本轮还需要重新求值的节点（工作队列的入口）。
     *
     * <p>一跳不再等于一轮：求值后只有输出真正变化的节点才会把邻居重新入队，
     * 因此无环网络的代价与网络长度同阶，链长不再受「一轮一跳」的轮数上限限制。
     * 预算用尽时剩余节点留在这里，交给下一次结算（下一轮或下一游戏刻）继续。</p>
     */
    final LongOpenHashSet dirtyNodes = new LongOpenHashSet();

    /**
     * 整网都需要重新求值（主输入重采样 / 初始化）。
     *
     * <p>用标记代替「把所有节点塞进 {@link #dirtyNodes}」：同一 tick 里多个区块加载 / 多次标脏
     * 会重复调用，标记只有一次开销，真正的遍历在传播开始时做一次。</p>
     */
    boolean dirtyAll = true;

    Network(Long2ObjectLinkedOpenHashMap<GateNode> nodes, boolean overflow, boolean hasStatefulGates, boolean hasNotGates,
            LongOpenHashSet notGateNodes, LongOpenHashSet statefulNodes, LongOpenHashSet remoteNodes,
            boolean hasFeedback) {
        this.nodes = nodes;
        this.overflow = overflow;
        this.hasStatefulGates = hasStatefulGates;
        this.hasNotGates = hasNotGates;
        this.notGateNodes = notGateNodes;
        this.statefulNodes = statefulNodes;
        this.remoteNodes = remoteNodes;
        this.hasFeedback = hasFeedback;

        // 计算区块归属
        for (LongIterator it = nodes.keySet().iterator(); it.hasNext();) {
            long pos = it.nextLong();
            chunks.add(ChunkPos.asLong(BlockPos.getX(pos) >> 4, BlockPos.getZ(pos) >> 4));
        }
    }

    int getOutputSignal(long pos, Direction direction) {
        GateNode node = nodes.get(pos);
        return node == null ? 0 : node.getOutput(direction);
    }

    void setOutputSignal(long pos, Direction direction, int signal) {
        GateNode node = nodes.get(pos);
        if (node != null) {
            node.setOutput(direction, signal);
            touchedNodes.add(pos);
        }
    }
}

/**
 * 逻辑门节点 - 单扇门的数据
 *
 * @param data          门输出存储
 * @param statefulMask  该位置上有状态门（计数 / 锁存 / 延时）的方向位掩码
 * @param outputMask    该位置上「会向外输出信号」的方向位（非门 / 与门 / 输出门 / 三种输出型状态门）
 * @param consumerMask  该位置上「读取邻居输出」的方向位（输入门 / 延时输入门）；建网时用于依赖图判环
 * @param block         该位置的方块（建网时缓存，通知邻居时不必再查方块状态）
 * @param logistics     该位置是否为物流载体（同上；求值热路径不再查方块状态）
 * @param carrier       该位置是否为镶嵌载体（同上，供锁存门写回设定值用）
 */
record GateNode(LogicGateOutputData data, BlockPos pos, int statefulMask, int outputMask, int consumerMask,
                Block block, boolean logistics, boolean carrier) {

    int getOutput(Direction direction) {
        return data == null ? 0 : data.getSignal(pos, direction);
    }

    void setOutput(Direction direction, int signal) {
        if (data == null) return;
        data.setSignal(pos, direction, signal);
    }

    /** 6 面输出压缩成一个 int（每面 4 bits），供快照 / 清零整块读写。 */
    int getPacked() {
        return data == null ? 0 : data.getPacked(pos);
    }

    void setPacked(int packed) {
        if (data == null) return;
        data.setPacked(pos, packed);
    }
}