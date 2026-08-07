package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;

import static dev.dubhe.anvilcraft.init.item.ModItems.MAGNET;
import static dev.dubhe.anvilcraft.init.item.ModItems.MULTITOOL_ITEM;

public class MagnetHandler {
    public static boolean hasAnvil(ItemStack stack) {
        ResourceLocation resourceLocation = stack.get(ModDataComponentTypes.MAGNET_HAS_ANVIL);
        if (resourceLocation == null) return false;
        return getAnvil(resourceLocation) != null;
    }

    public static AnvilBlock getAnvil(ResourceLocation location) {
        if (BuiltInRegistries.BLOCK.get(location) instanceof AnvilBlock anvil) return anvil;
        return null;
    }

    public static ResourceLocation getAnvilId(ItemStack stack) {
        return stack.get(ModDataComponentTypes.MAGNET_HAS_ANVIL);
    }

    public static void clearAnvilId(ItemStack stack) {
        stack.set(ModDataComponentTypes.MAGNET_HAS_ANVIL, null);
    }

    public static void setAnvil(ItemStack stack, ResourceLocation location) {
        stack.set(ModDataComponentTypes.MAGNET_HAS_ANVIL, location);
    }

    public static boolean isMagnet(ItemStack stack) {
        return stack.is(MAGNET) || stack.is(MULTITOOL_ITEM) && MultitoolItem.getMode(stack) == MultitoolItem.MAGNET_MODE;
    }
}
