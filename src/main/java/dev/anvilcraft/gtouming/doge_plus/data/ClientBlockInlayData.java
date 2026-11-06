package dev.anvilcraft.gtouming.doge_plus.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;

/**
 * 客户端侧的方块级镶嵌数据：由 {@code BlockInlaySyncPacket} 从服务端同步填充，
 * 供客户端物品实体执行与服务端一致的吸附等行为，避免双端不同步导致的抖动/瞬移。
 */
public class ClientBlockInlayData {

    /** 与服务端一样按打包坐标存：客户端渲染 / 粒子路径上的查询也不必再走 BlockPos 哈希。 */
    private static final Long2ObjectOpenHashMap<BlockInlays> INLAYS = new Long2ObjectOpenHashMap<>();

    public static void put(BlockPos pos, BlockInlays inlays) {
        INLAYS.put(pos.asLong(), inlays);
    }

    public static void remove(BlockPos pos) {
        INLAYS.remove(pos.asLong());
    }

    public static BlockInlays get(BlockPos pos) {
        return INLAYS.getOrDefault(pos.asLong(), BlockInlays.nulls());
    }
}
