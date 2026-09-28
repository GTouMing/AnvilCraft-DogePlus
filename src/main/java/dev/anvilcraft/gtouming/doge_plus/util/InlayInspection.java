package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.PipeCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 镶嵌载体 HUD 详细信息（头戴铁砧锤时显示）。
 *
 * <p>信息全部取自客户端已同步的方块状态、{@link BlockInlayManager}（客户端镜像）与载体方块实体
 * （各面红石信号强度），不需要服务端往返。
 * {@code focus} 为视线命中的那个面：非空时只展示该面，为空时退回展示全部面。</p>
 */
public final class InlayInspection {

    private static final String KEY = "tooltip.anvilcraft_doge_plus.inspection.";

    private InlayInspection() {
    }

    /**
     * 镶嵌载体。
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
        // 管道「存入」面：物流量在上、过滤在下（与世界渲染里「物品层在数字层之下」对应）。
        // 镶嵌载体的存入 / 取出来自材料，没有这两项参数，因此只对管道载体显示。
        if (state.getBlock() instanceof PipeCarrierBlock && data.getFace(direction) == FaceMode.INSERT) {
            lines.add(Component.translatable(KEY + "throughput", data.getThroughput(direction))
                    .withStyle(ChatFormatting.GRAY));
            ItemStack filter = data.getFilter(direction);
            lines.add(Component.translatable(KEY + "filter",
                            filter.isEmpty() ? Component.translatable(KEY + "filter_none") : filter.getHoverName())
                    .withStyle(ChatFormatting.GRAY));
        }
        FaceMode mode = data.getFace(direction);
        if (mode.isSettable()) {
            lines.add(Component.translatable(KEY + "value", data.getValue(direction))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (mode == FaceMode.COUNTER_GATE) {
            lines.add(Component.translatable(KEY + "counted", carrier.runtime(direction))
                    .withStyle(ChatFormatting.GRAY));
        } else if (mode == FaceMode.DELAY_GATE) {
            lines.add(Component.translatable(KEY + "elapsed", carrier.runtime(direction))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** 单个面的属性行：逻辑门 / 搬运角色优先，材料最后。 */
    private static Component faceLine(BlockInlays data, List<InlayEntry> inlays, Direction direction) {
        int slot = direction.ordinal();
        boolean hasMaterial = slot < inlays.size() && !inlays.get(slot).isEmpty();
        Component material = hasMaterial
                ? inlays.get(slot).toItemStack().getHoverName()
                : Component.literal("?");
        FaceMode mode = data.getFace(direction);
        // 逻辑载体没有镶孔材料：已编程的面只显示门名（复用两参数的行文案，不带材料）。
        if (mode.isRedstone() && !hasMaterial) {
            return Component.translatable(KEY + "line_material", faceName(direction), modeName(mode))
                    .withStyle(ChatFormatting.GRAY);
        }
        // 管道载体既没有镶孔也没有红石门：已编程的面显示搬运角色。少了这一支就会掉到下面的材料分支，
        // 把空材料渲染成 "?"（管道面永远没有材料）。
        if (mode.isTransfer() && !hasMaterial) {
            return Component.translatable(KEY + "line_material", faceName(direction), modeName(mode))
                    .withStyle(ChatFormatting.GRAY);
        }
        // 镶嵌载体：门种（若该面的材料是门）与材料名并列；搬运材料走纯材料行。
        Component line = mode.isRedstone()
                ? Component.translatable(KEY + "line_gate", faceName(direction), modeName(mode), material)
                : Component.translatable(KEY + "line_material", faceName(direction), material);
        return line.copy().withStyle(ChatFormatting.GRAY);
    }

    private static int activeCount(BlockState state) {
        int used = 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(InlayCarrierBlock.property(direction)).isInlaid()) used++;
        }
        return used;
    }

    private static Component title(BlockState state) {
        return Component.translatable(state.getBlock().getDescriptionId()).withStyle(ChatFormatting.GOLD);
    }

    private static Component faceName(Direction direction) {
        return Component.translatable(KEY + "dir." + direction.getName());
    }

    /** 面属性的显示名：红石门走 gate.* 文案，搬运角色走 transfer.* 文案。 */
    private static Component modeName(FaceMode mode) {
        return Component.translatable(KEY + (mode.isRedstone() ? "gate." : "transfer.") + mode.getSerializedName());
    }
}
