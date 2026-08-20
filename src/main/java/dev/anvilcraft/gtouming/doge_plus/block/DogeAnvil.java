package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.inventory.DogeMenu;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.ModSoundEvents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.CONFIG;

public class DogeAnvil extends BetterAnvilBlock implements IHammerRemovable {
    private static final VoxelShape BASE = Block.box(2.0, 0.0, 2.0, 14.0, 4.0, 14.0);
    private static final VoxelShape X_LEG1 = Block.box(4.0, 4.0, 5.0, 12.0, 10.0, 11.0);
    private static final VoxelShape X_TOP = Block.box(0.0, 10.0, 3.0, 16.0, 16.0, 13.0);
    private static final VoxelShape Z_LEG1 = Block.box(5.0, 4.0, 4.0, 11.0, 10.0, 12.0);
    private static final VoxelShape Z_TOP = Block.box(3.0, 10.0, 0.0, 13.0, 16.0, 16.0);
    private static final VoxelShape X_AXIS_AABB = Shapes.or(BASE, X_LEG1, X_TOP);
    private static final VoxelShape Z_AXIS_AABB = Shapes.or(BASE, Z_LEG1, Z_TOP);
    public static final Component CONTAINER_TITLE = Component.translatable("container.repair");

    /**
     * 成长值方块状态固定注册范围（方块状态在类加载时注册，早于配置加载；
     * 该范围需不小于配置中的成长上限，实际上限由配置 {@code dogeGrowthMax} 控制）。
     */
    public static final int GROWTH_PROPERTY_MAX = 1024;
    /** 成长值方块状态：0..GROWTH_PROPERTY_MAX。 */
    public static final IntegerProperty GROWTH = IntegerProperty.create("growth", 0, GROWTH_PROPERTY_MAX);

    @Override
    public MapCodec<AnvilBlock> codec() {
        return simpleCodec(DogeAnvil::new);
    }

    public DogeAnvil(Properties properties) {
        super(properties);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(GROWTH);
    }

    @Override
    public InteractionResult use(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        int growth = state.getValue(GROWTH);
        int maxGrowth = CONFIG.maxGrowth;
        // 成长值已满：任意右键尝试长成巨型 Doge 砧
        if (growth >= maxGrowth) {
            if (!tryGrowToGiant(level, pos)) {
                player.displayClientMessage(
                        Component.translatable("message.anvilcraft_doge_plus.doge_anvil.no_space"), true);
            }
            return InteractionResult.CONSUME;
        }

        // 手持生肉喂食，提升成长值
        ItemStack held = player.getItemInHand(hand);
        if (isRawMeat(held)) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            int newGrowth = Math.min(growth + CONFIG.growthPerMeat, maxGrowth);
            level.setBlock(pos, state.setValue(GROWTH, newGrowth), 3);
            player.displayClientMessage(
                    Component.translatable(
                            "message.anvilcraft_doge_plus.doge_anvil.growth", newGrowth, maxGrowth),
                    true);
            if (newGrowth >= maxGrowth && !tryGrowToGiant(level, pos)) {
                player.displayClientMessage(
                        Component.translatable("message.anvilcraft_doge_plus.doge_anvil.no_space"), true);
            }
            return InteractionResult.CONSUME;
        }

        // 否则打开铁砧菜单
        ModMenuTypes.open((ServerPlayer) player, Objects.requireNonNull(state.getMenuProvider(level, pos)));
        player.awardStat(Stats.INTERACT_WITH_ANVIL);
        return InteractionResult.CONSUME;
    }

    /**
     * 判断是否为生肉（前置模组 {@code c:foods/raw_*} 系列 tag）。
     */
    private static boolean isRawMeat(ItemStack stack) {
        return stack.is(ModItemTags.RAW_BEEF)
                || stack.is(ModItemTags.RAW_PORKCHOP)
                || stack.is(ModItemTags.RAW_CHICKEN)
                || stack.is(ModItemTags.RAW_MUTTON)
                || stack.is(ModItemTags.RAW_RABBIT);
    }

    /**
     * 把 {@code pos} 处的小型 Doge 砧原地长成 3×3×3 巨型 Doge 砧。
     * 以 {@code pos} 为巨型砧底部中心（BOTTOM_CENTER）。
     *
     * @return 空间不足时返回 false，不改变方块
     */
    private static boolean tryGrowToGiant(Level level, BlockPos pos) {
        // 检查除 pos 本身外其余 26 个部件位置是否可替换
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            if (part == Cube3x3PartHalf.BOTTOM_CENTER) continue;
            BlockPos bp = pos.offset(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());
            if (!level.getBlockState(bp).canBeReplaced()) return false;
        }
        BlockState base = ModBlocks.GIANT_DOGE_ANVIL.get().defaultBlockState();
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            BlockPos bp = pos.offset(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());
            BlockState partState = base
                    .setValue(GiantDogeAnvil.HALF, part)
                    .setValue(GiantDogeAnvil.CUBE,
                            part == Cube3x3PartHalf.MID_CENTER ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER);
            level.setBlockAndUpdate(bp, partState);
        }
        level.playSound(null, pos, ModSoundEvents.GIANT_ANVIL_LAND.get(), SoundSource.BLOCKS, 0.55F, 0.9F);
        return true;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return new SimpleMenuProvider(
                (syncId, inventory, player) ->
                        new DogeMenu(syncId, inventory, ContainerLevelAccess.create(level, pos)),
                CONTAINER_TITLE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction direction = state.getValue(FACING);
        return direction.getAxis() == Direction.Axis.X ? X_AXIS_AABB : Z_AXIS_AABB;
    }

    @Override
    public void falling(FallingBlockEntity entity) {
        entity.setHurtsEntities(2.0f, 20);
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        if (entity instanceof Player) {
            level.playSound(null, pos, SoundEvents.WOLF_AMBIENT, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        super.fallOn(level, state, pos, entity, fallDistance);
    }

    public static void damage(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        level.levelEvent(1029, pos, 0);
        if (EntityType.WOLF.spawn(
                (ServerLevel) level,
                null, null,
                pos,
                MobSpawnType.SPAWN_EGG,
                true,
                false
        ) != null) {
            level.gameEvent(null, GameEvent.ENTITY_PLACE, pos);
        }
    }
}
