package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 巨型 Doge 砧：由小型 {@link DogeAnvil} 喂满成长值后原地长成。
 * 复用前置模组 {@link GiantAnvilBlock} 的巨型铁砧功能（3×3×3 多方块、坠落、铁砧菜单）。
 */
public class GiantDogeAnvil extends GiantAnvilBlock {

    public GiantDogeAnvil(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GiantDogeAnvil> codec() {
        return simpleCodec(GiantDogeAnvil::new);
    }

    /**
     * 按模型轮廓做碰撞：根据巨型 Doge 砧模型的 16 个元素（含旋转元素包围盒），
     * 逐部件概括出碰撞形状，避免使用父类巨型铁砧的分部件形状造成"看不见的墙"把玩家弹开。
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            case BOTTOM_WN -> Shapes.or(Block.box(4, 0, 4, 16, 8, 16), Block.box(8, 8, 8, 16, 11, 16),
                    Block.box(12.7, 11, 12.7, 16, 16, 16), Block.box(10, 11, 10, 16, 16, 16));
            case BOTTOM_N -> Shapes.or(Block.box(0, 0, 4, 16, 8, 16), Block.box(0, 8, 8, 16, 11, 16),
                    Block.box(0, 11, 12.7, 16, 16, 16), Block.box(0, 7.7, 12.7, 16, 16, 16));
            case BOTTOM_EN -> Shapes.or(Block.box(0, 0, 4, 12, 8, 16), Block.box(0, 8, 8, 8, 11, 16),
                    Block.box(0, 11, 12.7, 3.3, 16, 16), Block.box(0, 11, 10, 6, 16, 16));
            case BOTTOM_W -> Shapes.or(Block.box(4, 0, 0, 16, 8, 16), Block.box(8, 8, 0, 16, 11, 16),
                    Block.box(12.7, 11, 0, 16, 16, 16), Block.box(12.7, 7.7, 0, 16, 16, 16));
            case BOTTOM_E -> Shapes.or(Block.box(0, 0, 0, 12, 8, 16), Block.box(0, 8, 0, 8, 11, 16),
                    Block.box(0, 11, 0, 3.3, 16, 16), Block.box(0, 7.7, 0, 3.3, 16, 16));
            case BOTTOM_WS -> Shapes.or(Block.box(4, 0, 0, 16, 8, 12), Block.box(8, 8, 0, 16, 11, 8),
                    Block.box(12.7, 11, 0, 16, 16, 3.3), Block.box(10, 11, 0, 16, 16, 6));
            case BOTTOM_S -> Shapes.or(Block.box(0, 0, 0, 16, 8, 12), Block.box(0, 8, 0, 16, 11, 8),
                    Block.box(0, 11, 0, 16, 16, 3.3), Block.box(0, 7.7, 0, 16, 16, 3.3));
            case BOTTOM_ES -> Shapes.or(Block.box(0, 0, 0, 12, 8, 12), Block.box(0, 8, 0, 8, 11, 8),
                    Block.box(0, 11, 0, 3.3, 16, 3.3), Block.box(0, 11, 0, 6, 16, 6));
            case MID_WN -> Shapes.or(Block.box(2, 15, 2, 16, 16, 16), Block.box(12.7, 0, 12.7, 16, 11, 16),
                    Block.box(10, 0, 10, 16, 11, 16), Block.box(2.8, 11, 2.8, 16, 15, 16));
            case MID_N -> Shapes.or(Block.box(0, 15, 2, 16, 16, 16), Block.box(0, 0, 12.7, 16, 11, 16),
                    Block.box(0, 0, 12.7, 16, 14.3, 16), Block.box(0, 11, 2.8, 16, 15, 16),
                    Block.box(6, 11, 10, 10, 15, 16));
            case MID_EN -> Shapes.or(Block.box(0, 15, 2, 14, 16, 16), Block.box(0, 0, 12.7, 3.3, 11, 16),
                    Block.box(0, 0, 10, 6, 11, 16), Block.box(0, 11, 2.8, 13.2, 15, 16));
            case MID_W -> Shapes.or(Block.box(2, 15, 0, 16, 16, 16), Block.box(12.7, 0, 0, 16, 11, 16),
                    Block.box(12.7, 0, 0, 16, 14.3, 16), Block.box(2.8, 11, 0, 16, 15, 16),
                    Block.box(10, 11, 6, 16, 15, 10));
            case MID_E -> Shapes.or(Block.box(0, 15, 0, 14, 16, 16), Block.box(0, 0, 0, 3.3, 11, 16),
                    Block.box(0, 0, 0, 3.3, 14.3, 16), Block.box(0, 11, 0, 13.2, 15, 16),
                    Block.box(0, 11, 6, 6, 15, 10));
            case MID_WS -> Shapes.or(Block.box(2, 15, 0, 16, 16, 14), Block.box(12.7, 0, 0, 16, 11, 3.3),
                    Block.box(10, 0, 0, 16, 11, 6), Block.box(2.8, 11, 0, 16, 15, 13.2));
            case MID_S -> Shapes.or(Block.box(0, 15, 0, 16, 16, 14), Block.box(0, 0, 0, 16, 11, 3.3),
                    Block.box(0, 0, 0, 16, 14.3, 3.3), Block.box(0, 11, 0, 16, 15, 13.2),
                    Block.box(6, 11, 0, 10, 15, 6));
            case MID_ES -> Shapes.or(Block.box(0, 16, 0, 14, 16, 14), Block.box(0, 0, 0, 3.3, 11, 3.3),
                    Block.box(0, 0, 0, 6, 11, 6), Block.box(0, 11, 0, 13.2, 15, 13.2));
            case TOP_WN -> Shapes.or(Block.box(1, 9, 1, 16, 16, 16), Block.box(2, 0, 2, 16, 9, 16));
            case TOP_EN -> Shapes.or(Block.box(0, 9, 1, 15, 16, 16), Block.box(0, 0, 2, 14, 9, 16));
            case TOP_WS -> Shapes.or(Block.box(1, 9, 0, 16, 16, 15), Block.box(2, 0, 0, 16, 9, 14));
            case TOP_ES -> Shapes.or(Block.box(0, 9, 0, 15, 16, 15), Block.box(0, 0, 0, 14, 9, 14));
            case TOP_CENTER, MID_CENTER, BOTTOM_CENTER -> Shapes.or(Block.box(0, 0, 0, 16, 16, 16));
            case TOP_N, TOP_W, TOP_E, TOP_S -> Shapes.or(Block.box(0, 0, 0, 16, 16, 16));
        };
    }

    /**
     * 落地逻辑与 {@link GiantAnvilBlock} 一致，仅把掉落物改为本模组的巨型 Doge 砧。
     */
    @Override
    public void onLand(
        Level level,
        BlockPos pos,
        BlockState state,
        BlockState replaceableState,
        FallingBlockEntity fallingBlock,
        float fallDistance
    ) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        BlockPos belowPos = pos.below();
        if (!canSurvive(state, level, belowPos)) {
            ItemEntity itemEntity = new ItemEntity(
                level, belowPos.getX(), belowPos.getY(), belowPos.getZ(), ModBlocks.GIANT_DOGE_ANVIL.asStack());
            itemEntity.setDefaultPickUpDelay();
            level.addFreshEntity(itemEntity);
            return;
        }
        for (Cube3x3PartHalf part : this.getParts()) {
            BlockState newState = state.setValue(HALF, part)
                .setValue(CUBE, part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
            level.setBlockAndUpdate(belowPos.offset(part.getOffset()), newState);
        }
        NeoForge.EVENT_BUS.post(
            new AnvilEvent.GiantOnLand(level, pos, (FallingGiantAnvilEntity) fallingBlock, fallDistance));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                BlockPos pos1 = belowPos.offset(new Vec3i(dx, 0, dz));
                NeoForge.EVENT_BUS.post(new AnvilEvent.OnLand(level, pos1, fallingBlock, fallDistance));
            }
        }

        level.playSound(
            null,
            belowPos,
            ModSoundEvents.GIANT_ANVIL_LAND.get(),
            SoundSource.BLOCKS,
            0.55f,
            level.random.nextFloat() * 0.1F + 0.55f);
    }
}
