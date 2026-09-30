package dev.anvilcraft.gtouming.doge_plus.logic;

import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
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

    /** 单次更新允许的最大收敛轮数（拓扑 + 信号批次），剩余脏数据留到 tick 继续收敛 */
    static final int MAX_SETTLING_PASSES = 16;

    /**
     * 单次结算里「每个节点」允许的最大求值次数。
     *
     * <p>取代原先「一轮一跳、最多 16 轮」的传播预算。取 16 与原预算的最坏代价持平（16 轮 × 每个节点），
     * 但工作队列下无环网络（门链）每个节点只被求值一两次，一次调用就能传到底，链长不再撞上限。
     * 吃满这个预算说明存在真正的即时反馈环（非门环等），仍按原路径记为振荡。</p>
     */
    static final int MAX_SETTLING_EVALUATIONS_PER_NODE = 16;

    /** 单个网络最大节点数 */
    static final int MAX_NETWORK_SIZE = 32768;

    /** 维度隔离缓存 */
    private static final Map<ServerLevel, LevelNetworks> LEVELS = new IdentityHashMap<>();

    /**
     * 抑制拓扑更新的开关。
     *
     * <p>仅用于外观（powered）刷新：载体的 powered 状态随信号变化，但门拓扑并未改变，
     * 若此时仍触发 {@link #topologyChanged} 会导致信号每次翻转都重建整个网络。</p>
     */
    private static boolean suppressTopologyChange = false;

    private LogicGateNetworkManager() {}

    // ==================== 公共 API ====================

    /**
     * 标记某个位置的逻辑门发生变化（放置/破坏/配置变更）
     */
    public static void topologyChanged(Level level, BlockPos pos) {
        if (suppressTopologyChange) return;
        if (level instanceof ServerLevel serverLevel) {
            state(serverLevel).requestTopologyUpdate(pos.asLong());
        }
    }

    /** 在抑制拓扑更新的状态下执行动作（用于只改变外观的刷新）。 */
    public static void runSuppressedTopologyChange(Runnable action) {
        boolean previous = suppressTopologyChange;
        suppressTopologyChange = true;
        try {
            action.run();
        } finally {
            suppressTopologyChange = previous;
        }
    }

    /**
     * 邻居变化时触发更新
     *
     * <p>这里不能因为「正在重建拓扑」就整段跳过。初次收敛（{@code buildNetwork → recompute → notifyNeighbors}）
     * 本身就发生在重建过程中，而它点亮红石粉之后，粉会反过来通知它旁边的门——这正是信号跨过外部方块
     * 传到下一张网（输出 → 红石粉 → 输入）的唯一途径。若把它吞掉，下一张网若是先于本网重建的，那次求值
     * 读到的还是旧信号，此后又再没有更新进来，输入门就会一直没信号。</p>
     *
     * <p>本方法只排「重采样输入」，不碰拓扑，重建期间调用没有重入问题（{@link LevelNetworks#runUpdates} 自带闸门），
     * 而 {@code byGate} 里失效的旧网络会在结算时被跳过。</p>
     */
    public static void neighborChanged(Level level, BlockPos pos, BlockPos neighborPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelNetworks state = state(serverLevel);

        long packedPos = pos.asLong();
        Network network = state.byGate.get(packedPos);

        if (network == null || !network.valid) {
            // 该位置本身不在任何已建网络里（世界上的绝大多数邻居变化都落在这里）。
            // 拓扑变化（放置 / 破坏 / 编程面）都由 topologyChanged 负责，网络失效时其节点也会被
            // 重新排进重建种子，所以这里不必再排一次重建：只需让紧邻的网络重采样输入。
            // 来源网络要排除掉——它刚收敛完，再标脏等于让同一张网整张重算第二遍。
            state.signalUpdateAdjacent(pos, state.byGate.get(neighborPos.asLong()));
            return;
        }

        // 变化的邻居是逻辑门时，它只是「输出」变了，拓扑并没有变（它本来就还是门）；
        // 同网络的邻居更是本次更新已经整体重算过的对象。
        // 两者都不能触发重建：否则链内每个节点的通知（约 6L 次）都会让整网 BFS 重建一次，
        // 并在重建后从全 0 重新结算一遍，长链上这就是「每次翻转慢好几 tick」的主要来源。
        if (state.byGate.get(neighborPos.asLong()) == network) {
            return;
        }

        // 其余情况（跨网络的逻辑门、红石粉/中继器等外部方块）都只是让本网重采样输入。
        if (!network.overflow) {
            state.requestSignalUpdate(network);
            // 同信道对端不相邻，收不到这次邻居变化：显式让它们重采样远程总线。
            state.dirtyChannelPeers(network);
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
     * 读取某面逻辑门当前的输出信号（只读，不触发网络构建）。
     *
     * <p>输出直接取自持久化存储：网络重算时逐节点 {@code setOutput} 会同步写入该存储。</p>
     */
    public static int peekOutput(Level level, BlockPos pos, Direction direction) {
        LogicGateOutputData data = LogicGateOutputData.get(level);
        return data == null ? 0 : data.getSignal(pos, direction);
    }

    /**
     * 读取某远程面当前所在总线的信号（只读，用于外观 / HUD 显示）。
     *
     * <p>物流载体的远程面归物品传输网，这里返回 0。</p>
     */
    public static int peekRemoteBus(Level level, BlockPos pos, Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        return state(serverLevel).remoteBusSignal(pos, direction);
    }

    /**
     * 读取某面输入门当前收到的信号（只读，不触发网络构建）。
     *
     * <p>输入门自身输出恒为 0，因此这里按 {@code collectInputs} 同一套规则即时求值：
     * 邻居是已建网络中的门则取其朝向本面的输出，否则读世界红石弱/强信号。</p>
     */
    public static int peekInput(Level level, BlockPos pos, Direction direction) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        BlockPos neighborPos = pos.relative(direction);
        Network network = state(serverLevel).byGate.get(neighborPos.asLong());
        if (network != null && network.valid) {
            return network.getOutputSignal(neighborPos.asLong(), direction.getOpposite());
        }
        // 邻居是尚未建网的门：直接读其持久化输出，避免触发建网副作用。
        if (isLogicGate(level, neighborPos)) {
            return peekOutput(level, neighborPos, direction.getOpposite());
        }
        int weak = level.getSignal(neighborPos, direction);
        int strong = level.getDirectSignal(neighborPos, direction);
        return Math.max(weak, strong);
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
     * 清除某面逻辑门的输出与运行状态（该面换料 / 移除镶嵌时调用）。
     *
     * <p>换料可能把该面从无状态门换成有状态门（计数 / 锁存 / 延时）。有状态门的输出由自身逻辑
     * 持有、不参与组合结算清零，若不显式清除，旧门的输出会被一直保留，红石读数不会更新。</p>
     */
    public static void clearFaceSignal(Level level, BlockPos pos, Direction face) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        state(serverLevel).clearFace(pos, face);
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
                if (gate.doge_plus$getGateType(level, pos, dir) != FaceMode.NONE) return true;
            }
        }
        return false;
    }
}

