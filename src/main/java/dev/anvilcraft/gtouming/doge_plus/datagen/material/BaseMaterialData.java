package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.AnvilcraftSmithingCompat;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

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
    public static final String EMERALD_BLOCK = "emerald_block";
    public static final String ENCHANTABLES = "enchantables";
    public static final String GEM_BLOCKS = "gem_blocks";
    public static final String MELEE_WEAPONS = "melee_weapons";
    public static final String MULTIPHASE_MATTER_BLOCK = "multiphase_matter_block";
    public static final String RUBY_BLOCK = "ruby_block";
    public static final String SAPPHIRE_BLOCK = "sapphire_block";
    public static final String TOOLS = "tools";
    public static final String TOPAZ_BLOCK = "topaz_block";
    public static final String DOGE_STEEL_BLOCK = "doge_steel_block";
    public static final String NETHERITE_UPGRADE_TEMPLATE = "netherite_upgrade_smithing_template";

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
            builder().name(EMERALD_BLOCK).item(Items.EMERALD_BLOCK).sockets(6).buildBase(),
            builder().name(ENCHANTABLES).tag(Tags.Items.ENCHANTABLES).sockets(2).buildBase(),
            builder().name(GEM_BLOCKS).tag(ModItemTags.GEM_BLOCKS).sockets(6).buildBase(),
            builder().name(MELEE_WEAPONS).tag(Tags.Items.MELEE_WEAPON_TOOLS).sockets(2).buildBase(),
            builder().name(MULTIPHASE_MATTER_BLOCK).item(ModBlocks.MULTIPHASE_MATTER_BLOCK).sockets(6).buildBase(),
            builder().name(RUBY_BLOCK).item(ModBlocks.RUBY_BLOCK).sockets(6).buildBase(),
            builder().name(SAPPHIRE_BLOCK).item(ModBlocks.SAPPHIRE_BLOCK).sockets(6).buildBase(),
            builder().name(TOOLS).tag(Tags.Items.TOOLS).sockets(2).buildBase(),
            builder().name(TOPAZ_BLOCK).item(ModBlocks.TOPAZ_BLOCK).sockets(6).buildBase(),
            builder().name(DOGE_STEEL_BLOCK)
                    .item(dev.anvilcraft.gtouming.doge_plus.init.ModBlocks.DOGE_STEEL_BLOCK).sockets(5).buildBase(),
            // 原版锻造兼容：下界合金升级模板开两个镶孔（镶孔可分别嵌入锻造材料与钻石装备）
            builder().name(NETHERITE_UPGRADE_TEMPLATE)
                    .item(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE).sockets(2).buildBase()
    };

    /**
     * 全部基材定义：手工清单 + 原版纹饰模板（每个开两镶孔：可纹饰装备 + 纹饰材料）
     * + 前置锻造模板（见 {@link AnvilcraftSmithingCompat}）。
     */
    public static final List<BaseMaterialData> ALL = new ArrayList<>();
    static {
        ALL.addAll(List.of(MANUAL));
        for (Item template : TRIM_TEMPLATES) {
            ALL.add(builder().name(trimTemplateName(template)).item(template).sockets(2).buildBase());
        }
        ALL.addAll(AnvilcraftSmithingCompat.baseMaterials());
    }
}
