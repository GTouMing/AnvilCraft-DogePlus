package dev.anvilcraft.gtouming.doge_plus.api.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像组件：横向渲染全部镶孔——已镶嵌画材料图标，空镶孔画中括号 {@code []}。
 *
 * @param materialStacks 已镶嵌的材料物品（按镶嵌顺序）
 * @param sockets        基材总镶孔数（含空镶孔）
 * @param hasDirection   是否具有「方向」性质；是则在每个镶孔正下方渲染该槽对应方位
 */
public record InlayTooltipComponent(
        List<ItemStack> materialStacks,
        int sockets,
        boolean hasDirection) implements TooltipComponent {
}
