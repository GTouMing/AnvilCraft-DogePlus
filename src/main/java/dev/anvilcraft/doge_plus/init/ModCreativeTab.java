package dev.anvilcraft.doge_plus.init;

import dev.anvilcraft.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnvilCraftDogePlus.MOD_ID);

    public static final Supplier<CreativeModeTab> DOGE_PLUS_TAB = CREATIVE_TABS.register("doge_plus",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.anvilcraft_doge_plus"))
                    .icon(() -> new ItemStack(ModBlocks.CHUTE_DISPENSER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.CHUTE_DISPENSER.get());
                        output.accept(ModBlocks.CHUTE_DROPPER.get());
                        output.accept(ModBlocks.MAGNETIC_CHUTE_DROPPER.get());
                        output.accept(ModBlocks.MAGNETIC_CHUTE_DISPENSER.get());
                    })
                    .build());
}