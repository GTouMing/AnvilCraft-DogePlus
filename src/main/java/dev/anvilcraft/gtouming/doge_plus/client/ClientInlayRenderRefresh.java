package dev.anvilcraft.gtouming.doge_plus.client;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.ClientBlockInlayData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 镶嵌数据同步到客户端后，主动把对应区块标脏。
 *
 * <p>载体 wire 的逻辑 / 物流样式由 {@code FaceWireModel} 在区块模型重建时读
 * {@link ClientBlockInlayData} 决定；同步包只更新数据，若不标脏，模型会一直停在
 * 重建前的样式，直到邻居更新等外部事件碰巧触发重建。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class ClientInlayRenderRefresh {

    private ClientInlayRenderRefresh() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (long packed : ClientBlockInlayData.drainDirtyPositions()) {
            BlockPos pos = BlockPos.of(packed);
            // 数据变动只影响该方块；带上邻居区块，避免跨区块边界的模型缓存仍用旧数据。
            level.setSectionDirtyWithNeighbors(
                    pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
        }
    }
}
