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
 * 逻辑门运行时状态（仅计数门 / 锁存门 / 延时门需要）。
 *
 * <p>门的输出信号仍存于 {@link LogicGateOutputData}；这里只存跨 tick 的状态：
 * 计数门的累计次数、锁存门是否处于「已记录」、延时门剩余 tick。每方向压缩进一个 int
 * （bits0-7 计数、bit8 已锁存、bits9-16 剩余 tick）。</p>
 */
public class LogicGateStateData extends SavedData {

    private static final String DATA_NAME = "doge_plus_logic_gate_state";
    private static final int DIRECTIONS = Direction.values().length;

    private static final int COUNT_MASK = 0xFF;
    private static final int LATCHED_BIT = 1 << 8;
    private static final int REMAINING_SHIFT = 9;
    private static final int REMAINING_MASK = 0xFF << REMAINING_SHIFT;

    /** 位置 -> 各方向压缩状态。 */
    private final Long2ObjectOpenHashMap<int[]> states = new Long2ObjectOpenHashMap<>();

    @Nullable
    public static LogicGateStateData get(Level level) {
        if (level instanceof ServerLevel server) {
            return server.getDataStorage().computeIfAbsent(
                    new Factory<>(LogicGateStateData::new, LogicGateStateData::load),
                    DATA_NAME
            );
        }
        return null;
    }

    // ==================== 计数门 ====================

    public int getCount(BlockPos pos, Direction direction) {
        return state(pos, direction) & COUNT_MASK;
    }

    public void setCount(BlockPos pos, Direction direction, int count) {
        setState(pos, direction, (state(pos, direction) & ~COUNT_MASK) | (count & COUNT_MASK));
    }

    // ==================== 锁存门 ====================

    public boolean isLatched(BlockPos pos, Direction direction) {
        return (state(pos, direction) & LATCHED_BIT) != 0;
    }

    public void setLatched(BlockPos pos, Direction direction, boolean latched) {
        int value = state(pos, direction);
        setState(pos, direction, latched ? value | LATCHED_BIT : value & ~LATCHED_BIT);
    }

    // ==================== 延时门 ====================

    public int getRemaining(BlockPos pos, Direction direction) {
        return (state(pos, direction) & REMAINING_MASK) >>> REMAINING_SHIFT;
    }

    public void setRemaining(BlockPos pos, Direction direction, int remaining) {
        int value = state(pos, direction);
        setState(pos, direction, (value & ~REMAINING_MASK) | ((remaining << REMAINING_SHIFT) & REMAINING_MASK));
    }

    /** 丢弃某位置的全部状态（方块被移除时使用）。 */
    public void clear(BlockPos pos) {
        if (states.remove(pos.asLong()) != null) {
            setDirty();
        }
    }

    // ==================== 内部 ====================

    private int state(BlockPos pos, Direction direction) {
        int[] values = states.get(pos.asLong());
        return values == null ? 0 : values[direction.ordinal()];
    }

    private void setState(BlockPos pos, Direction direction, int value) {
        long packed = pos.asLong();
        int[] values = states.get(packed);
        if (values == null) {
            if (value == 0) return;
            values = new int[DIRECTIONS];
            states.put(packed, values);
        }
        values[direction.ordinal()] = value;
        for (int current : values) {
            if (current != 0) {
                setDirty();
                return;
            }
        }
        states.remove(packed);
        setDirty();
    }

    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Long2ObjectOpenHashMap.Entry<int[]> entry : states.long2ObjectEntrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("P", entry.getLongKey());
            entryTag.putIntArray("S", entry.getValue());
            list.add(entryTag);
        }
        tag.put("States", list);
        return tag;
    }

    public static LogicGateStateData load(CompoundTag tag, HolderLookup.Provider registries) {
        LogicGateStateData data = new LogicGateStateData();
        ListTag list = tag.getList("States", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int[] values = entry.getIntArray("S");
            if (values.length == DIRECTIONS) {
                data.states.put(entry.getLong("P"), values.clone());
            }
        }
        return data;
    }
}
