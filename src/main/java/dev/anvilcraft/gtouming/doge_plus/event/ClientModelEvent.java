package dev.anvilcraft.gtouming.doge_plus.event;


import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
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
        event.register(ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/anvil_core")));
        event.register(ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/anvil_core1")));
        event.register(ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/anvil_core2")));
    }
}
