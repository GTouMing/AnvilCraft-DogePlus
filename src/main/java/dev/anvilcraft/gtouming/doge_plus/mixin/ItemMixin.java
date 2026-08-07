package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.entity.IAnvilTarget;
import dev.anvilcraft.gtouming.doge_plus.entity.FlyingAnvilEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.gtouming.doge_plus.util.MagnetHandler;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.CONFIG;
import static dev.anvilcraft.gtouming.doge_plus.util.MagnetHandler.*;

/**
 * 为磁铁蓄力发射铁砧 + 磁铁锭视线标记注入 Item 基类的钩子。
 */
@Mixin(Item.class)
public class ItemMixin {

    /**
     * 使用时长设为超大值（同弓 72000），确保玩家总是提前松开，
     * 从而必然走 {@code releaseUsing} 而非 {@code finishUsingItem}。
     */
    @Unique
    private static final int UNLIMITED_USE_DURATION = 72000;

    /**
     * 带铁砧的磁铁返回超大使用时长（否则为默认 0，右键不会蓄力）。
     */
    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void onGetUseDuration(ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        if (isMagnet(stack) && MagnetHandler.hasAnvil(stack)) {
            cir.setReturnValue(UNLIMITED_USE_DURATION);
        }
    }

    /**
     * 带铁砧的磁铁使用拉弓动画。
     */
    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void onGetUseAnimation(ItemStack stack, CallbackInfoReturnable<UseAnim> cir) {
        if (isMagnet(stack) && MagnetHandler.hasAnvil(stack)) {
            cir.setReturnValue(UseAnim.BOW);
        }
    }

    /**
     * 磁铁锭右键使用：沿视线射线寻找命中的非玩家生物并标记 +1，消耗一个磁铁锭。
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand usedHand,
                       CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide) return;
        ItemStack stack = player.getItemInHand(usedHand);
        if (!stack.is(ModItems.MAGNET_INGOT)) return;

        boolean consumed = false;
        double range = CONFIG.markRange;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB aabb = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, aabb,
                e -> e instanceof LivingEntity && e.isAlive() && e != player);
        if (hit != null && hit.getEntity() instanceof IAnvilTarget target) {
            target.doge_plus$setMarks(target.doge_plus$getMarks() + 1);
            // 标记成功消耗一个磁铁锭
            stack.shrink(1);
            consumed = true;
        }


        cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack, consumed || level.isClientSide()));
    }

    /**
     * 松开发射铁砧投射物（FlyingAnvilEntity）。蓄力固定为满（charge = 1）。
     */
    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void onReleaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft, CallbackInfo ci) {
        if (!(isMagnet(stack))) return;
        if (!MagnetHandler.hasAnvil(stack)) return;
        if (!(entity instanceof Player player)) return;
        if (level.isClientSide) return;

        ResourceLocation anvilId = MagnetHandler.getAnvilId(stack);
        if (anvilId == null) return;

        FlyingAnvilEntity anvil = new FlyingAnvilEntity(ModEntities.FLYING_ANVIL.get(), level);
        anvil.init(player, anvilId, (float) CONFIG.anvilSpeed);
        level.addFreshEntity(anvil);

        // 卸载铁砧
        MagnetHandler.clearAnvilId(stack);
        player.getCooldowns().addCooldown(stack.getItem(), 10);

        ci.cancel();
    }

    /**
     * 带铁砧的磁铁在松开时完成使用（同弓/弩）。
     */
    @Inject(method = "useOnRelease", at = @At("RETURN"), cancellable = true)
    private void onUseOnRelease(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(isMagnet(stack) && MagnetHandler.hasAnvil(stack) || stack.is(Items.CROSSBOW));
    }
}
