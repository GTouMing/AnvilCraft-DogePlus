package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * 物品级镶嵌的「永恒」行为：带永恒镶嵌的物品不消耗耐久。
 *
 * <p>工具挖掘、武器攻击、盔甲受击的耐久损耗最终都汇入
 * {@link ItemStack#hurtAndBreak(int, ServerLevel, LivingEntity, Consumer)}，
 * 在这里直接取消即可覆盖全部持有 / 装备状态的损耗；掉落实体的火焰、爆炸、
 * 仙人掌、时间与虚空免疫另见 {@link ItemEntityMixin}。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(
            method = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;"
                    + "Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V",
            at = @At("HEAD"),
            cancellable = true)
    private void doge_plus$eternalUnbreakable(
            int amount, ServerLevel level, LivingEntity entity, Consumer<Item> onBreak, CallbackInfo ci) {
        if (InlayUtil.hasProperty((ItemStack) (Object) this, InlayProperty.ETERNAL)) {
            ci.cancel();
        }
    }
}
