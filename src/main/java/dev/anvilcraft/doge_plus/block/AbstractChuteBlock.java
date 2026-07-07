package dev.anvilcraft.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.doge_plus.block.entity.AbstractChuteBlockEntity;
import dev.dubhe.anvilcraft.api.itemhandler.FilteredItemStackHandler;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import static dev.dubhe.anvilcraft.block.ChuteBlock.getFacing;
import static dev.dubhe.anvilcraft.block.ChuteBlock.isChuteBlock;

public abstract class AbstractChuteBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public AbstractChuteBlock(Properties properties) {
        super(properties);
    }

    /**
     * 磁力变体允许朝上放置（输出朝上），非磁力变体强制朝下。
     */
    protected boolean canFaceUp() {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction dir = context.getClickedFace().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) dir = dir.getOpposite();
        Direction facing = canFaceUp() ? dir : (dir.getAxis() == Direction.Axis.Y ? Direction.DOWN : dir);
        BlockState result = getState(context.getLevel(), context.getClickedPos(), facing);
        Player player = context.getPlayer();
        if (result == null && player != null) {
            player.displayClientMessage(Component.translatable("message.anvilcraft.chute.cannot_place"), true);
        }
        return result;
    }

    @Nullable
    BlockState getState(Level level, BlockPos pos, Direction facing) {
        BlockState result = this.defaultBlockState()
                .setValue(FACING, facing);
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            if (isChuteBlock(neighborState)) {
                if (getFacing(neighborState) == dir.getOpposite()) {
                    if (dir == Direction.DOWN) {
                        if (facing == Direction.DOWN) {
                            return null;
                        }
                    } else {
                        if (facing.getOpposite() == getFacing(neighborState)) {
                            facing = facing.getOpposite();
                        }
                        BlockState backState = level.getBlockState(pos.relative(facing));
                        if (isChuteBlock(backState) && getFacing(backState) == facing.getOpposite()) {
                            return null;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public abstract MapCodec<? extends BaseEntityBlock> codec();

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AbstractChuteBlockEntity) {
            player.openMenu((AbstractChuteBlockEntity) be, pos);
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof AbstractChuteBlockEntity chute) {
                chute.tick();
            }
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AbstractChuteBlockEntity be) {
            return be.getRedstoneSignal();
        }
        return 0;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            if (level.getBlockEntity(pos) instanceof BaseChuteBlockEntity entity) {
                Vec3 vec3 = entity.getBlockPos().getCenter();
                FilteredItemStackHandler depository = entity.getItemHandler();
                for (int slot = 0; slot < depository.getSlots(); slot++) {
                    Containers.dropItemStack(level, vec3.x, vec3.y, vec3.z, depository.getStackInSlot(slot));
                }
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}