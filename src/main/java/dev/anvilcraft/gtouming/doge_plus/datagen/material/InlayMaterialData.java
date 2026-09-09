package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.AnvilcraftSmithingCompat;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialBuilder.builder;

/**
 * 镶嵌材料（inlay material）源数据：{@code data/anvilcraft_doge_plus/material/inlay/<name>.json}。
 * <p>通过 {@link MaterialBuilder} 声明式描述 ingredient 匹配条件（物品/标签/带数据组件的药水），
 * 序列化后与原手写内容等价。effect 系列（普通/喷溅/滞留药水）由 {@link PotionKinds} 展开
 * 42 项数据组件条件。</p>
 * <p>材料文件键（{@code name}）集中定义为下方 {@code NAME_*} 常量；匹配物品/标签直接引用
 * vanilla / AnvilCraft / 本 mod 的注册条目（{@link Items} / {@link ModItems} entry / {@link ModItemTags} tag），
 * id 由注册表解析，避免手写字面量出错。添加新材料只需在下方加一行。</p>
 *
 * @param name       文件键（不含 {@code .json}）
 * @param ingredient ingredient 字段的 JSON 数组
 * @param attributes 属性名列表
 */
public record InlayMaterialData(String name, JsonArray ingredient, String... attributes) {

    // 材料文件键常量 —— 唯一字面量定义处，ALL 与 InlayRecipeData 均引用它们
    public static final String AND_GATE = "and_gate";
    public static final String ATTACK = "attack";
    public static final String COLD_FORGED = "cold_forged";
    public static final String DEFENSE = "defense";
    public static final String DIRECTION = "direction";
    public static final String EFFECT = "effect";
    public static final String EFFECT1 = "effect1";
    public static final String EFFECT2 = "effect2";
    public static final String EMBER_METAL_INGOT = "ember_metal_ingot";
    public static final String ENCHANT = "enchant";
    public static final String ETERNAL = "eternal";
    public static final String FIRE_PROOF = "fire_proof";
    public static final String GENERATOR = "generator";
    public static final String INPUT = "input";
    public static final String LIFE = "life";
    public static final String MAGNETIC = "magnetic";
    public static final String NOT_GATE = "not_gate";
    public static final String OR_GATE = "or_gate";
    public static final String OUTPUT = "output";
    public static final String RESONANCE = "resonance";
    public static final String TOTEMS = "totems";

    // ===== 原版锻造兼容（无属性材料）=====
    public static final String NETHERITE_INGOT = "netherite_ingot";
    public static final String DIAMOND_SWORD = "diamond_sword";
    public static final String DIAMOND_PICKAXE = "diamond_pickaxe";
    public static final String DIAMOND_AXE = "diamond_axe";
    public static final String DIAMOND_SHOVEL = "diamond_shovel";
    public static final String DIAMOND_HOE = "diamond_hoe";
    public static final String DIAMOND_HELMET = "diamond_helmet";
    public static final String DIAMOND_CHESTPLATE = "diamond_chestplate";
    public static final String DIAMOND_LEGGINGS = "diamond_leggings";
    public static final String DIAMOND_BOOTS = "diamond_boots";

    /** 下界合金升级可镶入模板的全部材料文件键（锻造材料 + 可被升级的钻石装备）。 */
    public static final List<String> NETHERITE_UPGRADE_INLAYS = List.of(
            NETHERITE_INGOT,
            DIAMOND_SWORD,
            DIAMOND_PICKAXE,
            DIAMOND_AXE,
            DIAMOND_SHOVEL,
            DIAMOND_HOE,
            DIAMOND_HELMET,
            DIAMOND_CHESTPLATE,
            DIAMOND_LEGGINGS,
            DIAMOND_BOOTS
    );

    // ===== 原版盔甲纹饰兼容（无属性材料，按标签匹配）=====
    public static final String TRIM_ARMOR = "trim_armor";
    public static final String TRIM_MATERIAL = "trim_material";

    /** 原版「可纹饰装备」物品标签（{#minecraft:trimmable_armor}）。 */
    public static final TagKey<Item> TRIM_ARMOR_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trimmable_armor"));
    /** 原版「纹饰材料」物品标签（{#minecraft:trim_materials}）。 */
    public static final TagKey<Item> TRIM_MATERIAL_TAG =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trim_materials"));

    /** 纹饰镶入模板的材料文件键（装备 + 纹饰材料）。 */
    public static final List<String> TRIM_INLAYS = List.of(TRIM_ARMOR, TRIM_MATERIAL);

    public static final List<InlayMaterialData> ALL = new ArrayList<>();
    static {
        ALL.addAll(List.of(
            builder().name(AND_GATE)
                    .item(Items.REPEATER)
                    .attributes(InlayProperty.AND_GATE).buildInlay(),
            builder().name(ATTACK)
                    .item(ModItems.CURSED_GOLD_INGOT)
                    .attributes(InlayProperty.ATTACK).buildInlay(),
            builder().name(COLD_FORGED)
                    .item(ModItems.FROST_METAL_INGOT)
                    .attributes(InlayProperty.COLD_FORGED).buildInlay(),
            builder().name(DEFENSE)
                    .item(Items.NETHERITE_INGOT)
                    .attributes(InlayProperty.DEFENSE).buildInlay(),
            builder().name(DIRECTION)
                    .item(ModItems.MULTIPHASE_MATTER)
                    .attributes(InlayProperty.DIRECTION).buildInlay(),
            builder().name(EMBER_METAL_INGOT)
                    .item(ModItems.EMBER_METAL_INGOT)
                    .attributes(InlayProperty.FIRE_PROOF, InlayProperty.HIGH_TEMP).buildInlay(),
            builder().name(ENCHANT)
                    .item(Items.ENCHANTED_BOOK)
                    .item(Items.BOOK)
                    .attributes(InlayProperty.ENCHANT).buildInlay(),
            builder().name(ETERNAL)
                    .item(ModItems.TRANSCENDIUM_INGOT)
                    .attributes(InlayProperty.ETERNAL).buildInlay(),
            builder().name(FIRE_PROOF)
                    .item(Items.NETHERITE_INGOT)
                    .attributes(InlayProperty.FIRE_PROOF).buildInlay(),
            builder().name(GENERATOR)
                    .item(ModItems.SUPER_CAPACITOR)
                    .attributes(InlayProperty.GENERATOR).buildInlay(),
            builder().name(INPUT)
                    .item(Items.OBSERVER)
                    .attributes(InlayProperty.INPUT).buildInlay(),
            builder().name(LIFE)
                    .item(ModItems.ROYAL_STEEL_INGOT)
                    .attributes(InlayProperty.LIFE).buildInlay(),
            builder().name(MAGNETIC)
                    .item(ModItems.MAGNET_INGOT)
                    .attributes(InlayProperty.MAGNETIC).buildInlay(),
            builder().name(NOT_GATE)
                    .item(Items.REDSTONE_TORCH)
                    .attributes(InlayProperty.NOT_GATE).buildInlay(),
            builder().name(OR_GATE)
                    .item(Items.COMPARATOR)
                    .attributes(InlayProperty.OR_GATE).buildInlay(),
            builder().name(OUTPUT)
                    .item(Items.REDSTONE)
                    .attributes(InlayProperty.OUTPUT).buildInlay(),
            builder().name(RESONANCE)
                    .item(Items.AMETHYST_SHARD)
                    .attributes(InlayProperty.RESONANCE).buildInlay(),
            builder().name(TOTEMS)
                    .tag(ModItemTags.TOTEM)
                    .attributes(InlayProperty.NIRVANA).buildInlay(),
            PotionKinds.potionInlay(EFFECT, Items.POTION),
            PotionKinds.potionInlay(EFFECT1, Items.SPLASH_POTION),
            PotionKinds.potionInlay(EFFECT2, Items.LINGERING_POTION),

            // ===== 原版锻造兼容：无属性材料（只参与镶合合成，不赋予任何性质）=====
            builder().name(NETHERITE_INGOT).item(Items.NETHERITE_INGOT).buildInlay(),
            builder().name(DIAMOND_SWORD).item(Items.DIAMOND_SWORD).buildInlay(),
            builder().name(DIAMOND_PICKAXE).item(Items.DIAMOND_PICKAXE).buildInlay(),
            builder().name(DIAMOND_AXE).item(Items.DIAMOND_AXE).buildInlay(),
            builder().name(DIAMOND_SHOVEL).item(Items.DIAMOND_SHOVEL).buildInlay(),
            builder().name(DIAMOND_HOE).item(Items.DIAMOND_HOE).buildInlay(),
            builder().name(DIAMOND_HELMET).item(Items.DIAMOND_HELMET).buildInlay(),
            builder().name(DIAMOND_CHESTPLATE).item(Items.DIAMOND_CHESTPLATE).buildInlay(),
            builder().name(DIAMOND_LEGGINGS).item(Items.DIAMOND_LEGGINGS).buildInlay(),
            builder().name(DIAMOND_BOOTS).item(Items.DIAMOND_BOOTS).buildInlay(),

            // ===== 原版盔甲纹饰兼容：按标签匹配的无属性材料 =====
            builder().name(TRIM_ARMOR).tag(TRIM_ARMOR_TAG).buildInlay(),
            builder().name(TRIM_MATERIAL).tag(TRIM_MATERIAL_TAG).buildInlay()
        ));
        // ===== 前置（AnvilCraft）锻造兼容：模板镶入成分 =====
        ALL.addAll(AnvilcraftSmithingCompat.inlayMaterials());
    }
}
