package dev.anvilcraft.gtouming.doge_plus.block.entity.chute;

import dev.anvilcraft.gtouming.doge_plus.block.chute.MagneticChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModMenuTypes;
import dev.anvilcraft.gtouming.doge_plus.inventory.MagneticDispenserMenu;
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

public class MagneticChuteDispenserBlockEntity extends AbstractChuteBlockEntity {

    public MagneticChuteDispenserBlockEntity(BlockEntityType<? extends BlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return false;
    }

    @Override
    protected boolean validateBlockState(BlockState state) {
        return state.is(ModBlocks.MAGNETIC_CHUTE_DISPENSER.get());
    }

    @Override
    protected DirectionProperty getFacingProperty() {
        return MagneticChuteDispenserBlock.FACING;
    }

    @Override
    protected Direction getInputDirection() {
        return getDirection().getOpposite();
    }

    @Override
    protected boolean isEnabled() {
        return !getBlockState().getValue(MagneticChuteDispenserBlock.TRIGGERED);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.anvilcraft_doge_plus.magnetic_chute_dispenser");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        if (player.isSpectator()) return null;
        return new MagneticDispenserMenu(ModMenuTypes.MAGNETIC_CHUTE_DISPENSER.get(), i, inventory, this);
    }
}