package dev.anvilcraft.gtouming.doge_plus.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 接受铁砧砸击的加工台方块实体。
 *
 * <p>事件层只做一次 {@code instanceof AnvilStrikeProcessor}，不关心具体是镶嵌台还是镶合台。</p>
 */
public interface AnvilStrikeProcessor {

    /**
     * 铁砧落在台面上方时调用。
     *
     * @param fallDistance 铁砧下落距离（镶嵌台用它选替换 / 取下的镶孔）
     * @return 是否实际处理了物品
     */
    boolean onAnvilStrike(Level level, BlockPos pos, float fallDistance);
}
