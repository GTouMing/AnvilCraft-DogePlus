package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

/**
 * 注册数据包重载监听器（材料/镶孔数据）。
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class DogePlusDataPackEvent {

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(MaterialManager.INSTANCE);
    }
}
