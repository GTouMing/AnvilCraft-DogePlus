package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.core.BlockPos;
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
 *
 * <p>放置时 {@code Level.setBlock} 内的 {@code onPlace} 已经先于本注入执行，
 * 那一刻 {@link BlockInlayManager} 里还没有镶嵌数据，逻辑门网络因此看不到门。
 * 写入后必须显式补一次拓扑更新，否则「带镶嵌的载体物品放置后」逻辑门不会被注册。</p>
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

    @Inject(method = "placeBlock", at = @At("TAIL"))
    private void doge_plus$recordInlaidBlockPlace(
            BlockPlaceContext context, BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) return;
        Level level = context.getLevel();
        if (level.isClientSide) return;
        List<InlayEntry> inlays = InlayUtil.getInlays(context.getItemInHand());
        if (inlays.isEmpty()) return;
        BlockPos mainPos = context.getClickedPos();
        BlockItem block = Util.cast(this);
        if (block.getBlock() instanceof IMultiPartBlock part) mainPos = part.doge_plus$getMainPos(mainPos, state);
        BlockInlayManager.put(level, mainPos, BlockInlays.fromInlays(state.getBlock(), inlays));
        LogicGateNetworkManager.topologyChanged(level, mainPos);
    }
}
