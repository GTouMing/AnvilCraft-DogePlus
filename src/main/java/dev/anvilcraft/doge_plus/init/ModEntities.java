package dev.anvilcraft.doge_plus.init;

import dev.anvilcraft.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.doge_plus.entity.NodeSupportEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, AnvilCraftDogePlus.MOD_ID);

    public static final Supplier<EntityType<NodeSupportEntity>> NODE_SUPPORT =
            ENTITIES.register("node_support", () -> NodeSupportEntity.createType("node_support"));

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
    }
}