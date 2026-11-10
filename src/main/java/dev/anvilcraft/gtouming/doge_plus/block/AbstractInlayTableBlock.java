package dev.anvilcraft.gtouming.doge_plus.block;

import dev.anvilcraft.gtouming.doge_plus.block.entity.AbstractProcessingTableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 镶嵌台 / 镶合台共用的工作方块。
 *
 * <p>统一的顶部输入区、空手取出、方块移除时掉落全部槽位物品。具体「怎么取、怎么放」
 * 由子类实现，两者的槽位规则差异不会散到方块类里。</p>
 */
public abstract class AbstractInlayTableBlock extends Block implements EntityBlock {

    private static final VoxelShape TABLE_BOX = Shapes.or(
            Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
            Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
            Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), TABLE_BOX, BooleanOp.ONLY_FIRST);

    protected AbstractInlayTableBlock(Properties properties) {
        super(properties);
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
        if (!(be instanceof AbstractProcessingTableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        boolean top = hit.getDirection() == Direction.UP;
        // 放入仅限顶面（输入区）
        if (!stack.isEmpty() && !top) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (stack.isEmpty()) {
            retrieve(table, level, pos, player);
        } else {
            place(table, level, pos, player, stack);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    /** 空手右键：取出槽位物品。 */
    protected abstract void retrieve(
            AbstractProcessingTableBlockEntity table, Level level, BlockPos pos, Player player);

    /** 顶面放入：把主手物品放入槽位。 */
    protected abstract void place(
            AbstractProcessingTableBlockEntity table, Level level, BlockPos pos, Player player, ItemStack stack);

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        // 方块被移除（换成了其它方块/空气）时，返还所有槽位物品
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof AbstractProcessingTableBlockEntity table) {
                for (int slot = 0; slot < table.getItemHandler().getSlots(); slot++) {
                    ItemStack stack = table.getItemHandler().getStackInSlot(slot);
                    if (stack.isEmpty()) continue;
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
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}
