package dev.anvilcraft.gtouming.doge_plus.logic;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 逻辑门持久化数据
 * 只存储输出信号，拓扑由世界状态重建
 */
public class LogicGateOutputData extends SavedData {

    private static final String DATA_NAME = "doge_plus_logic_gate_signals";

    private static final Factory<LogicGateOutputData> FACTORY =
            new Factory<>(LogicGateOutputData::new, LogicGateOutputData::load);

    /** 按 ServerLevel 对象身份缓存，避免热路径每次 new Factory + computeIfAbsent。 */
    private static final Map<ServerLevel, LogicGateOutputData> CACHE = new IdentityHashMap<>();

    /** 位置 -> 各方向信号 (压缩存储) */
    private final Long2ObjectOpenHashMap<DirectionalSignals> signals = new Long2ObjectOpenHashMap<>();

    @Nullable
    public static LogicGateOutputData get(Level level) {
        if (!(level instanceof ServerLevel server)) return null;
        LogicGateOutputData cached = CACHE.get(server);
        if (cached != null) return cached;
        LogicGateOutputData data = server.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        CACHE.put(server, data);
        return data;
    }

    /** 世界卸载时释放该维度的缓存。 */
    public static void clear(ServerLevel level) {
        CACHE.remove(level);
    }

    // ==================== 数据操作 ====================

    public int getSignal(BlockPos pos, Direction direction) {
        DirectionalSignals data = signals.get(pos.asLong());
        return data == null ? 0 : data.getSignal(direction);
    }

    /** 整块 6 面信号（每面 4 bits）；无记录为 0。 */
    public int getPacked(BlockPos pos) {
        DirectionalSignals data = signals.get(pos.asLong());
        return data == null ? 0 : data.getPacked();
    }

    /** 一次性写入 6 面信号；全 0 时移除记录。 */
    public void setPacked(BlockPos pos, int packed) {
        long key = pos.asLong();
        DirectionalSignals data = signals.get(key);
        if (packed == 0) {
            if (data != null) signals.remove(key);
        } else {
            if (data == null) {
                data = new DirectionalSignals();
                signals.put(key, data);
            }
            data.setPacked(packed);
        }
        setDirty();
    }

    public void setSignal(BlockPos pos, Direction direction, int signal) {
        long packed = pos.asLong();
        DirectionalSignals data = signals.computeIfAbsent(packed, k -> new DirectionalSignals());
        data.setSignal(direction, signal);
        if (data.isEmpty()) {
            signals.remove(packed);
        }
        setDirty();
    }

    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Long2ObjectOpenHashMap.Entry<DirectionalSignals> entry : signals.long2ObjectEntrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("P", entry.getLongKey());
            CompoundTag signals = new CompoundTag();
            signals.putInt("Packed", entry.getValue().getPacked());
            entryTag.put("S", signals);
            list.add(entryTag);
        }
        tag.put("Signals", list);
        return tag;
    }

    public static LogicGateOutputData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogicGateOutputData data = new LogicGateOutputData();
        ListTag list = tag.getList("Signals", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            long pos = entry.getLong("P");
            DirectionalSignals signals = new DirectionalSignals();
            // 保存时 Packed 写在嵌套的 "S" 里（见 save）；此前误从根 tag 读取，
            // 导致重载后所有门的输出都被读成 0。
            signals.setPacked(entry.getCompound("S").getInt("Packed"));
            data.signals.put(pos, signals);
        }
        return data;
    }

}