package dev.anvilcraft.gtouming.doge_plus.curios;

import dev.anvilcraft.gtouming.doge_plus.api.curios.ICurios;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.CuriosRenderer;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

/**
 * 有 Curios 环境下的移动式消音器实现：优先从 curios 槽查找，其次头部装备槽。
 *
 * <p>消音判定为无状态查询（{@code DogePlusSoundHelper} 直接扫 Level），
 * 无需在登录/换装时向任何监听列表注册 ItemStack。</p>
 */
public class CuriosExist implements ICurios {

    @Override
    public void register() {
        // 纯查询式消音，无事件需要注册
    }

    @Override
    public ItemStack findMobileSilencer(Player player) {
        var result = CuriosApi.getCuriosInventory(player);
        if (result.isEmpty()) return ItemStack.EMPTY;
        var handler = result.get();
        var found = handler.findFirstCurio(stack -> stack.is(ModItems.MOBILE_SILENCER));
        return found.map(SlotResult::stack).orElse(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    @Override
    public void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> CuriosRendererRegistry.register(ModItems.MOBILE_SILENCER.get(), CuriosRenderer::new));
    }
}
