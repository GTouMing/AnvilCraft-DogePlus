package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.api.SoundTransformer;
import dev.anvilcraft.gtouming.doge_plus.api.sound.DogePlusSoundHelper;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class MobileSilencerEvent {

    @SubscribeEvent
    public static void onSilencerEquipment(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.HEAD) return;
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        if (event.getFrom().getItem() instanceof MobileSilencer) {
            DogePlusSoundHelper.INSTANCE.unregister(SoundTransformer.asSoundListener(from));
        }
        if (event.getTo().getItem() instanceof MobileSilencer) {
            DogePlusSoundHelper.INSTANCE.register(SoundTransformer.asSoundListener(to));
        }
    }

    @SubscribeEvent
    public static void onPlaySoundAtEntity(PlayLevelSoundEvent.AtEntity event) {
        Entity entity = event.getEntity();
        if (event.getSound() == null) return;
        event.setCanceled(DogePlusSoundHelper.INSTANCE.shouldMute(
                event.getSound().value().getLocation(),
                new Vec3(entity.getX(), entity.getY(), entity.getZ())
        ));
    }

    @SubscribeEvent
    public static void onPlaySoundAtPosition(PlayLevelSoundEvent.AtPosition event) {
        if (event.getSound() == null) return;
        event.setCanceled(DogePlusSoundHelper.INSTANCE.shouldMute(
                event.getSound().value().getLocation(),
                event.getPosition()
        ));
    }
}
