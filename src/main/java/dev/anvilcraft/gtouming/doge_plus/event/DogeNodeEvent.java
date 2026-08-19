package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class DogeNodeEvent {

    @SubscribeEvent
    public static void onRegisterCapability(RegisterCapabilitiesEvent event) {
        event.registerEntity(Capabilities.ItemHandler.ENTITY_AUTOMATION,
                ModEntities.DOGE_NODE.get(), (a, b) -> a);
        // 镶嵌台物品处理能力（插入进 0/1，取出 2/3）
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.INLAY_TABLE.get(), (be, side) -> be);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Level level = event.getLevel();
        if (level.isClientSide) return;
        BlockPos pos = event.getPos();
        List<DogeNodeEntity> nodes = level.getEntitiesOfClass(DogeNodeEntity.class,
                new AABB(pos).expandTowards(0.0, 0.0625, 0.0));
        if (nodes.isEmpty()) return;

        Player player = event.getEntity();
        DogeNodeEntity node = nodes.getFirst();
        ItemStack item = player.getItemInHand(InteractionHand.MAIN_HAND);

        // 手持 doge钢 + shift：先移除节点
        if (item.is(ModItems.DOGE_MAGNET.get()) && player.isShiftKeyDown()) {
            event.setCanceled(true);
            node.removeNodeAndRelease();
            return;
        }
        // 空手右键：返还捕获的物品
        if (item.isEmpty()) {
            event.setCanceled(true);
            node.releaseToPlayer(player);
        }
    }
}
