package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.EffectManager;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.*;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class InlayEffectEvent {

    private static EffectManager effectManager;

    // ==================== 世界加载 ====================

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof Level level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        effectManager = EffectManager.get(level);
    }

    // ==================== 装备变化 ====================

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (effectManager == null) return;

        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        removeInlayEffects(entity, from);
        applyInlayEffects(entity, to);
    }

    // ================== 玩家死亡清理 ==================

    @SubscribeEvent
    public static void onPlayerDeath(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (effectManager == null) return;
        // 死亡时清理（玩家重生后会重新应用装备效果）
        effectManager.clearEntityEffects(event.getOriginal().getUUID());
    }

    // ============== 玩家清理效果 ==============

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();
        if(effect == null) return;
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (effectId == null) return;

        if (effectManager == null) return;
        UUID uuid = entity.getUUID();
        int count = effectManager.getEffectCount(uuid, effectId);
        if (count <= 0) return;
        event.setCanceled(true);
    }

    // ==================== 核心逻辑 ====================

    private static void applyInlayEffects(LivingEntity entity, ItemStack stack) {
        List<ResourceLocation> effectIds = extractEffectIds(stack);
        if (effectIds.isEmpty()) return;

        UUID uuid = entity.getUUID();

        for (ResourceLocation effectId : effectIds) {
            int newCount = effectManager.incrementEffect(uuid, subId(effectId));
            // 只有从 0→1 时才真正应用效果
            if (newCount == 1) {
                applyEffect(entity, effectId);
            }
        }
    }

    private static void removeInlayEffects(LivingEntity entity, ItemStack stack) {
        List<ResourceLocation> effectIds = extractEffectIds(stack);
        if (effectIds.isEmpty()) return;

        UUID uuid = entity.getUUID();

        for (ResourceLocation effectId : effectIds) {
            int newCount = effectManager.decrementEffect(uuid, subId(effectId));
            // 只有归零时才真正移除效果
            if (newCount == 0) {
                removeEffect(entity, effectId);
            }
        }
    }

    private static List<ResourceLocation> extractEffectIds(ItemStack stack) {
        List<ResourceLocation> result = new ArrayList<>();
        if (stack.isEmpty()) return result;

        List<InlayEntry> inlays = InlayUtil.getInlays(stack);
        for (InlayEntry entry : inlays) {
            if (!entry.containsAttributes(InlayProperty.EFFECT)) {
                continue;
            }
            for (ResourceLocation extraId : entry.extra()) {
                if (BuiltInRegistries.POTION.containsKey(extraId)) {
                    result.add(extraId);
                }
            }
        }
        return result;
    }

    private static void applyEffect(LivingEntity entity, ResourceLocation effectId) {
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(subId(effectId)).orElse(null);
        if (effect == null) return;
        boolean isStrong = effectId.getPath().contains("strong_");
        //boolean isLong = effectId.getPath().contains("long_");

        entity.addEffect(new MobEffectInstance(
                effect,
                -1,
                isStrong ? 1 : 0,
                false,
                false,
                true
        ));
    }

    private static void removeEffect(LivingEntity entity, ResourceLocation effectId) {
        Holder<MobEffect> effect = BuiltInRegistries.MOB_EFFECT.getHolder(subId(effectId)).orElse(null);
        if (effect == null) return;
        entity.removeEffect(effect);
    }

    private static ResourceLocation subId(ResourceLocation holdId) {
        ResourceLocation subId = holdId;
        String holdPath = holdId.getPath();
        if (holdPath.contains("long_")) {
            subId = subId.withPath(holdPath.substring("long_".length()));
        }
        if (holdPath.contains("strong_")) {
            subId = subId.withPath(holdPath.substring("strong_".length()));
        }
        return subId;
    }
}