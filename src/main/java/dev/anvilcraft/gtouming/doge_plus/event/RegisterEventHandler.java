package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import net.minecraft.core.Direction;
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

        // ===== 镶嵌台 / 镶合台 =====
        // 与前置加工台一致：底面（DOWN）不暴露物品能力（返回 null）。产物是从台体下方掉出来的，
        // 台下的溜槽输入端朝上、查询能力时给的正是 DOWN 侧，拿到 null 后才会回退去吸地上的产物；
        // 若底面也返回 handler，溜槽只会走「从材料槽抽取」的分支，永远吸不到产物。
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.INLAY_TABLE.get(),
                (be, side) -> side == Direction.DOWN ? null : be.getItemHandler()
        );
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.INLAY_CRAFTING_TABLE.get(),
                (be, side) -> side == Direction.DOWN ? null : be.getItemHandler()
        );
    }
}