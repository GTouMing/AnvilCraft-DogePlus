package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.TranscendiumInlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.TranscendiumInlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierPhase;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 镶嵌载体 HUD 详细信息（头戴铁砧锤时显示）。
 *
 * <p>信息全部取自客户端已同步的方块状态、{@link dev.anvilcraft.gtouming.doge_plus.data.ClientBlockInlayData}
 * 与载体方块实体（各面红石信号强度），不需要服务端往返。
 * {@code focus} 为视线命中的那个面：非空时只展示该面，为空时退回展示全部面。</p>
 */
public final class InlayInspection {

    private static final String KEY = "tooltip.anvilcraft_doge_plus.inspection.";

    /** 材料统计最多显示的行数，超出以“…另有 N 种”收尾。 */
    private static final int MAX_TALLY_LINES = 8;

    /** 超限载体单个面的槽位步长（槽位下标 i 对应面 {@code i % 6}）。 */
    private static final int FACES = 6;

    private InlayInspection() {
    }

    /**
     * 普通镶嵌载体。
     *
     * @param focus   视线命中的面；为空时列出全部已镶嵌面
     * @param carrier 客户端方块实体，提供各面已同步的红石信号强度
     */
    public static List<Component> carrierTooltip(
            Level level, BlockPos pos, BlockState state, @Nullable Direction focus, InlayCarrierBlockEntity carrier) {
        List<Component> lines = new ArrayList<>();
        lines.add(title(state));

        BlockInlays data = BlockInlayManager.get(level, pos);
        List<InlayEntry> inlays = data.inlays();
        int used = activeCount(state);

        if (focus != null) {
            addFace(lines, state, data, inlays, focus, carrier);
        } else if (used == 0) {
            lines.add(Component.translatable(KEY + "empty").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            for (Direction direction : Direction.values()) {
                if (state.getValue(InlayCarrierBlock.property(direction)).isInlaid()) {
                    addFace(lines, state, data, inlays, direction, carrier);
                }
            }
        }
        lines.add(Component.translatable(KEY + "sockets", used, InlayCarrierBlock.SOCKETS)
                .withStyle(ChatFormatting.GRAY));
        return lines;
    }

    /**
     * 超限镶嵌载体。
     *
     * @param focus 视线命中的面；为空时统计全部镶孔
     */
    public static List<Component> transcendiumTooltip(
            BlockState state, List<InlayEntry> inlays, @Nullable Direction focus) {
        List<Component> lines = new ArrayList<>();
        lines.add(title(state));
        lines.add(Component.translatable(KEY + "phase", phaseName(state.getValue(TranscendiumInlayCarrierBlock.PHASE)))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable(KEY + "sockets", inlays.size(), TranscendiumInlayCarrierBlockEntity.MAX_SOCKETS)
                .withStyle(ChatFormatting.GRAY));
        if (inlays.isEmpty()) {
            lines.add(Component.translatable(KEY + "empty").withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        Map<ResourceLocation, Integer> counts = focus == null ? tally(inlays, -1) : tally(inlays, focus.ordinal());
        if (counts.isEmpty()) {
            lines.add(Component.translatable(KEY + "face_empty", faceName(focus)).withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }
        int shown = 0;
        for (Map.Entry<ResourceLocation, Integer> entry : counts.entrySet()) {
            if (shown == MAX_TALLY_LINES) {
                lines.add(Component.translatable(KEY + "more", counts.size() - MAX_TALLY_LINES)
                        .withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            shown++;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.getKey()));
            lines.add(Component.translatable(KEY + "tally", stack.getHoverName(), entry.getValue())
                    .withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    // ==================== 内部工具 ====================

    /** 该面的两行：属性行 +（已镶嵌时）信号强度行；未镶嵌时只给占位提示。 */
    private static void addFace(
            List<Component> lines,
            BlockState state,
            BlockInlays data,
            List<InlayEntry> inlays,
            Direction direction,
            InlayCarrierBlockEntity carrier) {
        if (!state.getValue(InlayCarrierBlock.property(direction)).isInlaid()) {
            lines.add(Component.translatable(KEY + "face_empty", faceName(direction))
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        lines.add(faceLine(data, inlays, direction));
        lines.add(Component.translatable(KEY + "signal", carrier.signal(direction)).withStyle(ChatFormatting.GRAY));
        LogicGateType gate = data.getGateType(direction);
        if (gate.isSettable()) {
            lines.add(Component.translatable(KEY + "value", data.getValue(direction))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (gate == LogicGateType.COUNTER_GATE) {
            lines.add(Component.translatable(KEY + "counted", carrier.runtime(direction))
                    .withStyle(ChatFormatting.GRAY));
        } else if (gate == LogicGateType.DELAY_GATE) {
            lines.add(Component.translatable(KEY + "elapsed", carrier.runtime(direction))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** 单个面的属性行：逻辑门优先，材料最后。 */
    private static Component faceLine(BlockInlays data, List<InlayEntry> inlays, Direction direction) {
        int slot = direction.ordinal();
        Component material = slot < inlays.size() && !inlays.get(slot).isEmpty()
                ? inlays.get(slot).toItemStack().getHoverName()
                : Component.literal("?");
        LogicGateType gate = data.getGateType(direction);
        MutableComponent line = gate == LogicGateType.NONE
                ? Component.translatable(KEY + "line_material", faceName(direction), material)
                : Component.translatable(KEY + "line_gate", faceName(direction), gateName(gate), material);
        return line.withStyle(ChatFormatting.GRAY);
    }

    private static int activeCount(BlockState state) {
        int used = 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(InlayCarrierBlock.property(direction)).isInlaid()) used++;
        }
        return used;
    }

    /**
     * 按材料统计镶孔占用。
     *
     * @param faceOrdinal 只统计该面（槽位下标 {@code i % 6 == faceOrdinal}）；为 {@code -1} 时统计全部
     */
    private static Map<ResourceLocation, Integer> tally(List<InlayEntry> inlays, int faceOrdinal) {
        Map<ResourceLocation, Integer> counts = new LinkedHashMap<>();
        int step = faceOrdinal < 0 ? 1 : FACES;
        int start = Math.max(faceOrdinal, 0);
        for (int i = start; i < inlays.size(); i += step) {
            InlayEntry entry = inlays.get(i);
            if (entry.isEmpty()) continue;
            counts.merge(entry.id(), 1, Integer::sum);
        }
        return counts;
    }

    private static Component title(BlockState state) {
        return Component.translatable(state.getBlock().getDescriptionId()).withStyle(ChatFormatting.GOLD);
    }

    private static Component faceName(Direction direction) {
        return Component.translatable(KEY + "dir." + direction.getName());
    }

    private static Component gateName(LogicGateType type) {
        return Component.translatable(KEY + "gate." + type.getSerializedName());
    }

    private static Component phaseName(CarrierPhase phase) {
        return Component.translatable(KEY + "phase." + phase.getSerializedName());
    }
}
