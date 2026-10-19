package dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip;

import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 镶嵌信息的 tooltip 图像渲染：镶孔按每行 6 个排布，超过则换行；每个镶孔画原版槽位贴图，
 * 已镶嵌的再叠上材料图标，空镶孔表现为空槽位。
 *
 * <p>镶孔数由数据包定义，可能远大于实际操作数量，因此图像最多只画
 * {@link #MAX_VISIBLE_SOCKETS} 个（已镶嵌的排在前，优先显示），其余由调用方补一行提示文字，
 * 保证 tooltip 高度恒定有界、不会超出屏幕。</p>
 */
public class ClientInlayTooltip implements ClientTooltipComponent {

    private static final int SLOT_SIZE = 18;
    /** 每行最多镶孔数，超过则换行。 */
    private static final int COLUMNS = 6;
    /** 最多绘制的镶孔数（6 列 × 4 行）；超出部分不绘制，由调用方以文字说明。 */
    public static final int MAX_VISIBLE_SOCKETS = COLUMNS * 4;
    /** 原版槽位贴图（18×18，与 {@link #SLOT_SIZE} 一一对应）。 */
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");

    private final List<ItemStack> materialStacks;
    private final int sockets;

    public ClientInlayTooltip(InlayTooltipComponent tooltip) {
        this.materialStacks = tooltip.materialStacks();
        this.sockets = tooltip.sockets();
    }

    /** 实际绘制的镶孔数；已镶嵌的排在前面，因此优先显示的是材料。 */
    private int visible() {
        return Math.min(this.sockets, MAX_VISIBLE_SOCKETS);
    }

    private int rows() {
        return (this.visible() + COLUMNS - 1) / COLUMNS;
    }

    @Override
    public int getHeight() {
        return this.rows() * SLOT_SIZE;
    }

    @Override
    public int getWidth(Font font) {
        return Math.min(this.visible(), COLUMNS) * SLOT_SIZE;
    }

    @Override
    public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
        for (int i = 0; i < this.visible(); i++) {
            int slotX = x + (i % COLUMNS) * SLOT_SIZE;
            int slotY = y + (i / COLUMNS) * SLOT_SIZE;

            // 所有镶孔统一画槽位贴图，空镶孔自然呈现为空槽位。
            guiGraphics.blitSprite(SLOT_SPRITE, slotX, slotY, SLOT_SIZE, SLOT_SIZE);

            if (i < this.materialStacks.size() && !this.materialStacks.get(i).isEmpty()) {
                ItemStack renderStack = getRenderStack(this.materialStacks.get(i));
                guiGraphics.renderItem(renderStack, slotX + 1, slotY + 1);
                guiGraphics.renderItemDecorations(font, renderStack, slotX + 1, slotY + 1);
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
