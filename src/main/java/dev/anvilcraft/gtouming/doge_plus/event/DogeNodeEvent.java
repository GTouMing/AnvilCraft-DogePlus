package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
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
        // 空手右键：返还捕获的物品
        if (item.isEmpty()) {
            event.setCanceled(true);
            node.releaseToPlayer(player);
            return;
        }
        // shift + 右键非磁铁物品：投喂 1 个到节点（物品实体落在节点处，随后被吸附/捕获）
        if (player.isShiftKeyDown()) {
            event.setCanceled(true);
            ItemStack feed = item.copy();
            feed.setCount(1);
            if (!player.getAbilities().instabuild) item.shrink(1);
            ItemEntity itemEntity = new ItemEntity(level, node.getX(), node.getY(), node.getZ(), feed);
            itemEntity.setDeltaMovement(0, 0, 0);
            itemEntity.setPickUpDelay(60);
            level.addFreshEntity(itemEntity);
        }
    }
}
