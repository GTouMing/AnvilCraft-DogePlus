package dev.anvilcraft.gtouming.doge_plus.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public interface IMultiPartBlock {
    default BlockPos doge_plus$getMainPos(BlockPos pos, BlockState state) {
        return pos;
    }
}
