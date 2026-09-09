package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData;
import dev.anvilcraft.gtouming.doge_plus.datagen.material.InlayMaterialData;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialBuilder.builder;

/**
 * 前置（AnvilCraft）锻造配方的镶合兼容数据源。
 *
 * <p>为 AnvilCraft 的每条锻造配方生成镶合四件套：模板基材定义（开孔）、可镶入材料定义、
 * 材料 × 模板配对镶入配方、以及对应产物的 {@code inlay_crafting} 镶合配方。</p>
 *
 * <p>镶孔数规则：普通升级锻造（皇家钢 / 寒霜 / 余烬升级模板）与下界合金一致开 2 孔
 * （锻造材料 + 基材各占一孔）；多合一锻造按「n 合一开 n+1 孔」，即 n 个合一输入外加
 * 前置配方中的 catalyst 材料，对应 {@code two/four/eight_to_one_smithing_template}
 * 的 3 / 5 / 9 孔。</p>
 *
 * <p>产物为镶合配方支持的普通物品（不含前置锻造附带的数据组件，组件化产物留待后续扩展）。</p>
 */
public final class AnvilcraftSmithingCompat {

    private AnvilcraftSmithingCompat() {
    }

    /** 单成分：匹配某个物品或标签；文件键为 name。 */
    private record Spec(@Nullable ItemLike item, @Nullable TagKey<Item> tag, String name) {

        static Spec item(ItemLike item) {
            return new Spec(item, null, itemKey(item));
        }

        static Spec tag(String name, TagKey<Item> tag) {
            return new Spec(null, tag, name);
        }

        Ingredient ingredient() {
            return item != null ? Ingredient.of(item) : Ingredient.of(tag);
        }
    }

    /** 一条镶合配方行：id + 基材（模板物品）+ 镶孔内容 + 产物。 */
    private record Crafting(String id, Item base, List<Spec> inlays, ItemLike result) {
    }

    private static final List<Crafting> CRAFTINGS = new ArrayList<>();

    private static void craft(String id, ItemLike baseTemplate, ItemLike result, Spec... inlays) {
        CRAFTINGS.add(new Crafting(id, baseTemplate.asItem(), List.of(inlays), result));
    }

    private static String itemKey(ItemLike item) {
        return BuiltInRegistries.ITEM.getKey(item.asItem()).getPath();
    }

    // ==================== 基材：模板镶孔数 ====================

    /** 模板基材：开孔数。文件键 = 模板物品注册路径。 */
    public static final List<ItemLike> TEMPLATE_BASES = List.of(
            ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE,
            ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE,
            ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE,
            ModItems.TWO_TO_ONE_SMITHING_TEMPLATE,
            ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE,
            ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE
    );

    private static int socketsOf(ItemLike template) {
        return switch (itemKey(template)) {
            case "two_to_one_smithing_template" -> 3;
            case "four_to_one_smithing_template" -> 5;
            case "eight_to_one_smithing_template" -> 9;
            default -> 2;
        };
    }

    /** 模板基材定义。 */
    public static List<BaseMaterialData> baseMaterials() {
        List<BaseMaterialData> list = new ArrayList<>();
        for (ItemLike template : TEMPLATE_BASES) {
            list.add(builder().name(itemKey(template)).item(template).sockets(socketsOf(template)).buildBase());
        }
        return list;
    }

    // ==================== 普通升级锻造：工具 ====================

    private record ToolUpgrade(TagKey<Item> baseTag, ItemLike result) {
    }

    private static void toolUpgrades(ItemLike template, ItemLike ingot, List<ToolUpgrade> tools) {
        for (ToolUpgrade tool : tools) {
            craft(itemKey(tool.result), template, tool.result,
                    Spec.item(ingot),
                    Spec.tag(tool.baseTag.location().getPath(), tool.baseTag));
        }
    }

    private static final List<ToolUpgrade> ROYAL_TOOLS = List.of(
            new ToolUpgrade(ModItemTags.ROYAL_STEEL_PICKAXE_BASE, ModItems.ROYAL_STEEL_PICKAXE),
            new ToolUpgrade(ModItemTags.ROYAL_STEEL_AXE_BASE, ModItems.ROYAL_STEEL_AXE),
            new ToolUpgrade(ModItemTags.ROYAL_STEEL_SHOVEL_BASE, ModItems.ROYAL_STEEL_SHOVEL),
            new ToolUpgrade(ModItemTags.ROYAL_STEEL_HOE_BASE, ModItems.ROYAL_STEEL_HOE),
            new ToolUpgrade(ModItemTags.ROYAL_STEEL_SWORD_BASE, ModItems.ROYAL_STEEL_SWORD)
    );

    private static final List<ToolUpgrade> FROST_TOOLS = List.of(
            new ToolUpgrade(ModItemTags.FROST_METAL_PICKAXE_BASE, ModItems.FROST_METAL_PICKAXE),
            new ToolUpgrade(ModItemTags.FROST_METAL_AXE_BASE, ModItems.FROST_METAL_AXE),
            new ToolUpgrade(ModItemTags.FROST_METAL_SHOVEL_BASE, ModItems.FROST_METAL_SHOVEL),
            new ToolUpgrade(ModItemTags.FROST_METAL_HOE_BASE, ModItems.FROST_METAL_HOE),
            new ToolUpgrade(ModItemTags.FROST_METAL_SWORD_BASE, ModItems.FROST_METAL_SWORD)
    );

    private static final List<ToolUpgrade> EMBER_TOOLS = List.of(
            new ToolUpgrade(ModItemTags.EMBER_METAL_PICKAXE_BASE, ModItems.EMBER_METAL_PICKAXE),
            new ToolUpgrade(ModItemTags.EMBER_METAL_AXE_BASE, ModItems.EMBER_METAL_AXE),
            new ToolUpgrade(ModItemTags.EMBER_METAL_SHOVEL_BASE, ModItems.EMBER_METAL_SHOVEL),
            new ToolUpgrade(ModItemTags.EMBER_METAL_HOE_BASE, ModItems.EMBER_METAL_HOE),
            new ToolUpgrade(ModItemTags.EMBER_METAL_SWORD_BASE, ModItems.EMBER_METAL_SWORD)
    );

    // ==================== 普通升级锻造：杂项与方块 ====================

    private static void registerTransforms() {
        ItemLike royal = ModItems.ROYAL_STEEL_UPGRADE_SMITHING_TEMPLATE;
        ItemLike frost = ModItems.FROST_METAL_UPGRADE_SMITHING_TEMPLATE;
        ItemLike ember = ModItems.EMBER_METAL_UPGRADE_SMITHING_TEMPLATE;
        ItemLike royalBlock = ModBlocks.ROYAL_STEEL_BLOCK;
        ItemLike frostBlock = ModBlocks.FROST_METAL_BLOCK;
        ItemLike emberBlock = ModBlocks.EMBER_METAL_BLOCK;

        toolUpgrades(royal, ModItems.ROYAL_STEEL_INGOT, ROYAL_TOOLS);
        toolUpgrades(frost, ModItems.FROST_METAL_INGOT, FROST_TOOLS);
        toolUpgrades(ember, ModItems.EMBER_METAL_INGOT, EMBER_TOOLS);

        // 皇家钢：由原版物品起步
        craft("royal_anvil_hammer", royal, ModItems.ROYAL_ANVIL_HAMMER,
                Spec.item(royalBlock), Spec.item(ModItems.ANVIL_HAMMER));
        craft("royal_dragon_rod", royal, ModItems.ROYAL_DRAGON_ROD,
                Spec.item(royalBlock), Spec.item(ModItems.DRAGON_ROD));
        craft("royal_anvil", royal, ModBlocks.ROYAL_ANVIL,
                Spec.item(royalBlock), Spec.item(Items.ANVIL));
        craft("royal_grindstone", royal, ModBlocks.ROYAL_GRINDSTONE,
                Spec.item(royalBlock), Spec.item(Items.GRINDSTONE));
        craft("royal_smithing_table", royal, ModBlocks.ROYAL_SMITHING_TABLE,
                Spec.item(royalBlock), Spec.item(Items.SMITHING_TABLE));

        // 余烬：由皇家系列升级
        craft("ember_anvil_hammer", ember, ModItems.EMBER_ANVIL_HAMMER,
                Spec.item(emberBlock), Spec.item(ModItems.ROYAL_ANVIL_HAMMER));
        craft("ember_dragon_rod", ember, ModItems.EMBER_DRAGON_ROD,
                Spec.item(emberBlock), Spec.item(ModItems.ROYAL_DRAGON_ROD));
        craft("ember_anvil", ember, ModBlocks.EMBER_ANVIL,
                Spec.item(emberBlock), Spec.item(ModBlocks.ROYAL_ANVIL));
        craft("ember_grindstone", ember, ModBlocks.EMBER_GRINDSTONE,
                Spec.item(emberBlock), Spec.item(ModBlocks.ROYAL_GRINDSTONE));
        craft("ember_smithing_table", ember, ModBlocks.EMBER_SMITHING_TABLE,
                Spec.item(emberBlock), Spec.item(ModBlocks.ROYAL_SMITHING_TABLE));

        // 寒霜：由皇家系列升级
        craft("frost_anvil_hammer", frost, ModItems.FROST_ANVIL_HAMMER,
                Spec.item(frostBlock), Spec.item(ModItems.ROYAL_ANVIL_HAMMER));
        craft("frost_anvil", frost, ModBlocks.FROST_ANVIL,
                Spec.item(frostBlock), Spec.item(ModBlocks.ROYAL_ANVIL));
        craft("frost_grindstone", frost, ModBlocks.FROST_GRINDSTONE,
                Spec.item(frostBlock), Spec.item(ModBlocks.ROYAL_GRINDSTONE));
        craft("frost_smithing_table", frost, ModBlocks.FROST_SMITHING_TABLE,
                Spec.item(frostBlock), Spec.item(ModBlocks.ROYAL_SMITHING_TABLE));
    }

    // ==================== 多合一锻造 ====================

    private static void registerMultiToOnes() {
        ItemLike two = ModItems.TWO_TO_ONE_SMITHING_TEMPLATE;
        ItemLike four = ModItems.FOUR_TO_ONE_SMITHING_TEMPLATE;
        ItemLike eight = ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE;
        ItemLike transcendium = ModItems.MULTIPHASE_TRANSCENDIUM;

        // 2 合一：余烬 + 寒霜 → 超然（3 孔 = catalyst 材料 + 2 合一输入）
        craft("two_to_one_transcendence_anvil_hammer", two, ModItems.TRANSCENDENCE_ANVIL_HAMMER,
                Spec.item(transcendium),
                Spec.item(ModItems.EMBER_ANVIL_HAMMER), Spec.item(ModItems.FROST_ANVIL_HAMMER));
        craft("two_to_one_transcendence_dragon_rod", two, ModItems.TRANSCENDENCE_DRAGON_ROD,
                Spec.item(transcendium),
                Spec.item(ModItems.EMBER_DRAGON_ROD), Spec.item(ModItems.FROST_DRAGON_ROD));
        craft("two_to_one_transcendence_heavy_halberd", two, ModItems.TRANSCENDENCE_HEAVY_HALBERD,
                Spec.item(transcendium),
                Spec.item(ModItems.EMBER_METAL_HEAVY_HALBERD), Spec.item(ModItems.FROST_METAL_HEAVY_HALBERD));
        craft("two_to_one_transcendence_resonator", two, ModItems.TRANSCENDENCE_RESONATOR,
                Spec.item(transcendium),
                Spec.item(ModItems.EMBER_METAL_RESONATOR), Spec.item(ModItems.FROST_METAL_RESONATOR));
        craft("two_to_one_transcendence_anvil", two, ModBlocks.TRANSCENDENCE_ANVIL,
                Spec.item(transcendium),
                Spec.item(ModBlocks.EMBER_ANVIL), Spec.item(ModBlocks.FROST_ANVIL));
        craft("two_to_one_transcendence_grindstone", two, ModBlocks.TRANSCENDENCE_GRINDSTONE,
                Spec.item(transcendium),
                Spec.item(ModBlocks.EMBER_GRINDSTONE), Spec.item(ModBlocks.FROST_GRINDSTONE));
        craft("two_to_one_transcendence_smithing_table", two, ModBlocks.TRANSCENDENCE_SMITHING_TABLE,
                Spec.item(transcendium),
                Spec.item(ModBlocks.EMBER_SMITHING_TABLE), Spec.item(ModBlocks.FROST_SMITHING_TABLE));

        // 4 合一：戟 / 共振器 / 护符（5 孔 = catalyst 材料 + 4 合一输入）
        Spec mace = Spec.tag("mace", Tags.Items.TOOLS_MACE);
        craft("four_to_one_frost_heavy_halberd", four, ModItems.FROST_METAL_HEAVY_HALBERD,
                Spec.item(ModItems.HEAVY_HALBERD_CORE),
                Spec.item(ModItems.FROST_METAL_SWORD), Spec.item(ModItems.FROST_METAL_AXE),
                Spec.item(Items.TRIDENT), mace);
        craft("four_to_one_ember_heavy_halberd", four, ModItems.EMBER_METAL_HEAVY_HALBERD,
                Spec.item(ModItems.HEAVY_HALBERD_CORE),
                Spec.item(ModItems.EMBER_METAL_SWORD), Spec.item(ModItems.EMBER_METAL_AXE),
                Spec.item(Items.TRIDENT), mace);
        craft("four_to_one_frost_resonator", four, ModItems.FROST_METAL_RESONATOR,
                Spec.item(ModItems.RESONATOR_CORE),
                Spec.item(ModItems.FROST_METAL_AXE), Spec.item(ModItems.FROST_METAL_SHOVEL),
                Spec.item(ModItems.FROST_METAL_HOE), Spec.item(ModItems.FROST_METAL_PICKAXE));
        craft("four_to_one_ember_resonator", four, ModItems.EMBER_METAL_RESONATOR,
                Spec.item(ModItems.RESONATOR_CORE),
                Spec.item(ModItems.EMBER_METAL_AXE), Spec.item(ModItems.EMBER_METAL_SHOVEL),
                Spec.item(ModItems.EMBER_METAL_HOE), Spec.item(ModItems.EMBER_METAL_PICKAXE));
        craft("four_to_one_gem_amulet", four, ModItems.GEM_AMULET,
                Spec.item(ModBlocks.FROST_METAL_BLOCK),
                Spec.item(ModItems.SAPPHIRE_AMULET), Spec.item(ModItems.RUBY_AMULET),
                Spec.item(ModItems.TOPAZ_AMULET), Spec.item(ModItems.EMERALD_AMULET));
        craft("four_to_one_nature_amulet", four, ModItems.NATURE_AMULET,
                Spec.item(ModBlocks.FROST_METAL_BLOCK),
                Spec.item(ModItems.SILENCE_AMULET), Spec.item(ModItems.FEATHER_AMULET),
                Spec.item(ModItems.CAT_AMULET), Spec.item(ModItems.DOG_AMULET));

        // 8 合一：万能工具（9 孔 = catalyst 材料 + 8 合一输入）
        craft("eight_to_one_multitool", eight, ModItems.MULTITOOL_ITEM,
                Spec.item(ModItems.MULTIPHASE_MATTER),
                Spec.item(Items.SHEARS), Spec.item(Items.FLINT_AND_STEEL),
                Spec.item(Items.BRUSH), Spec.item(Items.SPYGLASS),
                Spec.item(ModItems.MAGNET), Spec.item(Items.FISHING_ROD),
                Spec.item(Items.CARROT_ON_A_STICK), Spec.item(Items.WARPED_FUNGUS_ON_A_STICK));
    }

    static {
        registerTransforms();
        registerMultiToOnes();
    }

    // ==================== 供各 ALL 列表拼接的数据 ====================

    /** 全部镶入成分定义（已存在的同名定义直接复用，不重复生成）。 */
    public static List<InlayMaterialData> inlayMaterials() {
        // 已定义过（带属性）的材料文件键：仅余烬锭，直接沿用其定义
        Set<String> existing = Set.of(InlayMaterialData.EMBER_METAL_INGOT);
        Map<String, Spec> defs = new LinkedHashMap<>();
        for (Crafting crafting : CRAFTINGS) {
            for (Spec spec : crafting.inlays()) {
                if (existing.contains(spec.name())) continue;
                defs.putIfAbsent(spec.name(), spec);
            }
        }
        List<InlayMaterialData> list = new ArrayList<>();
        for (Spec spec : defs.values()) {
            if (spec.item() != null) {
                list.add(builder().name(spec.name()).item(spec.item()).buildInlay());
            } else {
                list.add(builder().name(spec.name()).tag(spec.tag()).buildInlay());
            }
        }
        return list;
    }

    /** 材料 × 模板配对镶入配方。 */
    public static List<InlayRecipeData> inlayRecipes() {
        Set<String> seen = new LinkedHashSet<>();
        List<InlayRecipeData> list = new ArrayList<>();
        for (Crafting crafting : CRAFTINGS) {
            String base = itemKey(crafting.base());
            for (Spec spec : crafting.inlays()) {
                if (seen.add(base + "/" + spec.name())) {
                    list.add(new InlayRecipeData(spec.name(), base));
                }
            }
        }
        return list;
    }

    /** 前置锻造对应的镶合配方。 */
    public static List<InlayCraftingData> craftingRecipes() {
        List<InlayCraftingData> list = new ArrayList<>();
        for (Crafting crafting : CRAFTINGS) {
            List<Ingredient> inlays = new ArrayList<>(crafting.inlays().size());
            for (Spec spec : crafting.inlays()) {
                inlays.add(spec.ingredient());
            }
            list.add(new InlayCraftingData(
                    crafting.id(),
                    crafting.base(),
                    inlays,
                    crafting.result().asItem()
            ));
        }
        return list;
    }
}
