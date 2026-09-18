package dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip;

import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像渲染：镶孔按每行 6 个排布，超过则换行——已镶嵌画材料图标，
 * 空镶孔渲染中括号 {@code []}。
 */
public class ClientInlayTooltip implements ClientTooltipComponent {

    private static final int SLOT_SIZE = 18;
    /** 每行最多镶孔数，超过则换行。 */
    private static final int COLUMNS = 6;

    private final List<ItemStack> materialStacks;
    private final int sockets;

    public ClientInlayTooltip(InlayTooltipComponent tooltip) {
        this.materialStacks = tooltip.materialStacks();
        this.sockets = tooltip.sockets();
    }

    private int rows() {
        return (this.sockets + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public int getHeight() {
        return this.rows() * SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return Math.min(this.sockets, COLUMNS) * SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        for (int i = 0; i < this.sockets; i++) {
            int slotX = x + (i % COLUMNS) * SLOT_SIZE;
            int slotY = y + (i / COLUMNS) * SLOT_SIZE;

            if (i < this.materialStacks.size() && !this.materialStacks.get(i).isEmpty()) {
                ItemStack renderStack = getRenderStack(this.materialStacks.get(i));
                guiGraphics.renderItem(renderStack, slotX + 1, slotY + 1);
                guiGraphics.renderItemDecorations(font, renderStack, slotX + 1, slotY + 1);
            } else {
                guiGraphics.drawString(font, " [] ", slotX + 1, slotY + 5, 11184810);
            }
        }
    }

    /**
     * 获取用于渲染的物品栈
     * 如果是药水且存在 InlayEntry，则恢复药水组件
     */
    private ItemStack getRenderStack(ItemStack stack) {
        if (stack.isEmpty()) return stack;


        InlayEntry entry = InlayEntry.fromItemStack(stack);
        if (entry.isEmpty()) return stack;

        // 只有 EFFECT 属性的才需要恢复药水
        if (!entry.containsAttributes(InlayProperty.EFFECT)) return stack;

        // 从 InlayEntry 恢复 ItemStack（包含药水组件）
        ItemStack restored = entry.toItemStack();
        if (!restored.isEmpty()) {
            return restored;
        }

        return stack;
    }
}
