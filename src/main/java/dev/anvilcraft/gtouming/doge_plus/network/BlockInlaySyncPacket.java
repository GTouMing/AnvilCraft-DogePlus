package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.ClientBlockInlayData;
import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块级镶嵌数据同步包（服务端 → 客户端）：把某个坐标的镶嵌材料列表同步到客户端，
 * 空列表表示该坐标已无镶嵌。客户端据此执行与服务端一致的吸附等行为。
 */
public record BlockInlaySyncPacket(BlockPos pos, BlockInlays inlays) implements IClientboundPacket {

    public static final Type<BlockInlaySyncPacket> TYPE = IPacket.type(AnvilCraftDogePlus.of("block_inlay_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockInlaySyncPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BlockInlaySyncPacket::pos,
                    BlockInlays.STREAM_CODEC, BlockInlaySyncPacket::inlays,
                    BlockInlaySyncPacket::new
            );

    @Override
    public Type<BlockInlaySyncPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        // 用 BlockInlays.isEmpty() 而非镶嵌材料列表判空：逻辑载体没有镶孔，它的门种只在
        // directions 里，只看材料会把已编程的载体当成没有数据丢掉（客户端从此读不到门种，
        // 轮盘会默认为「未编程」，清除该面也会被判成未改动而不发包）。
        if (inlays.isEmpty()) {
            ClientBlockInlayData.remove(pos);
        } else {
            ClientBlockInlayData.put(pos, inlays);
        }
    }
}
