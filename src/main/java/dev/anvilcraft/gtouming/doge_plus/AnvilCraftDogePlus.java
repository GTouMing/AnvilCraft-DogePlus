package dev.anvilcraft.gtouming.doge_plus;

import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.ChuteDropperScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.ChuteDispenserScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MagneticChuteDropperScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MagneticDispenserScreen;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModCreativeTab;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.init.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(AnvilCraftDogePlus.MOD_ID)
public class AnvilCraftDogePlus {
    public static final String MOD_ID = "anvilcraft_doge_plus";

    public AnvilCraftDogePlus(IEventBus modEventBus, ModContainer ignoredModContainer) {

        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
        ModEntities.register(modEventBus);

        modEventBus.addListener(this::onRegisterScreens);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DOGE_CHUTE.get(), ChuteDispenserScreen::new);
        event.register(ModMenuTypes.MAGNETIC_CHUTE_DISPENSER.get(), MagneticDispenserScreen::new);
        event.register(ModMenuTypes.CHUTE_DROPPER.get(), ChuteDropperScreen::new);
        event.register(ModMenuTypes.MAGNETIC_CHUTE_DROPPER.get(), MagneticChuteDropperScreen::new);
    }
}
