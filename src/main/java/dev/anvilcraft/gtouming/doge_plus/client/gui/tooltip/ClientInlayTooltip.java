package dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip;

import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像渲染：所有镶孔横向排成一行——已镶嵌画材料图标，空镶孔渲染中括号 {@code []}。
 */
public class ClientInlayTooltip implements ClientTooltipComponent {

    private static final int SLOT_SIZE = 18;

    private final List<ItemStack> materialStacks;
    private final int sockets;

    public ClientInlayTooltip(InlayTooltipComponent tooltip) {
        this.materialStacks = tooltip.materialStacks();
        this.sockets = tooltip.sockets();
    }

    @Override
    public int getHeight() {
        return SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return this.sockets * SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        for (int i = 0; i < this.sockets; i++) {
            int slotX = x + i * SLOT_SIZE;
            if (i < this.materialStacks.size() && !this.materialStacks.get(i).isEmpty()) {
                guiGraphics.renderItem(this.materialStacks.get(i), slotX + 1, y + 1);
            } else {
                guiGraphics.drawString(font, "[]", slotX + 1, y + 5, 11184810);
            }
        }
    }
}
