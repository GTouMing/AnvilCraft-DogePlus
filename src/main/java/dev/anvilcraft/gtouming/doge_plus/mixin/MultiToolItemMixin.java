package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.util.MagnetAnvilUtil;
import dev.dubhe.anvilcraft.item.MultitoolItem;
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

/**
 * 磁铁模式的多功能工具：与 {@link MagnetItemMixin} 共享收纳/放置/蓄力逻辑。
 */
@Mixin(MultitoolItem.class)
public class MultiToolItemMixin {

    /**
     * 拾取铁砧：磁铁模式的多功能工具亦可收纳。
     */
    @Inject(method = "useOnAsMagnet", at = @At("HEAD"), cancellable = true)
    private void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (MagnetAnvilUtil.tryPickupAnvil(context)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /**
     * 放置铁砧。
     */
    @Inject(method = "useOnAsMagnet", at = @At("HEAD"), cancellable = true)
    private void useToPlace(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (MagnetAnvilUtil.tryPlaceAnvil(context)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /**
     * 磁铁模式的多功能工具带铁砧时，右键进入蓄力。
     */
    @Inject(method = "useAsMagnet", at = @At("HEAD"), cancellable = true)
    private void onUse(Level level, Player player, InteractionHand usedHand,
                       CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (MagnetAnvilUtil.tryStartCharge(level, player, usedHand)) {
            cir.setReturnValue(InteractionResultHolder.success(player.getItemInHand(usedHand)));
        }
    }
}
