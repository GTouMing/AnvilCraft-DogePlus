package dev.anvilcraft.doge_plus.init;

import dev.anvilcraft.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.doge_plus.inventory.ChuteDropperMenu;
import dev.anvilcraft.doge_plus.inventory.DogeChuteMenu;
import dev.anvilcraft.doge_plus.inventory.MagneticChuteDropperMenu;
import dev.anvilcraft.doge_plus.inventory.MagneticDispenserMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, AnvilCraftDogePlus.MOD_ID);

    public static final Supplier<MenuType<DogeChuteMenu>> DOGE_CHUTE =
            MENU_TYPES.register("doge_chute", () -> IMenuTypeExtension.create(DogeChuteMenu::fromNetwork));

    public static final Supplier<MenuType<MagneticDispenserMenu>> MAGNETIC_CHUTE_DISPENSER =
            MENU_TYPES.register("magnetic_chute_dispenser", () -> IMenuTypeExtension.create(MagneticDispenserMenu::fromNetwork));

    public static final Supplier<MenuType<ChuteDropperMenu>> CHUTE_DROPPER =
            MENU_TYPES.register("chute_dropper", () -> IMenuTypeExtension.create(ChuteDropperMenu::fromNetwork));

    public static final Supplier<MenuType<MagneticChuteDropperMenu>> MAGNETIC_CHUTE_DROPPER =
            MENU_TYPES.register("magnetic_chute_dropper", () -> IMenuTypeExtension.create(MagneticChuteDropperMenu::fromNetwork));
}
