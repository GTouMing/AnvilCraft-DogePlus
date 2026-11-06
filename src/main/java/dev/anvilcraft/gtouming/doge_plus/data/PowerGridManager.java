package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 电网数据管理器 - 使用 SavedData。
 * 维护一个 InlayPowerProducer 集合，提供 add/remove 方法。
 */
public class PowerGridManager extends SavedData {

    private static final String DATA_NAME = "doge_plus_power_grid";

    /** 按 ServerLevel 对象身份缓存，避免热路径每次 new Factory + computeIfAbsent。 */
    private static final Map<ServerLevel, PowerGridManager> CACHE = new IdentityHashMap<>();

    /** 坐标打包值 -> 发电节点；与镶嵌表一样避开 BlockPos 哈希。 */
    private final Long2ObjectOpenHashMap<InlayPowerProducer> producers = new Long2ObjectOpenHashMap<>();
    private final Level level;

    public PowerGridManager(Level level) {
        this.level = level;
    }

    @Nullable
    public static PowerGridManager get(Level level) {
        if (!(level instanceof ServerLevel server)) return null;
        PowerGridManager cached = CACHE.get(server);
        if (cached != null) return cached;
        PowerGridManager manager = server.getDataStorage().computeIfAbsent(
                new Factory<>(
                        () -> new PowerGridManager(level),  // 创建时传入 level
                        (tag, registries) -> load(tag, registries, level)  // 加载时传入 level
                ),
                DATA_NAME
        );
        CACHE.put(server, manager);
        return manager;
    }

    /** 世界卸载时释放该维度的缓存。 */
    public static void clear(ServerLevel level) {
        CACHE.remove(level);
    }

    /**
     * 添加发电节点 - 内部自动调用 PowerGrid.addComponent()。
     */
    public void add(BlockPos pos, InlayPowerProducer producer) {
        InlayPowerProducer previous = producers.put(pos.asLong(), producer);
        // 同一坐标换了节点：旧节点先从铁砧工艺电网移除，避免残留引用被继续调用。
        if (previous != null) {
            PowerGrid.removeComponent(previous);
        }
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
        InlayPowerProducer producer = producers.remove(pos.asLong());
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
        return producers.get(pos.asLong());
    }

    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Long2ObjectMap.Entry<InlayPowerProducer> entry : producers.long2ObjectEntrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("pos", entry.getLongKey());
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
