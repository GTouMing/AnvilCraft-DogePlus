package dev.anvilcraft.gtouming.doge_plus.event;


import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.model.CarrierWireGeometry;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public class ClientModelEvent {
    @SubscribeEvent
    public static void onModelRegister(ModelEvent.RegisterAdditional event) {
        event.register(ModelResourceLocation.standalone(AnvilCraftDogePlus.of("item/mobile_silencer_3d")));
        event.register(ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/giant_doge_anvil_light")));
    }

    @SubscribeEvent
    public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        event.register(AnvilCraftDogePlus.of("carrier_wire"), new CarrierWireGeometry.Loader());
    }
}
