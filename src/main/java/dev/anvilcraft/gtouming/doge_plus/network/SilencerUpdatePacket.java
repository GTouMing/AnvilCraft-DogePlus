package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public record SilencerUpdatePacket(List<ResourceLocation> sounds) implements IInsensitiveBiPacket {
    public static final Type<SilencerUpdatePacket> TYPE = IPacket.type(AnvilCraftDogePlus.of("silencer_update"));
    public static final StreamCodec<ByteBuf, SilencerUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC),
            SilencerUpdatePacket::sounds,
            SilencerUpdatePacket::new
    );

    @Override
    public Type<SilencerUpdatePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        var stack = MobileSilencer.findMobileSilencer(player);
        MobileSilencer.setMutedSounds(stack, sounds);
    }
}
