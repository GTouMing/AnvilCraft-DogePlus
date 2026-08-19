package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方块级镶嵌的「耐火」「永恒」行为（NeoForge 按坐标提供的方法位于
 * {@link IBlockExtension} 接口默认方法，目标为接口，故本 Mixin 声明为接口形式）：
 * <ul>
 *   <li>{@code getFlammability}/{@code getFireSpreadSpeed}：耐火镶嵌方块不可燃、不引燃。</li>
 *   <li>{@code getExplosionResistance}：永恒镶嵌方块免疫爆炸（抵抗设为极大值）。</li>
 * </ul>
 */
@Mixin(IBlockExtension.class)
public interface IBlockExtensionMixin {

    @Inject(method = "getFlammability", at = @At("HEAD"), cancellable = true)
    default void doge_plus$fireProofFlammability(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (BlockInlayManager.hasProperty(level, pos, InlayProperty.FIRE_PROOF)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getFireSpreadSpeed", at = @At("HEAD"), cancellable = true)
    default void doge_plus$fireProofSpread(
            BlockState state, BlockGetter level, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir) {
        if (BlockInlayManager.hasProperty(level, pos, InlayProperty.FIRE_PROOF)) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "getExplosionResistance", at = @At("HEAD"), cancellable = true)
    default void doge_plus$eternalExplosionResistance(
            BlockState state, BlockGetter level, BlockPos pos, Explosion explosion, CallbackInfoReturnable<Float> cir) {
        if (BlockInlayManager.hasProperty(level, pos, InlayProperty.ETERNAL)) {
            cir.setReturnValue(Float.MAX_VALUE);
        }
    }
}
