package dev.anvilcraft.gtouming.doge_plus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;

/**
 * 磁铁（MagnetItem / 磁铁模式的多功能工具）铁砧收纳、放置、蓄力的共享逻辑。
 * <p>供 {@code MagnetItemMixin} 与 {@code MultiToolItemMixin} 复用。</p>
 */
public class MagnetAnvilUtil {

    /**
     * 拾取铁砧方块到磁铁：任一只手是磁铁，另一只手为空或铁砧物品。
     *
     * @return 是否成功拾取（此时调用方应返回 {@link InteractionResult#CONSUME}）
     */
    public static boolean tryPickupAnvil(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return false;
        Player player = context.getPlayer();
        if (player == null) return false;
        BlockPos pos = context.getClickedPos();
        Block block = level.getBlockState(pos).getBlock();
        if (!(block instanceof AnvilBlock)) return false;

        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();

        // 两只手都能收纳：任一只手持有磁铁
        ItemStack magnet;
        if (MagnetHandler.isMagnet(off)) magnet = off;
        else if (MagnetHandler.isMagnet(main)) magnet = main;
        else return false;

        if (MagnetHandler.hasAnvil(magnet)) return false;

        // 另一只手：空 或 铁砧物品
        ItemStack other = (magnet == off) ? main : off;
        if (!other.isEmpty() && !(other.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AnvilBlock)) return false;

        MagnetHandler.setAnvil(magnet, BuiltInRegistries.BLOCK.getKey(block));
        level.removeBlock(pos, false);
        return true;
    }

    /**
     * 放置磁铁存储的铁砧（副手磁铁，主手为空）。
     *
     * @return 是否成功放置（此时调用方应返回 {@link InteractionResult#CONSUME}）
     */
    public static boolean tryPlaceAnvil(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) return false;
        Player player = context.getPlayer();
        if (player == null) return false;
        InteractionHand hand = context.getHand();
        ItemStack off = player.getOffhandItem();
        ItemStack main = player.getMainHandItem();

        if (hand != InteractionHand.OFF_HAND) return false;
        if (!(MagnetHandler.isMagnet(off) && main.isEmpty())) return false;
        if (!MagnetHandler.hasAnvil(off)) return false;
        ResourceLocation location = MagnetHandler.getAnvilId(off);
        BlockItem item = (BlockItem) BuiltInRegistries.ITEM.get(location);
        if (item.place(new BlockPlaceContext(context)) != InteractionResult.FAIL) {
            // BlockItem.place 会消耗手中物品，补回以不消耗磁铁
            player.getItemInHand(context.getHand()).grow(1);
            MagnetHandler.clearAnvilId(off);
            return true;
        }
        return false;
    }

    /**
     * 带铁砧的磁铁右键进入蓄力。
     *
     * @return 是否开始蓄力（此时调用方应返回 success）
     */
    public static boolean tryStartCharge(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (!MagnetHandler.hasAnvil(stack)) return false;
        if (player.isShiftKeyDown()) return false;
        if (!level.isClientSide) player.startUsingItem(usedHand);
        return true;
    }
}
