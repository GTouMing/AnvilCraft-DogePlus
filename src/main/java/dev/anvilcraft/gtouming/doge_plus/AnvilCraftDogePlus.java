package dev.anvilcraft.gtouming.doge_plus;

import dev.anvilcraft.gtouming.doge_plus.config.DogePlusConfig;
import dev.anvilcraft.gtouming.doge_plus.init.*;
import dev.anvilcraft.lib.v2.config.ConfigManager;
import dev.anvilcraft.lib.v2.network.register.NetworkRegistrar;
import dev.anvilcraft.lib.v2.registrum.Registrum;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@Mod(AnvilCraftDogePlus.MOD_ID)
public class AnvilCraftDogePlus {
    public static final String MOD_ID = "anvilcraft_doge_plus";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Registrum REGISTRUM = Registrum.create(MOD_ID);
    public static final DogePlusConfig CONFIG = ConfigManager.register(AnvilCraftDogePlus.MOD_ID, DogePlusConfig::new);

    public AnvilCraftDogePlus(IEventBus modEventBus, ModContainer ignoredModContainer) {
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
        ModDataComponentTypes.register(modEventBus);

        ModCurios.register();
        ModBlocks.register();
        ModItems.register();
        ModEntities.register();
        ModBlockEntities.register();
        ModMenuTypes.register();
        ModRecipeTypes.register(modEventBus);

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
