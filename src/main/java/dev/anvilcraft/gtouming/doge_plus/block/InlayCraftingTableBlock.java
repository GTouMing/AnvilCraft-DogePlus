package dev.anvilcraft.gtouming.doge_plus.block;

import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCraftingTableBlockEntity;
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
 * 镶合台：镶合配方的工作方块。
 *
 * <p>一个基材槽 + 由目标产物过滤/配方决定数量的材料槽（最多 6 个，每槽对应一条镶孔要求）。顶面为输入区：手持物品在顶面放入（优先并入空槽/同类槽，
 * 全满时替换并返还旧物），空手右键取出全部；顶部被铁砧砸击时先执行静默镶嵌、再匹配镶合配方
 * （见 {@link InlayCraftingTableBlockEntity#processCrafting}），产物以掉落物形式生成在台体下方，
 * 供漏斗/溜槽等自动化收集。</p>
 */
public class InlayCraftingTableBlock extends Block implements EntityBlock {

    private static final VoxelShape AABB = Shapes.or(
            Block.box(2.0, 12.0, 2.0, 14.0, 16.0, 14.0),
            Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0),
            Block.box(4.0, 0.0, 0.0, 12.0, 10.0, 16.0),
            Block.box(0.0, 0.0, 4.0, 16.0, 10.0, 12.0));
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), AABB, BooleanOp.ONLY_FIRST);

    public InlayCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InlayCraftingTableBlockEntity(ModBlockEntities.INLAY_CRAFTING_TABLE.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.INLAY_CRAFTING_TABLE.get()
                ? (l, p, s, be) -> ((InlayCraftingTableBlockEntity) be).tick()
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
        if (!(be instanceof InlayCraftingTableBlockEntity table)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        boolean top = hit.getDirection() == Direction.UP;
        // 放入仅限顶面（输入区）
        if (!stack.isEmpty() && !top) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        if (stack.isEmpty()) {
            retrieve(table, level, pos, player);
        } else {
            placeStack(table, level, pos, player, stack);
        }
        return ItemInteractionResult.sidedSuccess(false);
    }

    /** 空手取出全部槽位物品。 */
    private static void retrieve(InlayCraftingTableBlockEntity table, Level level, BlockPos pos, Player player) {
        boolean any = false;
        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            ItemStack slotStack = table.getItemHandler().getStackInSlot(slot);
            if (slotStack.isEmpty()) continue;
            player.getInventory().placeItemBackInInventory(slotStack.copy());
            table.setStackInSlot(slot, ItemStack.EMPTY);
            any = true;
        }
        if (any) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
    }

    /** 放入：优先并入可合并的空槽/同类槽；全满时替换第一个接受的槽，旧物返还玩家。 */
    private static void placeStack(
            InlayCraftingTableBlockEntity table, Level level, BlockPos pos, Player player, ItemStack stack) {
        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            if (!table.accepts(slot, stack)) continue;
            ItemStack existing = table.getItemHandler().getStackInSlot(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) continue;

            ItemStack remaining = table.getItemHandler().insertItem(slot, stack, false);
            int moved = stack.getCount() - remaining.getCount();
            if (moved <= 0) continue;
            if (!player.getAbilities().instabuild) stack.shrink(moved);
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            return;
        }

        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            if (!table.accepts(slot, stack)) continue;
            ItemStack existing = table.getItemHandler().getStackInSlot(slot);
            ItemStack stored = stack.copy();
            table.setStackInSlot(slot, stored);
            if (!player.getAbilities().instabuild) stack.shrink(stored.getCount());
            if (!existing.isEmpty()) {
                player.getInventory().placeItemBackInInventory(existing.copy());
            }
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            return;
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        // 方块被移除（换成了其它方块/空气）时，返还全部槽位物品
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof InlayCraftingTableBlockEntity table) {
                for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
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
