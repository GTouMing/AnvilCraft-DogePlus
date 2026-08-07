package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.util.MagnetHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AnvilBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 磁铁铁砧交互：右手持铁砧物品时收纳进磁铁。
 * <p>磁铁锭视线标记目标实现在 {@code ItemMixin}。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class MagnetAnvilEvent {

    /**
     * 手持铁砧物品时收纳进磁铁：任一只手是未装铁砧的磁铁（MagnetItem 或磁铁模式多功能工具），
     * 另一只手是铁砧物品，右键空气即可将铁砧物品收入磁铁（消耗该物品）。
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player.isShiftKeyDown()) return;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // 找到未装铁砧的磁铁与另一只手的铁砧物品
        ItemStack magnet;
        ItemStack anvilItem;
        if (MagnetHandler.isMagnet(main) && !MagnetHandler.hasAnvil(main)) {
            magnet = main;
            anvilItem = off;
        } else if (MagnetHandler.isMagnet(off) && !MagnetHandler.hasAnvil(off)) {
            magnet = off;
            anvilItem = main;
        } else {
            return;
        }
        if (!(anvilItem.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AnvilBlock)) return;

        // 客户端取消以阻止本地 use() 预测；数据包仍会发送，由服务端处理
        if (player.level().isClientSide) {
            event.setCanceled(true);
            return;
        }

        // 服务端：将铁砧物品收纳进磁铁
        MagnetHandler.setAnvil(magnet, BuiltInRegistries.BLOCK.getKey(((BlockItem) anvilItem.getItem()).getBlock()));
        anvilItem.shrink(1);
        player.getCooldowns().addCooldown(magnet.getItem(), 5);
        event.setCanceled(true);
    }
}
