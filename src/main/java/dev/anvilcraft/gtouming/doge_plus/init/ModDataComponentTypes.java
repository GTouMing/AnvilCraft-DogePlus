package dev.anvilcraft.gtouming.doge_plus.init;

import com.mojang.serialization.Codec;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
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

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<List<ResourceLocation>>> SOUNDS_LIST =
            COMPONENTS.register(
                    "sounds_list",
                    () -> DataComponentType.<List<ResourceLocation>>builder()
                            .persistent(ResourceLocation.CODEC.listOf())
                            .networkSynchronized(ByteBufCodecs.fromCodec(ResourceLocation.CODEC.listOf()))
                            .build()
            );

    public static final DeferredHolder<DataComponentType<?>,DataComponentType<ResourceLocation>> MAGNET_HAS_ANVIL =
            COMPONENTS.register(
                    "magnet_has_anvil",
                    () -> DataComponentType.<ResourceLocation>builder()
                            .persistent(ResourceLocation.CODEC)
                            .networkSynchronized(ByteBufCodecs.fromCodec(ResourceLocation.CODEC))
                            .build()
            );

    /** 镶嵌：记录已镶嵌的材料物品 ID 列表（长度 = 已占用镶孔数）。 */
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<List<InlayEntry>>> INLAY =
            COMPONENTS.register(
                    "inlay",
                    () -> DataComponentType.<List<InlayEntry>>builder()
                            .persistent(InlayEntry.CODEC.listOf())
                            .networkSynchronized(InlayEntry.STREAM_CODEC.apply(ByteBufCodecs.list()))
                            .build()
            );

    /** 高温：累加的伤害值。 */
    public static final DeferredHolder<DataComponentType<?>,DataComponentType<Integer>> HEAT =
            COMPONENTS.register(
                    "heat",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .build()
            );

    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
    }
}
