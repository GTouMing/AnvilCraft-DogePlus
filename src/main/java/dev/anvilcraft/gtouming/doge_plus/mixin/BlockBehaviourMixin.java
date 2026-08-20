package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.data.*;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void onGetDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        Level level = params.getLevel();
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockInlays bi = BlockInlayManager.get(level, pos);
        List<ResourceLocation> inlays = bi.inlays();
        if (inlays.isEmpty()) return;
        Item item = state.getBlock().asItem();
        List<ItemStack> drops = new ArrayList<>(cir.getReturnValue());
        if (item == Items.AIR) return;
        for (ItemStack drop : drops) {
            if (!drop.is(item)) continue;
            InlayUtil.setInlays(drop, new ArrayList<>(inlays));
            InlayUtil.reapplyAttributeModifiers(drop);
            break;
        }
        cir.setReturnValue(drops);
        BlockInlayManager.remove(level, pos);
    }

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void doge_plus$eternalUnbreakable(
            BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (BlockInlayManager.hasProperty(level, pos, InlayProperty.ETERNAL)) {
            cir.setReturnValue(0.0F);
        }
    }

    /**
     * 方块放置时触发更新。
     */
    @Inject(
            method = "onPlace",
            at = @At("TAIL")
    )
    private void doge_plus$onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean isMoving,
            CallbackInfo ci
    ) {
        if (level.isClientSide()) return;
        if (state.getBlock() == oldState.getBlock()) return;

        BlockInlayManager.updatePowerProducer(level, pos);
        LogicGateManager manager = LogicGateManager.get(level);
        if (manager == null) return;

        if (!manager.hasLogicGate(pos)) return;
        manager.notifyNeighbors(pos);
    }
    /**
     * 方块放置时触发更新。
     */
    @Inject(
            method = "onRemove",
            at = @At("TAIL")
    )
    private void doge_plus$onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston,
            CallbackInfo ci) {
        BlockInlayManager.updatePowerProducer(level, pos);
    }

    /**
     * 注入 neighborChanged 方法。
     * 只更新当前方块，让传播链自然传播，避免死循环。
     */
    @Inject(
            method = "neighborChanged",
            at = @At("HEAD")
    )
    private void doge_plus$onNeighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston,
            CallbackInfo ci
    ) {
        if (level.isClientSide()) return;

        LogicGateManager manager = LogicGateManager.get(level);
        if (manager == null) return;

        if (!manager.hasLogicGate(pos)) return;

        manager.onNeighborChanged(pos);
    }


    /**
     * 完全接管 getSignal（弱信号）。
     */
    @Inject(
            method = "getSignal",
            at = @At("RETURN"),
            cancellable = true
    )
    private void doge_plus$onGetSignal(
            BlockState state, BlockGetter getter, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir
    ) {
        Level level = (Level) getter;
        if (level.isClientSide()) return;

        LogicGateManager manager = LogicGateManager.get(level);
        if (manager == null) return;

        if (!manager.hasLogicGate(pos)) return;

        // 计算门逻辑输出
        int gateSignal = manager.getSignal(pos, direction);
        if (gateSignal > 0) {
            cir.setReturnValue(gateSignal);
        }
    }

    /**
     * 完全接管 getDirectSignal（强信号）。
     */
    @Inject(
            method = "getDirectSignal",
            at = @At("RETURN"),
            cancellable = true
    )
    private void doge_plus$onGetDirectSignal(
            BlockState state, BlockGetter getter, BlockPos pos, Direction direction, CallbackInfoReturnable<Integer> cir
    ) {
        Level level = (Level) getter;
        if (level.isClientSide()) return;

        LogicGateManager manager = LogicGateManager.get(level);
        if (manager == null) return;

        if (!manager.hasLogicGate(pos)) return;

        cir.setReturnValue(0);
    }
}