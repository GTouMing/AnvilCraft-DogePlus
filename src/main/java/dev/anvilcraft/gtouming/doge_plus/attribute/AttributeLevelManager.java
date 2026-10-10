package dev.anvilcraft.gtouming.doge_plus.attribute;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 属性等级存档：记录每个实体当前由装备镶嵌产生的 {@code (属性键, 数量, 来源装备槽)}。
 *
 * <p>键为属性 id（defense / life / attack）或具体 mob effect id（effect）。穿脱装备时由
 * {@link InlayLevelService} 全量重算并与本记录比对，只对数量变化者重新施加。</p>
 */
public class AttributeLevelManager extends SavedData {

    private static final String DATA_NAME = "doge_plus_attribute_levels";

    /** 单个属性键的计数与来源装备槽。 */
    public record Entry(int count, Set<EquipmentSlot> sources) {
        public Entry {
            sources = Set.copyOf(sources);
        }
    }

    /** 实体 UUID → (属性键 → 计数)。 */
    private final Map<UUID, Map<ResourceLocation, Entry>> LEVELS = new HashMap<>();

    @Nullable
    public static AttributeLevelManager get(Level level) {
        if (level instanceof ServerLevel server) {
            return server.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(AttributeLevelManager::new, AttributeLevelManager::load),
                    DATA_NAME);
        }
        return null;
    }

    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag entityList = new ListTag();
        for (Map.Entry<UUID, Map<ResourceLocation, Entry>> entityEntry : LEVELS.entrySet()) {
            CompoundTag entityTag = new CompoundTag();
            entityTag.putUUID("uuid", entityEntry.getKey());

            ListTag entryList = new ListTag();
            for (Map.Entry<ResourceLocation, Entry> e : entityEntry.getValue().entrySet()) {
                CompoundTag entryTag = new CompoundTag();
                entryTag.putString("key", e.getKey().toString());
                entryTag.putInt("count", e.getValue().count());

                ListTag sourceList = new ListTag();
                for (EquipmentSlot slot : e.getValue().sources()) {
                    CompoundTag sourceTag = new CompoundTag();
                    sourceTag.putString("slot", slot.getName());
                    sourceList.add(sourceTag);
                }
                entryTag.put("sources", sourceList);
                entryList.add(entryTag);
            }
            entityTag.put("entries", entryList);
            entityList.add(entityTag);
        }
        tag.put("entities", entityList);
        return tag;
    }

    private static AttributeLevelManager load(CompoundTag tag, HolderLookup.Provider provider) {
        AttributeLevelManager data = new AttributeLevelManager();
        ListTag entityList = tag.getList("entities", Tag.TAG_COMPOUND);
        for (int i = 0; i < entityList.size(); i++) {
            CompoundTag entityTag = entityList.getCompound(i);
            UUID uuid = entityTag.getUUID("uuid");

            Map<ResourceLocation, Entry> entries = new HashMap<>();
            ListTag entryList = entityTag.getList("entries", Tag.TAG_COMPOUND);
            for (int j = 0; j < entryList.size(); j++) {
                CompoundTag entryTag = entryList.getCompound(j);
                ResourceLocation key = ResourceLocation.parse(entryTag.getString("key"));
                int count = entryTag.getInt("count");

                Set<EquipmentSlot> sources = EnumSet.noneOf(EquipmentSlot.class);
                ListTag sourceList = entryTag.getList("sources", Tag.TAG_COMPOUND);
                for (int k = 0; k < sourceList.size(); k++) {
                    sources.add(slotByName(sourceList.getCompound(k).getString("slot")));
                }
                entries.put(key, new Entry(count, sources));
            }
            if (!entries.isEmpty()) {
                data.LEVELS.put(uuid, entries);
            }
        }
        return data;
    }

    private static EquipmentSlot slotByName(String name) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getName().equals(name)) return slot;
        }
        return EquipmentSlot.MAINHAND;
    }

    // ==================== 数据操作 ====================

    /** 某实体当前记录；无记录返回空表。 */
    public Map<ResourceLocation, Entry> get(UUID uuid) {
        return LEVELS.getOrDefault(uuid, Map.of());
    }

    /** 覆盖某实体的记录；空记录则移除，避免存档膨胀。 */
    public void set(UUID uuid, Map<ResourceLocation, Entry> entries) {
        if (entries.isEmpty()) {
            if (LEVELS.remove(uuid) != null) setDirty();
        } else {
            LEVELS.put(uuid, Map.copyOf(entries));
            setDirty();
        }
    }

    public void clear(UUID uuid) {
        if (LEVELS.remove(uuid) != null) setDirty();
    }
}
