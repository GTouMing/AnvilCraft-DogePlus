package dev.anvilcraft.gtouming.doge_plus.recipe.inlay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 镶嵌性质（可注册，带命名空间）。
 *
 * <p>性质与材料一一对应、由材料数据包 {@code material/inlays/} 分配；
 * 具体行为由各系统读取 {@code INLAY} 组件后施加。其他模组可通过
 * {@link #register(InlayProperty)} 注册自定义性质。</p>
 *
 * @param id             性质的唯一 ID（命名空间:路径）。
 * @param descriptionKey 性质完整描述（tooltip 行）的翻译 key。
 * @param color          性质颜色（RGB 整数值，默认 0xFFFFFF 白色）。
 */
public record InlayProperty(ResourceLocation id, String descriptionKey, int color) {

    private static final Map<ResourceLocation, InlayProperty> REGISTRY = new LinkedHashMap<>();

    // ==================== 本模组内置性质 ====================

    /** 耐火：携带此性质的物品免疫火焰与岩浆伤害。 */
    public static final InlayProperty FIRE_PROOF = register(new InlayProperty(
            AnvilCraftDogePlus.of("fire_proof"),
            "tooltip.anvilcraft_doge_plus.inlay_property.fire_proof",
            0xFFAA00)); // 金色
    /** 磁性：携带此性质的物品会被磁铁吸附。 */
    public static final InlayProperty MAGNETIC = register(new InlayProperty(
            AnvilCraftDogePlus.of("magnetic"),
            "tooltip.anvilcraft_doge_plus.inlay_property.magnetic",
            0xFFAA00)); // 金色
    /** 高温：在熔岩或火中越久，累加的伤害越高；攻击时消耗累加的伤害。 */
    public static final InlayProperty HIGH_TEMP = register(new InlayProperty(
            AnvilCraftDogePlus.of("high_temp"),
            "tooltip.anvilcraft_doge_plus.inlay_property.high_temp",
            0xFF5500)); // 橙色
    /** 冷锻：在水中或细雪中缓慢回复耐久（仅耐久物品生效）。 */
    public static final InlayProperty COLD_FORGED = register(new InlayProperty(
            AnvilCraftDogePlus.of("cold_forged"),
            "tooltip.anvilcraft_doge_plus.inlay_property.cold_forged",
            0x55FFFF)); // 青色
    /** 永恒：无法破坏，免疫火焰、爆炸、仙人掌、时间与虚空。 */
    public static final InlayProperty ETERNAL = register(new InlayProperty(
            AnvilCraftDogePlus.of("eternal"),
            "tooltip.anvilcraft_doge_plus.inlay_property.eternal",
            0xAA55FF)); // 紫色
    /** 涅槃：死亡时触发图腾，然后该材料碎裂。 */
    public static final InlayProperty NIRVANA = register(new InlayProperty(
            AnvilCraftDogePlus.of("nirvana"),
            "tooltip.anvilcraft_doge_plus.inlay_property.nirvana",
            0xFF55FF)); // 粉紫
    /** 防御：手持或装备时提升 2 点盔甲值。 */
    public static final InlayProperty DEFENSE = register(new InlayProperty(
            AnvilCraftDogePlus.of("defense"),
            "tooltip.anvilcraft_doge_plus.inlay_property.defense",
            0x55AAFF)); // 蓝色
    /** 生命：手持或装备时提升 2 点生命。 */
    public static final InlayProperty LIFE = register(new InlayProperty(
            AnvilCraftDogePlus.of("life"),
            "tooltip.anvilcraft_doge_plus.inlay_property.life",
            0x55FF55)); // 绿色
    /** 攻击：手持或装备时提升 2 点攻击力。 */
    public static final InlayProperty ATTACK = register(new InlayProperty(
            AnvilCraftDogePlus.of("attack"),
            "tooltip.anvilcraft_doge_plus.inlay_property.attack",
            0xFF5555)); // 红色
    /** 附魔：镶嵌时合并附魔，移除时提取附魔。 */
    public static final InlayProperty ENCHANT = register(new InlayProperty(
            AnvilCraftDogePlus.of("enchant"),
            "tooltip.anvilcraft_doge_plus.inlay_property.enchant",
            0xAA55FF)); // 紫色

    /** 共鸣：增强其他镶孔的部分材料属性。 */
    public static final InlayProperty RESONANCE = register(new InlayProperty(
            AnvilCraftDogePlus.of("resonance"),
            "tooltip.anvilcraft_doge_plus.inlay_property.resonance",
            0xFF66CC
    ));

    public static final InlayProperty EFFECT = register(new InlayProperty(
            AnvilCraftDogePlus.of("effect"),
            "tooltip.anvilcraft_doge_plus.inlay_property.effect",
            0x66CCFF
    ));

    // ==================== 方向/红石性质 ====================

    /** 方向：使镶孔具有方向性，镶孔数为6时生效。 */
    public static final InlayProperty DIRECTION = register(new InlayProperty(
            AnvilCraftDogePlus.of("direction"),
            "tooltip.anvilcraft_doge_plus.inlay_property.direction",
            0x00CCFF)); // 天蓝色

    /** 输出：该面输出红石信号。 */
    public static final InlayProperty OUTPUT = register(new InlayProperty(
            AnvilCraftDogePlus.of("output"),
            "tooltip.anvilcraft_doge_plus.inlay_property.output",
            0xFF3333)); // 亮红色

    /** 输出：该面输出红石信号。 */
    public static final InlayProperty INPUT = register(new InlayProperty(
            AnvilCraftDogePlus.of("input"),
            "tooltip.anvilcraft_doge_plus.inlay_property.input",
            0xFF3333)); // 亮红色

    /** 非门：该面输出反面输入的反信号。 */
    public static final InlayProperty NOT_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("not_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.not_gate",
            0xFF8800)); // 橙色

    /** 与门：该面输出邻面输入的与信号（按序查找）。 */
    public static final InlayProperty AND_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("and_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.and_gate",
            0x00FF44)); // 亮绿色

    /** 或门：该面输出邻面输入的或信号（按序查找）。 */
    public static final InlayProperty OR_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("or_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.or_gate",
            0x44FF00)); // 黄绿色

    /** 发电：放置后产生 512 kW 电力*/
    public static final InlayProperty GENERATOR = register(new InlayProperty(
            AnvilCraftDogePlus.of("generator"),
            "tooltip.anvilcraft_doge_plus.inlay_property.generator",
            0xFF8800
    ));

    // ==================== 数据 ====================

    /** tooltip 描述行组件（带颜色）。 */
    public Component getTooltip() {
        return Component.translatable(this.descriptionKey)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color)));
    }

    /** 获取颜色值。 */
    public int getColor() {
        return color;
    }

    /** 获取 TextColor 对象。 */
    public TextColor getTextColor() {
        return TextColor.fromRgb(color);
    }

    // ==================== 注册表（参考 AbstractRegistrum 模式） ====================

    /** 编解码器：按名称解析（裸字符串按本模组命名空间，如 "fire_proof" → anvilcraft_doge_plus:fire_proof）。 */
    public static final Codec<InlayProperty> CODEC = Codec.STRING.flatXmap(
            name -> {
                InlayProperty property = fromPath(name);
                return property != null
                        ? DataResult.success(property)
                        : DataResult.error(() -> "Unknown inlay property: " + name);
            },
            property -> DataResult.success(property.id.toString()));

    /** 注册一个性质；重复 ID 会覆盖。 */
    public static InlayProperty register(InlayProperty property) {
        REGISTRY.put(property.id, property);
        return property;
    }

    /** 按 ID 查询性质；未注册返回 null。 */
    @Nullable
    public static InlayProperty get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /** 按名称查询：裸字符串按本模组命名空间解析（如 "fire_proof" → anvilcraft_doge_plus:fire_proof），也可写完整 ID。 */
    @Nullable
    public static InlayProperty fromPath(String name) {
        ResourceLocation id = name.contains(":") ? ResourceLocation.parse(name) : AnvilCraftDogePlus.of(name);
        return get(id);
    }

    /** 全部已注册性质（按注册顺序）。 */
    public static Collection<InlayProperty> values() {
        return REGISTRY.values();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof InlayProperty p && this.id.equals(p.id));
    }

    @Override
    public int hashCode() {
        return this.id.hashCode();
    }

    @Override
    public String toString() {
        return this.id.toString();
    }
}