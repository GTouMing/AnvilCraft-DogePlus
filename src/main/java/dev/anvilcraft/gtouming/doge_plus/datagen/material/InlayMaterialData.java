package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.AnvilcraftSmithingRecipes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String COUNTER_GATE = "counter_gate";
    public static final String DEFENSE = "defense";
    public static final String DELAY_GATE = "delay_gate";
    public static final String EFFECT = "effect";
    public static final String EFFECT1 = "effect1";
    public static final String EFFECT2 = "effect2";
    public static final String EMBER_METAL_INGOT = "ember_metal_ingot";
    public static final String ENCHANT = "enchant";
    public static final String ETERNAL = "eternal";
    public static final String FIRE_PROOF = "fire_proof";
    public static final String GENERATOR = "generator";
    public static final String INPUT = "input";
    public static final String LATCH_GATE = "latch_gate";
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

    // ===== 空心磁铁块镶合（无属性材料）=====
    public static final String IRON_INGOT = "iron_ingot";

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

    /** 本 mod 内置镶嵌材料（含属性）。 */
    private static final List<InlayMaterialData> MANUAL = new ArrayList<>();
    static {
        MANUAL.addAll(List.of(
            builder().name(AND_GATE)
                    .item(Items.REPEATER)
                    .attributes(InlayProperty.AND_GATE).buildInlay(),
            builder().name(ATTACK)
                    .item(ModItems.CURSED_GOLD_INGOT)
                    .attributes(InlayProperty.ATTACK).buildInlay(),
            builder().name(COLD_FORGED)
                    .item(ModItems.FROST_METAL_INGOT)
                    .attributes(InlayProperty.COLD_FORGED).buildInlay(),
            builder().name(COUNTER_GATE)
                    .tag(ItemTags.BUTTONS)
                    .attributes(InlayProperty.COUNTER_GATE).buildInlay(),
            builder().name(DELAY_GATE)
                    .tag(ItemTags.WOODEN_PRESSURE_PLATES)
                    .item(Items.STONE_PRESSURE_PLATE)
                    .item(Items.POLISHED_BLACKSTONE_PRESSURE_PLATE)
                    .item(Items.LIGHT_WEIGHTED_PRESSURE_PLATE)
                    .item(Items.HEAVY_WEIGHTED_PRESSURE_PLATE)
                    .attributes(InlayProperty.DELAY_GATE).buildInlay(),
            builder().name(DEFENSE)
                    .item(Items.NETHERITE_INGOT)
                    .attributes(InlayProperty.DEFENSE).buildInlay(),
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
                    .item(ModBlocks.REDSTONE_WIRE)
                    .attributes(InlayProperty.INPUT).buildInlay(),
            builder().name(LATCH_GATE)
                    .item(Items.LEVER)
                    .attributes(InlayProperty.LATCH_GATE).buildInlay(),
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

            // ===== 空心磁铁块镶合：铁锭（无属性材料）=====
            builder().name(IRON_INGOT).item(Items.IRON_INGOT).buildInlay(),

            // ===== 原版盔甲纹饰兼容：按标签匹配的无属性材料 =====
            builder().name(TRIM_ARMOR).tag(TRIM_ARMOR_TAG).buildInlay(),
            builder().name(TRIM_MATERIAL).tag(TRIM_MATERIAL_TAG).buildInlay()
        ));
    }

    /**
     * 全部镶嵌材料定义：内置 + 前置（AnvilCraft）锻造成分 + 前置珠宝复制配方的材料。
     *
     * <p>成分由 {@link AnvilcraftSmithingRecipes.Entry} 派生；已内置定义（含属性）的同名材料
     * 不重复生成，避免无属性定义覆盖内置属性。珠宝复制材料同样只需被镶入参与合成、
     * 不需要赋予任何性质，故写出 {@code "attributes": []}。</p>
     *
     * @param smithing 前置锻造配方
     * @param jewel    前置珠宝复制配方（材料数已由读取器筛选）
     */
    public static List<InlayMaterialData> all(
            List<AnvilcraftSmithingRecipes.Entry> smithing,
            List<AnvilcraftSmithingRecipes.Entry> jewel) {
        List<InlayMaterialData> list = new ArrayList<>(MANUAL);
        Set<String> defined = new HashSet<>();
        for (InlayMaterialData material : MANUAL) {
            defined.add(material.name());
        }
        Map<String, AnvilcraftSmithingRecipes.Spec> derived = new LinkedHashMap<>();
        List<AnvilcraftSmithingRecipes.Entry> entries = new ArrayList<>(smithing);
        entries.addAll(jewel);
        for (AnvilcraftSmithingRecipes.Entry entry : entries) {
            for (AnvilcraftSmithingRecipes.Spec spec : entry.ingredients()) {
                if (!defined.contains(spec.name())) derived.putIfAbsent(spec.name(), spec);
            }
        }
        for (AnvilcraftSmithingRecipes.Spec spec : derived.values()) {
            list.add(spec.item() != null
                    ? builder().name(spec.name()).item(spec.item()).buildInlay()
                    : builder().name(spec.name()).tag(spec.tag()).buildInlay());
        }
        return list;
    }
}
