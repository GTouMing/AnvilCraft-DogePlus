package dev.anvilcraft.gtouming.doge_plus.init;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.MOD_ID;

public class ModDataComponentTypes {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(BuiltInRegistries.DATA_COMPONENT_TYPE, MOD_ID);

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<List<ResourceLocation>>> SOUNDS_SET =
            COMPONENTS.register(
                    "sounds_set",
                    () -> DataComponentType.<List<ResourceLocation>>builder()
                            .persistent(ResourceLocation.CODEC.listOf())
                            .networkSynchronized(ByteBufCodecs.fromCodec(ResourceLocation.CODEC.listOf()))
                            .build()
            );

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
