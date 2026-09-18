package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateOutputData;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateStateData;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
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
 * 调整逻辑门设定值（客户端 → 服务端）：铁砧锤指向门所在面时 Ctrl + 滚轮增减。
 */
public record AdjustGateValuePacket(BlockPos pos, Direction face, int delta) implements IInsensitiveBiPacket {

    public static final Type<AdjustGateValuePacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("adjust_gate_value"));
    public static final StreamCodec<ByteBuf, AdjustGateValuePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, AdjustGateValuePacket::pos,
            Direction.STREAM_CODEC, AdjustGateValuePacket::face,
            ByteBufCodecs.VAR_INT, AdjustGateValuePacket::delta,
            AdjustGateValuePacket::new
    );

    @Override
    public Type<AdjustGateValuePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock)) return;

        BlockInlays inlays = BlockInlayManager.get(level, pos);
        LogicGateType gateType = inlays.getGateType(face);
        if (!gateType.isSettable()) return;

        int value = Math.clamp(inlays.getValue(face) + delta, minValue(gateType), maxValue(gateType));
        BlockInlayManager.put(level, pos, inlays.withValue(face, value));

        if (gateType == LogicGateType.LATCH_GATE) {
            // 锁存门的「设定值」就是当前记录：同步写入其输出与已锁存标记。
            LogicGateOutputData outputData = LogicGateOutputData.get(level);
            if (outputData != null) outputData.setSignal(pos, face, value);
            LogicGateStateData stateData = LogicGateStateData.get(level);
            if (stateData != null) stateData.setLatched(pos, face, value > 0);
        }

        // 设定值影响门的输出，需要重算网络并刷新载体外观 / 客户端同步。
        LogicGateNetworkManager.topologyChanged(level, pos);
        InlayCarrierBlock.refreshState(level, pos);
    }

    /** 该门类型设定值的下限。 */
    private static int minValue(LogicGateType gateType) {
        return gateType == LogicGateType.COUNTER_GATE ? 1 : 0;
    }

    /** 该门类型设定值的上限（计数 / 延时门走配置）。 */
    private static int maxValue(LogicGateType gateType) {
        return switch (gateType) {
            case COUNTER_GATE -> AnvilCraftDogePlus.CONFIG.counterMaxCount;
            case DELAY_GATE -> AnvilCraftDogePlus.CONFIG.delayMaxTicks;
            default -> BlockInlays.DEFAULT_VALUE;
        };
    }
}
