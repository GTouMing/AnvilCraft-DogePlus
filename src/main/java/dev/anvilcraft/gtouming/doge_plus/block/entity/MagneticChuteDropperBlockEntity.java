package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.block.MagneticChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.inventory.MagneticChuteDropperMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class MagneticChuteDropperBlockEntity extends AbstractChuteBlockEntity {

    public MagneticChuteDropperBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.MAGNETIC_CHUTE_DROPPER.get(), pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return false;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.MAGNETIC_CHUTE_DROPPER.get());
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return MagneticChuteDropperBlock.FACING;
    }

    @Override
    protected Direction getInputDirection() {
        return getDirection().getOpposite();
    }

    @Override
    protected boolean isEnabled() {
        return true;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.anvilcraft_doge_plus.magnetic_chute_dropper");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new MagneticChuteDropperMenu(i, inventory, this);
    }
}