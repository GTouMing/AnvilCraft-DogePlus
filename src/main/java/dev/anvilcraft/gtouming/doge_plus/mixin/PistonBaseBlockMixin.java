package dev.anvilcraft.gtouming.doge_plus.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PistonBaseBlock.class, priority = 944)
public class PistonBaseBlockMixin {

    @Inject(
            method = "moveBlocks",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
                    ordinal = 0
            )
    )
    private void doge_plus$afterSetBlockEntity(
            Level level, BlockPos ignore, Direction facing, boolean extending, CallbackInfoReturnable<Boolean> cir, @Local(name = "blockpos3") BlockPos pos, @Local(name = "direction") Direction pushDirection
    ) {
        // ⭐ 保存镶嵌数据：从源位置取出暂存
        if (level.isClientSide()) return;
        BlockPos sourcePos = pos.relative(pushDirection.getOpposite());
        BlockInlayManager.stashInlayForMove(level, sourcePos);
    }
}