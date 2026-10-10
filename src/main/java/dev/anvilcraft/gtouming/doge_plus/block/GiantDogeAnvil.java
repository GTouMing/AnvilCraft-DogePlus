package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.GiantDogeAnvilRenderer;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.lib.v2.util.ShapeUtil;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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

    // ==================== 碰撞形状 ====================
    // 仿前置 GiantAnvilBlock：形状写成 0..16 的静态字段（初始化只算一次）；每层只定义「西北角」「北边」
    // 「中心」三个 canonical，其余按 Y 轴 90°/180°/270° 旋转得到（模型 90° 对称）。
    // 碰撞/遮挡走 getPartShape（前置 AbstractMultiPartBlock 的约定）；45° 的角柱 / 斜撑用外接盒概括即可——
    // 贴合模型的斜面由「选中描边」负责（anvillib-cube 的 CubeSelection 直接按模型几何画，见 AnvilCraftDogePlusClient）。
    // 三个 CENTER 部件取整块实心（同前置 Shapes.block()）：模型在中心留出了旋转核心的空腔，若碰撞也照模型留空，
    // 从模型高度到 corner 就会有一段可站的空白，整块填实才与外观一致。

    /** 底部：两层台基 + 四角 45° 柱（外接盒）。 */
    private static final VoxelShape BOTTOM_WN = ShapeUtil.merge(
            new AABB(4, 0, 4, 16, 8, 16),
            new AABB(8, 8, 8, 16, 11, 16),
            new AABB(12.757, 11, 12.757, 16, 16, 16));
    private static final VoxelShape BOTTOM_N = ShapeUtil.merge(
            new AABB(0, 0, 4, 16, 8, 16),
            new AABB(0, 8, 8, 16, 11, 16),
            new AABB(0, 11, 12.757, 5.243, 16, 16),
            new AABB(10.757, 11, 12.757, 16, 16, 16));
    private static final VoxelShape BOTTOM_CENTER = Shapes.block();
    private static final VoxelShape BOTTOM_WS = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_WN);
    private static final VoxelShape BOTTOM_ES = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_WN);
    private static final VoxelShape BOTTOM_EN = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_WN);
    private static final VoxelShape BOTTOM_W = ShapeUtil.rotate(Direction.Axis.Y, 90, BOTTOM_N);
    private static final VoxelShape BOTTOM_S = ShapeUtil.rotate(Direction.Axis.Y, 180, BOTTOM_N);
    private static final VoxelShape BOTTOM_E = ShapeUtil.rotate(Direction.Axis.Y, 270, BOTTOM_N);

    /** 中部：四角柱 + 十字梁 + 台面下沿 + 角撑（外接盒）。 */
    private static final VoxelShape MID_WN = ShapeUtil.merge(
            new AABB(1, 14, 1, 16, 16, 16),
            new AABB(12.757, 0, 12.757, 16, 11, 16),
            new AABB(11.172, 10, 11.172, 16, 14, 16));
    private static final VoxelShape MID_N = ShapeUtil.merge(
            new AABB(0, 14, 1, 16, 16, 16),
            new AABB(0, 0, 12.757, 5.243, 11, 16),
            new AABB(10.757, 0, 12.757, 16, 11, 16),
            new AABB(0, 11, 9, 16, 14, 16),
            new AABB(15.172, 10, 11.172, 16, 14, 16),
            new AABB(0, 10, 11.172, 0.828, 14, 16));
    private static final VoxelShape MID_CENTER = Shapes.block();
    private static final VoxelShape MID_WS = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_WN);
    private static final VoxelShape MID_ES = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_WN);
    private static final VoxelShape MID_EN = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_WN);
    private static final VoxelShape MID_W = ShapeUtil.rotate(Direction.Axis.Y, 90, MID_N);
    private static final VoxelShape MID_S = ShapeUtil.rotate(Direction.Axis.Y, 180, MID_N);
    private static final VoxelShape MID_E = ShapeUtil.rotate(Direction.Axis.Y, 270, MID_N);

    /** 顶部：三级台面，下窄上宽。 */
    private static final VoxelShape TOP_WN = ShapeUtil.merge(
            new AABB(0, 8, 0, 16, 16, 16),
            new AABB(1, 0, 1, 16, 4, 16),
            new AABB(4, 4, 4, 16, 8, 16));
    private static final VoxelShape TOP_N = ShapeUtil.merge(
            new AABB(0, 8, 0, 16, 16, 16),
            new AABB(0, 0, 1, 16, 4, 16),
            new AABB(0, 4, 4, 16, 8, 16));
    private static final VoxelShape TOP_CENTER = Shapes.block();
    private static final VoxelShape TOP_WS = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_WN);
    private static final VoxelShape TOP_ES = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_WN);
    private static final VoxelShape TOP_EN = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_WN);
    private static final VoxelShape TOP_W = ShapeUtil.rotate(Direction.Axis.Y, 90, TOP_N);
    private static final VoxelShape TOP_S = ShapeUtil.rotate(Direction.Axis.Y, 180, TOP_N);
    private static final VoxelShape TOP_E = ShapeUtil.rotate(Direction.Axis.Y, 270, TOP_N);

    public GiantDogeAnvil(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GiantDogeAnvil> codec() {
        return simpleCodec(GiantDogeAnvil::new);
    }

    /**
     * 按模型轮廓给出每个「部件」自身的碰撞 / 遮挡形状（仿前置 {@code GiantAnvilBlock#getPartShape}）。
     *
     * <p>前置 {@code AbstractMultiPartBlock} 用本方法作为 {@code getCollisionShape} 与 {@code getOcclusionShape}；
     * 描边用的 {@code getShape} 则把所有部件的本形状按各自偏移并起来得到整套结构轮廓。所以这里覆写的是
     * {@code getPartShape}，而不是 {@code getShape}。</p>
     */
    @Override
    public VoxelShape getPartShape(BlockState state) {
        return switch (state.getValue(HALF)) {
            case BOTTOM_WN -> BOTTOM_WN;
            case BOTTOM_N -> BOTTOM_N;
            case BOTTOM_EN -> BOTTOM_EN;
            case BOTTOM_W -> BOTTOM_W;
            case BOTTOM_E -> BOTTOM_E;
            case BOTTOM_WS -> BOTTOM_WS;
            case BOTTOM_S -> BOTTOM_S;
            case BOTTOM_ES -> BOTTOM_ES;
            case BOTTOM_CENTER -> BOTTOM_CENTER;
            case MID_WN -> MID_WN;
            case MID_N -> MID_N;
            case MID_EN -> MID_EN;
            case MID_W -> MID_W;
            case MID_E -> MID_E;
            case MID_WS -> MID_WS;
            case MID_S -> MID_S;
            case MID_ES -> MID_ES;
            case MID_CENTER -> MID_CENTER;
            case TOP_WN -> TOP_WN;
            case TOP_N -> TOP_N;
            case TOP_EN -> TOP_EN;
            case TOP_W -> TOP_W;
            case TOP_E -> TOP_E;
            case TOP_WS -> TOP_WS;
            case TOP_S -> TOP_S;
            case TOP_ES -> TOP_ES;
            case TOP_CENTER -> TOP_CENTER;
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
