package dev.anvilcraft.gtouming.doge_plus.curios;

import dev.anvilcraft.gtouming.doge_plus.api.curios.ICurios;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 无 Curios 环境下的移动式消音器实现：直接查头部装备槽。
 *
 * <p>消音判定为无状态查询（{@code DogePlusSoundHelper} 直接扫 Level），
 * 无需在登录/换装时向任何监听列表注册 ItemStack。</p>
 */
public class CuriosDisappear implements ICurios {

    @Override
    public void register() {
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
    }

    @Override
    public ItemStack findMobileSilencer(Player player) {
        ItemStack stack = player.getItemBySlot(EquipmentSlot.HEAD);
        return stack.is(ModItems.MOBILE_SILENCER) ? stack : ItemStack.EMPTY;
    }
}
