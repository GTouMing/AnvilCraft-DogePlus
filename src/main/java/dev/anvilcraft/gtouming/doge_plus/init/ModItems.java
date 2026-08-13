package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.item.DogeMagnetItem;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.lib.v2.registrum.util.entry.ItemEntry;
import net.minecraft.world.item.Item;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModItems {
    static {
        REGISTRUM.defaultCreativeTab(ModCreativeTab.DOGE_PLUS_TAB.getKey());
    }

    /** Doge 钢锭：材料（高温熔炼/合成材料）。 */
    public static final ItemEntry<Item> DOGE_STEEL_INGOT =
            REGISTRUM.item("doge_steel_ingot", Item::new)
                    .properties(p -> p.stacksTo(64))
                    .register();

    /** 手持 Doge 钢：磁铁工具（可收纳/发射铁砧、放置 Doge 节点）。 */
    public static final ItemEntry<DogeMagnetItem> DOGE_MAGNET =
            REGISTRUM.item("doge_magnet", DogeMagnetItem::new)
                    .properties(p -> p.durability(255))
                    .register();

    public static final ItemEntry<MobileSilencer> MOBILE_SILENCER =
            REGISTRUM.item("mobile_silencer", MobileSilencer::new)
                    .properties(p -> p.stacksTo(1))
                    .register();

    public static void register() {
    }
}
