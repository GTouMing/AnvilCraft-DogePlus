package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 放置 hook：方块物品放置成功后，把物品上的 {@code INLAY} 组件记录到
 * {@link BlockInlayManager}，使镶嵌方块在放置后保留镶嵌属性。
 *
 * <p>注入 {@code placeBlock}（唯一真正执行 {@code setBlock} 的位置）而非 {@code place}，
 * 保证只有放置成功时才记录；此时 {@code context.getItemInHand()} 尚未被消耗。</p>
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "placeBlock", at = @At("TAIL"))
    private void doge_plus$recordInlaidBlockPlace(
            BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Level level = context.getLevel();
        if (level.isClientSide) return;
        List<ResourceLocation> inlays = InlayUtil.getInlays(context.getItemInHand());
        if (inlays.isEmpty()) return;
        BlockPos mainPos = context.getClickedPos();
        BlockItem block = Util.cast(this);
        if (block.getBlock() instanceof IMultiPartBlock part) mainPos = part.doge_plus$getMainPos(mainPos, state);
        BlockInlayManager.put(level, mainPos, BlockInlays.fromInlays(state.getBlock(), inlays));
        // 放置逻辑门后立即重建网络拓扑，避免依赖邻居变化才触发导致门无输出
        LogicGateNetworkManager.topologyChanged(level, mainPos);
    }
}
