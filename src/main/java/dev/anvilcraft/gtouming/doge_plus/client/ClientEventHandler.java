package dev.anvilcraft.gtouming.doge_plus.client;

import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MobileSilencerScreen;
import dev.anvilcraft.gtouming.doge_plus.init.ModCurios;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!KeyBindings.OPEN_SILENCER.consumeClick()) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;

        var stack = ModCurios.ICURIOS.findMobileSilencer(player);
        if (stack == null) return;
        if (stack.getItem() instanceof MobileSilencer) {
            minecraft.setScreen(new MobileSilencerScreen(stack));
        }
    }
}
