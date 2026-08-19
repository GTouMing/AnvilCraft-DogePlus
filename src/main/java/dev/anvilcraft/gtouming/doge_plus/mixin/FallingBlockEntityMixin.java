package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity {

    public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    @Shadow
    private BlockState blockState;

    /**
     * 在 FallingBlockEntity.fall() 中，方块被移除前保存镶嵌数据。
     * 注入点：level.setBlock(pos, blockState.getFluidState().createLegacyBlock(), 3)
     */
    @Inject(
            method = "fall",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private static void doge_plus$saveInlayOnFall(
            Level level,
            BlockPos pos,
            BlockState blockState,
            CallbackInfoReturnable<FallingBlockEntity> cir
    ) {
        if (level.isClientSide()) return;
        BlockInlayManager.stashInlayForMove(level, pos);
    }

    /**
     * 在 FallingBlockEntity.tick() 中，方块落地后恢复镶嵌数据。
     * 注入点：this.level().setBlock(blockpos, this.blockState, 3)
     */
    @Inject(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
            )
    )
    private void doge_plus$restoreInlayOnLand(CallbackInfo ci) {
        Level level =  this.level();
        if (level.isClientSide()) return;
        BlockInlayManager.restoreInlayForMove(level, BlockPos.containing(this.position()), blockState.getBlock());
    }
}