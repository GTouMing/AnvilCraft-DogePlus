package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.block.ChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.inventory.DogeChuteMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ChuteDispenserBlockEntity extends AbstractChuteBlockEntity {

    public ChuteDispenserBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CHUTE_DISPENSER.get(), pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return Direction.UP == direction;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.CHUTE_DISPENSER.get());
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return ChuteDispenserBlock.FACING;
    }

    @Override
    protected Direction getInputDirection() {
        return Direction.UP;
    }

    @Override
    protected boolean isEnabled() {
        return !getBlockState().getValue(ChuteDispenserBlock.TRIGGERED);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.anvilcraft_doge_plus.chute_dispenser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new DogeChuteMenu(i, inventory, this);
    }
}