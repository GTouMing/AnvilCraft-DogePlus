package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class RegisterEventHandler {

    @SubscribeEvent
    public static void onRegisterCapability(RegisterCapabilitiesEvent event) {
        // DogeNode 实体物品处理能力
        event.registerEntity(
                Capabilities.ItemHandler.ENTITY_AUTOMATION,
                ModEntities.DOGE_NODE.get(),
                (a, b) -> a.getItemHandler()
        );

        // ===== 所有溜槽变体 =====
        List.of(
                ModBlockEntities.CHUTE_DISPENSER.get(),
                ModBlockEntities.CHUTE_DROPPER.get(),
                ModBlockEntities.MAGNETIC_CHUTE_DROPPER.get(),
                ModBlockEntities.MAGNETIC_CHUTE_DISPENSER.get()
        ).forEach(beType ->
                event.registerBlockEntity(
                        Capabilities.ItemHandler.BLOCK,
                        beType,
                        (be, side) -> be.getItemHandler()
                )
        );

        // ===== 镶嵌台 =====
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.INLAY_TABLE.get(),
                (be, side) -> be.getItemHandler()
        );
    }
}