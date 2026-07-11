package dev.anvilcraft.gtouming.doge_plus.client.gui.screen;

import dev.anvilcraft.gtouming.doge_plus.inventory.MagneticChuteDropperMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MagneticChuteDropperScreen extends AbstractChuteScreen<MagneticChuteDropperMenu> {
    public MagneticChuteDropperScreen(MagneticChuteDropperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return false;
    }
}