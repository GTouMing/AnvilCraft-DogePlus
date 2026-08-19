package dev.anvilcraft.gtouming.doge_plus.curios;

import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.util.SoundTransformer;
import dev.anvilcraft.gtouming.doge_plus.api.curios.ICurios;
import dev.anvilcraft.gtouming.doge_plus.api.sound.DogePlusSoundHelper;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class CuriosDisappear implements ICurios {
    public void register() {
        var bus = NeoForge.EVENT_BUS;
        bus.addListener(this::onPlayerLogin);
    }

    public void onClientSetup(FMLClientSetupEvent event) {
    }

    @Override
    public ItemStack findMobileSilencer(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        return stack.is(ModItems.MOBILE_SILENCER) ? stack : ItemStack.EMPTY;
    }

    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        var stack = findMobileSilencer(event.getEntity());
        if (stack == null) return;
        if (!(stack.getItem() instanceof MobileSilencer)) return;

        DogePlusSoundHelper.INSTANCE.register(SoundTransformer.asSoundListener(stack));
    }
}
