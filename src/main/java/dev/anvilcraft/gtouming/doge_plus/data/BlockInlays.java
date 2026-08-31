package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BlockInlays(Block block, List<InlayEntry> inlays, Map<Direction, LogicGateType> directions) {
    public static final StreamCodec<ByteBuf, BlockInlays> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.map(
                    BuiltInRegistries.BLOCK::get,
                    BuiltInRegistries.BLOCK::getKey
            ), BlockInlays::block,
            ByteBufCodecs.collection(ArrayList::new, InlayEntry.STREAM_CODEC), BlockInlays::inlays,
            ByteBufCodecs.map(
                    HashMap::new,
                    Direction.STREAM_CODEC,
                    LogicGateType.STREAM_CODEC
            ), BlockInlays::directions,
            BlockInlays::new
    );

    public static BlockInlays nulls() {
        return new BlockInlays(Blocks.AIR, List.of(), Map.of());
    }

    /**
     * 从镶嵌材料列表生成方向-逻辑门映射。
     * 按东南西北上下顺序遍历槽位，检测每个槽位的材料是否包含门逻辑属性。
     */
    public static BlockInlays fromInlays(Block block, List<InlayEntry> inlays) {
        // 方向顺序：东、南、西、上、北、下（对应槽位 0-5）
        List<Direction> directionOrder = List.of(Direction.values());

        Map<Direction, LogicGateType> directions = new HashMap<>();

        for (Direction dir : directionOrder) {
            directions.put(dir, LogicGateType.NONE);
        }

        // 遍历镶孔，填充对应方向的门类型
        for (int i = 0; i < Math.min(inlays.size(), directionOrder.size()); i++) {
            InlayEntry entry = inlays.get(i);
            Direction dir = directionOrder.get(i);

            // 获取材料定义
            LogicGateType gateType = detectGateType(entry);
            // 无论 gateType 是否为 NONE，都更新到 Map 中
            directions.put(dir, gateType);
        }

        return new BlockInlays(block, inlays, directions);
    }

    /**
     * 检测材料包含的门逻辑类型。
     * 优先级：非门 > 与门 > 或门 > 红石 > 方向
     */
    private static LogicGateType detectGateType(InlayEntry entry) {
        if (entry.containsAttributes(InlayProperty.NOT_GATE)) {
            return LogicGateType.NOT_GATE;
        }
        if (entry.containsAttributes(InlayProperty.AND_GATE)) {
            return LogicGateType.AND_GATE;
        }
        if (entry.containsAttributes(InlayProperty.OR_GATE)) {
            return LogicGateType.OR_GATE;
        }
        if (entry.containsAttributes(InlayProperty.OUTPUT)) {
            return LogicGateType.OUTPUT;
        }
        if (entry.containsAttributes(InlayProperty.INPUT)) {
            return LogicGateType.INPUT;
        }
        return LogicGateType.NONE;
    }

    /**
     * 获取指定方向的门逻辑类型。
     */
    public LogicGateType getGateType(Direction direction) {
        return directions.getOrDefault(direction, LogicGateType.NONE);
    }
}
