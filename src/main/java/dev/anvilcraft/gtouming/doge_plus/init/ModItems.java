package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModItems {
    static {
        REGISTRUM.defaultCreativeTab(ModCreativeTab.DOGE_PLUS_TAB.getKey());
    }

    public static final ItemEntry<MobileSilencer> MOBILE_SILENCER =
            REGISTRUM.item("mobile_silencer", MobileSilencer::new)
                    .properties(p -> p.stacksTo(1))
                    .register();

    public static void register() {
    }
}
