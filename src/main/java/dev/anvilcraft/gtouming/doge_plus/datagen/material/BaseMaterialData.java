package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.AnvilcraftSmithingRecipes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialBuilder.builder;

/**
 * 基材（base material）源数据：{@code data/anvilcraft_doge_plus/material/base/<name>.json}。
 * <p>通过 {@link MaterialBuilder} 声明式描述 ingredient 匹配条件（物品/标签），
 * 序列化后与原先手写内容等价。</p>
 * <p>基材文件键（{@code name}）集中定义为下方 {@code NAME_*} 常量；匹配物品/标签直接引用
 * vanilla / AnvilCraft / 本 mod 的注册条目与官方标签常量。其中「工具子标签」等路径型
 * 标签（如 {@code c:tools/melee_weapon}）以 {@link Tags.Items} 的常量表达，父子层级
 * 由标签系统承载，无需在本类重复建模。</p>
 *
 * @param name       文件键（不含 {@code .json}）
 * @param sockets    镶孔数
 * @param ingredient ingredient 字段的 JSON 数组
 */
public record BaseMaterialData(String name, int sockets, JsonArray ingredient) {

    // 基材文件键常量 —— 唯一字面量定义处，ALL 与 InlayRecipeData 均引用它们
    public static final String ACCELERATION_RING = "acceleration_ring";
    public static final String ARMORS = "armors";
    public static final String CRAB_CLAW = "crab_claw";
    public static final String DEFLECTION_RING = "deflection_ring";
    public static final String ENCHANTABLES = "enchantables";
    public static final String MELEE_WEAPONS = "melee_weapons";
    public static final String MULTIPHASE_MATTER_BLOCK = "multiphase_matter_block";
    public static final String TOOLS = "tools";
    public static final String DOGE_STEEL_BLOCK = "doge_steel_block";
    public static final String INLAY_CARRIER_BLOCK = "inlay_carrier_block";
    public static final String TRANSCENDIUM_INLAY_CARRIER_BLOCK = "transcendium_inlay_carrier_block";
    public static final String NETHERITE_UPGRADE_TEMPLATE = "netherite_upgrade_smithing_template";
    public static final String HOLLOW_MAGNET_BLOCK = "hollow_magnet_block";

    /** 空心磁铁块镶孔数：每个镶孔镶入一个铁锭，镶合出等量磁铁锭。 */
    public static final int HOLLOW_MAGNET_BLOCK_SOCKETS = 4;

    /** 原版全部盔甲纹饰模板（18 种，1.21.1）。 */
    public static final List<Item> TRIM_TEMPLATES = List.of(
            Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE,
            Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE
    );

    /** 纹饰模板基材文件键 = 模板物品路径。 */
    public static String trimTemplateName(Item template) {
        return BuiltInRegistries.ITEM.getKey(template).getPath();
    }

    /** 纹饰模板基材文件键列表（供配方对/镶合配方引用）。 */
    public static final List<String> TRIM_TEMPLATE_NAMES =
            TRIM_TEMPLATES.stream().map(BaseMaterialData::trimTemplateName).toList();

    private static final BaseMaterialData[] MANUAL = {
            builder().name(ACCELERATION_RING).item(ModBlocks.ACCELERATION_RING).sockets(1).buildBase(),
            builder().name(ARMORS).tag(Tags.Items.ARMORS).sockets(3).buildBase(),
            builder().name(CRAB_CLAW).item(ModItems.CRAB_CLAW).sockets(3).buildBase(),
            builder().name(DEFLECTION_RING).item(ModBlocks.DEFLECTION_RING).sockets(1).buildBase(),
            builder().name(ENCHANTABLES).tag(Tags.Items.ENCHANTABLES).sockets(2).buildBase(),
            builder().name(MELEE_WEAPONS).tag(Tags.Items.MELEE_WEAPON_TOOLS).sockets(2).buildBase(),
            builder().name(MULTIPHASE_MATTER_BLOCK).item(ModBlocks.MULTIPHASE_MATTER_BLOCK).sockets(6).buildBase(),
            builder().name(TOOLS).tag(Tags.Items.TOOLS).sockets(2).buildBase(),
            builder().name(DOGE_STEEL_BLOCK)
                    .item(dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.DOGE_STEEL_BLOCK).sockets(5).buildBase(),
            // 镶嵌载体：固定 6 镶孔
            builder().name(INLAY_CARRIER_BLOCK)
                    .item(dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.INLAY_CARRIER).sockets(6).buildBase(),
            // 超限镶嵌载体：镶孔数写死为「已镶嵌数量 + 1」，此处 sockets 仅为占位，运行时由
            // MaterialManager 特判覆盖，数据包无法更改实际镶孔数
            builder().name(TRANSCENDIUM_INLAY_CARRIER_BLOCK)
                    .item(dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.TRANSCENDIUM_INLAY_CARRIER).sockets(1).buildBase(),
            // 原版锻造兼容：下界合金升级模板开两个镶孔（镶孔可分别嵌入锻造材料与钻石装备）
            builder().name(NETHERITE_UPGRADE_TEMPLATE)
                    .item(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).sockets(2).buildBase(),
            // 空心磁铁块：每个镶孔可镶入一个铁锭，镶合出等量磁铁锭
            builder().name(HOLLOW_MAGNET_BLOCK)
                    .item(ModBlocks.HOLLOW_MAGNET_BLOCK).sockets(HOLLOW_MAGNET_BLOCK_SOCKETS).buildBase()
    };

    /**
     * 本 mod 内置基材（手工清单 + 原版纹饰模板：每个开两镶孔，可纹饰装备 + 纹饰材料）。
     */
    private static final List<BaseMaterialData> BASE = new ArrayList<>();
    static {
        BASE.addAll(List.of(MANUAL));
        for (Item template : TRIM_TEMPLATES) {
            BASE.add(builder().name(trimTemplateName(template)).item(template).sockets(2).buildBase());
        }
    }

    /**
     * 全部基材定义：内置 + 前置（AnvilCraft）锻造模板 + 前置珠宝复制配方的被复制物。
     *
     * <p>模板基材由 {@link AnvilcraftSmithingRecipes.Entry} 派生：镶孔数 = 非模板成分数
     * （普通锻造为 2：追加材料 + 基材；n 合一锻造为 n+1：材料 + n 个输入）。珠宝复制配方
     * 以被复制物作模具，镶孔数 = 该配方材料数。同名基材只生成一次，内置定义优先。</p>
     *
     * @param smithing 前置锻造配方
     * @param jewel    前置珠宝复制配方（材料数已由读取器筛选）
     */
    public static List<BaseMaterialData> all(
            List<AnvilcraftSmithingRecipes.Entry> smithing,
            List<AnvilcraftSmithingRecipes.Entry> jewel) {
        List<BaseMaterialData> list = new ArrayList<>(BASE);
        Set<String> names = new HashSet<>();
        for (BaseMaterialData base : BASE) {
            names.add(base.name());
        }
        List<AnvilcraftSmithingRecipes.Entry> entries = new ArrayList<>(smithing);
        entries.addAll(jewel);
        for (AnvilcraftSmithingRecipes.Entry entry : entries) {
            String name = AnvilcraftSmithingRecipes.itemPath(entry.template());
            if (names.add(name)) {
                list.add(builder().name(name).item(entry.template()).sockets(entry.ingredients().size()).buildBase());
            }
        }
        return list;
    }
}
