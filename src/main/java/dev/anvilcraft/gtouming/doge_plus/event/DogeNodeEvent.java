package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.List;

@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class DogeNodeEvent {

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Level level = event.getLevel();
        if (level.isClientSide) {
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }
        BlockPos pos = event.getPos();
        List<DogeNodeEntity> nodes = level.getEntitiesOfClass(DogeNodeEntity.class,
                new AABB(pos).expandTowards(0.0, 0.0625, 0.0));
        if (nodes.isEmpty()) return;

        Player player = event.getEntity();
        DogeNodeEntity node = nodes.getFirst();
        ItemStack item = player.getItemInHand(InteractionHand.MAIN_HAND);

        // 手持 doge 磁铁 + shift：移除节点
        if (item.is(ModItems.DOGE_MAGNET.get()) && player.isShiftKeyDown()) {
            event.setCanceled(true);
            node.removeNodeAndRelease();
            return;
        }
        // 空手右键：有捕获的物品时取回；节点为空时不拦截，交给方块自身处理
        // （如鱼缸 / 大型炼药锅空手取物，否则物品会被节点一直挡住取不出来）
        if (item.isEmpty()) {
            if (node.getCapturedItems().isEmpty()) return;
            event.setCanceled(true);
            node.releaseToPlayer(player);
        }
    }
}
