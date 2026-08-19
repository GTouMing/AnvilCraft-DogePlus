package dev.anvilcraft.gtouming.doge_plus.mixin;


import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlidingBlockSection.class)
public class SlidingBlockSectionMixin {
    @Inject(
            method = "setBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    shift = At.Shift.AFTER
            )
    )
    private void doge_plus$beforeSetBlock(
            Level level, BlockPos center, Entity entity, CallbackInfo ci, @Local(name = "pos") BlockPos pos, @Local(name = "state")BlockState state
            ) {
        BlockInlayManager.restoreInlayForMove(level, pos, state.getBlock());
        AnvilCraftDogePlus.LOGGER.debug(pos.toShortString());
    }
}
