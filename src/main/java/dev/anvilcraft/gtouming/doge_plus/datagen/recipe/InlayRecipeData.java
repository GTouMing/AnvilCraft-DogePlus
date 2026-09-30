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
 * @param inlay 材料文件名键（见 {@link InlayMaterialData#all}）
 * @param base  基材文件名键（见 {@link BaseMaterialData#all}）
 */
public record InlayRecipeData(String inlay, String base) {

    /** 配方输出文件名：{@code recipe/inlay/<inlay>_<base>.json}。 */
    public String fileName() {
        return inlay + "_" + base;
    }

    /**
     * 镶嵌载体可镶入的全部材料：八个逻辑门 + 远程门 + 物品搬运用的溜槽 / 磁性溜槽。
     * <p>逻辑门材料原先镶入宝石块（红石/紫水晶等），现统一迁到两种镶嵌载体；
     * 溜槽给该面「存入」、磁性溜槽给该面「取出」（搬运行为由后续版本实现）。</p>
     */
    public static final List<String> CARRIER_INLAYS = List.of(
            NOT_GATE, AND_GATE, OUTPUT, INPUT,
            COUNTER_GATE, LATCH_GATE, DELAY_GATE, DELAY_INPUT_GATE, REMOTE,
            CHUTE, MAGNETIC_CHUTE
    );

    /** 可镶入上述材料的基材：镶嵌载体。 */
    public static final List<String> CARRIER_BASES = List.of(
            INLAY_CARRIER_BLOCK
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
            new InlayRecipeData(TOTEMS, CRAB_CLAW),
            // 超温余烬金属块不接受显式镶嵌配方：中子锭没有材料定义，
            // 由「静默镶嵌」按 neutronium_transmutation 镶合条件放行
            // 充能中子锭：2 镶孔，可镶书 / 附魔书（enchant）与紫水晶（resonance）
            new InlayRecipeData(ENCHANT, CHARGED_NEUTRONIUM_INGOT),
            new InlayRecipeData(RESONANCE, CHARGED_NEUTRONIUM_INGOT)
    );

    /**
     * 本 mod 内置 inlay 配方：手工清单 + 两种镶嵌载体（全部逻辑门）。
     *
     * <p>下界升级模板与盔甲纹饰模板<b>不写镶嵌配方</b>：它们要镶入的锻造材料 / 纹饰材料
     * 没有属性，由「静默镶嵌」按对应镶合（{@code kind = smithing}）的 {@code inlays}
     * 条件放行，因此不必在 {@code recipe/inlay} 里占条目，也不会出现在 JEI 的镶嵌配方中。</p>
     */
    private static final List<InlayRecipeData> VANILLA = new ArrayList<>(BASE_RECIPES);
    static {
        for (String base : CARRIER_BASES) {
            for (String inlay : CARRIER_INLAYS) {
                VANILLA.add(new InlayRecipeData(inlay, base));
            }
        }
    }

    /**
     * 全部 inlay 配方：只保留内置清单。
     *
     * <p>前置（AnvilCraft）锻造 / 珠宝复制派生的镶嵌配方<b>不再生成</b>——那些组合改为
     * 运行时「静默镶嵌」（{@code InlayRecipe.canSilentlyInlay}）：只要符合对应镶合配方的
     * 条件就允许镶上去，但不写进配方表、也不出现在 JEI。材料与基材定义仍由 datagen 派生。</p>
     *
     */
    public static List<InlayRecipeData> all() {
        return new ArrayList<>(VANILLA);
    }
}
