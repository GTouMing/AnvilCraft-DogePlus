package dev.anvilcraft.gtouming.doge_plus.attribute;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 等级属性注册表：把「镶嵌属性 → 施加方式」固定下来，每级数值由
 * {@code data/<ns>/attributes/<属性>.json} 提供（见 {@link AttributeManager}）。
 *
 * <p>等级 = 全部已装备物品上「携带该属性」的镶嵌条目总数（带共鸣的物品贡献翻倍），
 * 具体施加逻辑见 {@link InlayLevelService}。</p>
 */
public final class LeveledAttributes {

    /** 等级属性的作用方式。 */
    public enum Kind {
        /** 施加为实体的原版属性修饰器（defense / life / attack）。 */
        ATTRIBUTE,
        /** 施加为药水效果，等级即强度（effect）。 */
        EFFECT
    }

    /**
     * @param property  对应的镶嵌属性
     * @param kind      作用方式
     * @param attribute 原版属性（{@link Kind#ATTRIBUTE} 用；{@link Kind#EFFECT} 为 null）
     */
    public record Def(InlayProperty property, Kind kind, @Nullable Holder<Attribute> attribute) {}

    private static final Map<ResourceLocation, Def> REGISTRY = new LinkedHashMap<>();

    /** 防御：每级按 attributes/defense.json 提升盔甲值。 */
    public static final Def DEFENSE = register(InlayProperty.DEFENSE, Kind.ATTRIBUTE, Attributes.ARMOR);
    /** 生命：每级按 attributes/life.json 提升生命上限。 */
    public static final Def LIFE = register(InlayProperty.LIFE, Kind.ATTRIBUTE, Attributes.MAX_HEALTH);
    /** 攻击：每级按 attributes/attack.json 提升攻击力。 */
    public static final Def ATTACK = register(InlayProperty.ATTACK, Kind.ATTRIBUTE, Attributes.ATTACK_DAMAGE);
    /** 效果：每级按 attributes/effect.json 提升药水强度。 */
    public static final Def EFFECT = register(InlayProperty.EFFECT, Kind.EFFECT, null);

    private LeveledAttributes() {}

    private static Def register(InlayProperty property, Kind kind, @Nullable Holder<Attribute> attribute) {
        Def def = new Def(property, kind, attribute);
        REGISTRY.put(property.id(), def);
        return def;
    }

    /** 按属性 id 查询；未纳入等级系统返回 null。 */
    @Nullable
    public static Def get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /** 该镶嵌属性是否纳入等级系统。 */
    public static boolean isLeveled(InlayProperty property) {
        return REGISTRY.containsKey(property.id());
    }

    /** 全部等级属性（按注册顺序）。 */
    public static Collection<Def> values() {
        return REGISTRY.values();
    }
}
