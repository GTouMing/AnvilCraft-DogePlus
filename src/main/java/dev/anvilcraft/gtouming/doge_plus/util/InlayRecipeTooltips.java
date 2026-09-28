package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayValue;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 镶合配方 tooltip 的公共文本：JEI 分类与 Ageratum 手册的配方渲染共用同一份，避免两处写歪。
 *
 * <p>「按属性指定镶孔」（{@link InlayMatcher.ByProperty}）的镶孔没有具体物品可列举——
 * 材料只要携带相应属性即可——因此候选物品只能由展示槽里的示例承担，必须另外用文字点名要哪种属性。</p>
 */
public final class InlayRecipeTooltips {

    /** 「按属性指定镶孔」的表头。 */
    private static final String REQUIREMENT_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.property_requirement";
    /** 同一要求要占多个镶孔时的数量后缀。 */
    private static final String TIMES_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.property_times";
    /** 「任一属性」的数量说明。 */
    private static final String ANY_OF_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.property_any_of";

    /** 概率行（固定概率，沿用前置「概率：%s%%」写法）。 */
    private static final String CHANCE_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.chance";
    /** 超限合金配方的附魔条数要求（同前置文案，金色）。 */
    private static final String TRANSCENDIUM_ENCHANTMENTS_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.transcendium.enchantments";
    /** 「N 及以上」的附魔条数下限写法。 */
    private static final String TRANSCENDIUM_AT_LEAST_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.transcendium.enchantments_at_least";
    /** 超限合金配方的数量说明（同前置文案，金色）。 */
    private static final String TRANSCENDIUM_AMOUNT_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.transcendium.amount";
    /** 超限合金配方的概率说明（同前置文案，金色）。 */
    private static final String TRANSCENDIUM_PROBABILITY_KEY =
            "gui.anvilcraft_doge_plus.jei.inlay_crafting.transcendium.probability";

    /** 数字格式化（与前置一致，最多 3 位小数）。 */
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    private InlayRecipeTooltips() {
    }

    /**
     * 配方里「按属性指定」的镶孔要求：一条表头 + 每种属性一行；没有这类镶孔时返回空列表。
     *
     * <p>按「每种属性一行」组织：逻辑载体是六种门各要一个，于是读作一条表头 + 六行门名。</p>
     */
    public static List<Component> propertyRequirements(List<InlayMatcher> inlays) {
        Map<List<InlayProperty>, Integer> groups = new LinkedHashMap<>();
        for (InlayMatcher matcher : inlays) {
            if (matcher instanceof InlayMatcher.ByProperty(List<InlayProperty> properties)) {
                groups.merge(properties, 1, Integer::sum);
            }
        }
        if (groups.isEmpty()) return List.of();

        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(REQUIREMENT_KEY));
        for (Map.Entry<List<InlayProperty>, Integer> group : groups.entrySet()) {
            List<InlayProperty> properties = group.getKey();
            int count = group.getValue();
            if (properties.size() == 1) {
                // 指定某一种属性：只列属性名；同一要求要占多个镶孔时补「×N」。
                InlayProperty property = properties.getFirst();
                lines.add(count > 1
                        ? propertyName(property).copy().append(Component.translatable(TIMES_KEY, count))
                        : propertyName(property));
                continue;
            }
            // 任一属性（携带其中一条即可）：先写明数量，再逐个列属性名。
            lines.add(Component.translatable(ANY_OF_KEY, count));
            for (InlayProperty property : properties) {
                lines.add(propertyName(property));
            }
        }
        return lines;
    }

    /**
     * 属性名：把属性描述从首个冒号处截断，只留「名称」那一半（保留属性自身的颜色）。
     *
     * <p>镶孔要求只是点名要哪种属性，完整描述会把 tooltip 挤爆。描述行统一写成
     * 「名称：说明」（中文用「：」、英文用 {@code :}），截到冒号前正是属性名；
     * 没有冒号（第三方自定义描述）时原样返回。</p>
     */
    public static Component propertyName(InlayProperty property) {
        Component tooltip = property.getTooltip();
        String text = tooltip.getString();
        int ascii = text.indexOf(':');
        int wide = text.indexOf('：');
        int split = ascii < 0 ? wide : wide < 0 ? ascii : Math.min(ascii, wide);
        return Component.literal(split < 0 ? text : text.substring(0, split))
                .withStyle(tooltip.getStyle());
    }

    // ==================== 超限合金配方：附魔条数 / 数量 / 概率 ====================

    /** 固定概率行（前置写法：百分数，灰色）。 */
    public static Component chance(InlayValue value) {
        return Component.translatable(CHANCE_KEY, FORMATTER.format(value.evaluate(0) * 100))
                .withStyle(ChatFormatting.GRAY);
    }

    /** 附魔条数要求行（金色），如「魔咒数量为 1-10」「魔咒数量为 16 及以上」。 */
    public static Component transcendiumEnchantments(InlayMatcher.EnchantRange range) {
        return Component.translatable(TRANSCENDIUM_ENCHANTMENTS_KEY, enchantRangeLabel(range))
                .withStyle(ChatFormatting.GOLD);
    }

    /** 数量随附魔条数变化时的说明行（金色），如「数量为魔咒数量 × 3」。 */
    public static Component transcendiumAmount(double multiplier) {
        return Component.translatable(TRANSCENDIUM_AMOUNT_KEY, FORMATTER.format(multiplier))
                .withStyle(ChatFormatting.GOLD);
    }

    /** 概率随附魔条数变化时的说明行（金色），如「概率：(10 × (魔咒数量)) / 100」。 */
    public static Component transcendiumChance(double multiplier) {
        return Component.translatable(TRANSCENDIUM_PROBABILITY_KEY, FORMATTER.format(multiplier * 100))
                .withStyle(ChatFormatting.GOLD);
    }

    /** 附魔条数范围的写法：定值「15」、区间「1-10」、开区间「16 及以上」。 */
    private static Component enchantRangeLabel(InlayMatcher.EnchantRange range) {
        if (range.min().isPresent() && range.max().isPresent()) {
            int min = range.min().get();
            int max = range.max().get();
            return Component.literal(min == max ? FORMATTER.format(min) : min + "-" + max);
        }
        if (range.min().isPresent()) {
            return Component.translatable(TRANSCENDIUM_AT_LEAST_KEY, FORMATTER.format(range.min().get()));
        }
        return Component.literal(range.max().map(max -> "0-" + max).orElse(""));
    }
}
