package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 玩家手持镶嵌物品时的性质行为：
 * <ul>
 *   <li>高温：攻击时消耗累加的高温伤害。</li>
 *   <li>涅磐：死亡时触发不死图腾，随后该材料（物品）碎裂销毁。</li>
 * </ul>
 */
@Mixin(Player.class)
public abstract class PlayerInlayMixin {

    /** 高温：攻击时对目标造成累加的高温伤害并清零。 */
    @Inject(method = "attack", at = @At("RETURN"))
    private void doge_plus$highTempAttack(Entity target, CallbackInfo ci) {
        Player self = (Player) (Object) this;
        ItemStack weapon = self.getMainHandItem();
        if (weapon.isEmpty() || !InlayUtil.hasProperty(weapon, InlayProperty.HIGH_TEMP)) return;
        int heat = weapon.getOrDefault(ModDataComponentTypes.HEAT, 0);
        if (heat <= 0) return;
        if (target.isAlive()) {
            target.hurt(self.damageSources().playerAttack(self), heat);
        }
        weapon.set(ModDataComponentTypes.HEAT, 0);
    }
}
