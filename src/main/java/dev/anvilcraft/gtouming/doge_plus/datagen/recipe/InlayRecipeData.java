package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData;
import dev.anvilcraft.gtouming.doge_plus.datagen.material.InlayMaterialData;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData.*;
import static dev.anvilcraft.gtouming.doge_plus.datagen.material.InlayMaterialData.*;

/**
 * inlay（镶嵌）配方的源数据。
 *
 * <p>inlay 配方 JSON 只含两个字段：{@code inlay}（材料定义文件名）与 {@code base}（基材定义文件名），
 * 实际物品解析由运行时 {@code MaterialManager} 读取 {@code data/<ns>/material/} 完成。
 * 配方文件路径由 {@link #fileName()} 按 {@code <inlay>_<base>} 约定推导，无需手写。</p>
 *
 * <p>{@code inlay} / {@code base} 引用 {@link InlayMaterialData} 与 {@link BaseMaterialData}
 * 中集中定义的 {@code NAME_*} 常量，保证配方引用的文件键一定存在且拼写一致。</p>
 *
 * @param inlay 材料文件名键（见 {@link InlayMaterialData#all}）
 * @param base  基材文件名键（见 {@link BaseMaterialData#all}）
 */
public record InlayRecipeData(String inlay, String base) {

    /** 配方输出文件名：{@code recipe/inlay/<inlay>_<base>.json}。 */
    public String fileName() {
        return inlay + "_" + base;
    }

    /**
     * 镶嵌载体可镶入的全部红石材料：八个逻辑门。
     * <p>这些材料原先镶入宝石块（红石/紫水晶等），现统一迁到两种镶嵌载体。</p>
     */
    public static final List<String> CARRIER_INLAYS = List.of(
            NOT_GATE, AND_GATE, OR_GATE, OUTPUT, INPUT,
            COUNTER_GATE, LATCH_GATE, DELAY_GATE
    );

    /** 可镶入上述材料的基材：镶嵌载体与超限镶嵌载体。 */
    public static final List<String> CARRIER_BASES = List.of(
            INLAY_CARRIER_BLOCK, TRANSCENDIUM_INLAY_CARRIER_BLOCK
    );

    /**
     * 材料 × 基材的显式配方组合。添加配方时在此增加一行即可，
     * 文件名由 {@link #fileName()} 自动推导，不会与文件键拼写不一致。
     */
    private static final List<InlayRecipeData> BASE_RECIPES = List.of(
            new InlayRecipeData(ATTACK, MELEE_WEAPONS),
            new InlayRecipeData(COLD_FORGED, TOOLS),
            new InlayRecipeData(DEFENSE, ARMORS),
            new InlayRecipeData(EFFECT, ARMORS),
            new InlayRecipeData(EFFECT1, MELEE_WEAPONS),
            new InlayRecipeData(EFFECT2, DOGE_STEEL_BLOCK),
            new InlayRecipeData(EMBER_METAL_INGOT, TOOLS),
            new InlayRecipeData(ENCHANT, ENCHANTABLES),
            new InlayRecipeData(ETERNAL, ENCHANTABLES),
            new InlayRecipeData(GENERATOR, ACCELERATION_RING),
            new InlayRecipeData(GENERATOR, DEFLECTION_RING),
            new InlayRecipeData(IRON_INGOT, HOLLOW_MAGNET_BLOCK),
            new InlayRecipeData(LIFE, ARMORS),
            new InlayRecipeData(MAGNETIC, DOGE_STEEL_BLOCK),
            new InlayRecipeData(RESONANCE, ENCHANTABLES),
            new InlayRecipeData(TOTEMS, CRAB_CLAW)
    );

    /**
     * 本 mod 内置 inlay 配方：手工清单 + 两种镶嵌载体（全部逻辑门）
     * + 原版锻造兼容（下界合金升级模板可镶入
     * {@link InlayMaterialData#NETHERITE_UPGRADE_INLAYS} 中的锻造材料/钻石装备）
     * + 原版盔甲纹饰兼容（每个纹饰模板可镶入可纹饰装备/纹饰材料）。
     */
    private static final List<InlayRecipeData> VANILLA = new ArrayList<>(BASE_RECIPES);
    static {
        for (String base : CARRIER_BASES) {
            for (String inlay : CARRIER_INLAYS) {
                VANILLA.add(new InlayRecipeData(inlay, base));
            }
        }
        for (String inlay : NETHERITE_UPGRADE_INLAYS) {
            VANILLA.add(new InlayRecipeData(inlay, NETHERITE_UPGRADE_TEMPLATE));
        }
        for (String trimBase : BaseMaterialData.TRIM_TEMPLATE_NAMES) {
            for (String trimInlay : InlayMaterialData.TRIM_INLAYS) {
                VANILLA.add(new InlayRecipeData(trimInlay, trimBase));
            }
        }
    }

    /**
     * 全部 inlay 配方：内置 + 前置（AnvilCraft）锻造模板 / 珠宝复制被复制物的成分镶入配方。
     *
     * <p>由 {@link AnvilcraftSmithingRecipes.Entry} 派生：每个成分 × 其基材，
     * 同一「基材 + 成分」只生成一次（珠宝复制配方中的重复材料只占一个同名配方）。</p>
     *
     * @param smithing 前置锻造配方
     * @param jewel    前置珠宝复制配方（材料数已由读取器筛选）
     */
    public static List<InlayRecipeData> all(
            List<AnvilcraftSmithingRecipes.Entry> smithing,
            List<AnvilcraftSmithingRecipes.Entry> jewel) {
        List<InlayRecipeData> list = new ArrayList<>(VANILLA);
        Set<String> seen = new LinkedHashSet<>();
        List<AnvilcraftSmithingRecipes.Entry> entries = new ArrayList<>(smithing);
        entries.addAll(jewel);
        for (AnvilcraftSmithingRecipes.Entry entry : entries) {
            String base = AnvilcraftSmithingRecipes.itemPath(entry.template());
            for (AnvilcraftSmithingRecipes.Spec spec : entry.ingredients()) {
                if (seen.add(base + "/" + spec.name())) {
                    list.add(new InlayRecipeData(spec.name(), base));
                }
            }
        }
        return list;
    }
}
