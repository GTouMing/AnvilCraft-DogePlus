package dev.anvilcraft.gtouming.doge_plus.block.entity.chute;

import dev.anvilcraft.gtouming.doge_plus.block.chute.ChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.AbstractChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModMenuTypes;
import dev.anvilcraft.gtouming.doge_plus.inventory.ChuteDropperMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ChuteDropperBlockEntity extends AbstractChuteBlockEntity {

    public ChuteDropperBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return Direction.UP == direction;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.CHUTE_DROPPER.get());
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return ChuteDropperBlock.FACING;
    }

    @Override
    protected Direction getInputDirection() {
        return Direction.UP;
    }

    @Override
    protected boolean isEnabled() {
        return !getBlockState().getValue(AbstractChuteDropperBlock.TRIGGERED);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.anvilcraft_doge_plus.chute_dropper");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new ChuteDropperMenu(ModMenuTypes.CHUTE_DROPPER.get(), i, inventory, this);
    }
}