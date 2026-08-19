package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

/**
 * 加速环/偏转环在 {@code attractGianAnvil} 中吸引巨型铁砧后，
 * 重建方块时硬编码使用了基础巨型铁砧（{@code ModBlocks.GIANT_ANVIL}），
 * 导致巨型 Doge 砧等 {@link GiantAnvilBlock} 子类被抬升后退化为巨型铁砧。
 * 此 mixin 记录实际巨型铁砧方块类型（已放置的方块或下坠中的实体），并按该类型重建。
 */
@Mixin({AccelerationRingBlockEntity.class, DeflectionRingBlockEntity.class})
public class GiantRingBlockEntityMixin {

    /** 已放置的巨型铁砧方块类型。 */
    @Unique
    private Block doge_plus$attractedGiantBlock;

    /** 被吸引的下坠巨型铁砧的方块状态（放置型方块不存在时使用）。 */
    @Unique
    private BlockState doge_plus$fallingGiantState;

    @Inject(method = "attractGianAnvil", at = @At("HEAD"))
    private void doge_plus$resetAttractedGiant(CallbackInfo ci) {
        this.doge_plus$attractedGiantBlock = null;
        this.doge_plus$fallingGiantState = null;
    }

    /** 捕获已放置的巨型铁砧方块类型（移除旧结构前）。 */
    @Redirect(
        method = "attractGianAnvil",
        at = @At(
            value = "INVOKE",
            target = "Ldev/dubhe/anvilcraft/block/GiantAnvilBlock;removePartsAndUpdate(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"
        )
    )
    private void doge_plus$captureAttractedGiant(GiantAnvilBlock block, Level level, BlockPos pos) {
        this.doge_plus$attractedGiantBlock = block;
        block.removePartsAndUpdate(level, pos);
    }

    /** 捕获被吸引的下坠巨型铁砧的方块状态（取离环中心最近的）。 */
    @Redirect(
        method = "attractGianAnvil",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"
        )
    )
    private List<FallingGiantAnvilEntity> doge_plus$captureFallingGiantState(
        Level level, Class<FallingGiantAnvilEntity> cls, AABB aabb
    ) {
        List<FallingGiantAnvilEntity> list = level.getEntitiesOfClass(cls, aabb);
        BlockPos center = ((net.minecraft.world.level.block.entity.BlockEntity) (Object) this).getBlockPos();
        FallingGiantAnvilEntity nearest = null;
        double best = Double.POSITIVE_INFINITY;
        for (FallingGiantAnvilEntity entity : list) {
            double d = entity.position().distanceToSqr(center.getCenter());
            if (d < best) {
                best = d;
                nearest = entity;
            }
        }
        if (nearest != null) {
            this.doge_plus$fallingGiantState = nearest.getBlockState();
        }
        return list;
    }

    /** 重建时按实际巨型铁砧方块类型（已放置 → 下坠 → 回退基础型）。 */
    @Redirect(
        method = "attractGianAnvil",
        at = @At(
            value = "INVOKE",
            target = "Ldev/anvilcraft/lib/v2/registrum/util/entry/BlockEntry;getDefaultState()Lnet/minecraft/world/level/block/state/BlockState;"
        )
    )
    private BlockState doge_plus$rebuildWithAttractedGiant(BlockEntry<?> entry) {
        if (this.doge_plus$attractedGiantBlock != null) {
            return this.doge_plus$attractedGiantBlock.defaultBlockState();
        }
        return Objects.requireNonNullElseGet(this.doge_plus$fallingGiantState, entry::getDefaultState);
    }
}
