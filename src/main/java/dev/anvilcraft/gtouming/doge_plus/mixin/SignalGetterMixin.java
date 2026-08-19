package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.behavior.ILogicGate;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.util.DirectionsOrder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.SignalGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SignalGetter.class)
public interface SignalGetterMixin {

    /**
     * 完全接管 getSignal（弱信号）。
     */
    @Inject(
            method = "getSignal",
            at = @At("RETURN"),
            cancellable = true
    )
    private void doge_plus$onGetSignal(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Integer> cir
    ) {
        Level level = (Level) this;
        if (level.isClientSide()) return;

        // 检查当前位置是否属于逻辑门方块
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof ILogicGate gate)) return;
        if (!gate.doge_plus$hasLogicGate(level, pos)) return;

        // 计算门逻辑输出
        int gateSignal = doge_plus$calculateGateOutput(level, pos, direction);
        if (gateSignal > 0) {
            cir.setReturnValue(gateSignal);
        }
    }

    /**
     * 完全接管 getDirectSignal（强信号）。
     */
    @Inject(
            method = "getDirectSignal",
            at = @At("RETURN"),
            cancellable = true
    )
    private void doge_plus$onGetDirectSignal(
            BlockPos pos,
            Direction direction,
            CallbackInfoReturnable<Integer> cir
    ) {
        Level level = (Level) this;
        if (level.isClientSide()) return;

        // 检查当前位置是否属于逻辑门方块
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!(block instanceof ILogicGate gate)) return;
        if (!gate.doge_plus$hasLogicGate(level, pos)) return;

        // 强信号与弱信号相同
        int gateSignal = doge_plus$calculateGateOutput(level, pos, direction);
        if (gateSignal > 0) {
            cir.setReturnValue(gateSignal);
        }
    }

    /**
     * 完全接管 hasNeighborSignal。
     */
    @Inject(
            method = "hasNeighborSignal",
            at = @At("RETURN"),
            cancellable = true
    )
    private void doge_plus$onHasNeighborSignal(
            BlockPos pos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        Level level = (Level) this;
        if (level.isClientSide()) return;

        // 检查所有方向是否有信号（包括门逻辑）
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            Block neighborBlock = neighborState.getBlock();

            if (neighborBlock instanceof ILogicGate gate && gate.doge_plus$hasLogicGate(level, neighborPos)) {
                // 检查邻居门是否向当前方向输出信号
                int signal = gate.doge_plus$getSignal(neighborState, level, neighborPos, direction.getOpposite());
                if (signal > 0) {
                    cir.setReturnValue(true);
                    return;
                }
            }

            // 检查普通红石信号
            if (level.getSignal(neighborPos, direction.getOpposite()) > 0) {
                cir.setReturnValue(true);
                return;
            }
        }
    }

    /**
     * 计算门逻辑输出信号。
     *
     * @param level     世界
     * @param pos       门位置
     * @param outputDir 输出方向（当前查询的方向）
     * @return 信号强度 0-15
     */
    @Unique
    private static int doge_plus$calculateGateOutput(Level level, BlockPos pos, Direction outputDir) {
        // 获取该位置的门类型
        ILogicGate.GateType gateType = BlockInlayManager.get(level, pos).getGateType(outputDir);

        // 计算该方向的信号
        return doge_plus$calculateInputSignal(level, pos, outputDir, gateType);
    }

    /**
     * 获取输入信号。
     * 根据门类型决定从哪个方向获取输入。
     */
    @Unique
    private static int doge_plus$calculateInputSignal(Level level, BlockPos pos, Direction outputDir, ILogicGate.GateType gateType) {
        if (gateType == ILogicGate.GateType.NONE) {
            return 0;
        }

        switch (gateType) {
            case NOT_GATE -> {
                // 非门：从反面获取输入
                Direction inputDir = outputDir.getOpposite();
                BlockPos inputPos = pos.relative(inputDir);
                return level.getSignal(inputPos, inputDir.getOpposite());
            }

            case AND_GATE -> {
                // 与门：从第一个匹配的方向与其对向获取输入
                // 查找该方向的门逻辑配置
                Direction inputDir1 = DirectionsOrder.getNextDirection(outputDir);
                Direction inputDir2 = inputDir1.getOpposite();
                BlockPos inputPos1 = pos.relative(inputDir1);
                BlockPos inputPos2 = pos.relative(inputDir2);
                int input1 = level.getSignal(inputPos1, inputDir1.getOpposite());
                int input2 = level.getSignal(inputPos2, inputDir2.getOpposite());
                return Math.min(input1, input2);
            }

            case OR_GATE -> {
                // 或门：从第一个匹配的方向与其对向获取输入
                // 查找该方向的门逻辑配置
                Direction inputDir1 = DirectionsOrder.getNextDirection(outputDir);
                Direction inputDir2 = inputDir1.getOpposite();
                BlockPos inputPos1 = pos.relative(inputDir1);
                BlockPos inputPos2 = pos.relative(inputDir2);
                int input1 = level.getSignal(inputPos1, inputDir1.getOpposite());
                int input2 = level.getSignal(inputPos2, inputDir2.getOpposite());
                return Math.max(input1, input2);
            }

            case RED_STONE -> {
                // 红石：从所有方向获取信号（取最大）
                int maxSignal = 0;
                for (Direction dir : Direction.values()) {
                    BlockPos inputPos = pos.relative(dir);
                    int signal = level.getSignal(inputPos, dir.getOpposite());
                    maxSignal = Math.max(maxSignal, signal);
                }
                return maxSignal;
            }

            default -> {
                return 0;
            }
        }
    }
}