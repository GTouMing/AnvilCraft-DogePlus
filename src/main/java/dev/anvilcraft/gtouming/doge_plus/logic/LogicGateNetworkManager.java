package dev.anvilcraft.gtouming.doge_plus.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 逻辑门网络管理器
 * 按维度维护逻辑门的连通网络和信号传播
 */
public final class LogicGateNetworkManager {

    /** 单次更新允许的最大收敛轮数 */
    static final int MAX_SETTLING_PASSES = 16;

    /** 单个网络最大节点数 */
    static final int MAX_NETWORK_SIZE = 32768;

    /** 维度隔离缓存 */
    private static final Map<ServerLevel, LevelNetworks> LEVELS = new IdentityHashMap<>();

    private LogicGateNetworkManager() {}

    // ==================== 公共 API ====================

    /**
     * 标记某个位置的逻辑门发生变化（放置/破坏/配置变更）
     */
    public static void topologyChanged(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            state(serverLevel).requestTopologyUpdate(pos.asLong());
        }
    }

    /**
     * 邻居变化时触发更新
     */
    public static void neighborChanged(Level level, BlockPos pos, BlockPos neighborPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelNetworks state = state(serverLevel);

        // 防止重入
        if (state.applyingTopology) {
            return;
        }

        long packedPos = pos.asLong();
        Network network = state.byGate.get(packedPos);

        if (network == null || !network.valid) {
            state.requestTopologyUpdate(packedPos);
            return;
        }

        // 如果变化的邻居是逻辑门，可能需要重建拓扑
        if (isLogicGate(level, neighborPos)) {
            state.requestTopologyUpdate(packedPos);
            return;
        }

        // 非逻辑门邻居变化，只需重算信号
        if (!network.overflow) {
            state.requestSignalUpdate(network);
        }
    }

    /**
     * Tick 处理积压更新
     */
    public static void tick() {
        for (LevelNetworks state : LEVELS.values()) {
            state.tick();
        }
    }

    /**
     * 清空维度缓存
     */
    public static void clear(ServerLevel level) {
        LEVELS.remove(level);
    }

    /**
     * 获取指定位置的逻辑门输出信号
     */
    public static int getSignal(Level level, BlockPos pos, Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        Network network = state(serverLevel).getOrBuildNetwork(pos.asLong());
        if (network == null || !network.valid || network.overflow) {
            return 0;
        }
        return network.getOutputSignal(pos.asLong(), direction);
    }

    /**
     * 设置逻辑门的输出信号（由逻辑门自身调用）
     */
    public static void setOutputSignal(Level level, BlockPos pos, Direction direction, int signal) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelNetworks state = state(serverLevel);
        Network network = state.byGate.get(pos.asLong());
        if (network != null && network.valid) {
            network.setOutputSignal(pos.asLong(), direction, Math.clamp(signal, 0, 15));
            state.requestSignalUpdate(network);
        }
    }

    /**
     * 区块加载时扫描逻辑门
     */
    public static void chunkLoaded(ServerLevel level, ChunkAccess chunk) {
        LevelNetworks state = state(level);
        chunk.findBlocks(
                (blockState) -> true,
                (pos, blockState) -> {
                    if (isLogicGate(level, pos))
                        state.topologySeeds.add(pos.asLong());
                }
        );
    }

    /**
     * 区块卸载时清理
     */
    public static void chunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        LevelNetworks state = LEVELS.get(level);
        if (state != null) {
            state.chunkUnloaded(chunkPos.toLong());
        }
    }

    // ==================== 内部方法 ====================

    private static LevelNetworks state(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, LevelNetworks::new);
    }

    public static boolean isLogicGate(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof ILogicGate gate) {
            for (Direction dir : Direction.values()) {
                if (gate.doge_plus$getGateType(level, pos, dir) != LogicGateType.NONE) return true;
            }
        }
        return false;
    }
}

