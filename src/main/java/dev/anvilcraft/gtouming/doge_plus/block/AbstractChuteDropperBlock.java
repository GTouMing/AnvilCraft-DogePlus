package dev.anvilcraft.gtouming.doge_plus.block;

import dev.anvilcraft.gtouming.doge_plus.block.entity.AbstractChuteBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.items.IItemHandler;

public abstract class AbstractChuteDropperBlock extends AbstractChuteBlock {
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public AbstractChuteDropperBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(TRIGGERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        boolean hasPower = level.hasNeighborSignal(pos) || level.hasNeighborSignal(pos.above());
        boolean isTriggered = state.getValue(TRIGGERED);
        if (hasPower && !isTriggered) {
            level.scheduleTick(pos, this, 4);
            level.setBlock(pos, state.setValue(TRIGGERED, true), 2);
        } else if (!hasPower && isTriggered) {
            level.setBlock(pos, state.setValue(TRIGGERED, false), 2);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        dispenseFrom(level, state, pos);
    }

    void dispenseFrom(ServerLevel level, BlockState state, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (!(be instanceof AbstractChuteBlockEntity chute)) return;

        IItemHandler itemHandler = chute.getItemHandler();
        Direction facing = state.getValue(FACING);
        boolean anyDispensed = false;

        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack stack = itemHandler.extractItem(slot, 1, true);
            if (stack.isEmpty()) continue;

            itemHandler.extractItem(slot, 1, false);
            anyDispensed = true;
            ejectItem(level, pos, facing, stack);
        }

        if (!anyDispensed) {
            level.levelEvent(1001, pos, 0);
        }
    }

    static void ejectItem(Level level, BlockPos pos, Direction facing, ItemStack stack) {
        double x = pos.getX() + 0.5 + facing.getStepX() * 0.7;
        double y = pos.getY() + 0.5 + facing.getStepY() * 0.7;
        double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.7;
        var itemEntity = new ItemEntity(level, x, y, z, stack, 0, 0, 0);
        itemEntity.setDeltaMovement(
                facing.getStepX() * 0.3,
                facing.getStepY() * 0.3,
                facing.getStepZ() * 0.3
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
        level.levelEvent(1000, pos, 0);
    }
}
