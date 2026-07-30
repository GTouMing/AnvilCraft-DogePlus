package dev.anvilcraft.gtouming.doge_plus.item;

import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MobileSilencer extends Item implements Equipable {

    public MobileSilencer(Properties properties) {
        super(properties);
    }

    // ===== 持久化 =====

    public static List<ResourceLocation> getMutedSounds(ItemStack stack) {
        var list = stack.get(ModDataComponentTypes.SOUNDS_SET.get());
        if (list == null) return new CopyOnWriteArrayList<>();
        return list;
    }

    public static void setMutedSounds(ItemStack stack, List<ResourceLocation> sounds) {
        if (!(stack.getItem() instanceof MobileSilencer)) return;
        stack.set(ModDataComponentTypes.SOUNDS_SET.get(), sounds);
    }


    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }
}
