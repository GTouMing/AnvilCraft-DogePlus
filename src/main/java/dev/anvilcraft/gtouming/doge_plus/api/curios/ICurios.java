package dev.anvilcraft.gtouming.doge_plus.api.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public interface ICurios {

    void register();

    ItemStack findMobileSilencer(Player player);

    void onClientSetup(FMLClientSetupEvent event);
}
