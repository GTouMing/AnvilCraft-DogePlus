package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.logic.GateChainBuilder;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * 拆除一条门链并返还逻辑载体（客户端 → 服务端）。
 *
 * <p>由「铁砧锤 + 按住 Ctrl」的移除手势发出：客户端只做预览与沿链行走，服务端是权威方，
 * 会重新校验链的相邻性与每个位置是否确为逻辑载体后才拆除（见 {@link GateChainBuilder}）。</p>
 *
 * @param chain 客户端推导出的待拆方块序列（相邻、从点击的载体开始）
 */
public record RemoveGateChainPacket(List<BlockPos> chain) implements IInsensitiveBiPacket {

    public static final Type<RemoveGateChainPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("remove_gate_chain"));
    public static final StreamCodec<ByteBuf, RemoveGateChainPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC), RemoveGateChainPacket::chain,
            RemoveGateChainPacket::new
    );

    @Override
    public Type<RemoveGateChainPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        GateChainBuilder.remove(player, chain);
    }
}
