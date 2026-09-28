package dev.anvilcraft.gtouming.doge_plus.init;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 本模组使用的物品标签。
 *
 * <p>{@link #TOTEMS} 走 NeoForge 约定的 {@code c:totems}：前置新版本移除了原先的
 * {@code anvilcraft:totem}，而「涅槃」镶嵌材料仍需要一个图腾标签，故改由本模组按约定命名，
 * 并在 {@code data/c/tags/item/totems.json} 中补齐成员（{@code replace: false}，其它模组可继续追加）。</p>
 */
public class ModItemTags {

    /** 图腾：不死图腾 + 前置的追溯图腾 / 狂暴图腾。 */
    public static final TagKey<Item> TOTEMS = TagKey.create(
            Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "totems"));
}
