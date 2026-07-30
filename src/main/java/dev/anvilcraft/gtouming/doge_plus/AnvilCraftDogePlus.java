package dev.anvilcraft.gtouming.doge_plus;

import dev.anvilcraft.gtouming.doge_plus.init.*;
import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(AnvilCraftDogePlus.MOD_ID)
public class AnvilCraftDogePlus {
    public static final String MOD_ID = "anvilcraft_doge_plus";
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);

    public AnvilCraftDogePlus(IEventBus modEventBus, ModContainer ignoredModContainer) {

        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
        ModCurios.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModDataComponentTypes.register(modEventBus);
        modEventBus.addListener(AnvilCraftDogePlus::registerPayload);
        modEventBus.addListener(ModCurios.ICURIOS::onClientSetup);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static void registerPayload(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        NetworkRegistrar.register(registrar, AnvilCraftDogePlus.MOD_ID);
    }
}
