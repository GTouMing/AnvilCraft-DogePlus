package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.entity.CapturedItemsProvider;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import dev.dubhe.anvilcraft.event.PlayerEventListener;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(PlayerEventListener.class)
public class PlayerEventListenerMixin {

    @Inject(method = "playerRightClickMagnetizedNode", at = @At(value = "HEAD"), cancellable = true)
    private static void onPlayerRightClickNode(PlayerInteractEvent.RightClickBlock event, CallbackInfo ci) {
        ci.cancel();
        InteractionHand hand = event.getHand();
        if (hand != InteractionHand.MAIN_HAND) return;
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        Player player = event.getEntity();
        ItemStack item = player.getItemInHand(hand);
        List<MagnetizedNodeEntity> entities = level.getEntitiesOfClass(
                MagnetizedNodeEntity.class,
                new AABB(pos).expandTowards(0.0, 0.0625, 0.0)
        );
        if (
                item.is(ModItems.MAGNET)
                        || (item.is(ModItems.MULTITOOL_ITEM) && MultitoolItem.getMode(item) == MultitoolItem.MAGNET_MODE)
                        || item.is(ModItemTags.ANVIL_HAMMER)
        ) {
            return;
        }
        if (entities.isEmpty()) return;
        MagnetizedNodeEntity node = entities.getFirst();
        if (!(node instanceof CapturedItemsProvider provider)) return;
        ItemStack stack = ItemStack.EMPTY;
        if (player.isShiftKeyDown()) stack = player.isCreative() ? item.copy() : item.copyAndClear();


        if (item.isEmpty()) provider.doge_plus$releaseToPlayer(player);
        else {
            ItemEntity fresh = new ItemEntity(level, node.position().x, node.position().y, node.position().z, stack);
            fresh.setDeltaMovement(Vec3.ZERO);
            level.addFreshEntity(fresh);
        }
    }
}
