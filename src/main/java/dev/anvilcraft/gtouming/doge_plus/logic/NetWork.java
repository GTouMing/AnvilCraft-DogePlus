package dev.anvilcraft.gtouming.doge_plus.logic;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;

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

    /** 网络跨越的区块 */
    final LongOpenHashSet chunks = new LongOpenHashSet();

    /** 是否有效 */
    boolean valid = true;

    /**
     * 结算前是否先把所有输出清零，以组合逻辑语义求最小不动点。
     * <p>网络没有时序元件，输出只应由主输入决定；若沿用作初值，纯反馈环会自锁在非零值。</p>
     */
    boolean needsReset = true;

    /** 本轮结算开始前的输出快照（下标同 {@link Direction#ordinal()}），用于判断收敛后是否真的变化。 */
    Long2ObjectOpenHashMap<int[]> baseline;

    /**
     * 本轮结算（可能跨多轮 / 跨游戏刻）内输出发生过变化的非门位置。
     *
     * <p>结算途中的翻转只是「未收敛」的候选：组合逻辑从全 0 迭代求不动点时中间值不代表真实输出。
     * 只有网络最终仍未收敛（即真正的即时反馈环）才会据此判定振荡。</p>
     */
    final LongOpenHashSet churningNotGates = new LongOpenHashSet();

    Network(Long2ObjectLinkedOpenHashMap<GateNode> nodes, boolean overflow, boolean hasStatefulGates) {
        this.nodes = nodes;
        this.overflow = overflow;
        this.hasStatefulGates = hasStatefulGates;

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
        }
    }
}

/**
 * 逻辑门节点 - 单扇门的数据
 *
 * @param data         门输出存储
 * @param statefulMask 该位置上有状态门（计数 / 锁存 / 延时）的方向位掩码
 */
record GateNode(LogicGateOutputData data, BlockPos pos, int statefulMask) {

    int getOutput(Direction direction) {
        return data == null ? 0 : data.getSignal(pos, direction);
    }

    void setOutput(Direction direction, int signal) {
        if (data == null) return;
        data.setSignal(pos, direction, signal);
    }
}