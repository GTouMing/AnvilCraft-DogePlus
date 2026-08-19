package dev.anvilcraft.gtouming.doge_plus.api.behavior;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 红石逻辑门接口。
 * 实现此接口的方块可以自定义红石信号行为。
 */
public interface ILogicGate {
    /**
     * 通知红石信号变化。
     */
    default void notifyNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        // 通知所有邻居
        level.updateNeighborsAt(pos, (Block) this);
    }

    /**
     * 通知红石信号变化（排除指定方向）。
     */
    default void notifyNeighborsExcept(Level level, BlockPos pos, Direction direction) {
        if (level.isClientSide()) return;
        level.updateNeighborsAtExceptFromFacing(pos, (Block) this, direction);
    }

    /**
     * 检查是否有邻居需要更新信号。
     */
    default void checkNeighborSignals(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        Block block = (Block) this;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            level.neighborChanged(neighborPos, block, pos);
        }
    }

    /**
     * 获取弱信号（可被红石粉读取）。
     */
    default int doge_plus$getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    /**
     * 获取强信号（可直接连接红石粉）。
     */
    default int doge_plus$getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    /**
     * 检查方块是否有红石逻辑门属性。
     */
    default boolean doge_plus$hasLogicGate(Level level, BlockPos pos) {
        return false;
    }

    enum GateType {
        NONE,
        NOT_GATE,
        AND_GATE,
        OR_GATE,
        RED_STONE;

        public static final StreamCodec<ByteBuf, GateType> STREAM_CODEC = StreamCodec.of(
                (buf, value) -> buf.writeInt(value.ordinal()),
                buf -> GateType.values()[buf.readInt()]
        );
    }
}