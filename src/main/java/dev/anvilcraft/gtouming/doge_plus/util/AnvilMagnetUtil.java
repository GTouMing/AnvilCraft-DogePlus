package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 磁性镶嵌方块与铁砧交互的共享判断与搬移。
 *
 * <p>「吸引态」= 该方块带「磁性」镶嵌 且 未收到红石信号。
 * 吸引态方块会把其下方的铁砧吸到正下方（参考前置 {@code MagnetBlock}）；
 * 收到红石信号（强弱皆可）时进入释放态：放行铁砧下落并按磁级提供等效额外下落高度。</p>
 */
public final class AnvilMagnetUtil {

    private AnvilMagnetUtil() {}

    /** 该位置方块是否处于「吸引态」（带磁性镶嵌且无红石信号）。 */
    public static boolean isAttracts(Level level, BlockPos pos) {
        if (level.isClientSide()) return false;
        if (!hasMagnetic(level, pos)) return false;
        return !level.hasNeighborSignal(pos);
    }

    /** 该位置方块是否带磁性镶嵌，并返回磁级（磁性材料个数）。 */
    public static boolean hasMagnetic(Level level, BlockPos pos) {
        return countMagnetic(level, pos) > 0;
    }

    /** 统计该方块镶嵌中「磁性」性质的数量（多级磁性 → 更强的吸引/加速）。 */
    public static int countMagnetic(Level level, BlockPos pos) {
        BlockInlays inlays = BlockInlayManager.get(level, pos);
        int count = 0;
        for (InlayEntry entry : inlays.inlays()) {
            if (entry.containsAttributes(InlayProperty.MAGNETIC)) count++;
        }
        return count;
    }

    /**
     * 从 {@code magnetPos} 向下逐格扫描，返回范围内第一个铁砧方块的位置；
     * 找不到时返回 {@code magnetPos} 本身（哨兵，调用方需判断不等于起点）。
     */
    public static BlockPos findAnvilBelow(Level level, BlockPos magnetPos, int distance) {
        BlockPos cursor = magnetPos;
        for (int i = 0; i < distance; i++) {
            cursor = cursor.below();
            BlockState s = level.getBlockState(cursor);
            if (s.is(BlockTags.ANVIL)) return cursor;
            if (!s.isAir() && s.getFluidState().isEmpty()) return magnetPos; // 实心挡路
        }
        return magnetPos;
    }

    /**
     * 从 {@code anvilPos} 向上逐格扫描，返回范围内第一个「吸引态」磁块位置；
     * 找不到时返回 {@code anvilPos} 本身（哨兵，调用方需判断不等于起点）。
     */
    public static BlockPos findAttractingMagnetAbove(Level level, BlockPos anvilPos, int distance) {
        BlockPos cursor = anvilPos;
        for (int i = 0; i < distance; i++) {
            cursor = cursor.above();
            if (isAttracts(level, cursor)) return cursor;
            BlockState s = level.getBlockState(cursor);
            if (!s.isAir() && s.getFluidState().isEmpty()) return anvilPos; // 实心挡路
        }
        return anvilPos;
    }

    /**
     * 把铁砧方块从 {@code from} 静默搬到 {@code to}（连同镶嵌数据迁移）。
     * 目标格若有非铁砧方块则破坏掉落；若 {@code from} 已在 {@code to} 则无操作。
     */
    public static void moveAnvilBlock(Level level, BlockPos from, BlockPos to) {
        if (level.isClientSide()) return;
        if (from.equals(to)) return;
        BlockState anvilState = level.getBlockState(from);
        if (!anvilState.is(BlockTags.ANVIL)) return;
        if (level.getBlockState(to).is(BlockTags.ANVIL)) return;

        // 迁移镶嵌数据：原位移除记录 → 目标位写入
        BlockInlays inlays = BlockInlayManager.get(level, from);
        if (!inlays.inlays().isEmpty()) {
            BlockInlayManager.remove(level, from);
            BlockInlayManager.put(level, to, inlays);
        }
        // 静默搬移：不触发 onPlace/neighborChanged 的下落检测
        level.setBlock(from, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(to, anvilState, 2);
    }

    /**
     * 磁块侧：把下方范围内第一个铁砧抬到 {@code magnetPos} 正下方。
     * 找不到铁砧时无操作。用于磁块被放置/更新时。
     */
    public static void attractAnvilToMagnet(Level level, BlockPos magnetPos) {
        if (level.isClientSide()) return;
        if (!isAttracts(level, magnetPos)) return;
        int distance = AnvilCraftDogePlus.CONFIG.magnetAttractsDistance;
        BlockPos anvil = findAnvilBelow(level, magnetPos, distance);
        if (!anvil.equals(magnetPos)) moveAnvilBlock(level, anvil, magnetPos.below());
    }

    /**
     * 铁砧侧：铁砧方块被计划刻唤醒（下落检测）时调用。
     * 若上方存在「吸引态」磁块则把自身搬移到其正下方并返回 true（本次 tick 不应下落）；
     * 否则返回 false（按 vanilla 正常检测下落）。
     */
    public static boolean liftAnvilByMagnetAbove(Level level, BlockPos anvilPos) {
        if (level.isClientSide()) return false;
        int distance = AnvilCraftDogePlus.CONFIG.magnetAttractsDistance;
        BlockPos magnet = findAttractingMagnetAbove(level, anvilPos, distance);
        if (magnet.equals(anvilPos)) return false;
        moveAnvilBlock(level, anvilPos, magnet.below());
        return true;
    }

    /**
     * 磁块邻居变化（红石信号边沿）时统一处理：
     * <ul>
     *   <li>磁块当前收到红石信号（上升沿后，释放态）：让停在 {@code pos.below()} 的铁砧
     *       重新执行下落检测（scheduleTick）；若铁砧下方无支撑则生成 FallingBlockEntity
     *       下落，随后被实体 mixin 施加等效初速。</li>
     *   <li>磁块当前无红石信号（下降沿后，吸引态）：重新向下扫描并吸引铁砧到正下方
     *       （把刚被释放、尚在下落/未落远的铁砧吸回来）。</li>
     * </ul>
     *
     * <p>注：不能用 {@code setBlock(同 state, 3)} 重放——同 state 非 FORCE 时
     * vanilla {@code setBlock} 直接返回，不会触发任何通知。</p>
     */
    public static void onMagnetNeighborChanged(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        if (!hasMagnetic(level, pos)) return;
        if (level.hasNeighborSignal(pos)) {
            // 释放态：让停靠的铁砧重新检测下落
            BlockPos below = pos.below();
            BlockState belowState = level.getBlockState(below);
            if (belowState.is(BlockTags.ANVIL)) {
                // scheduleTick → FallingBlock.tick 检测下方无支撑则下落（此时磁块为释放态，
                // FallingBlockMixin 不会取消，实体 mixin 会施加等效初速）
                level.scheduleTick(below, belowState.getBlock(), 2);
            }
        } else {
            // 吸引态（下降沿）：重新把下方铁砧吸到正下方
            attractAnvilToMagnet(level, pos);
        }
    }
}

