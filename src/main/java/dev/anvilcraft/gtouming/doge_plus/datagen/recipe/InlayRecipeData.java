package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData;
import dev.anvilcraft.gtouming.doge_plus.datagen.material.InlayMaterialData;

import java.util.ArrayList;
import java.util.List;

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
 * @param inlay 材料文件名键（见 {@link InlayMaterialData#ALL}）
 * @param base  基材文件名键（见 {@link BaseMaterialData#ALL}）
 */
public record InlayRecipeData(String inlay, String base) {

    /** 配方输出文件名：{@code recipe/inlay/<inlay>_<base>.json}。 */
    public String fileName() {
        return inlay + "_" + base;
    }

    /**
     * 材料 × 基材的显式配方组合。添加配方时在此增加一行即可，
     * 文件名由 {@link #fileName()} 自动推导，不会与文件键拼写不一致。
     */
    private static final List<InlayRecipeData> BASE_RECIPES = List.of(
            new InlayRecipeData(AND_GATE, SAPPHIRE_BLOCK),
            new InlayRecipeData(ATTACK, MELEE_WEAPONS),
            new InlayRecipeData(COLD_FORGED, TOOLS),
            new InlayRecipeData(DEFENSE, ARMORS),
            new InlayRecipeData(DIRECTION, GEM_BLOCKS),
            new InlayRecipeData(EFFECT, ARMORS),
            new InlayRecipeData(EFFECT1, MELEE_WEAPONS),
            new InlayRecipeData(EFFECT2, DOGE_STEEL_BLOCK),
            new InlayRecipeData(EMBER_METAL_INGOT, TOOLS),
            new InlayRecipeData(ENCHANT, ENCHANTABLES),
            new InlayRecipeData(ETERNAL, ENCHANTABLES),
            new InlayRecipeData(GENERATOR, ACCELERATION_RING),
            new InlayRecipeData(GENERATOR, DEFLECTION_RING),
            new InlayRecipeData(INPUT, GEM_BLOCKS),
            new InlayRecipeData(LIFE, ARMORS),
            new InlayRecipeData(MAGNETIC, DOGE_STEEL_BLOCK),
            new InlayRecipeData(NOT_GATE, RUBY_BLOCK),
            new InlayRecipeData(OR_GATE, TOPAZ_BLOCK),
            new InlayRecipeData(OUTPUT, EMERALD_BLOCK),
            new InlayRecipeData(RESONANCE, ENCHANTABLES),
            new InlayRecipeData(TOTEMS, CRAB_CLAW)
    );

    /**
     * 全部 inlay 配方：手工清单 + 原版锻造兼容（下界合金升级模板可镶入
     * {@link InlayMaterialData#NETHERITE_UPGRADE_INLAYS} 中的锻造材料/钻石装备）
     * + 原版盔甲纹饰兼容（每个纹饰模板可镶入可纹饰装备/纹饰材料）
     * + 前置（AnvilCraft）锻造兼容（模板镶入成分，见 {@link AnvilcraftSmithingCompat}）。
     */
    public static final List<InlayRecipeData> ALL = new ArrayList<>(BASE_RECIPES);
    static {
        for (String inlay : NETHERITE_UPGRADE_INLAYS) {
            ALL.add(new InlayRecipeData(inlay, NETHERITE_UPGRADE_TEMPLATE));
        }
        for (String trimBase : BaseMaterialData.TRIM_TEMPLATE_NAMES) {
            for (String trimInlay : InlayMaterialData.TRIM_INLAYS) {
                ALL.add(new InlayRecipeData(trimInlay, trimBase));
            }
        }
        ALL.addAll(AnvilcraftSmithingCompat.inlayRecipes());
    }
}
