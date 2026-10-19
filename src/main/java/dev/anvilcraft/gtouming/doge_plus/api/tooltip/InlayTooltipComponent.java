package dev.anvilcraft.gtouming.doge_plus.api.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像组件：按行渲染全部镶孔——每个镶孔画槽位贴图，已镶嵌再叠材料图标。
 *
 * @param materialStacks 已镶嵌的材料物品（按镶嵌顺序）
 * @param sockets        基材总镶孔数（含空镶孔）
 */
public record InlayTooltipComponent(
        List<ItemStack> materialStacks,
        int sockets) implements TooltipComponent {
}
