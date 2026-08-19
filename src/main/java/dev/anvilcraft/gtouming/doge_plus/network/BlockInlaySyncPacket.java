package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.ClientBlockInlayData;
import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
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
    public static final StreamCodec<ByteBuf, BlockInlaySyncPacket> STREAM_CODEC = StreamCodec.composite(
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
        if (inlays.inlays().isEmpty()) {
            ClientBlockInlayData.remove(pos);
        } else {
            ClientBlockInlayData.put(pos, inlays);
        }
    }
}
