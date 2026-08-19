package dev.anvilcraft.gtouming.doge_plus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.dubhe.anvilcraft.block.sliding.ISlidingRail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ISlidingRail.class)
public interface SlidingRailMixin {
    @Inject(
            method = "moveBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"

            )
    )
    private static void doge_plus$saveInlayBeforeDestroy(
            Level level, BlockPos pos, Direction facing,
            CallbackInfoReturnable<Boolean> cir,
            @Local(name = "toPushPos") BlockPos toPushPos
    ) {
        if (level.isClientSide()) return;
        BlockInlayManager.stashInlayForMove(level, toPushPos);
        AnvilCraftDogePlus.LOGGER.debug(toPushPos.toShortString());
    }
}
