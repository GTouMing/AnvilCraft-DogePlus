package dev.anvilcraft.gtouming.doge_plus.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;

public class MagnetAnvilUtil {

    /**
     * 拾取铁砧方块到磁铁：任一只手是磁铁，另一只手为空或铁砧物品。
     *
     * <p>前置校验两侧一致（客户端据此预测成功、正常挥臂），世界改动只走服务端。</p>
     *
     * @return 是否成功拾取（此时调用方应返回 {@link InteractionResult#sidedSuccess}）
     */
    public static boolean tryPickupAnvil(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return false;
        if (!player.mayBuild()) return false;
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
        if (!other.isEmpty() && !isAnvilItem(other)) return false;

        if (level.isClientSide) return true;

        MagnetHandler.setAnvil(magnet, BuiltInRegistries.BLOCK.getKey(block));
        level.removeBlock(pos, false);
        return true;
    }

    /**
     * 放置磁铁存储的铁砧：手持（任一手都行）的磁铁含铁砧时，右键方块即把铁砧放在点上。
     *
     * <p>前置校验两侧一致（客户端据此预测成功、正常挥臂），世界改动只走服务端。</p>
     *
     * @return 是否成功放置（此时调用方应返回 {@link InteractionResult#sidedSuccess}）
     */
    public static boolean tryPlaceAnvil(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return false;
        ItemStack stack = context.getItemInHand();
        if (!MagnetHandler.isMagnet(stack) || !MagnetHandler.hasAnvil(stack)) return false;
        if (!player.mayBuild()) return false;
        ResourceLocation location = MagnetHandler.getAnvilId(stack);
        // 记录的铁砧方块可能没有对应的 BlockItem（异 id 的第三方铁砧）：这里兜底，别强转崩服。
        if (location == null
                || !(BuiltInRegistries.ITEM.get(location) instanceof BlockItem item)
                || !(item.getBlock() instanceof AnvilBlock)) {
            return false;
        }

        if (level.isClientSide) return true;

        if (item.place(new BlockPlaceContext(context)) != InteractionResult.FAIL) {
            // BlockItem.place 消耗的是手持物（这里是磁铁），补回以不消耗磁铁
            player.getItemInHand(context.getHand()).grow(1);
            MagnetHandler.clearAnvilId(stack);
            return true;
        }
        return false;
    }

    /** 该物品是否为「铁砧方块物品」（可被磁铁收纳 / 放置的铁砧类）。 */
    private static boolean isAnvilItem(ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AnvilBlock;
    }
}
