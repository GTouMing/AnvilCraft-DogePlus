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
    dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType doge_plus$getGateType(Level level, BlockPos pos, Direction outputDir);

    /**
     * 计算输出信号
     */
    int doge_plus$calculateOutput(Level level, BlockPos pos, Direction outputDir, DirectionalSignals inputs);
}
