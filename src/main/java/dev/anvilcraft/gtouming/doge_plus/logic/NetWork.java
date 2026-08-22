package dev.anvilcraft.gtouming.doge_plus.logic;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
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

    /** 网络跨越的区块 */
    final LongOpenHashSet chunks = new LongOpenHashSet();

    /** 是否有效 */
    boolean valid = true;

    Network(Long2ObjectLinkedOpenHashMap<GateNode> nodes, boolean overflow) {
        this.nodes = nodes;
        this.overflow = overflow;

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
 * @param data 门配置（类型、方向等）
 */
record GateNode(LogicGateOutputData data, BlockPos pos) {

    int getOutput(Direction direction) {
        return data == null ? 0 : data.getSignal(pos, direction);
    }

    void setOutput(Direction direction, int signal) {
        if (data == null) return;
        data.setSignal(pos, direction, signal);
    }
}