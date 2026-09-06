package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.util.AnvilMagnetUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 铁砧侧磁吸（挂在计划刻回调上）：
 *
 * <p>铁砧方块被放置/邻居变化时会 scheduleTick，计划刻唤醒后进入
 * {@link FallingBlock#tick} 做下落检测。注入该入口：若当前是铁砧
 * （{@link BlockTags#ANVIL}）且正上方存在「吸引态」磁性镶嵌方块，
 * 则把铁砧抬到磁块正下方并取消本次下落检测（被磁力吸住）；
 * 否则放行 vanilla 逻辑（下方无支撑则生成 FallingBlockEntity 下落，
 * 由 {@link FallingBlockEntityMixin} 施加等效初速 / 再次吸回）。</p>
 */
@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void doge_plus$magnetHoldAnvil(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci
    ) {
        // 仅铁砧（含 BetterAnvilBlock 系）；沙/砾石等其它 FallingBlock 不受影响
        if (!state.is(BlockTags.ANVIL)) return;
        // 上方有吸引态磁块 → 吸到磁块正下方，不放行下落
        if (AnvilMagnetUtil.liftAnvilByMagnetAbove(level, pos)) {
            ci.cancel();
        }
    }
}
