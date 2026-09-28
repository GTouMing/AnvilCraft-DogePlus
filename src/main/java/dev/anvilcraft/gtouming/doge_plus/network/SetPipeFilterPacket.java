package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.PipeCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
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
 * 设置 / 替换 / 清除管道载体「存入」面的过滤物品（客户端 → 服务端）。
 *
 * <p>主手铁砧锤<b>点按</b>右键该面时发出：过滤取自副手物品（空栈表示清除）。
 * 与 {@link SetCarrierFacePacket} 平行；过滤物品可以是普通物品，也可以是前置的过滤器物品
 * （匹配时走 {@code FilterItem.filter}，其内嵌的 {@code FilterContent} 一并生效）。</p>
 */
public record SetPipeFilterPacket(BlockPos pos, Direction face, ItemStack filter) implements IInsensitiveBiPacket {

    /** 交互距离上限的平方（8 格，远大于正常触及距离，只用于挡住伪造包）。 */
    private static final double MAX_DISTANCE_SQR = 64.0;

    public static final Type<SetPipeFilterPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("set_pipe_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetPipeFilterPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetPipeFilterPacket::pos,
                    Direction.STREAM_CODEC, SetPipeFilterPacket::face,
                    ItemStack.OPTIONAL_STREAM_CODEC, SetPipeFilterPacket::filter,
                    SetPipeFilterPacket::new
            );

    @Override
    public Type<SetPipeFilterPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeCarrierBlock)) return;
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

        BlockInlays inlays = BlockInlayManager.get(level, pos);
        // 只有「存入」面能设过滤（与物流量一致）。
        if (inlays.getFace(face) != FaceMode.INSERT) return;

        BlockInlayManager.put(level, pos, inlays.withFilter(face, filter));
    }
}
