package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.logic.GateChainBuilder;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 生成一条门链（客户端 → 服务端）。
 *
 * <p>由「逻辑载体 + 按住 Ctrl」的放置手势发出：客户端只做预览与折线推导，服务端是权威方，
 * 会重新校验起点、链长、相邻性与可替换性后才落地（见 {@link GateChainBuilder}）。</p>
 *
 * <p>包编解码器必须只留<b>一个</b>静态 final {@code StreamCodec} 字段：库的注册器是反射遍历本类的
 * 静态 final {@code StreamCodec} 字段来挑包编解码器的（取最后一个匹配项），多一个字段就可能被挑走，
 * 整条包会被解析错。同理内部用的链路列表编解码器写成私有方法而不是字段。</p>
 *
 * @param originPos  手势起始的方块位置（第一个点击面所在方块）
 * @param originFace 起始面（在该方块上）
 * @param chain      客户端推导出的方块序列（相邻、从起始面外侧第一格开始）
 * @param gateType   副手材料决定的门种；{@link FaceMode#NONE} 表示默认（输出门）
 * @param finalFace  松开 Ctrl 时命中的最终点击面；未命中方块（看向天空等）为 {@code null}。
 *                   链尾的门会写进该面的<b>反向</b>（即链尾朝向被点击方块的那一面）
 */
public record BuildGateChainPacket(
        BlockPos originPos,
        Direction originFace,
        List<BlockPos> chain,
        FaceMode gateType,
        @Nullable Direction finalFace) implements IInsensitiveBiPacket {

    public static final Type<BuildGateChainPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("build_gate_chain"));
    public static final StreamCodec<ByteBuf, BuildGateChainPacket> STREAM_CODEC =
            StreamCodec.of(BuildGateChainPacket::write, BuildGateChainPacket::read);

    /** 链路列表编解码器；写成方法而不是静态字段，避免干扰上面的反射注册。 */
    private static StreamCodec<ByteBuf, List<BlockPos>> chainCodec() {
        return ByteBufCodecs.collection(ArrayList::new, BlockPos.STREAM_CODEC);
    }

    private static void write(ByteBuf buffer, BuildGateChainPacket packet) {
        BlockPos.STREAM_CODEC.encode(buffer, packet.originPos());
        Direction.STREAM_CODEC.encode(buffer, packet.originFace());
        chainCodec().encode(buffer, packet.chain());
        FaceMode.STREAM_CODEC.encode(buffer, packet.gateType());
        Direction finalFace = packet.finalFace();
        buffer.writeBoolean(finalFace != null);
        if (finalFace != null) {
            Direction.STREAM_CODEC.encode(buffer, finalFace);
        }
    }

    private static BuildGateChainPacket read(ByteBuf buffer) {
        BlockPos originPos = BlockPos.STREAM_CODEC.decode(buffer);
        Direction originFace = Direction.STREAM_CODEC.decode(buffer);
        List<BlockPos> chain = chainCodec().decode(buffer);
        FaceMode gateType = FaceMode.STREAM_CODEC.decode(buffer);
        Direction finalFace = buffer.readBoolean() ? Direction.STREAM_CODEC.decode(buffer) : null;
        return new BuildGateChainPacket(originPos, originFace, chain, gateType, finalFace);
    }

    @Override
    public Type<BuildGateChainPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        GateChainBuilder.build(player, originPos, originFace, chain, gateType, finalFace);
    }
}
