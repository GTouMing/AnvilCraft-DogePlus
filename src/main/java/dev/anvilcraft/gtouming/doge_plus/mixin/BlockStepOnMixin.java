package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.util.InlayEffectUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 踩踏 hook：实体踩上携带「效果」镶嵌的方块时，对其施加药水效果。
 *
 * <p>注入 {@link Block#stepOn} 而非各方块覆写，普通方块（如带镶嵌的 Doge 钢块）
 * 未覆写 {@code stepOn}，统一走此注入点。效果冷却见
 * {@link InlayEffectUtil#STEP_EFFECT_PERIOD}，防止每 tick 刷新叠加。</p>
 */
@Mixin(Block.class)
public abstract class BlockStepOnMixin {

    @Inject(method = "stepOn", at = @At("HEAD"))
    private void doge_plus$stepOnEffect(
            Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        InlayEffectUtil.applyStepEffects(level, pos, entity);
    }
}
