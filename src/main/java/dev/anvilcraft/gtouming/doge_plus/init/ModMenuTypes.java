package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.ChuteDispenserScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.ChuteDropperScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MagneticChuteDropperScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MagneticDispenserScreen;
import dev.anvilcraft.gtouming.doge_plus.inventory.ChuteDropperMenu;
import dev.anvilcraft.gtouming.doge_plus.inventory.ChuteDispenserMenu;
import dev.anvilcraft.gtouming.doge_plus.inventory.MagneticChuteDropperMenu;
import dev.anvilcraft.gtouming.doge_plus.inventory.MagneticDispenserMenu;
import dev.anvilcraft.lib.v2.registrum.util.entry.MenuEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModMenuTypes {
    public static final MenuEntry<ChuteDispenserMenu> CHUTE_DISPENSER =
            REGISTRUM.menu("chute_dispenser", ChuteDispenserMenu::new, () -> ChuteDispenserScreen::new)
                    .register();

    public static final MenuEntry<MagneticDispenserMenu> MAGNETIC_CHUTE_DISPENSER =
            REGISTRUM.menu("magnetic_chute_dispenser", MagneticDispenserMenu::new, () -> MagneticDispenserScreen::new)
                    .register();

    public static final MenuEntry<ChuteDropperMenu> CHUTE_DROPPER =
            REGISTRUM.menu("chute_dropper", ChuteDropperMenu::new, () -> ChuteDropperScreen::new)
                    .register();

    public static final MenuEntry<MagneticChuteDropperMenu> MAGNETIC_CHUTE_DROPPER =
            REGISTRUM.menu("magnetic_chute_dropper", MagneticChuteDropperMenu::new, () -> MagneticChuteDropperScreen::new)
                    .register();

    public static void register() {
    }
}
