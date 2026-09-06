package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.util.AnvilMagnetUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.CONFIG;

@Mixin(FallingBlockEntity.class)
public abstract class FallingBlockEntityMixin extends Entity {

    public FallingBlockEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }
    @Shadow
    private BlockState blockState;

    /** 上次已施加过「释放初速」的磁块位置，避免同一磁块上升沿重复累加。 */
    @Unique
    private BlockPos doge_plus$lastBoostMagnet;

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
        Level level = this.level();
        if (level.isClientSide()) return;
        BlockInlayManager.restoreInlayForMove(level, BlockPos.containing(this.position()), blockState.getBlock());
    }

    /**
     * 磁性镶嵌方块吸引铁砧：铁砧下落时向上探测，命中带「磁性」镶嵌的方块时，
     * 若该方块未收到红石信号则把铁砧搬移到其正下方落地（参考前置 MagnetBlock.attract），
     * 使铁砧停靠在磁块下方。
     *
     * <p>红石边沿语义：</p>
     * <ul>
     *   <li>磁块收到红石信号（上升沿，释放态）：不吸引；每个铁砧对同一磁块只预置一次
     *       {@code fallDistance}（额外计入 magnetFallHeight × 磁级 的落地高度），
     *       之后按自然重力下落——每 tick 下落 vanilla 会把实际位移累加到 fallDistance，
     *       因此落地高度 = 实际下落距离 + 磁级 × magnetFallHeight。</li>
     *   <li>磁块恢复无信号（下降沿）：铁砧重新被吸回（下落中每 tick 探测，命中即搬移）。</li>
     * </ul>
     *
     * <p>探测途中若遇到非空、非液体的实心方块挡路则停止（与前置一致）。</p>
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void doge_plus$magneticAttract(CallbackInfo ci) {
        Level level = this.level();
        if (level.isClientSide()) return;
        // 仅普通铁砧（含前置/本模组 BetterAnvilBlock 系）受磁吸；巨型砧走 FallingGiantAnvilEntity，不在本类
        if (!blockState.is(BlockTags.ANVIL)) return;

        // 从铁砧所在格正上方开始，逐格向上探测磁性方块（仿前置 attract 逐格扫描）
        BlockPos cursor = BlockPos.containing(this.position());
        int distance = CONFIG.magnetAttractsDistance;

        for (int i = 0; i < distance; i++) {
            cursor = cursor.above();
            BlockState aboveState = level.getBlockState(cursor);
            if (!AnvilMagnetUtil.hasMagnetic(level, cursor)) {
                // 途中遇非空非液体实心方块挡路即停止探测（仿前置 attract）
                if (!aboveState.isAir() && aboveState.getFluidState().isEmpty()) return;
                continue;
            }

            if (level.hasNeighborSignal(cursor)) {
                // 释放态：不吸引；只预置额外落地高度，不改速度（避免高速穿地）。
                // fallDistance 是 Entity public 字段，下落中 vanilla 每 tick 累加实际位移，
                // 落地快照 = 实际高度 + 额外计入高度。同一磁块只预置一次（上升沿）。
                if (CONFIG.magnetFallHeight > 0 && !cursor.equals(doge_plus$lastBoostMagnet)) {
                    int magneticCount = AnvilMagnetUtil.countMagnetic(level, cursor);
                    this.fallDistance += CONFIG.magnetFallHeight * magneticCount;
                    doge_plus$lastBoostMagnet = cursor.immutable();
                }
                return;
            }

            // 吸引态：搬移到磁性方块正下方落地
            doge_plus$attractTo(cursor.below());
            ci.cancel();
            return;
        }
    }

    /** 把本铁砧搬移到目标格落地，并迁移镶嵌数据。 */
    @Unique
    private void doge_plus$attractTo(BlockPos targetPos) {
        Level level = this.level();
        if (level.getBlockState(targetPos).is(BlockTags.ANVIL)) return;
        // 目标格已有其它方块 → 破坏并掉落（与前置 attract 一致）
        if (!level.isEmptyBlock(targetPos)) {
            level.destroyBlock(targetPos, true);
        }
        // 静默放置（flag 2 仅同步客户端、不触发邻居更新/onPlace）：
        // 避免放下的铁砧因下方无支撑立刻再次 falling，形成「落下→吸回→落下」循环。
        level.setBlock(targetPos, blockState, 2);
        // 迁移本次下落的镶嵌数据到新落点（本实体的数据在下落开始时就已入暂存队列）
        BlockInlayManager.restoreInlayForMove(level, targetPos, blockState.getBlock());
        this.discard();
    }
}

