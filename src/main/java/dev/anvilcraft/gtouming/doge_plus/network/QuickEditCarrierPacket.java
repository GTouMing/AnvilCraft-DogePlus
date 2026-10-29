package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.util.CarrierQuickEdit;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 载体快捷编辑（客户端 → 服务端）。
 *
 * <p>主手同类型载体右键：客户端点按时记录目标与面，松手发出本包；{@code invert} 是松手时是否按住 Ctrl。
 * 服务端按 {@link CarrierQuickEdit#apply} 重新校验方块类型与主手物品后再写入，客户端侧只负责手势。</p>
 */
public record QuickEditCarrierPacket(BlockPos pos, Direction face, boolean invert) implements IInsensitiveBiPacket {

    /** 交互距离上限的平方（8 格，只用于挡住伪造包）。 */
    private static final double MAX_DISTANCE_SQR = 64.0;

    public static final Type<QuickEditCarrierPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("quick_edit_carrier"));
    public static final StreamCodec<ByteBuf, QuickEditCarrierPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, QuickEditCarrierPacket::pos,
            Direction.STREAM_CODEC, QuickEditCarrierPacket::face,
            ByteBufCodecs.BOOL, QuickEditCarrierPacket::invert,
            QuickEditCarrierPacket::new
    );

    @Override
    public Type<QuickEditCarrierPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

        CarrierQuickEdit.apply(level, pos, face, player.getMainHandItem(), invert);
    }
}
