package dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip;

import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像渲染：所有镶孔横向排成一行——已镶嵌画材料图标，空镶孔渲染中括号 {@code []}。
 * 具有「方向」性质时，在每个镶孔正下方渲染该槽对应的方位。
 */
public class ClientInlayTooltip implements ClientTooltipComponent {

    private static final int SLOT_SIZE = 18;
    private static final int TEXT_HEIGHT = 9;

    private final List<ItemStack> materialStacks;
    private final int sockets;
    private final boolean hasDirection;

    public ClientInlayTooltip(InlayTooltipComponent tooltip) {
        this.materialStacks = tooltip.materialStacks();
        this.sockets = tooltip.sockets();
        this.hasDirection = tooltip.hasDirection();
    }

    @Override
    public int getHeight() {
        return SLOT_SIZE + (this.hasDirection ? TEXT_HEIGHT : 0);
    }

    @Override
    public int getWidth(Font font) {
        return this.sockets * SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        List<Direction> order = List.of(Direction.values());
        for (int i = 0; i < this.sockets; i++) {
            int slotX = x + i * SLOT_SIZE;

            if (i < this.materialStacks.size() && !this.materialStacks.get(i).isEmpty()) {
                ItemStack renderStack = getRenderStack(this.materialStacks.get(i));
                guiGraphics.renderItem(renderStack, slotX + 1, y + 1);
                guiGraphics.renderItemDecorations(font, renderStack, slotX + 1, y + 1);
            } else {
                guiGraphics.drawString(font, " [] ", slotX + 1, y + 5, 11184810);
            }

            if (this.hasDirection && i < order.size()) {
                Direction dir = order.get(i);
                Component label = Component.literal(dir.getName().substring(0, 1).toUpperCase());
                guiGraphics.drawString(font, label, slotX + 7, y + SLOT_SIZE, 5592405);
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
