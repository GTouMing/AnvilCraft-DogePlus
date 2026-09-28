package dev.anvilcraft.gtouming.doge_plus.transfer;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 物品传输网管理器（与 {@link dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager} 同构）。
 *
 * <p>按维度维护由「本方存入面 ↔ 正邻格的取出面」连起来的有向网络：拓扑变化时重建、重建时重算
 * 每个「存入」面的最近货源，每 server tick 按 7 游戏刻冷却把货物从货源容器搬到存入容器。</p>
 */
public final class ItemTransferNetworkManager {

    /** 维度隔离缓存（{@link ServerLevel} 没有稳定值语义，按对象身份隔离）。 */
    private static final Map<ServerLevel, LevelTransferNetworks> LEVELS = new IdentityHashMap<>();

    private ItemTransferNetworkManager() {
    }

    /**
     * 标记某个位置的传输拓扑可能变化（放置 / 移除方块、镶嵌增删）。
     *
     * <p>位置本身不是传输节点也没关系：它旁边若是网络，重建时会被一并纳入并重算记录
     * （例如端点容器的放置与破坏）。</p>
     */
    public static void topologyChanged(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            state(serverLevel).requestTopologyUpdate(pos.asLong());
        }
    }

    /**
     * 标记某个位置上的端点容器出现 / 消失（方块容器、以及矿车 / 船 / Doge 节点这类实体容器）。
     *
     * <p>与 {@link #topologyChanged} 的区别：这里拓扑没变，只重算端点记录，代价小得多，
     * 适合矿车跨格这种高频触发。</p>
     */
    public static void containerChanged(Level level, BlockPos pos) {
        if (level instanceof ServerLevel serverLevel) {
            state(serverLevel).containerChanged(pos);
        }
    }

    /** 每 server tick 推进搬运（与逻辑门网络同一个 tick 钩子并列调用）。 */
    public static void tick() {
        for (LevelTransferNetworks state : LEVELS.values()) {
            state.tick();
        }
    }

    /** 区块加载时扫描其中的传输节点并作为建网种子。 */
    public static void chunkLoaded(ServerLevel level, ChunkAccess chunk) {
        state(level).chunkLoaded(chunk);
    }

    /** 区块卸载时移除其中节点，并重建仍处于加载状态的部分。 */
    public static void chunkUnloaded(ServerLevel level, ChunkPos chunkPos) {
        LevelTransferNetworks state = LEVELS.get(level);
        if (state != null) {
            state.chunkUnloaded(chunkPos.toLong());
        }
    }

    /** 世界卸载时释放该维度的全部缓存。 */
    public static void clear(ServerLevel level) {
        LEVELS.remove(level);
    }

    private static LevelTransferNetworks state(ServerLevel level) {
        return LEVELS.computeIfAbsent(level, LevelTransferNetworks::new);
    }
}
