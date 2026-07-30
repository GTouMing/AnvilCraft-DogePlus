package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AnvilCraftDogePlus.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> DOGE_PLUS_TAB = CREATIVE_TABS.register("doge_plus",
            () -> CreativeModeTab.builder()
                    .title(REGISTRUM.addLang("itemGroup", AnvilCraftDogePlus.of("doge_plus"), "AnvilCraft:Doge+"))
                    .icon(ModBlocks.CHUTE_DISPENSER::asStack)
                    .displayItems((parameters, output) -> {})
                    .build());
}