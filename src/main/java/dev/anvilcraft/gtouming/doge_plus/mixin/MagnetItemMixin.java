package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.util.MagnetAnvilUtil;
import dev.dubhe.anvilcraft.item.MagnetItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MagnetItem.class)
public class MagnetItemMixin {

    /**
     * 拾取铁砧：任一只手是磁铁 + 另一只手空/铁砧物品，右键铁砧方块收纳。
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (MagnetAnvilUtil.tryPickupAnvil(context)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /**
     * 放置铁砧：副手磁铁（带铁砧）+ 主手空，右键放置。
     */
    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void useToPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (MagnetAnvilUtil.tryPlaceAnvil(context)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /**
     * 手持带铁砧的磁铁时，右键进入蓄力。
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand usedHand,
                       CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (MagnetAnvilUtil.tryStartCharge(level, player, usedHand)) {
            cir.setReturnValue(InteractionResultHolder.success(player.getItemInHand(usedHand)));
        }
    }
}
