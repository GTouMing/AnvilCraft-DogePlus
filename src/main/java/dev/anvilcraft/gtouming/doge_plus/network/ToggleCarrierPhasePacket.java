package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierPhase;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 切换超限镶嵌载体相位（客户端 → 服务端）：写入主手物品的 {@code CARRIER_PHASE} 组件。
 */
public record ToggleCarrierPhasePacket(CarrierPhase phase) implements IInsensitiveBiPacket {

    public static final Type<ToggleCarrierPhasePacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("toggle_carrier_phase"));
    public static final StreamCodec<ByteBuf, ToggleCarrierPhasePacket> STREAM_CODEC = StreamCodec.composite(
            CarrierPhase.STREAM_CODEC,
            ToggleCarrierPhasePacket::phase,
            ToggleCarrierPhasePacket::new
    );

    @Override
    public Type<ToggleCarrierPhasePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        ItemStack stack = player.getMainHandItem();
        if (stack.is(ModBlocks.TRANSCENDIUM_INLAY_CARRIER.get().asItem())) {
            stack.set(ModDataComponentTypes.CARRIER_PHASE, this.phase);
        }
    }
}
