package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class LevelEventListener {

    /**
     * 每 tick 处理积压的拓扑/信号更新。
     * 放置、拆除、邻居变化可能在更新进行中（processingUpdates）排入种子，
     * 由这里兜底收敛，否则这些变更永远不会生效。
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        LogicGateNetworkManager.tick();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // 网络缓存不持久化，区块进入服务端内存时必须用其中的导线作为重建种子。
            LogicGateNetworkManager.chunkLoaded(serverLevel, event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // 主动拆除跨区块缓存，避免网络继续引用已卸载节点或从其读取幽灵信号。
            LogicGateNetworkManager.chunkUnloaded(serverLevel, event.getChunk().getPos());
        } else if (event.getLevel() instanceof Level level) {
            //RedstoneWireClientPowerCache.clearChunk(level, event.getChunk().getPos());
        }
    }

    /**
     * 世界加载事件
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
    }

    /**
     * 世界卸载事件
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            //RedstoneWireClientPowerCache.clear(level);
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // LEVELS 按 ServerLevel 对象持有强引用，世界卸载时清理才能释放整张拓扑缓存。
            LogicGateNetworkManager.clear(serverLevel);
        }
    }
}
