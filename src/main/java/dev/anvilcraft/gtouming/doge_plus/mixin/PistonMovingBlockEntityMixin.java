package dev.anvilcraft.gtouming.doge_plus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PistonMovingBlockEntity.class)
public class PistonMovingBlockEntityMixin {

    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    ordinal = 1
            )
    )
    private static void doge_plus$onSetBlock(Level level, BlockPos pos, BlockState state, PistonMovingBlockEntity blockEntity, CallbackInfo ci, @Local(name = "blockstate")BlockState blockState) {
        BlockInlayManager.restoreInlayForMove(level, pos, blockState.getBlock());
    }
}