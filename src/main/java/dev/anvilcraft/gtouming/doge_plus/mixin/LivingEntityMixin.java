package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import dev.dubhe.anvilcraft.api.totem.TotemManager;
import dev.dubhe.anvilcraft.api.totem.handler.TotemHandler;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.function.Predicate;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Unique
    private boolean doge_plus$raged = false;
    @Unique
    private int doge_plus$rageTick;

    @Shadow
    public abstract ItemStack getItemInHand(InteractionHand hand);
    @Shadow
    protected abstract boolean checkTotemDeathProtection(DamageSource source);
    @Shadow
    public abstract void kill();


    @Redirect(
            method = "hurt",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/world/entity/LivingEntity;checkTotemDeathProtection(Lnet/minecraft/world/damagesource/DamageSource;)Z"
            )
    )
    private boolean doge_plus$checkTotemDeathProtection(LivingEntity instance, DamageSource damageSource) {
        ItemStack main = getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack off = getItemInHand(InteractionHand.OFF_HAND);
        ItemStack handStack = null;
        if (InlayUtil.hasProperty(main, InlayProperty.NIRVANA)) handStack = main;
        else if (InlayUtil.hasProperty(off, InlayProperty.NIRVANA)) handStack = off;
        if (handStack == null) return this.checkTotemDeathProtection(damageSource);

        Map<Item, TotemHandler> totemMap = TotemManager.INSTANCE.getTotemMap();
        Predicate<ResourceLocation> p = null;
        Item totemItem = null;
        TotemHandler handler = null;
        for (ResourceLocation id : InlayUtil.getInlays(handStack)) {
            if (!totemMap.containsKey(BuiltInRegistries.ITEM.get(id))) continue;
            Item item = BuiltInRegistries.ITEM.get(id);
            TotemHandler handler1 = totemMap.get(item);
            if (!handler1.canExecute(damageSource, instance, item.getDefaultInstance())) continue;
            if (!CommonHooks.onLivingUseTotem(instance, damageSource, item.getDefaultInstance(), handStack.equals(main) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND)) continue;
            p = (i) -> i == id;
            totemItem = item;
            handler = handler1;
        }

        if (p == null) {
            return false;
        }

        ItemStack itemStack = totemItem.getDefaultInstance();
        boolean result = handler.execute(damageSource, instance, itemStack);
        if (result) {
            ArrayList<ResourceLocation> inlayList = InlayUtil.getInlays(handStack);
            inlayList.removeIf(p);
            InlayUtil.setInlays(handStack, inlayList);
            if (itemStack.is(ModItems.TOTEM_OF_RAGE)) {
                this.doge_plus$raged = true;
            }
        }
        return result;
    }

    @Inject(
            method = "baseTick",
            at = @At(
                    value = "HEAD"
            )
    )
    private void dieOfRage(CallbackInfo ci) {
        if (this.doge_plus$raged) {
            if (this.doge_plus$rageTick >= 1200) {
                if ((LivingEntity) (Object) this instanceof Player player) {
                    if (player instanceof ServerPlayer serverPlayer) {
                        if (serverPlayer.gameMode.getGameModeForPlayer() == GameType.SURVIVAL
                                || serverPlayer.gameMode.getGameModeForPlayer() == GameType.ADVENTURE) {
                            player.kill();
                        }
                    }
                } else {
                    this.kill();
                }
                this.doge_plus$raged = false;
                this.doge_plus$rageTick = 0;
            } else {
                this.doge_plus$rageTick++;
            }
        }
    }
}
