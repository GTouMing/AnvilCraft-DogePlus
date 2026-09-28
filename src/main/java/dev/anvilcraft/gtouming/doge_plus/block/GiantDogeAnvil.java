package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.GiantDogeAnvilRenderer;
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
import net.minecraft.world.level.LevelReader;
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
 *
 * <p>没有方块实体：中心那三个旋转的核心由
 * {@code client.renderer.GiantDogeAnvilRenderer} 在 {@code RenderLevelStageEvent} 里按世界时间自己画，
 * 位置也由它自己在客户端发现。</p>
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
     * 逐部件概括出碰撞形状。
     */
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(HALF)) {
            // ==================== 底部 (BOTTOM) ====================
            case BOTTOM_WN -> Shapes.or(
                    Block.box(4, 0, 4, 16, 8, 16),
                    Block.box(8, 8, 8, 16, 11, 16),
                    Block.box(10, 11, 10, 16, 16, 16));
            case BOTTOM_N -> Shapes.or(
                    Block.box(0, 0, 4, 16, 8, 16),
                    Block.box(0, 8, 8, 16, 11, 16));
            case BOTTOM_EN -> Shapes.or(
                    Block.box(0, 0, 4, 12, 8, 16),
                    Block.box(0, 8, 8, 8, 11, 16),
                    Block.box(0, 11, 10, 6, 16, 16));
            case BOTTOM_W -> Shapes.or(
                    Block.box(4, 0, 0, 16, 8, 16),
                    Block.box(8, 8, 0, 16, 11, 16));
            case BOTTOM_E -> Shapes.or(
                    Block.box(0, 0, 0, 12, 8, 16),
                    Block.box(0, 8, 0, 8, 11, 16));
            case BOTTOM_WS -> Shapes.or(
                    Block.box(4, 0, 0, 16, 8, 12),
                    Block.box(8, 8, 0, 16, 11, 8),
                    Block.box(10, 11, 0, 16, 16, 6));
            case BOTTOM_S -> Shapes.or(
                    Block.box(0, 0, 0, 16, 8, 12),
                    Block.box(0, 8, 0, 16, 11, 8));
            case BOTTOM_ES -> Shapes.or(
                    Block.box(0, 0, 0, 12, 8, 12),
                    Block.box(0, 8, 0, 8, 11, 8),
                    Block.box(0, 11, 0, 6, 16, 6));
            case BOTTOM_CENTER, TOP_CENTER, MID_CENTER -> Shapes.or(Block.box(0, 0, 0, 16, 16, 16));

            // ==================== 中部 (MID) ====================
            // 角 (4个box): 柱(0-11) + 下梁(11-13) + 上梁(13-15) + 台(15-16)
            case MID_WN -> Shapes.or(
                    Block.box(10, 0, 10, 16, 11, 16),      // 柱
                    Block.box(8, 11, 8, 16, 13, 16),       // 下梁
                    Block.box(6, 13, 6, 16, 15, 16),       // 上梁
                    Block.box(2, 15, 2, 16, 16, 16));      // 台
            case MID_EN -> Shapes.or(
                    Block.box(0, 0, 10, 6, 11, 16),        // 柱
                    Block.box(0, 11, 8, 8, 13, 16),        // 下梁
                    Block.box(0, 13, 6, 10, 15, 16),        // 上梁
                    Block.box(0, 15, 2, 14, 16, 16));      // 台
            case MID_WS -> Shapes.or(
                    Block.box(10, 0, 0, 16, 11, 6),        // 柱
                    Block.box(8, 11, 0, 16, 13, 8),        // 下梁
                    Block.box(6, 13, 0, 16, 15, 10),        // 上梁
                    Block.box(2, 15, 0, 16, 16, 14));      // 台
            case MID_ES -> Shapes.or(
                    Block.box(0, 0, 0, 6, 11, 6),          // 柱
                    Block.box(0, 11, 0, 8, 13, 8),         // 下梁
                    Block.box(0, 13, 0, 10, 15, 10),         // 上梁
                    Block.box(0, 15, 0, 14, 16, 14));      // 台

            // 边 (3个box): 下梁(11-13) + 上梁(13-15) + 台(15-16)，没有柱
            case MID_N -> Shapes.or(
                    Block.box(0, 11, 8, 16, 13, 16),       // 下梁
                    Block.box(0, 13, 6, 16, 15, 16),       // 上梁
                    Block.box(0, 15, 2, 16, 16, 16));      // 台
            case MID_S -> Shapes.or(
                    Block.box(0, 11, 0, 16, 13, 8),        // 下梁
                    Block.box(0, 13, 0, 16, 15, 10),        // 上梁
                    Block.box(0, 15, 0, 16, 16, 14));      // 台
            case MID_W -> Shapes.or(
                    Block.box(8, 11, 0, 16, 13, 16),       // 下梁
                    Block.box(6, 13, 0, 16, 15, 16),       // 上梁
                    Block.box(2, 15, 0, 16, 16, 16));      // 台
            case MID_E -> Shapes.or(
                    Block.box(0, 11, 0, 8, 13, 16),        // 下梁
                    Block.box(0, 13, 0, 10, 15, 16),        // 上梁
                    Block.box(0, 15, 0, 14, 16, 16));      // 台

            // ==================== 顶部 (TOP) ====================
            case TOP_WN -> Shapes.or(
                    Block.box(2, 0, 2, 16, 9, 16),      // 下部分 檐 (y=0-9)
                    Block.box(1, 9, 1, 16, 16, 16));    // 上部分 台面 (y=9-16)
            case TOP_EN -> Shapes.or(
                    Block.box(0, 0, 2, 14, 9, 16),      // 下部分 檐 (y=0-9)
                    Block.box(0, 9, 1, 15, 16, 16));    // 上部分 台面 (y=9-16)
            case TOP_WS -> Shapes.or(
                    Block.box(2, 0, 0, 16, 9, 14),      // 下部分 檐 (y=0-9)
                    Block.box(1, 9, 0, 16, 16, 15));    // 上部分 台面 (y=9-16)
            case TOP_ES -> Shapes.or(
                    Block.box(0, 0, 0, 14, 9, 14),      // 下部分 檐 (y=0-9)
                    Block.box(0, 9, 0, 15, 16, 15));    // 上部分 台面 (y=9-16)
            case TOP_N -> Shapes.or(
                    Block.box(0, 0, 2, 16, 9, 16),      // 下部分 檐 (y=0-9)
                    Block.box(0, 9, 1, 16, 16, 16));    // 上部分 台面 (y=9-16)
            case TOP_S -> Shapes.or(
                    Block.box(0, 0, 0, 16, 9, 14),      // 下部分 檐 (y=0-9)
                    Block.box(0, 9, 0, 16, 16, 15));    // 上部分 台面 (y=9-16)
            case TOP_W -> Shapes.or(
                    Block.box(2, 0, 0, 16, 9, 16),      // 下部分 檐 (y=0-9)
                    Block.box(1, 9, 0, 16, 16, 16));    // 上部分 台面 (y=9-16)
            case TOP_E -> Shapes.or(
                    Block.box(0, 0, 0, 14, 9, 16),      // 下部分 檐 (y=0-9)
                    Block.box(0, 9, 0, 15, 16, 16));    // 上部分 台面 (y=9-16)
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

    /**
     * 客户端侧登记锚点：放置、生长、落地重建都会触发，中心核心当场就在新位置，不用等客户端扫描。
     *
     * <p>这里用的是 NeoForge 的方块扩展钩子 {@code onBlockStateChange}——它两侧都会调用。原版的
     * {@code onPlace}/{@code onRemove} 在 {@code LevelChunk.setBlockState} 里被 {@code !isClientSide}
     * 挡掉，客户端收不到，所以不能拿它们做这件事。{@code isClientSide} 为假时不会走到客户端那个类。</p>
     */
    @Override
    public void onBlockStateChange(LevelReader level, BlockPos pos, BlockState oldState, BlockState newState) {
        super.onBlockStateChange(level, pos, oldState, newState);
        if (level.isClientSide()) GiantDogeAnvilRenderer.updateAnchor(newState, pos);
    }
}
