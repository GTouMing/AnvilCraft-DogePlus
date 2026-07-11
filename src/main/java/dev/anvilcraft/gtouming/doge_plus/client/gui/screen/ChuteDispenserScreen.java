package dev.anvilcraft.gtouming.doge_plus.client.gui.screen;

import dev.anvilcraft.gtouming.doge_plus.inventory.DogeChuteMenu;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ChuteDispenserScreen extends AbstractChuteScreen<DogeChuteMenu> {
    public ChuteDispenserScreen(DogeChuteMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected boolean shouldSkipDirection(Direction direction) {
        return direction == Direction.UP;
    }
}