package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 编程载体某个面的方向性属性（客户端 → 服务端）。
 *
 * <p>由铁砧锤长按右键呼出的轮盘发出：写入该面的面属性（逻辑载体写门类型、物流载体写搬运角色），
 * 并立即刷新外观、重算网络、通知邻居，使该面「外显」；{@link FaceMode#NONE} 表示清除该面的编程。</p>
 *
 * <p>「方块类型 ↔ 取值域」的校验由 {@link InlayCarrierBlock#programFaces} 负责，
 * 这里只挡住伪造包（距离与方块类型）。</p>
 */
public record SetCarrierFacePacket(BlockPos pos, Direction face, FaceMode mode) implements IInsensitiveBiPacket {

    /** 交互距离上限的平方（8 格，远大于正常触及距离，只用于挡住伪造包）。 */
    private static final double MAX_DISTANCE_SQR = 64.0;

    public static final Type<SetCarrierFacePacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("set_carrier_face"));
    public static final StreamCodec<ByteBuf, SetCarrierFacePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SetCarrierFacePacket::pos,
            Direction.STREAM_CODEC, SetCarrierFacePacket::face,
            FaceMode.STREAM_CODEC, SetCarrierFacePacket::mode,
            SetCarrierFacePacket::new
    );

    @Override
    public Type<SetCarrierFacePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock carrier) || carrier.acceptsInlays()) return;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

        InlayCarrierBlock.programFace(level, pos, face, mode);
    }
}
