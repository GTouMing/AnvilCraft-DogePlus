package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkWatchEvent;

/**
 * 玩家开始跟踪区块时，把该区块内已有的方块级镶嵌数据同步给玩家。
 * 增量变更（放置/移除）由 {@link BlockInlayManager} 即时广播。
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class BlockInlaySyncEvent {

    @SubscribeEvent
    public static void onChunkWatch(ChunkWatchEvent.Watch event) {
        BlockInlayManager.syncChunkToPlayer(event.getLevel(), event.getPos(), event.getPlayer());
    }
}
