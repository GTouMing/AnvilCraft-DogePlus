package dev.anvilcraft.gtouming.doge_plus.item;

import dev.anvilcraft.gtouming.doge_plus.entity.FlyingAnvilEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.util.DogeNodeUtil;
import dev.anvilcraft.gtouming.doge_plus.util.MagnetAnvilUtil;
import dev.anvilcraft.gtouming.doge_plus.util.MagnetHandler;
import dev.dubhe.anvilcraft.util.MagnetUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;

public class DogeMagnetItem extends Item {

    private static final int UNLIMITED_USE_DURATION = 72000;

    public DogeMagnetItem(Properties properties) {
        super(properties);
    }

    // ==================== 右键方块 ====================

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // 铁砧收纳 / 放置
        if (MagnetAnvilUtil.tryPickupAnvil(context)) return InteractionResult.CONSUME;
        if (MagnetAnvilUtil.tryPlaceAnvil(context)) return InteractionResult.CONSUME;
        // shift 右键放置 Doge 节点
        return DogeNodeUtil.placeDogeNode(this, context);
    }

    // ==================== 右键空气 ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (level.isClientSide) return InteractionResultHolder.sidedSuccess(stack, true);
        ItemStack other = usedHand == InteractionHand.MAIN_HAND ? player.getOffhandItem() : player.getMainHandItem();

        // 1. 收纳另一只手上的铁砧（本磁铁未装铁砧、非潜行）
        if (!MagnetHandler.hasAnvil(stack) && !player.isShiftKeyDown()
                && other.getItem() instanceof BlockItem bi && bi.getBlock() instanceof AnvilBlock) {
            MagnetHandler.setAnvil(stack, BuiltInRegistries.BLOCK.getKey(bi.getBlock()));
            other.shrink(1);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        // 2. 带铁砧：进入蓄力（拉弓）
        if (MagnetHandler.hasAnvil(stack) && !player.isShiftKeyDown()) {
            player.startUsingItem(usedHand);
            return InteractionResultHolder.consume(stack);
        }

        // 3. 吸引附近物品/经验
        return MagnetUtil.magnetizeItems(this, level, player, usedHand);
    }

    // ==================== 蓄力发射 ====================

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        if (MagnetHandler.hasAnvil(stack)) return UNLIMITED_USE_DURATION;
        return super.getUseDuration(stack, entity);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        if (MagnetHandler.hasAnvil(stack)) return UseAnim.BOW;
        return super.getUseAnimation(stack);
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return MagnetHandler.hasAnvil(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (level.isClientSide) return;
        if (!MagnetHandler.hasAnvil(stack)) return;
        if (!(entity instanceof Player player)) return;
        ResourceLocation anvilId = MagnetHandler.getAnvilId(stack);
        if (anvilId == null) return;

        FlyingAnvilEntity anvil = new FlyingAnvilEntity(level);
        anvil.init(player, anvilId);
        level.addFreshEntity(anvil);
        MagnetHandler.clearAnvilId(stack);
        player.getCooldowns().addCooldown(stack.getItem(), 10);
    }

    // ==================== 其它 ====================

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ModItems.DOGE_STEEL_INGOT.get());
    }
}
