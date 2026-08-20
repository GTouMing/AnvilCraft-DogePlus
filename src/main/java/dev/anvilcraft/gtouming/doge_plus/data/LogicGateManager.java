package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.api.behavior.ILogicGate;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.util.DirectionsOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 逻辑门数据管理器：持久化存储每个位置的逻辑门输出信号。
 */
public class LogicGateManager extends SavedData {

    private static final String DATA_NAME = "doge_plus_logic_gates";

    private Level level;
    private final Map<BlockPos, SignalOutput> signals = new HashMap<>();

    // ==================== 获取实例 ====================

    @Nullable
    public static LogicGateManager get(Level level) {
        if (level instanceof ServerLevel server) {
            LogicGateManager manager = server.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(LogicGateManager::new, LogicGateManager::load),
                    DATA_NAME
            );
            manager.level = level;
            return manager;
        }
        return null;
    }

    // ==================== 数据操作 ====================

    public int getSignal(BlockPos pos, Direction direction) {
        SignalOutput data = signals.get(pos);
        if (data == null) return 0;
        return data.getSignal(direction);
    }

    public void setSignal(BlockPos pos, Direction direction, int signal) {
        SignalOutput existing = signals.getOrDefault(pos, SignalOutput.defaults());
        signals.put(pos.immutable(), existing.withSignal(direction, Math.clamp(signal, 0, 15)));
        setDirty();
    }

    // ==================== 通知邻居 ====================

    public void notifyNeighbors(BlockPos pos) {
        if (level.isClientSide()) return;
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        level.updateNeighborsAt(pos, block);
    }

    // ==================== 核心逻辑 ====================

    public boolean hasLogicGate(BlockPos pos) {
        return BlockInlayManager.hasProperty(level, pos, InlayProperty.DIRECTION)
                && (BlockInlayManager.hasProperty(level, pos, InlayProperty.NOT_GATE)
                || BlockInlayManager.hasProperty(level, pos, InlayProperty.AND_GATE)
                || BlockInlayManager.hasProperty(level, pos, InlayProperty.OR_GATE)
                || BlockInlayManager.hasProperty(level, pos, InlayProperty.REDSTONE));
    }

    public void updateOutputSignals(BlockPos pos) {
        if (level.isClientSide()) return;

        BlockInlays inlays = BlockInlayManager.get(level, pos);

        for (Direction outputDir : Direction.values()) {
            ILogicGate.GateType gateType = inlays.getGateType(outputDir);
            int oldSignal = getSignal(pos, outputDir);
            int newSignal = 0;

            if (gateType != ILogicGate.GateType.NONE) {
                newSignal = calculateOutputSignal(pos, outputDir, gateType);
            }

            if (oldSignal != newSignal) {
                setSignal(pos, outputDir, newSignal);
            }
        }
        notifyNeighbors(pos);
    }

    private int calculateOutputSignal(BlockPos pos, Direction outputDir, ILogicGate.GateType gateType) {
        return switch (gateType) {
            case NOT_GATE -> {
                int input = getNeighborSignal(pos, outputDir);
                yield input > 0 ? 0 : 15;
            }
            case AND_GATE -> {
                Direction inputDir1 = DirectionsOrder.getNextDirection(outputDir);
                Direction inputDir2 = inputDir1.getOpposite();
                int input1 = getNeighborSignal(pos, inputDir1);
                int input2 = getNeighborSignal(pos, inputDir2);
                yield Math.min(input1, input2);
            }
            case OR_GATE -> {
                Direction inputDir1 = DirectionsOrder.getNextDirection(outputDir);
                Direction inputDir2 = inputDir1.getOpposite();
                int input1 = getNeighborSignal(pos, inputDir1);
                int input2 = getNeighborSignal(pos, inputDir2);
                yield Math.max(input1, input2);
            }
            case RED_STONE -> {
                int maxSignal = 0;
                for (Direction dir : Direction.values()) {
                    int signal = getNeighborSignal(pos, dir);
                    maxSignal = Math.max(maxSignal, signal);
                }
                yield maxSignal;
            }
            default -> 0;
        };
    }

    /**
     * 获取邻居方向的信号（优先读取 ILogicGate 的输出信号）。
     */
    private int getNeighborSignal(BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);

        if (hasLogicGate(neighborPos)) {
            return getSignal(neighborPos, direction.getOpposite());
        }

        return level.getSignal(neighborPos, direction.getOpposite());
    }

    // ==================== 邻居变化处理 ====================

    private static final ThreadLocal<Set<BlockPos>> UPDATING = ThreadLocal.withInitial(HashSet::new);

    public void onNeighborChanged(BlockPos pos) {
        if (level.isClientSide()) return;
        if (!hasLogicGate(pos)) return;

        // 防止死循环：使用线程本地标记
        Set<BlockPos> updating = UPDATING.get();
        if (updating.contains(pos)) return;
        updating.add(pos);

        try {
            updateOutputSignals(pos);
        } finally {
            updating.remove(pos);
        }
    }

    // ==================== 持久化 ====================

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, SignalOutput> entry : signals.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("P", entry.getKey().asLong());

            SignalOutput data = entry.getValue();
            CompoundTag signalTag = new CompoundTag();
            for (Direction direction : Direction.values()) {
                signalTag.putInt(direction.getName(), data.getSignal(direction));
            }
            entryTag.put("S", signalTag);
            list.add(entryTag);
        }
        tag.put("Signals", list);
        return tag;
    }

    public static LogicGateManager load(CompoundTag tag, HolderLookup.Provider registries) {
        LogicGateManager manager = new LogicGateManager();
        ListTag list = tag.getList("Signals", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = BlockPos.of(entryTag.getLong("P"));

            CompoundTag signalTag = entryTag.getCompound("S");
            Map<Direction, Integer> signalMap = new HashMap<>();
            for (Direction direction : Direction.values()) {
                if (signalTag.contains(direction.getName())) {
                    signalMap.put(direction, signalTag.getInt(direction.getName()));
                }
            }
            manager.signals.put(pos, SignalOutput.of(signalMap));
        }
        return manager;
    }
}