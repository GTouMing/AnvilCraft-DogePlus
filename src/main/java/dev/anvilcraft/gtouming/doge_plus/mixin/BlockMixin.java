package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "stepOn", at = @At("HEAD"))
    private void doge_plus$highTempStepOn(
            Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (level.isClientSide()) return;
        if (!BlockInlayManager.hasProperty(level, pos, InlayProperty.HIGH_TEMP)) return;
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().hotFloor(), 2.0F);
        }
    }
}