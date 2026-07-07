package dev.anvilcraft.doge_plus.init;

import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.doge_plus.AnvilCraftDogePlus.MOD_ID;
import static dev.anvilcraft.doge_plus.init.ModBlocks.CHUTE_DISPENSER;
import static dev.anvilcraft.doge_plus.init.ModBlocks.CHUTE_DROPPER;
import static dev.anvilcraft.doge_plus.init.ModBlocks.MAGNETIC_CHUTE_DISPENSER;
import static dev.anvilcraft.doge_plus.init.ModBlocks.MAGNETIC_CHUTE_DROPPER;

public class ModItems {
    public static final DeferredRegister.Items MOD_ITEMS =
            DeferredRegister.createItems(MOD_ID);

    public static final DeferredItem<BlockItem> CHUTE_DISPENSER_ITEM =
            MOD_ITEMS.registerSimpleBlockItem(CHUTE_DISPENSER);

    public static final DeferredItem<BlockItem> CHUTE_DROPPER_ITEM =
            MOD_ITEMS.registerSimpleBlockItem(CHUTE_DROPPER);

    public static final DeferredItem<BlockItem> MAGNETIC_CHUTE_DROPPER_ITEM =
            MOD_ITEMS.registerSimpleBlockItem(MAGNETIC_CHUTE_DROPPER);

    public static final DeferredItem<BlockItem> MAGNETIC_CHUTE_DISPENSER_ITEM =
            MOD_ITEMS.registerSimpleBlockItem(MAGNETIC_CHUTE_DISPENSER);

    public static void register(IEventBus bus) {
        MOD_ITEMS.register(bus);
    }
}
