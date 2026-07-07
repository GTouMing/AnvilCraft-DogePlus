package dev.anvilcraft.doge_plus.client.gui.screen;

import dev.anvilcraft.doge_plus.inventory.MagneticDispenserMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MagneticDispenserScreen extends AbstractChuteScreen<MagneticDispenserMenu> {
    public MagneticDispenserScreen(MagneticDispenserMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return false;
    }
}