package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 电网数据管理器 - 使用 SavedData。
 * 维护一个 InlayPowerProducer 集合，提供 add/remove 方法。
 */
public class PowerGridManager extends SavedData {

    private static final String DATA_NAME = "doge_plus_power_grid";

    private final Map<BlockPos, InlayPowerProducer> producers = new HashMap<>();
    private final Level level;

    public PowerGridManager(Level level) {
        this.level = level;
    }

    @Nullable
    public static PowerGridManager get(Level level) {
        if (level instanceof ServerLevel server) {
            return server.getDataStorage().computeIfAbsent(
                    new Factory<>(
                            () -> new PowerGridManager(level),  // 创建时传入 level
                            (tag, registries) -> load(tag, registries, level)  // 加载时传入 level
                    ),
                    DATA_NAME
            );
        }
        return null;
    }

    /**
     * 添加发电节点 - 内部自动调用 PowerGrid.addComponent()。
     */
    public void add(BlockPos pos, InlayPowerProducer producer) {
        pos = pos.immutable();
        // 如果已存在，先移除旧的
        if (producers.containsKey(pos)) {
            remove(pos);
        }
        producers.put(pos, producer);
        // ⭐ 加入铁砧工艺电网
        if (!level.isClientSide()) {
            PowerGrid.addComponent(producer);
        }
        setDirty();
    }

    /**
     * 按方块当前镶嵌数据同步发电节点。
     *
     * <p>{@code BlockBehaviour.onPlace} 早于 {@code BlockItemMixin} 写入镶嵌数据，
     * 因此放置路径必须在写入后再调用一次本方法；同时它也负责清掉同一坐标上
     * 已经不携带发电属性的旧节点。</p>
     */
    public static void addIfGenerator(Level level, BlockPos pos) {
        PowerGridManager manager = get(level);
        if (manager == null) return;

        if (!BlockInlayManager.hasProperty(level, pos, InlayProperty.GENERATOR)) {
            manager.remove(pos);
            return;
        }
        // 同一坐标的镶嵌数据未变时复用；数据变了则替换，让旧的异步入队项因快照失效被跳过。
        InlayPowerProducer existing = manager.get(pos);
        if (existing != null && existing.isCurrent()) return;
        manager.add(pos, new InlayPowerProducer(level, pos));
    }

    /**
     * 移除发电节点 - 内部自动调用 PowerGrid.removeComponent()。
     */
    public void remove(BlockPos pos) {
        pos = pos.immutable();
        InlayPowerProducer producer = producers.remove(pos);
        if (producer != null) {
            // ⭐ 从铁砧工艺电网移除
            PowerGrid.removeComponent(producer);
            setDirty();
        }
    }

    /**
     * 获取发电节点。
     */
    @Nullable
    public InlayPowerProducer get(BlockPos pos) {
        return producers.get(pos);
    }

    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, InlayPowerProducer> entry : producers.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getKey().asLong());
            list.add(entryTag);
        }
        tag.put("producers", list);
        return tag;
    }

    public static PowerGridManager load(CompoundTag tag, HolderLookup.Provider ignoredRegistries, Level level) {
        PowerGridManager manager = new PowerGridManager(level);
        ListTag list = tag.getList("producers", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = BlockPos.of(entryTag.getLong("pos"));
            InlayPowerProducer producer = new InlayPowerProducer(manager.level, pos);
            manager.add(pos, producer);
        }
        return manager;
    }
}