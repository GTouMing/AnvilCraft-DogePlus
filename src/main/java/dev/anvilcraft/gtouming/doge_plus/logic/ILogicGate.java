package dev.anvilcraft.gtouming.doge_plus.logic;

import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * 逻辑门接口
 */
public interface ILogicGate {
    /**
     * 获取该面的方向性属性
     */
    FaceMode doge_plus$getGateType(Level level, BlockPos pos, Direction outputDir);

    /**
     * 获取该面逻辑门的设定值（0-15；仅输入门 / 输出门有意义，其余门忽略）。
     */
    int doge_plus$getValue(Level level, BlockPos pos, Direction outputDir);
}
