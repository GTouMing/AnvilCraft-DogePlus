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

/**
 * 逻辑门持久化数据
 * 只存储输出信号，拓扑由世界状态重建
 */
public class LogicGateOutputData extends SavedData {

    private static final String DATA_NAME = "doge_plus_logic_gate_signals";

    /** 位置 -> 各方向信号 (压缩存储) */
    private final Long2ObjectOpenHashMap<DirectionalSignals> signals = new Long2ObjectOpenHashMap<>();

    @Nullable
    public static LogicGateOutputData get(Level level) {
        if (level instanceof ServerLevel server) {
            return server.getDataStorage().computeIfAbsent(
                    new Factory<>(LogicGateOutputData::new, LogicGateOutputData::load),
                    DATA_NAME
            );
        }
        return null;
    }

    // ==================== 数据操作 ====================

    public int getSignal(BlockPos pos, Direction direction) {
        DirectionalSignals data = signals.get(pos.asLong());
        return data == null ? 0 : data.getSignal(direction);
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
            signals.setPacked(tag.getInt("Packed"));
            data.signals.put(pos, signals);
        }
        return data;
    }

}