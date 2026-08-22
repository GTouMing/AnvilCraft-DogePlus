package dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip;

import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.util.DirectionsOrder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像渲染：所有镶孔横向排成一行——已镶嵌画材料图标，空镶孔渲染中括号 {@code []}。
 * 具有「方向」性质时，在每个镶孔正下方渲染该槽对应的方位（{@link DirectionsOrder} 顺序）。
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
        List<Direction> order = DirectionsOrder.getOrder();
        for (int i = 0; i < this.sockets; i++) {
            int slotX = x + i * SLOT_SIZE;
            if (i < this.materialStacks.size() && !this.materialStacks.get(i).isEmpty()) {
                guiGraphics.renderItem(this.materialStacks.get(i), slotX + 1, y + 1);
            } else {
                guiGraphics.drawString(font, " [] ", slotX + 1, y + 5, 11184810);
            }
            // 具有方向性质时，在镶孔（材料图标或空镶孔）正下方渲染该槽对应方位
            if (this.hasDirection && i < order.size()) {
                Direction dir = order.get(i);
                Component label = Component.literal(dir.getName().substring(0, 1).toUpperCase());
                guiGraphics.drawString(font, label, slotX + 7, y + SLOT_SIZE, 5592405);
            }
        }
    }
}
