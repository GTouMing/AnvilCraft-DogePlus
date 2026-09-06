package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonObject;

/**
 * effect 系列镶嵌材料的药水匹配条件生成。
 *
 * <p>与原手写 JSON 相同：普通/喷溅/滞留三种药水 × 42 种 potion 值，
 * 每种都是 {@code neoforge:components} 数据组件匹配条件。通过 {@link MaterialBuilder}
 * 的 {@code dataComponent()} 级联加入。</p>
 */
final class PotionKinds {

    private PotionKinds() {
    }

    private static final String[] POTIONS = {
            "minecraft:water", "minecraft:mundane", "minecraft:thick", "minecraft:awkward",
            "minecraft:night_vision", "minecraft:long_night_vision",
            "minecraft:invisibility", "minecraft:long_invisibility",
            "minecraft:leaping", "minecraft:long_leaping", "minecraft:strong_leaping",
            "minecraft:fire_resistance", "minecraft:long_fire_resistance",
            "minecraft:swiftness", "minecraft:long_swiftness", "minecraft:strong_swiftness",
            "minecraft:slowness", "minecraft:long_slowness", "minecraft:strong_slowness",
            "minecraft:turtle_master", "minecraft:long_turtle_master", "minecraft:strong_turtle_master",
            "minecraft:water_breathing", "minecraft:long_water_breathing",
            "minecraft:healing", "minecraft:strong_healing",
            "minecraft:harming", "minecraft:strong_harming",
            "minecraft:poison", "minecraft:long_poison", "minecraft:strong_poison",
            "minecraft:regeneration", "minecraft:long_regeneration", "minecraft:strong_regeneration",
            "minecraft:strength", "minecraft:long_strength", "minecraft:strong_strength",
            "minecraft:weakness", "minecraft:long_weakness",
            "minecraft:luck", "minecraft:slow_falling", "minecraft:long_slow_falling"
    };

    /**
     * 生成「药水类」镶嵌材料：{@code <name>} 为文件键，物品为 {@code minecraft:<itemId>}。
     *
     * @param name   文件键（不含 {@code .json}）
     * @param itemId 药水物品 id 后缀，如 {@code potion} / {@code splash_potion} / {@code lingering_potion}
     */
    static InlayMaterialData potionInlay(String name, String itemId) {
        MaterialBuilder builder = MaterialBuilder.builder().name(name).attributes("effect");
        for (String potion : POTIONS) {
            JsonObject contents = new JsonObject();
            JsonObject potionContents = new JsonObject();
            potionContents.addProperty("potion", potion);
            contents.add("minecraft:potion_contents", potionContents);
            builder.dataComponent("minecraft:" + itemId, contents);
        }
        return builder.buildInlay();
    }
}