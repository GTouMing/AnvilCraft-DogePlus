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
    // 所有内置性质使用互不相同的颜色；像素点阵与 tooltip 文本都按此区分。

    /** 耐火：携带此性质的物品免疫火焰与岩浆伤害。 */
    public static final InlayProperty FIRE_PROOF = register(new InlayProperty(
            AnvilCraftDogePlus.of("fire_proof"),
            "tooltip.anvilcraft_doge_plus.inlay_property.fire_proof",
            0xFF6B35)); // 橙红
    /** 磁性：携带此性质的物品/方块具有引力，激活时具有斥力。 */
    public static final InlayProperty MAGNETIC = register(new InlayProperty(
            AnvilCraftDogePlus.of("magnetic"),
            "tooltip.anvilcraft_doge_plus.inlay_property.magnetic",
            0xFFC857)); // 琥珀金
    /** 高温：在熔岩或火中越久，累加的伤害越高；攻击时消耗累加的伤害。 */
    public static final InlayProperty HIGH_TEMP = register(new InlayProperty(
            AnvilCraftDogePlus.of("high_temp"),
            "tooltip.anvilcraft_doge_plus.inlay_property.high_temp",
            0xFF3B30)); // 朱红
    /** 冷锻：在水中或细雪中缓慢回复耐久（仅耐久物品生效）。 */
    public static final InlayProperty COLD_FORGED = register(new InlayProperty(
            AnvilCraftDogePlus.of("cold_forged"),
            "tooltip.anvilcraft_doge_plus.inlay_property.cold_forged",
            0x63E6FF)); // 冰青
    /** 永恒：无法破坏，免疫火焰、爆炸、仙人掌、时间与虚空。 */
    public static final InlayProperty ETERNAL = register(new InlayProperty(
            AnvilCraftDogePlus.of("eternal"),
            "tooltip.anvilcraft_doge_plus.inlay_property.eternal",
            0xA66BFF)); // 紫罗兰
    /** 涅槃：死亡时触发图腾，然后该材料碎裂。 */
    public static final InlayProperty NIRVANA = register(new InlayProperty(
            AnvilCraftDogePlus.of("nirvana"),
            "tooltip.anvilcraft_doge_plus.inlay_property.nirvana",
            0xFF6BCB)); // 桃粉
    /** 防御：手持或装备时提升 2 点盔甲值。 */
    public static final InlayProperty DEFENSE = register(new InlayProperty(
            AnvilCraftDogePlus.of("defense"),
            "tooltip.anvilcraft_doge_plus.inlay_property.defense",
            0x5B8CFF)); // 宝蓝
    /** 生命：手持或装备时提升 2 点生命。 */
    public static final InlayProperty LIFE = register(new InlayProperty(
            AnvilCraftDogePlus.of("life"),
            "tooltip.anvilcraft_doge_plus.inlay_property.life",
            0x4BE36B)); // 翠绿
    /** 攻击：手持或装备时提升 2 点攻击力。 */
    public static final InlayProperty ATTACK = register(new InlayProperty(
            AnvilCraftDogePlus.of("attack"),
            "tooltip.anvilcraft_doge_plus.inlay_property.attack",
            0xFF4D4D)); // 正红
    /** 附魔：镶嵌时合并附魔，移除时提取附魔。 */
    public static final InlayProperty ENCHANT = register(new InlayProperty(
            AnvilCraftDogePlus.of("enchant"),
            "tooltip.anvilcraft_doge_plus.inlay_property.enchant",
            0xC88CFF)); // 薰衣草紫

    /** 共鸣：增强其他镶孔的部分材料属性。 */
    public static final InlayProperty RESONANCE = register(new InlayProperty(
            AnvilCraftDogePlus.of("resonance"),
            "tooltip.anvilcraft_doge_plus.inlay_property.resonance",
            0xFF9EC4
    ));

    /** 效果：手持、装备或放置时提供药水效果。 */
    public static final InlayProperty EFFECT = register(new InlayProperty(
            AnvilCraftDogePlus.of("effect"),
            "tooltip.anvilcraft_doge_plus.inlay_property.effect",
            0x5CC8FF
    ));

    // ==================== 红石性质 ====================

    /** 输出：该面输出红石信号。 */
    public static final InlayProperty OUTPUT = register(new InlayProperty(
            AnvilCraftDogePlus.of("output"),
            "tooltip.anvilcraft_doge_plus.inlay_property.output",
            0xFF3D8B)); // 洋红

    /** 输入：该面作为红石输入。 */
    public static final InlayProperty INPUT = register(new InlayProperty(
            AnvilCraftDogePlus.of("input"),
            "tooltip.anvilcraft_doge_plus.inlay_property.input",
            0xFF9F43)); // 蜜橙

    /** 非门：该面输出反面输入的反信号。 */
    public static final InlayProperty NOT_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("not_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.not_gate",
            0xFF8A3D)); // 橘橙

    /** 与门：该面输出邻面输入的与信号（按序查找）。 */
    public static final InlayProperty AND_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("and_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.and_gate",
            0x9DFF3D)); // 青柠

    /** 计数门：累计输入脉冲次数，达到设定次数时输出 1 tick 的 15。 */
    public static final InlayProperty COUNTER_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("counter_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.counter_gate",
            0x2EE6D6)); // 绿松石

    /** 锁存门：收到输入则记录并输出该信号，再收到则清除。 */
    public static final InlayProperty LATCH_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("latch_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.latch_gate",
            0xFF3DFF)); // 品红

    /** 延时门：收到输入起输出该信号，持续设定 tick 数后停止。 */
    public static final InlayProperty DELAY_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("delay_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.delay_gate",
            0xE6E63D)); // 柠檬黄

    /** 延时输入门：记录输入信号，等待设定 tick 数后作为该面的输入向本方块的门送出 1 tick。 */
    public static final InlayProperty DELAY_INPUT_GATE = register(new InlayProperty(
            AnvilCraftDogePlus.of("delay_input_gate"),
            "tooltip.anvilcraft_doge_plus.inlay_property.delay_input_gate",
            0xC6FF66)); // 黄绿

    /** 远程门：同信道编号的远程面互连为一条总线；红石侧为双向导线，物品侧提供远程查找取出货源。 */
    public static final InlayProperty REMOTE = register(new InlayProperty(
            AnvilCraftDogePlus.of("remote"),
            "tooltip.anvilcraft_doge_plus.inlay_property.remote",
            0x3DFFC6)); // 青绿

    /** 发电：放置后作为发电机接入电网输出电力（功率见配置 {@code generatorPower}）。 */
    public static final InlayProperty GENERATOR = register(new InlayProperty(
            AnvilCraftDogePlus.of("generator"),
            "tooltip.anvilcraft_doge_plus.inlay_property.generator",
            0x7B61FF
    ));

    // ==================== 物品搬运属性 ====================

    /** 合成存入：在存入前尝试用取出源中的材料合成过滤物品。 */
    public static final InlayProperty CRAFT_INSERT = register(new InlayProperty(
            AnvilCraftDogePlus.of("craft_insert"),
            "tooltip.anvilcraft_doge_plus.inlay_property.craft_insert",
            0x3DFF9E)); // 薄荷绿

    /** 存入：将物品存入面朝的容器。 */
    public static final InlayProperty INSERT = register(new InlayProperty(
            AnvilCraftDogePlus.of("insert"),
            "tooltip.anvilcraft_doge_plus.inlay_property.insert",
            0x1FCB7A)); // 祖母绿

    /** 取出：从面朝的容器取出物品。 */
    public static final InlayProperty EXTRACT = register(new InlayProperty(
            AnvilCraftDogePlus.of("extract"),
            "tooltip.anvilcraft_doge_plus.inlay_property.extract",
            0xE6B566)); // 沙金

    // ==================== 数据 ====================

    /** tooltip 描述行组件（带颜色）。随配置变化的数值（如发电功率）在此取实时值。 */
    public Component getTooltip() {
        return (this == GENERATOR
                ? Component.translatable(this.descriptionKey, AnvilCraftDogePlus.CONFIG.generatorPower)
                : Component.translatable(this.descriptionKey))
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(this.color)));
    }

    /** 获取颜色值。 */
    public int getColor() {
        return color;
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