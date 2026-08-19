package dev.anvilcraft.gtouming.doge_plus.data;

import net.minecraft.core.BlockPos;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端侧的方块级镶嵌数据：由 {@code BlockInlaySyncPacket} 从服务端同步填充，
 * 供客户端物品实体执行与服务端一致的吸附等行为，避免双端不同步导致的抖动/瞬移。
 */
public class ClientBlockInlayData {

    private static final Map<BlockPos, BlockInlays> INLAYS = new HashMap<>();

    public static void put( BlockPos pos, BlockInlays inlays) {
        INLAYS.put(pos.immutable(), inlays);
    }

    public static void remove(BlockPos pos) {
        INLAYS.remove(pos);
    }

    public static BlockInlays get(BlockPos pos) {
        return INLAYS.getOrDefault(pos, BlockInlays.nulls());
    }
}
