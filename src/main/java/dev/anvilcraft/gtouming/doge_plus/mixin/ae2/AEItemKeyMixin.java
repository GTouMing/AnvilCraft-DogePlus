package dev.anvilcraft.gtouming.doge_plus.mixin.ae2;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * AE2 19.2.18 的 {@code AEItemKey.hasComponents()} 返回值写反了
 * （返回 {@code stack.getComponents().isEmpty()}，上游 main 已修为
 * {@code !stack.isComponentsPatchEmpty()}）。这里修正，避免合成 / 切石样板的
 * 替换缓存把「带组件」与「不带组件」的物品混为一谈。
 */
@Mixin(targets = "appeng.api.stacks.AEItemKey", remap = false)
public abstract class AEItemKeyMixin {

    @Shadow
    @Final
    private ItemStack stack;

    @Overwrite
    public boolean hasComponents() {
        return !stack.isComponentsPatchEmpty();
    }
}
