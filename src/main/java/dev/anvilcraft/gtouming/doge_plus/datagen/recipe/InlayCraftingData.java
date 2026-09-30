package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData;
import dev.anvilcraft.gtouming.doge_plus.datagen.material.InlayMaterialData;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayOutput;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayValue;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * inlay_crafting（镶合）配方的源数据。
 *
 * <p>基材（模具物品）+ 镶孔要求（顺序无关）+ 产物列表；JSON 与运行时
 * {@code InlayCraftingRecipe} 的字段一一对应，配方输出路径为
 * {@code data/anvilcraft_doge_plus/recipe/<kind.folder()>/<id>.json}。</p>
 *
 * <p>镶孔要求用 {@link InlayMatcher}：
 * {@link #properties} 按材料属性匹配（如逻辑载体六孔各要一种门），
 * {@link #item} / {@link #tag} 按物品或标签匹配，{@link #itemWithEnchants} 再限定附魔条数范围。</p>
 *
 * <p>产物用 {@link InlayOutput}：
 * {@link #fixed(Item)} / {@link #fixed(Item, int)} 固定数量，
 * {@link #scaled} 数量 = 倍率 × 附魔条数，{@link #scaledChance} 概率 = 倍率 × 附魔条数，
 * {@link #cursed} 附带消失诅咒。模具返还（锻造模板 / 被复制物）也作为普通产物写出。</p>
 *
 * @param id      配方文件 id（不含路径与扩展名）
 * @param base    基材物品（模具，需要预先开镶孔）
 * @param inlays  期望的镶孔要求集合（顺序无关）
 * @param results 产物列表（可多件、可带概率、可动态推导）
 * @param kind    镶合类别（决定输出目录与 JEI 分类）
 */
public record InlayCraftingData(
        String id,
        Item base,
        List<InlayMatcher> inlays,
        List<InlayOutput> results,
        InlayCraftingRecipe.Kind kind
) {

    // ==================== 镶孔要求构造辅助 ====================

    /** 按属性匹配（携带任一属性即命中）。 */
    public static InlayMatcher properties(InlayProperty... properties) {
        return InlayMatcher.byProperty(List.of(properties));
    }

    /** 按物品匹配。 */
    public static InlayMatcher item(Item... items) {
        return InlayMatcher.byItem(items);
    }

    /** 按标签匹配。 */
    public static InlayMatcher tag(TagKey<Item> tag) {
        return InlayMatcher.byTag(tag);
    }

    /** 按物品 + 附魔条数范围匹配（{@code min} 可为 0，{@code max} 为空表示不限）。 */
    public static InlayMatcher itemWithEnchants(Optional<Integer> min, Optional<Integer> max, Item... items) {
        return InlayMatcher.byItem(new InlayMatcher.EnchantRange(min, max), items);
    }

    // ==================== 产物构造辅助 ====================

    /** 固定产物：单件、概率 100%、无诅咒。 */
    public static InlayOutput fixed(Item item) {
        return new InlayOutput.Fixed(item, InlayValue.ONE, InlayValue.ONE, false);
    }

    /** 固定产物：指定数量、概率 100%、无诅咒。 */
    public static InlayOutput fixed(Item item, int count) {
        return new InlayOutput.Fixed(item, InlayValue.of(count), InlayValue.ONE, false);
    }

    /** 数量 = {@code multiplier × 附魔条数}（基数 0，如 3n 粒）。 */
    public static InlayOutput scaled(Item item, double multiplier) {
        return new InlayOutput.Fixed(item, InlayValue.perEnchantment(multiplier), InlayValue.ONE, false);
    }

    /** 固定数量 + 概率 = {@code multiplier × 附魔条数}（如 10n% 概率返还）。 */
    public static InlayOutput scaledChance(Item item, int count, double multiplier) {
        return new InlayOutput.Fixed(item, InlayValue.of(count), InlayValue.perEnchantment(multiplier), false);
    }

    /** 带 1 级消失诅咒的固定产物（前置珠宝复制的复制品）。 */
    public static InlayOutput cursed(Item item) {
        return new InlayOutput.Fixed(item, InlayValue.ONE, InlayValue.ONE, true);
    }

    /** 盔甲纹饰推导产物（动态产物）。 */
    public static InlayOutput armorTrim() {
        return new InlayOutput.ArmorTrim();
    }

    // ==================== 内置配方 ====================

    /** 原版「可纹饰装备」物品标签。 */
    private static final TagKey<Item> TRIM_ARMOR =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trimmable_armor"));
    /** 原版「纹饰材料」物品标签。 */
    private static final TagKey<Item> TRIM_MATERIAL =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trim_materials"));

    /** 原版锻造：钻石装备 → 对应下界合金装备（钻石物品在前）。 */
    private static final List<Item[]> NETHERITE_UPGRADES = List.of(
            new Item[] {Items.DIAMOND_SWORD, Items.NETHERITE_SWORD},
            new Item[] {Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE},
            new Item[] {Items.DIAMOND_AXE, Items.NETHERITE_AXE},
            new Item[] {Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL},
            new Item[] {Items.DIAMOND_HOE, Items.NETHERITE_HOE},
            new Item[] {Items.DIAMOND_HELMET, Items.NETHERITE_HELMET},
            new Item[] {Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
            new Item[] {Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
            new Item[] {Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}
    );

    private static final String TRIM_ID_SUFFIX = "_armor_trim_smithing_template";

    // 超温余烬→中子锭产物（物品 id 引用的都是前置 AnvilCraft 的条目）
    private static final Item TRANSCENDIUM_INGOT = item("anvilcraft:transcendium_ingot");
    private static final Item TRANSCENDIUM_NUGGET = item("anvilcraft:transcendium_nugget");
    private static final Item TRANSCENDIUM_BLOCK = item("anvilcraft:transcendium_block");
    private static final Item NEUTRONIUM_INGOT = item("anvilcraft:neutronium_ingot");

    /** 本 mod 内置镶合配方（9 下界合金升级 + 18 盔甲纹饰 + 磁铁 / 载体 / 中子锭）。 */
    private static final List<InlayCraftingData> VANILLA = new ArrayList<>();
    static {
        // ===== 原版锻造：下界合金升级（成品 + 返还模板）=====
        for (Item[] pair : NETHERITE_UPGRADES) {
            Item diamond = pair[0];
            Item netherite = pair[1];
            String name = itemName(netherite);
            VANILLA.add(new InlayCraftingData(
                    name + "_upgrade",
                    Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                    List.of(item(Items.NETHERITE_INGOT), item(diamond)),
                    List.of(fixed(netherite), fixed(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE)),
                    InlayCraftingRecipe.Kind.SMITHING
            ));
        }

        // ===== 原版盔甲纹饰：模板 + [可纹饰装备, 纹饰材料] → 推导纹饰装备 + 返还模板 =====
        for (Item template : BaseMaterialData.TRIM_TEMPLATES) {
            String itemName = itemName(template);
            String shortName = itemName.endsWith(TRIM_ID_SUFFIX)
                    ? itemName.substring(0, itemName.length() - TRIM_ID_SUFFIX.length())
                    : itemName;
            VANILLA.add(new InlayCraftingData(
                    shortName + "_trim",
                    template,
                    List.of(tag(TRIM_ARMOR), tag(TRIM_MATERIAL)),
                    List.of(armorTrim(), fixed(template)),
                    InlayCraftingRecipe.Kind.SMITHING
            ));
        }

        // ===== 空心磁铁块：镶入 n 个铁锭即镶合出 n 个磁铁锭（n = 1..镶孔数）=====
        for (int count = 1; count <= BaseMaterialData.HOLLOW_MAGNET_BLOCK_SOCKETS; count++) {
            List<InlayMatcher> inlays = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                inlays.add(item(Items.IRON_INGOT));
            }
            VANILLA.add(new InlayCraftingData(
                    "magnet_ingot_from_hollow_magnet_block_" + count,
                    ModBlocks.HOLLOW_MAGNET_BLOCK.asItem(),
                    inlays,
                    List.of(fixed(ModItems.MAGNET_INGOT.get(), count)),
                    InlayCraftingRecipe.Kind.CRAFTING
            ));
        }

        // ===== 逻辑载体：六个镶孔各镶一种门（六种门各一种）→ 逻辑载体 =====
        // 每条要求只认一种门属性，六条合起来要求六个镶孔恰好是这六种门
        // （忽略顺序、可重复的门种在这里天然被排除——每种门只占一条要求）。
        List<InlayMatcher> gateInlays = new ArrayList<>(InlayMaterialData.GATE_PROPERTIES.size());
        for (InlayProperty gate : InlayMaterialData.GATE_PROPERTIES) {
            gateInlays.add(properties(gate));
        }
        VANILLA.add(new InlayCraftingData(
                "logic_carrier",
                dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.INLAY_CARRIER.asItem(),
                gateInlays,
                List.of(fixed(dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.LOGIC_CARRIER.asItem(), 6)),
                InlayCraftingRecipe.Kind.CRAFTING
        ));

        // ===== 物流载体：镶嵌载体分别镶入「存入」与「取出」各一件 → 4 个物流载体 =====
        // 两个镶孔各只认一种属性，因此必须一件溜槽 + 一件磁性溜槽；产率 4:1。
        VANILLA.add(new InlayCraftingData(
                "logistics_carrier",
                dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.INLAY_CARRIER.asItem(),
                List.of(properties(InlayProperty.INSERT), properties(InlayProperty.EXTRACT)),
                List.of(fixed(dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.LOGISTICS_CARRIER.asItem(), 4)),
                InlayCraftingRecipe.Kind.CRAFTING
        ));

        // ===== 约束仓 + 充能中子锭 → 约束中子锭（无特殊条件，基底不返还）=====
        // 前置的 item_inject/confined_neutronium_ingot 的镶合版：约束仓被"注入"后变成约束中子锭。
        VANILLA.add(new InlayCraftingData(
                "confined_neutronium_ingot_from_confinement_chamber",
                ModBlocks.CONFINEMENT_CHAMBER.asItem(),
                List.of(item(ModItems.CHARGED_NEUTRONIUM_INGOT.asItem())),
                List.of(fixed(ModBlocks.CONFINED_NEUTRONIUM_INGOT_BLOCK.asItem())),
                InlayCraftingRecipe.Kind.CRAFTING
        ));

        // ===== 超温余烬金属块 + 充能中子锭：按孔中中子锭的附魔条数出产物（照抄前置档位表）=====
        // 该方块只有 1 个镶孔：每档一条配方，用 enchants_count 范围区分；
        // 数量 / 概率用 基数 × 乘数 引用附魔条数。
        addNeutroniumTiers();
    }

    /**
     * 超温余烬→中子锭的档位配方（照抄前置 {@code TranscendiumBehavior}）：
     * <pre>
     * 0     → 4 超限合金锭
     * 1–10  → 4 超限合金锭 + 3n 超限合金粒 +（10n% 概率）1 中子锭
     * 11–14 → 4 超限合金锭 + 3n 超限合金粒 + 1 中子锭
     * 15    → 1 中子锭 + 1 超限合金块
     * ≥16   → n 超限合金粒 + 1 中子锭 + 1 超限合金块
     * </pre>
     */
    private static void addNeutroniumTiers() {
        Item base = ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.asItem();
        Item inlay = ModItems.CHARGED_NEUTRONIUM_INGOT.asItem();
        VANILLA.add(neutroniumTier("neutronium_transmutation_0", base, inlay, 0, 0,
                List.of(fixed(TRANSCENDIUM_INGOT, 4))));
        VANILLA.add(neutroniumTier("neutronium_transmutation_1_to_10", base, inlay, 1, 10,
                List.of(
                        fixed(TRANSCENDIUM_INGOT, 4),
                        scaled(TRANSCENDIUM_NUGGET, 3),
                        scaledChance(NEUTRONIUM_INGOT, 1, 0.1))));
        VANILLA.add(neutroniumTier("neutronium_transmutation_11_to_14", base, inlay, 11, 14,
                List.of(
                        fixed(TRANSCENDIUM_INGOT, 4),
                        scaled(TRANSCENDIUM_NUGGET, 3),
                        fixed(NEUTRONIUM_INGOT))));
        VANILLA.add(neutroniumTier("neutronium_transmutation_15", base, inlay, 15, 15,
                List.of(fixed(NEUTRONIUM_INGOT), fixed(TRANSCENDIUM_BLOCK))));
        VANILLA.add(neutroniumTier("neutronium_transmutation_16_plus", base, inlay, 16, null,
                List.of(
                        fixed(NEUTRONIUM_INGOT),
                        fixed(TRANSCENDIUM_BLOCK),
                        scaled(TRANSCENDIUM_NUGGET, 1))));
    }

    private static InlayCraftingData neutroniumTier(
            String id, Item base, Item inlay, int min, Integer max, List<InlayOutput> results) {
        return new InlayCraftingData(
                id,
                base,
                List.of(itemWithEnchants(Optional.of(min), Optional.ofNullable(max), inlay)),
                results,
                InlayCraftingRecipe.Kind.CRAFTING);
    }

    /**
     * 全部镶合配方：内置 + 前置（AnvilCraft）锻造配方 / 珠宝复制配方派生的镶合配方。
     *
     * <p>由 {@link AnvilcraftSmithingRecipes.Entry} 派生：基材 + 全部成分 → 产物；
     * 配方 id 镜像前置配方的路径（见 {@link AnvilcraftSmithingRecipes#craftingId}）。
     * 锻造类返还模板，珠宝复制类以被复制物为基材、产物为「带诅咒的复制品 + 干净的被复制物」。</p>
     *
     * @param smithing 前置锻造配方
     * @param jewel    前置珠宝复制配方（材料数已由读取器筛选）
     */
    public static List<InlayCraftingData> all(
            List<AnvilcraftSmithingRecipes.Entry> smithing,
            List<AnvilcraftSmithingRecipes.Entry> jewel) {
        List<InlayCraftingData> list = new ArrayList<>(VANILLA);
        for (AnvilcraftSmithingRecipes.Entry entry : smithing) {
            list.add(of(entry, InlayCraftingRecipe.Kind.SMITHING));
        }
        for (AnvilcraftSmithingRecipes.Entry entry : jewel) {
            list.add(of(entry, InlayCraftingRecipe.Kind.COPYING));
        }
        return list;
    }

    private static InlayCraftingData of(AnvilcraftSmithingRecipes.Entry entry, InlayCraftingRecipe.Kind kind) {
        List<InlayMatcher> inlays = entry.ingredients().stream()
                .map(InlayCraftingData::matcher)
                .toList();
        List<InlayOutput> results = new ArrayList<>();
        if (kind == InlayCraftingRecipe.Kind.COPYING) {
            // 珠宝复制：复制品带消失诅咒，被复制物原样返还
            results.add(cursed(entry.result()));
            results.add(fixed(entry.result()));
        } else {
            // 锻造：成品 + 返还模板
            results.add(fixed(entry.result()));
            results.add(fixed(entry.template()));
        }
        return new InlayCraftingData(
                AnvilcraftSmithingRecipes.craftingId(entry.source()),
                entry.template(),
                inlays,
                results,
                kind);
    }

    /** 前置成分（物品或标签）转成镶孔要求。 */
    private static InlayMatcher matcher(AnvilcraftSmithingRecipes.Spec spec) {
        if (spec.item() != null) return item(spec.item());
        if (spec.tag() != null) return tag(spec.tag());
        return InlayMatcher.byItem();
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    private static String itemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
