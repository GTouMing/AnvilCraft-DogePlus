package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * 镶嵌台：镶嵌配方的工作方块。
 *
 * <p>顶面为输入区：手持物品放入时按栈式交互（全空→基材，否则→镶嵌材料，
 * 替换仅能替换镶嵌材料）；空手右键顶面取出全部（基材+材料+产品+旧镶嵌物）。
 * 其他面空手右键取出产品与旧镶嵌物。
 * 顶部被铁砧砸击时执行镶嵌（见 {@link InlayTableBlockEntity#processInlay}）。</p>
 */
public class InlayTableBlock extends Block implements EntityBlock {

    private static final VoxelShape AABB = Shapes.or(
            Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
            Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
            Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), AABB, BooleanOp.ONLY_FIRST);

    public InlayTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<InlayTableBlock> codec() {
        return simpleCodec(InlayTableBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InlayTableBlockEntity(ModBlockEntities.INLAY_TABLE.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.INLAY_TABLE.get()
                ? (l, p, s, be) -> ((InlayTableBlockEntity) be).tick()
                : null;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof InlayTableBlockEntity table)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        boolean top = hit.getDirection() == Direction.UP;

        // 放入仅限顶面（输入区）
        if (!stack.isEmpty() && !top) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (stack.isEmpty()) {

            if (top) {
                retrieve(table, level, pos, player, InlayTableBlockEntity.SLOT_MATERIAL);
                retrieve(table, level, pos, player, InlayTableBlockEntity.SLOT_BASE);
            }
            retrieve(table, level, pos, player, InlayTableBlockEntity.SLOT_PRODUCT);
            retrieve(table, level, pos, player, InlayTableBlockEntity.SLOT_OLD_MATERIAL);
        } else {
            // 顶面放入：材料与基材按栈式交互
            placeStack(table, level, pos, player, stack);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    /** 空手取出指定槽位的物品。 */
    private static void retrieve(InlayTableBlockEntity table, Level level, BlockPos pos, Player player, int slot) {
        ItemStack slotStack = table.getStackInSlot(slot);
        if (slotStack.isEmpty()) return;
        player.getInventory().placeItemBackInInventory(slotStack.copy());
        table.setStackInSlot(slot, ItemStack.EMPTY);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
    }

    /**
     * 放入：基材槽为空则放基材，否则放镶嵌材料（材料槽已有则替换，旧材料返还玩家）。
     * 掉落物统一收集为材料（见 {@code InlayTableBlockEntity#tick}），基材经右键放入。
     */
    private static void placeStack(InlayTableBlockEntity table, Level level, BlockPos pos, Player player, ItemStack stack) {
        int target = table.getStackInSlot(InlayTableBlockEntity.SLOT_BASE).isEmpty()
                ? InlayTableBlockEntity.SLOT_BASE
                : InlayTableBlockEntity.SLOT_MATERIAL;

        ItemStack existing = table.getStackInSlot(target);
        ItemStack stored = stack.copy();
        table.setStackInSlot(target, stored);
        if (!player.getAbilities().instabuild) stack.shrink(stored.getCount());
        if (!existing.isEmpty()) {
            player.getInventory().placeItemBackInInventory(existing.copy());
        }
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        // 方块被移除（换成了其它方块/空气）时，返还所有槽位物品
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InlayTableBlockEntity table) {
                for (int i = 0; i < InlayTableBlockEntity.SLOT_COUNT; i++) {
                    ItemStack stack = table.getStackInSlot(i);
                    if (!stack.isEmpty()) {
                        ItemEntity item = new ItemEntity(
                                level,
                                pos.getX() + 0.5,
                                pos.getY() + 0.5,
                                pos.getZ() + 0.5,
                                stack);
                        item.setDefaultPickUpDelay();
                        level.addFreshEntity(item);
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
