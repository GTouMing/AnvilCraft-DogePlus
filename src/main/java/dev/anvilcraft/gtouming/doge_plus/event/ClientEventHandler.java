package dev.anvilcraft.gtouming.doge_plus.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MobileSilencerScreen;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.GiantDogeAnvilRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    public static final KeyMapping OPEN_SILENCER = new KeyMapping(
            "key.anvilcraft_doge_plus.open_silencer",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.anvilcraft_doge_plus"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SILENCER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!OPEN_SILENCER.consumeClick()) return;
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;

        var stack = MobileSilencer.findMobileSilencer(player);
        if (stack.is(ModItems.MOBILE_SILENCER)) {
            minecraft.setScreen(new MobileSilencerScreen(stack));
        }
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_TABLE.get(), InlayTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GIANT_DOGE_ANVIL.get(), GiantDogeAnvilRenderer::new);
    }
}
