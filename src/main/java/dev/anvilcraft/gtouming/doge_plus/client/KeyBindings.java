package dev.anvilcraft.gtouming.doge_plus.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(value = Dist.CLIENT)
public class KeyBindings {
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
}
