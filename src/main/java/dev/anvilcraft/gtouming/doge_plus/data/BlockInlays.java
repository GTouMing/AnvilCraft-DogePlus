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

/**
 * 方块级镶嵌数据：材料列表 + 各面门类型 + 各面逻辑门设定值。
 *
 * @param values 各面逻辑门的设定值（仅输入门 / 输出门有意义）；缺省即 {@link #DEFAULT_VALUE}
 */
public record BlockInlays(
        Block block,
        List<InlayEntry> inlays,
        Map<Direction, LogicGateType> directions,
        Map<Direction, Integer> values) {

    /** 逻辑门设定值的默认值（也是上限）。 */
    public static final int DEFAULT_VALUE = 15;

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
            ByteBufCodecs.map(
                    HashMap::new,
                    Direction.STREAM_CODEC,
                    ByteBufCodecs.VAR_INT
            ), BlockInlays::values,
            BlockInlays::new
    );

    public static BlockInlays nulls() {
        return new BlockInlays(Blocks.AIR, List.of(), Map.of(), Map.of());
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

            // 空镶孔（取出过的槽位）不携带任何门逻辑
            if (entry.isEmpty()) {
                directions.put(dir, LogicGateType.NONE);
                continue;
            }

            // 获取材料定义
            LogicGateType gateType = detectGateType(entry);
            // 无论 gateType 是否为 NONE，都更新到 Map 中
            directions.put(dir, gateType);
        }

        return new BlockInlays(block, inlays, directions, Map.of());
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
        if (entry.containsAttributes(InlayProperty.COUNTER_GATE)) {
            return LogicGateType.COUNTER_GATE;
        }
        if (entry.containsAttributes(InlayProperty.LATCH_GATE)) {
            return LogicGateType.LATCH_GATE;
        }
        if (entry.containsAttributes(InlayProperty.DELAY_GATE)) {
            return LogicGateType.DELAY_GATE;
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

    /** 指定面逻辑门的设定值；未设定时为 {@link #DEFAULT_VALUE}。 */
    public int getValue(Direction direction) {
        return values.getOrDefault(direction, DEFAULT_VALUE);
    }

    /** 返回把指定面设定值改为 {@code value} 的新实例；等于默认值时移除该键。 */
    public BlockInlays withValue(Direction direction, int value) {
        Map<Direction, Integer> updated = new HashMap<>(values);
        if (value == DEFAULT_VALUE) {
            updated.remove(direction);
        } else {
            updated.put(direction, value);
        }
        return withValues(updated);
    }

    /** 返回把各面设定值整体替换为 {@code newValues}（缺省的面走默认值）的新实例。 */
    public BlockInlays withValues(Map<Direction, Integer> newValues) {
        return new BlockInlays(block, inlays, directions, Map.copyOf(newValues));
    }
}
