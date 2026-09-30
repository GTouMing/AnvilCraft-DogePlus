package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.RemoteChannel;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 设置远程门面的「信道标识物品」（客户端 → 服务端）。
 *
 * <p>与 {@link SetLogisticsFilterPacket} 平行：主手铁砧锤<b>点按</b>右键远程面时发出，物品取自副手
 * （空栈表示不指定物品）。信道 = 物品类型 + 数字，数字由 {@link AdjustRemoteChannelPacket} 调整。</p>
 */
public record SetRemoteChannelItemPacket(BlockPos pos, Direction face, ItemStack item) implements IInsensitiveBiPacket {

    /** 交互距离上限的平方（8 格，远大于正常触及距离，只用于挡住伪造包）。 */
    private static final double MAX_DISTANCE_SQR = 64.0;

    public static final Type<SetRemoteChannelItemPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("set_remote_channel_item"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetRemoteChannelItemPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetRemoteChannelItemPacket::pos,
                    Direction.STREAM_CODEC, SetRemoteChannelItemPacket::face,
                    ItemStack.OPTIONAL_STREAM_CODEC, SetRemoteChannelItemPacket::item,
                    SetRemoteChannelItemPacket::new
            );

    @Override
    public Type<SetRemoteChannelItemPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock)) return;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

        BlockInlays inlays = BlockInlayManager.get(level, pos);
        // 只有远程面有信道语义。
        if (inlays.getFace(face) != FaceMode.REMOTE) return;

        RemoteChannel current = inlays.getChannel(face);
        RemoteChannel next = new RemoteChannel(
                item.isEmpty() ? ItemStack.EMPTY : item.copyWithCount(1), current.number());
        BlockInlayManager.put(level, pos, inlays.withChannel(face, next));
        InlayCarrierBlock.channelChanged(level, pos);
    }
}
