package dev.anvilcraft.gtouming.doge_plus.client;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.world.item.ItemStack;

/**
 * AE2 可选兼容：把 AE2 用于虚拟槽 / 样板显示的 {@link GenericStack} 包装物品解包成真实物品，
 * 以便读取其 {@code INLAY} 组件。
 *
 * <p>本类只在 AE2 已加载时由 {@code ClientTooltipEvent} 调用，AE2 缺席时不会被类加载。</p>
 */
public final class Ae2TooltipCompat {

    private Ae2TooltipCompat() {
    }

    public static ItemStack unwrap(ItemStack stack) {
        GenericStack generic = GenericStack.unwrapItemStack(stack);
        if (generic == null || !(generic.what() instanceof AEItemKey key)) {
            return stack;
        }
        return key.toStack((int) Math.max(1, generic.amount()));
    }
}
