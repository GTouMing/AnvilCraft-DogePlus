package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import dev.anvilcraft.gtouming.doge_plus.data.*;
import dev.anvilcraft.gtouming.doge_plus.logic.ILogicGate;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.lib.v2.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
public abstract class BlockBehaviourMixin implements ILogicGate {

    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void onGetDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        Level level = params.getLevel();
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockInlays bi = BlockInlayManager.get(level, pos);
        List<InlayEntry> inlays = bi.inlays();
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
        LogicGateNetworkManager.topologyChanged(level, pos);
        if (state.getBlock() == oldState.getBlock() || state.getBlock() == Blocks.AIR) return;

        BlockPos mainPos = pos;
        if (state.getBlock() instanceof IMultiPartBlock part) mainPos = part.doge_plus$getMainPos(pos, state);
        //非巨构中心坐标则返回
        if (!mainPos.equals(pos)) return;
        PowerGridManager manager = PowerGridManager.get(level);
        if (manager == null) return;

        if (BlockInlayManager.hasProperty(level, mainPos, InlayProperty.GENERATOR))
            manager.add(mainPos, new InlayPowerProducer(level, mainPos));
    }
    /**
     * 方块移除时触发更新。
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
        if (level.isClientSide()) return;
        LogicGateNetworkManager.topologyChanged(level, pos);
        if (state.getBlock() == newState.getBlock() || state.getBlock() == Blocks.AIR) return;

        BlockPos mainPos = pos;
        if (Util.cast(this) instanceof IMultiPartBlock part) mainPos = part.doge_plus$getMainPos(pos, state);
        //非巨构中心坐标则返回
        if (!mainPos.equals(pos)) return;
        BlockInlayManager.remove(level, pos);
        PowerGridManager pManager = PowerGridManager.get(level);
        if (pManager == null) return;

        if (pManager.get(pos) != null)
            pManager.remove(mainPos);
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
        LogicGateNetworkManager.neighborChanged(level, pos, neighborPos);
    }


    /**
     * 完全接管 getSignal（弱信号）。
     *
     * <p>vanilla 红石线检测邻居时传 direction = 「红石线 → 门」的方向，
     * 而门在 {@code direction} 的对面（{@code direction.getOpposite()}）面输出，
     * 因此取反方向查询门的输出，使红石线在门输出方向的那一面收到信号
     * （否则门在 UP 输出会表现为「从下面输出」）。</p>
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
        // 只要 pos 是逻辑门，就接管弱信号读取并取反方向。
        // 不能加「相对方向邻居非门」条件：红石线在门东面检测时 direction=WEST，
        // 若门西面恰好是另一个门，该条件为假 → 取反不生效 → 收不到输出。
        if (LogicGateNetworkManager.isLogicGate(level, pos))
            cir.setReturnValue(LogicGateNetworkManager.getSignal(level, pos, direction.getOpposite()));
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
        if (LogicGateNetworkManager.isLogicGate(level, pos)) cir.setReturnValue(0);
    }

    @Override
    public LogicGateType doge_plus$getGateType(Level level, BlockPos pos, Direction outputDir) {
        return BlockInlayManager.get(level, pos).getGateType(outputDir);
    }
}