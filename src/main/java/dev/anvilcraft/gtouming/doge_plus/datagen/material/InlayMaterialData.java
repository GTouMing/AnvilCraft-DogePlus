package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialBuilder.builder;

/**
 * 镶嵌材料（inlay material）源数据：{@code data/anvilcraft_doge_plus/material/inlay/<name>.json}。
 * <p>通过 {@link MaterialBuilder} 声明式描述 ingredient 匹配条件（物品/标签/带数据组件的药水），
 * 序列化后与原手写内容等价。effect 系列（普通/喷溅/滞留药水）由 {@link PotionKinds} 展开
 * 42 项数据组件条件。</p>
 *
 * @param name       文件键（不含 {@code .json}）
 * @param ingredient ingredient 字段的 JSON 数组
 * @param attributes 属性名列表
 */
public record InlayMaterialData(String name, JsonArray ingredient, String... attributes) {

    public static final InlayMaterialData[] ALL = {
            builder().name("and_gate").item("minecraft:repeater").attributes("and_gate").buildInlay(),
            builder().name("attack").item("anvilcraft:cursed_gold_ingot").attributes("attack").buildInlay(),
            builder().name("cold_forged").item("anvilcraft:frost_metal_ingot").attributes("cold_forged").buildInlay(),
            builder().name("defense").item("minecraft:netherite_ingot").attributes("defense").buildInlay(),
            builder().name("direction").item("anvilcraft:multiphase_matter").attributes("direction").buildInlay(),
            PotionKinds.potionInlay("effect", "potion"),
            PotionKinds.potionInlay("effect1", "splash_potion"),
            PotionKinds.potionInlay("effect2", "lingering_potion"),
            builder().name("ember_metal_ingot")
                    .item("anvilcraft:ember_metal_ingot")
                    .attributes("fire_proof", "high_temp").buildInlay(),
            builder().name("enchant")
                    .item("minecraft:enchanted_book")
                    .item("minecraft:book")
                    .attributes("enchant").buildInlay(),
            builder().name("eternal").item("anvilcraft:transcendium_ingot").attributes("eternal").buildInlay(),
            builder().name("fire_proof").item("minecraft:netherite_ingot").attributes("fire_proof").buildInlay(),
            builder().name("generator").item("anvilcraft:supercapacitor").attributes("generator").buildInlay(),
            builder().name("input").item("minecraft:observer").attributes("input").buildInlay(),
            builder().name("life").item("anvilcraft:royal_steel_ingot").attributes("life").buildInlay(),
            builder().name("magnetic").item("anvilcraft:magnet_ingot").attributes("magnetic").buildInlay(),
            builder().name("not_gate").item("minecraft:redstone_torch").attributes("not_gate").buildInlay(),
            builder().name("or_gate").item("minecraft:comparator").attributes("or_gate").buildInlay(),
            builder().name("output").item("minecraft:redstone").attributes("output").buildInlay(),
            builder().name("resonance").item("minecraft:amethyst_shard").attributes("resonance").buildInlay(),
            builder().name("totems").tag("anvilcraft:totem").attributes("nirvana").buildInlay()
    };
}
