package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.entity.IAnvilTarget;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.CONFIG;

/**
 * 磁铁锭标记目标：手持 {@code anvilcraft:magnet_ingot} 右键，沿视线射线标记命中的
 * 非玩家生物（标记数 +1），并消耗一个磁铁锭。被标记实体受飞行铁砧命中时，
 * 每个标记提供 {@code CONFIG.perMark} 额外伤害。
 */
@Mixin(Item.class)
public abstract class MagnetIngotMarkMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void doge_plus$markTarget(
            Level level, Player player, InteractionHand usedHand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide) return;
        ItemStack stack = player.getItemInHand(usedHand);
        if (!stack.is(ModItems.MAGNET_INGOT)) return;

        double range = CONFIG.markRange;
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getLookAngle();
        Vec3 end = eye.add(look.scale(range));
        AABB aabb = player.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(level, player, eye, end, aabb,
                e -> e instanceof LivingEntity && e.isAlive() && e != player);
        if (hit != null && hit.getEntity() instanceof IAnvilTarget target) {
            target.doge_plus$setMarks(target.doge_plus$getMarks() + 1);
            stack.shrink(1);
            cir.setReturnValue(InteractionResultHolder.sidedSuccess(stack, false));
        }
    }
}
