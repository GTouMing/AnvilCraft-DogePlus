package dev.anvilcraft.gtouming.doge_plus.client.chain;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;

/**
 * 门链手势的客户端输入接线。
 *
 * <p>拦截使用/攻击键：按住 Ctrl 且手持逻辑载体（放置）或持铁砧锤对准逻辑载体（移除）时，
 * 用键开始手势并取消原版放置；手势进行中右键固定拐点、左键移除拐点。每 tick 推进手势状态
 * （Ctrl 松开即生效、手持物变更即取消）。具体判定都在 {@link GateChainGesture#start} 内。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class GateChainClientEvents {

    private GateChainClientEvents() {
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) return;

        if (event.isUseItem()) {
            if (GateChainGesture.isActive()) {
                event.setCanceled(true);
                GateChainGesture.addWaypoint(minecraft);
                return;
            }
            if (GateChainGesture.start(minecraft)) {
                event.setCanceled(true);
            }
            return;
        }

        if (event.isAttack() && GateChainGesture.isActive()) {
            event.setCanceled(true);
            GateChainGesture.removeWaypoint();
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        GateChainGesture.tick(Minecraft.getInstance());
    }
}
