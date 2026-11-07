package dev.anvilcraft.gtouming.doge_plus.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

/**
 * 客户端侧的方块级镶嵌数据：由 {@code BlockInlaySyncPacket} 从服务端同步填充，
 * 供客户端物品实体执行与服务端一致的吸附等行为，避免双端不同步导致的抖动/瞬移。
 */
public class ClientBlockInlayData {

    /** 与服务端一样按打包坐标存：客户端渲染 / 粒子路径上的查询也不必再走 BlockPos 哈希。 */
    private static final Long2ObjectOpenHashMap<BlockInlays> INLAYS = new Long2ObjectOpenHashMap<>();

    /**
     * 数据变化过、但还没通知渲染器重建模型的位置。
     *
     * <p>镶嵌载体的 wire 样式（逻辑 / 物流）由动态模型在区块重建时读本表决定；同步包到达后
     * 只改数据不会自动重建区块，必须由客户端 tick 统一标脏。</p>
     */
    private static final LongOpenHashSet DIRTY_POSITIONS = new LongOpenHashSet();
    private static final long[] NO_POSITIONS = new long[0];

    public static synchronized void put(BlockPos pos, BlockInlays inlays) {
        INLAYS.put(pos.asLong(), inlays);
        DIRTY_POSITIONS.add(pos.asLong());
    }

    public static synchronized void remove(BlockPos pos) {
        INLAYS.remove(pos.asLong());
        DIRTY_POSITIONS.add(pos.asLong());
    }

    public static synchronized BlockInlays get(BlockPos pos) {
        return INLAYS.getOrDefault(pos.asLong(), BlockInlays.nulls());
    }

    /** 取走本 tick 需要重建模型的坐标；没有时返回空数组。 */
    public static synchronized long[] drainDirtyPositions() {
        if (DIRTY_POSITIONS.isEmpty()) return NO_POSITIONS;
        long[] positions = DIRTY_POSITIONS.toLongArray();
        DIRTY_POSITIONS.clear();
        return positions;
    }
}
