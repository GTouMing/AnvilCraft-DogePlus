package dev.anvilcraft.gtouming.doge_plus.logic;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.Map;

/**
 * 逻辑门类型
 */
public enum LogicGateType implements StringRepresentable {
    /** 无 */
    NONE,

    /** 非门：输入 > 0 ? 0 : 15 */
    NOT_GATE,

    /** 与门：min(输入1, 输入2) */
    AND_GATE,

    /** 或门：max(输入1, 输入2) */
    OR_GATE,

    /** 输出：最大输入值 */
    OUTPUT,

    /** 输入：来自红石信号或逻辑门 */
    INPUT,

    /** 计数门：累计输入脉冲次数，达到设定次数时输出 1 tick 的 15 并清零。 */
    COUNTER_GATE,

    /** 锁存门：收到输入则记录并输出该信号，再收到则清除记录、停止输出。 */
    LATCH_GATE,

    /** 延时门：收到输入起输出该信号，持续设定 tick 数后停止。 */
    DELAY_GATE;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    /** 该门是否有内部时序状态：输出由每 tick 驱动写入，而非输入面的纯函数。 */
    public boolean isStateful() {
        return this == COUNTER_GATE || this == LATCH_GATE || this == DELAY_GATE;
    }

    /** 该门是否消耗输入面（与门 / 或门 / 输出门 / 三种有状态门）。 */
    public boolean consumesInputFace() {
        return this == AND_GATE || this == OR_GATE || this == OUTPUT || isStateful();
    }

    /** 该门的设定值是否可由玩家调整（输入门 / 输出门 / 三种有状态门）。 */
    public boolean isSettable() {
        return this == INPUT || this == OUTPUT || isStateful();
    }
    public static final Codec<LogicGateType> CODEC = StringRepresentable.fromEnum(LogicGateType::values);

    public static final StreamCodec<ByteBuf, LogicGateType> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /**
     * 计算输出。
     *
     * @param outputDir 输出面方向
     * @param inputs    输入面输入信号（仅含标记为 {@link #INPUT} 的方向）
     * @param value     该面逻辑门的设定值（仅 {@link #OUTPUT} 使用：输出取 max 输入与设定值的较小者）
     */
    public int calculate(Direction outputDir, Map<Direction, Integer> inputs, int value) {
        return switch (this) {
            // 有状态门的输出由 LevelNetworks 的每 tick 驱动写入，不经过本方法。
            case NONE, INPUT, COUNTER_GATE, LATCH_GATE, DELAY_GATE -> 0;
            case NOT_GATE -> {
                // 非门：输出面（outputDir）的输入来自其反面（outputDir.getOpposite()）。
                // 该面不是 INPUT 面（即全局无输入）时视为输入 0，输出 15 —— 便于单独使用非门。
                Direction inputDir = outputDir.getOpposite();
                int input = inputs.getOrDefault(inputDir, 0);
                yield input > 0 ? 0 : 15;
            }
            case AND_GATE -> {
                // 与运算需至少两个输入面，否则不满足条件，永远返回 0
                if (inputs.size() < 2) yield 0;
                int min = 15;
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    min = Math.min(min, face.getValue());
                }
                yield min;
            }
            case OR_GATE -> {
                if (inputs.isEmpty()) yield 0;
                int max = 0;
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    max = Math.max(max, face.getValue());
                }
                yield max;
            }
            case OUTPUT -> {
                // 输出门：取最大输入，并按设定值取小截断
                if (inputs.isEmpty()) yield 0;
                int max = 0;
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    max = Math.max(max, face.getValue());
                }
                yield Math.min(max, value);
            }
        };
    }
}