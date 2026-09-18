package dev.anvilcraft.gtouming.doge_plus.logic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 逻辑门接口
 */
public interface ILogicGate {
    /**
     * 获取门类型
     */
    LogicGateType doge_plus$getGateType(Level level, BlockPos pos, Direction outputDir);

    /**
     * 获取该面逻辑门的设定值（0-15；仅输入门 / 输出门有意义，其余门忽略）。
     */
    int doge_plus$getValue(Level level, BlockPos pos, Direction outputDir);
}
