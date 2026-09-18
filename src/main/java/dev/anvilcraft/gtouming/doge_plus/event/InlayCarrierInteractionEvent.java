package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.TranscendiumInlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.TranscendiumInlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 镶嵌载体交互：主手铁砧锤左键点击载体方块。
 *
 * <p>副手存在合法镶嵌材料 → 添加/替换点击面的镶嵌；否则移除点击面的镶嵌。
 * 被替换/移除的材料返还给玩家。超限载体走其方块实体，普通载体走 {@link BlockInlayManager}。</p>
 *
 * <p>普通载体的目标面按准星命中的<b>模型部件</b>判定（见 {@link InlayCarrierBlock#pickFace}），
 * 而不是方块面，避免从侧面看到通道时误判成侧面的镶孔；超限载体仍按方块面定位。</p>
 *
 * <p>潜行左键作用于点击面的<b>反面</b>（便于在不便对准背面时操作）。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class InlayCarrierInteractionEvent {

    /** 部件射线检测的长度：只需覆盖到载体所在方块，取比常规触及距离略长即可。 */
    private static final double RAY_LENGTH = 6.0;

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Player player = event.getEntity();
        if (!event.getItemStack().is(ModItemTags.ANVIL_HAMMER)) return;

        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        boolean transcendium = block instanceof TranscendiumInlayCarrierBlock;
        boolean normal = block instanceof InlayCarrierBlock;
        if (!normal && !transcendium) return;

        Direction face = event.getFace();
        if (face == null) return;
        // 普通载体：按准星命中的模型部件（通道/棱角）判定面，而不是单纯的方块面。
        if (normal) {
            Direction picked = pickPartFace(player, pos, state);
            if (picked != null) face = picked;
        }
        // 潜行左键操作点击面的反面（同样：副手有合法材料则替换，否则移除）
        if (player.isShiftKeyDown()) face = face.getOpposite();

        // 阻止左键开始破坏方块（客户端也要拦截，避免预测破坏）
        event.setCanceled(true);
        if (level.isClientSide) return;

        ItemStack offhand = player.getOffhandItem();
        MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(offhand);

        if (material != null) {
            InlayEntry entry = InlayEntry.fromItemStack(offhand, material);
            if (entry.isEmpty()) return;
            InlayEntry replaced;
            if (transcendium) {
                if (!(level.getBlockEntity(pos) instanceof TranscendiumInlayCarrierBlockEntity be)) return;
                replaced = be.addOnFace(face.ordinal(), entry);
            } else {
                replaced = addOnCarrier(level, pos, face, entry);
            }
            returnToPlayer(player, replaced);
            if (!player.getAbilities().instabuild) offhand.shrink(1);
        } else {
            if (transcendium) {
                if (!(level.getBlockEntity(pos) instanceof TranscendiumInlayCarrierBlockEntity be)) return;
                returnToPlayer(player, be.removeOnFace(face.ordinal()));
            } else {
                returnToPlayer(player, removeOnCarrier(level, pos, face));
            }
        }
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 沿玩家视线对普通载体的各部件做射线检测，返回命中的面；仅命中中心体时返回 null。 */
    @Nullable
    private static Direction pickPartFace(Player player, BlockPos pos, BlockState state) {
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
        return InlayCarrierBlock.pickFace(state, pos, from, to);
    }

    /** 普通载体：把镶嵌写入该面对应的槽位。 */
    private static InlayEntry addOnCarrier(Level level, BlockPos pos, Direction face, InlayEntry entry) {
        List<InlayEntry> existing = new ArrayList<>(BlockInlayManager.get(level, pos).inlays());
        int slot = face.ordinal();
        InlayEntry old = slot < existing.size() ? existing.get(slot) : InlayEntry.nulls();
        applyCarrier(level, pos, face, InlayCarrierBlock.withSlot(existing, slot, entry));
        return old;
    }

    /** 普通载体：清空该面对应的槽位。 */
    private static InlayEntry removeOnCarrier(Level level, BlockPos pos, Direction face) {
        List<InlayEntry> existing = new ArrayList<>(BlockInlayManager.get(level, pos).inlays());
        int slot = face.ordinal();
        if (slot >= existing.size()) return InlayEntry.nulls();
        InlayEntry old = existing.get(slot);
        if (old.isEmpty()) return InlayEntry.nulls();
        existing.set(slot, InlayEntry.nulls());
        applyCarrier(level, pos, face, existing);
        return old;
    }

    private static void applyCarrier(Level level, BlockPos pos, Direction face, List<InlayEntry> inlays) {
        Block block = level.getBlockState(pos).getBlock();
        // 该面换料后设定值回到默认，其余面保留既有设定值。
        Map<Direction, Integer> values = new HashMap<>(BlockInlayManager.get(level, pos).values());
        values.remove(face);
        BlockInlayManager.put(level, pos, BlockInlays.fromInlays(block, inlays).withValues(values));
        InlayCarrierBlock.refreshState(level, pos);
        // 换料可能只改变门类型而面的镶嵌状态不变（外观由 refreshState 更新），
        // 这里显式补一次拓扑更新，确保门类型变更一定被逻辑网络看到。
        LogicGateNetworkManager.topologyChanged(level, pos);
        // 该面的镶嵌状态变化会改变 canConnectRedstone，邻居红石导线需重建拓扑以接入或断开该面。
        BlockPos wirePos = pos.relative(face);
        if (level.getBlockState(wirePos).getBlock() instanceof RedstoneWireBlock) {
            RedstoneWireNetworkManager.topologyChanged(level, wirePos);
        }
    }

    private static void returnToPlayer(Player player, InlayEntry entry) {
        if (entry == null || entry.isEmpty()) return;
        ItemStack stack = entry.toItemStack();
        if (!stack.isEmpty()) player.getInventory().placeItemBackInInventory(stack);
    }
}
