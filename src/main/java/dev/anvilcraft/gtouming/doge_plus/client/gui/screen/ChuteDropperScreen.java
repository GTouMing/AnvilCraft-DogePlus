package dev.anvilcraft.gtouming.doge_plus.client.gui.screen;

import dev.anvilcraft.gtouming.doge_plus.inventory.ChuteDropperMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ChuteDropperScreen extends AbstractChuteScreen<ChuteDropperMenu> {
    public ChuteDropperScreen(ChuteDropperMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return direction == Direction.UP;
    }
}