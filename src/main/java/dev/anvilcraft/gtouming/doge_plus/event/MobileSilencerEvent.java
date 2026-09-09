package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.api.sound.DogePlusSoundHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;

/**
 * 移动式消音器：拦截 {@link PlayLevelSoundEvent}（实体与位置两类），
 * 由无状态查询（{@link DogePlusSoundHelper}）判断声源是否处于某位玩家消音器的范围内。
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class MobileSilencerEvent {

    @SubscribeEvent
    public static void onPlaySoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        if (event.getSound() == null) return;
        Entity entity = event.getEntity();
        event.setCanceled(DogePlusSoundHelper.INSTANCE.shouldMute(
                event.getLevel(),
                event.getSound().value().getLocation(),
                entity.position()
        ));
    }

    @SubscribeEvent
    public static void onPlaySoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null) return;
        Vec3 position = event.getPosition();
        event.setCanceled(DogePlusSoundHelper.INSTANCE.shouldMute(
                event.getLevel(),
                event.getSound().value().getLocation(),
                position
        ));
    }
}
