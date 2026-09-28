package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.PipeCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
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
 * 调整管道载体「存入」面的物流量（客户端 → 服务端）：铁砧锤指向该面时 Ctrl + 滚轮增减。
 *
 * <p>与 {@link AdjustGateValuePacket} 平行，但物流量不影响红石与形状，因此只写数据并同步
 * （{@link BlockInlayManager#put} 自带客户端广播），无需重建网络或刷新方块状态。</p>
 */
public record AdjustPipeThroughputPacket(BlockPos pos, Direction face, int delta) implements IInsensitiveBiPacket {

    public static final Type<AdjustPipeThroughputPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("adjust_pipe_throughput"));
    public static final StreamCodec<ByteBuf, AdjustPipeThroughputPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AdjustPipeThroughputPacket::pos,
            Direction.STREAM_CODEC, AdjustPipeThroughputPacket::face,
            ByteBufCodecs.VAR_INT, AdjustPipeThroughputPacket::delta,
            AdjustPipeThroughputPacket::new
    );

    @Override
    public Type<AdjustPipeThroughputPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof PipeCarrierBlock)) return;

        BlockInlays inlays = BlockInlayManager.get(level, pos);
        // 只有「存入」面有物流量语义（取出面固定整叠）。
        if (inlays.getFace(face) != FaceMode.INSERT) return;

        int value = Math.clamp(inlays.getThroughput(face) + delta, 1, AnvilCraftDogePlus.CONFIG.pipeThroughputMax);
        BlockInlayManager.put(level, pos, inlays.withThroughput(face, value));
    }
}
