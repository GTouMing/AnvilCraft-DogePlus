package dev.anvilcraft.gtouming.doge_plus.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EffectManager extends SavedData {
    private static final String DATA_NAME = "doge_plus_effect";

    private final Map<UUID, Map<ResourceLocation, Integer>> LIVING_ENTITY_EFFECTS = new HashMap<>();

    // ==================== 工厂方法 ====================
    @Nullable
    public static EffectManager get(Level level) {
        if (level instanceof ServerLevel server) {
            return server.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(EffectManager::new, EffectManager::load),
                    DATA_NAME);
        }
        return null;
    }


    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag compoundTag, HolderLookup.Provider provider) {
        ListTag entityList = new ListTag();

        for (Map.Entry<UUID, Map<ResourceLocation, Integer>> entityEntry : LIVING_ENTITY_EFFECTS.entrySet()) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.putUUID("uuid", entityEntry.getKey());

            ListTag effectList = new ListTag();
            for (Map.Entry<ResourceLocation, Integer> effectEntry : entityEntry.getValue().entrySet()) {
                CompoundTag effectTag = new CompoundTag();
                effectTag.putString("id", effectEntry.getKey().toString());
                effectTag.putInt("count", effectEntry.getValue());
                effectList.add(effectTag);
            }
            entityTag.put("effects", effectList);
            entityList.add(entityTag);
        }

        compoundTag.put("entities", entityList);
        return compoundTag;
    }

    private static EffectManager load(CompoundTag tag, HolderLookup.Provider provider) {
        EffectManager data = new EffectManager();

        ListTag entityList = tag.getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entityList.size(); i++) {
            CompoundTag entityTag = entityList.getCompound(i);
            UUID uuid = entityTag.getUUID("uuid");

            Map<ResourceLocation, Integer> effectMap = new HashMap<>();
            ListTag effectList = entityTag.getList("effects", Tag.TAG_COMPOUND);
            for (int j = 0; j < effectList.size(); j++) {
                CompoundTag effectTag = effectList.getCompound(j);
                ResourceLocation id = ResourceLocation.parse(effectTag.getString("id"));
                int count = effectTag.getInt("count");
                effectMap.put(id, count);
            }

            if (!effectMap.isEmpty()) {
                data.LIVING_ENTITY_EFFECTS.put(uuid, effectMap);
            }
        }
        return data;
    }

    // ==================== 数据操作 ====================

    /**
     * 获取实体的效果计数 Map
     */
    public Map<ResourceLocation, Integer> getEntityEffects(UUID uuid) {
        return LIVING_ENTITY_EFFECTS.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    /**
     * 获取指定效果在实体上的计数
     */
    public int getEffectCount(UUID uuid, ResourceLocation effectId) {
        Map<ResourceLocation, Integer> effects = LIVING_ENTITY_EFFECTS.get(uuid);
        if (effects == null) return 0;
        return effects.getOrDefault(effectId, 0);
    }

    /**
     * 增加效果计数
     * @return 增加后的计数
     */
    public int incrementEffect(UUID uuid, ResourceLocation effectId) {
        Map<ResourceLocation, Integer> effects = getEntityEffects(uuid);
        int newCount = effects.getOrDefault(effectId, 0) + 1;
        effects.put(effectId, newCount);
        setDirty();
        return newCount;
    }

    /**
     * 减少效果计数
     * @return 减少后的计数，如果计数归零则移除该效果条目
     */
    public int decrementEffect(UUID uuid, ResourceLocation effectId) {
        Map<ResourceLocation, Integer> effects = LIVING_ENTITY_EFFECTS.get(uuid);
        if (effects == null) return 0;

        int oldCount = effects.getOrDefault(effectId, 0);
        if (oldCount <= 0) return 0;

        int newCount = oldCount - 1;
        if (newCount == 0) {
            effects.remove(effectId);
        } else {
            effects.put(effectId, newCount);
        }

        if (effects.isEmpty()) {
            LIVING_ENTITY_EFFECTS.remove(uuid);
        }

        setDirty();
        return newCount;
    }

    /**
     * 移除实体的所有效果计数
     */
    public void clearEntityEffects(UUID uuid) {
        LIVING_ENTITY_EFFECTS.remove(uuid);
        setDirty();
    }

    /**
     * 获取实体当前拥有计数 > 0 的效果列表
     */
    public Map<ResourceLocation, Integer> getActiveEffects(UUID uuid) {
        Map<ResourceLocation, Integer> effects = LIVING_ENTITY_EFFECTS.get(uuid);
        if (effects == null) return new HashMap<>();
        return new HashMap<>(effects);
    }

    /**
     * 检查实体是否拥有某个效果（计数 > 0）
     */
    public boolean hasEffect(UUID uuid, ResourceLocation effectId) {
        return getEffectCount(uuid, effectId) > 0;
    }
}