package dev.anvilcraft.gtouming.doge_plus.attribute;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 等级重算引擎：按实体当前装备重建每个等级属性的计数，与旧记录比对后重新施加变更者。
 *
 * <p>等级 = 全部已装备物品上携带该属性的镶嵌条目总数；带 {@link InlayProperty#RESONANCE}
 * 的物品其贡献翻倍（保留旧的共鸣 ×2 语义）。重算对每个键幂等施加，因此不依赖「旧记录是否等于
 * 新记录」来判断是否需要施加——新实体（重生/换维度）即便计数不变也必须重新挂上修饰器与效果。</p>
 *
 * <p>effect 类：键为具体 mob effect id，计数取自镶嵌材料记录的<b>真实药水效果</b>
 * （{@code Potion.getEffects()}），等级映射为药水强度（amplifier），效果为无限时长，
 * 并由 {@code MobEffectEvent.Remove} 保护不被牛奶 / {@code /effect clear} 清除。</p>
 */
public final class InlayLevelService {

    /** 参与统计的装备槽。 */
    private static final EquipmentSlot[] EQUIPMENT = {
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND,
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    /** 重算自身移除效果时置位，供 {@code MobEffectEvent.Remove} 放行（区别于外部清除）。 */
    private static boolean internalEffectRemoval = false;

    private InlayLevelService() {}

    public static boolean isInternalEffectRemoval() {
        return internalEffectRemoval;
    }

    /** 重算并施加该实体的全部等级属性。 */
    public static void reconcile(ServerLevel level, LivingEntity entity) {
        AttributeLevelManager manager = AttributeLevelManager.get(level);
        if (manager == null) return;

        Map<ResourceLocation, AttributeLevelManager.Entry> fresh = count(entity);
        Map<ResourceLocation, AttributeLevelManager.Entry> old = manager.get(entity.getUUID());

        Set<ResourceLocation> keys = new HashSet<>(old.keySet());
        keys.addAll(fresh.keySet());
        for (ResourceLocation key : keys) {
            apply(entity, key, levelOf(fresh.get(key)));
        }

        manager.set(entity.getUUID(), fresh);
    }

    private static int levelOf(AttributeLevelManager.Entry entry) {
        return entry == null ? 0 : entry.count();
    }

    // ==================== 统计 ====================

    /** 统计当前装备上各等级属性的数量与来源；共鸣物品的贡献翻倍。 */
    private static Map<ResourceLocation, AttributeLevelManager.Entry> count(LivingEntity entity) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        Map<ResourceLocation, Set<EquipmentSlot>> sources = new HashMap<>();

        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) continue;
            int weight = InlayUtil.hasProperty(stack, InlayProperty.RESONANCE) ? 2 : 1;
            for (InlayEntry entry : InlayUtil.getInlays(stack)) {
                if (entry.isEmpty()) continue;
                // 属性类：defense / life / attack
                for (LeveledAttributes.Def def : LeveledAttributes.values()) {
                    if (def.kind() != LeveledAttributes.Kind.ATTRIBUTE) continue;
                    if (!entry.containsAttributes(def.property())) continue;
                    add(counts, sources, def.property().id(), weight, slot);
                }
                // 效果类：用材料的真实药水效果，逐个 mob effect 计数
                if (entry.containsAttributes(InlayProperty.EFFECT)) {
                    for (ResourceLocation effectId : effectIdsOf(entry)) {
                        add(counts, sources, effectId, weight, slot);
                    }
                }
            }
        }

        Map<ResourceLocation, AttributeLevelManager.Entry> result = new HashMap<>();
        for (Map.Entry<ResourceLocation, Integer> e : counts.entrySet()) {
            result.put(e.getKey(), new AttributeLevelManager.Entry(e.getValue(), sources.get(e.getKey())));
        }
        return result;
    }

    private static void add(
            Map<ResourceLocation, Integer> counts,
            Map<ResourceLocation, Set<EquipmentSlot>> sources,
            ResourceLocation key,
            int weight,
            EquipmentSlot slot) {
        counts.merge(key, weight, Integer::sum);
        sources.computeIfAbsent(key, k -> EnumSet.noneOf(EquipmentSlot.class)).add(slot);
    }

    /** 镶嵌条目记录的药水所含的全部 mob effect id（不猜 id，直接用 {@code Potion.getEffects()}）。 */
    private static List<ResourceLocation> effectIdsOf(InlayEntry entry) {
        ResourceLocation potionId = entry.potion().orElse(null);
        if (potionId == null) return List.of();
        var potion = BuiltInRegistries.POTION.getHolder(potionId).orElse(null);
        if (potion == null) return List.of();

        List<ResourceLocation> ids = new ArrayList<>();
        for (MobEffectInstance instance : potion.value().getEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(instance.getEffect().value());
            if (id != null) ids.add(id);
        }
        return ids;
    }

    // ==================== 施加 ====================

    private static void apply(LivingEntity entity, ResourceLocation key, int level) {
        LeveledAttributes.Def def = LeveledAttributes.get(key);
        if (def != null && def.kind() == LeveledAttributes.Kind.ATTRIBUTE) {
            applyAttribute(entity, key, def, level);
            return;
        }
        // effect：键是具体 mob effect id，不在等级属性注册表里
        if (BuiltInRegistries.MOB_EFFECT.containsKey(key)) {
            applyEffect(entity, key, level);
        }
    }

    /** 以固定 id 幂等地增删实体属性修饰器。 */
    private static void applyAttribute(LivingEntity entity, ResourceLocation key, LeveledAttributes.Def def, int level) {
        if (def.attribute() == null) return;
        AttributeInstance instance = entity.getAttribute(def.attribute());
        if (instance == null) return;

        ResourceLocation modifierId = AnvilCraftDogePlus.of("inlay_" + def.property().id().getPath());
        instance.removeModifier(modifierId);

        double value = AttributeManager.valueOf(key, level);
        if (value != 0.0) {
            instance.addTransientModifier(new AttributeModifier(
                    modifierId, value, AttributeModifier.Operation.ADD_VALUE));
        }

        if (def.attribute().equals(Attributes.MAX_HEALTH) && entity.getHealth() > entity.getMaxHealth()) {
            entity.setHealth(entity.getMaxHealth());
        }
    }

    /** 等级 → 药水强度；等级 0 移除该效果。 */
    private static void applyEffect(LivingEntity entity, ResourceLocation key, int level) {
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(key).orElse(null);
        if (effect == null) return;

        MobEffectInstance current = entity.getEffect(effect);
        if (level <= 0) {
            if (current != null) removeEffectInternally(entity, effect);
            return;
        }

        int amplifier = (int) AttributeManager.valueOf(InlayProperty.EFFECT.id(), level);
        if (current != null && current.getAmplifier() == amplifier && current.getDuration() == -1) return;

        removeEffectInternally(entity, effect);
        entity.addEffect(new MobEffectInstance(effect, -1, amplifier, false, false, true));
    }

    /** 自身发起的移除：置位放行标志，避免被反移除保护挡住。 */
    private static void removeEffectInternally(LivingEntity entity, Holder<MobEffect> effect) {
        internalEffectRemoval = true;
        try {
            entity.removeEffect(effect);
        } finally {
            internalEffectRemoval = false;
        }
    }
}
