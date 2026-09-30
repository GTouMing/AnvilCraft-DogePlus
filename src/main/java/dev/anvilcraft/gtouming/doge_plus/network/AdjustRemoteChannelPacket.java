package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.RemoteChannel;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 调整远程门面的「信道数字」（客户端 → 服务端）：铁砧锤指向远程面时 Ctrl + 滚轮增减。
 *
 * <p>与 {@link AdjustGateValuePacket} / {@link AdjustLogisticsThroughputPacket} 平行；数字范围
 * 0-{@code CONFIG.remoteChannelMax}。</p>
 */
public record AdjustRemoteChannelPacket(BlockPos pos, Direction face, int delta) implements IInsensitiveBiPacket {

    public static final Type<AdjustRemoteChannelPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("adjust_remote_channel"));
    public static final StreamCodec<ByteBuf, AdjustRemoteChannelPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AdjustRemoteChannelPacket::pos,
            Direction.STREAM_CODEC, AdjustRemoteChannelPacket::face,
            ByteBufCodecs.VAR_INT, AdjustRemoteChannelPacket::delta,
            AdjustRemoteChannelPacket::new
    );

    @Override
    public Type<AdjustRemoteChannelPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock)) return;

        BlockInlays inlays = BlockInlayManager.get(level, pos);
        if (inlays.getFace(face) != FaceMode.REMOTE) return;

        RemoteChannel current = inlays.getChannel(face);
        int number = Math.clamp(current.number() + delta, 0, RemoteChannel.maxNumber());
        BlockInlayManager.put(level, pos, inlays.withChannel(face, new RemoteChannel(current.item(), number)));
        InlayCarrierBlock.channelChanged(level, pos);
    }
}
