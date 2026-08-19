package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.behavior.ILogicGate;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 方块级镶嵌的掉落写回与「高温」踩踏行为：
 * <ul>
 *   <li>{@code getDrops}（静态）：方块掉落物生成时把 {@link BlockInlayManager} 记录的
 *       INLAY 组件写回掉落物中的方块物品（重定向原掉落而非额外生成，避免双掉落），
 *       随后清理记录。</li>
 *   <li>{@code playerWillDestroy}：创造模式挖掘无掉落，直接清理记录
 *       （否则记录会残留，因创造不走 getDrops）。</li>
 *   <li>{@code stepOn}：高温镶嵌方块灼烧踩踏者（非潜行时，效果同岩浆块）。</li>
 * </ul>
 */
@Mixin(Block.class)
public abstract class BlockMixin extends BlockBehaviour implements ILogicGate {

    public BlockMixin(Properties properties) {
        super(properties);
    }

    /** 高温镶嵌方块灼烧踩踏者（非潜行时）。 */
    @Inject(method = "stepOn", at = @At("HEAD"))
    private void doge_plus$highTempStepOn(
            Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        if (level.isClientSide) return;
        if (!BlockInlayManager.hasProperty(level, pos, InlayProperty.HIGH_TEMP)) return;
        if (!entity.isSteppingCarefully() && entity instanceof LivingEntity living) {
            living.hurt(level.damageSources().hotFloor(), 2.0F);
        }
    }

    @Override
    public int doge_plus$getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return ILogicGate.super.doge_plus$getSignal(state, level, pos, direction);
    }

    @Override
    public int doge_plus$getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return ILogicGate.super.doge_plus$getDirectSignal(state, level, pos, direction);
    }

    @Override
    public boolean doge_plus$hasLogicGate(Level level, BlockPos pos) {
        return BlockInlayManager.hasProperty(level, pos, InlayProperty.DIRECTION)
                && (BlockInlayManager.hasProperty(level, pos, InlayProperty.NOT_GATE)
                || BlockInlayManager.hasProperty(level, pos, InlayProperty.AND_GATE)
                || BlockInlayManager.hasProperty(level, pos, InlayProperty.OR_GATE));
    }
}