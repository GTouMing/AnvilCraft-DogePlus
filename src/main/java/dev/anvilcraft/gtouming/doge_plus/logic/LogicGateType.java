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
    INPUT;

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }
    public static final Codec<LogicGateType> CODEC = StringRepresentable.fromEnum(LogicGateType::values);

    public static final StreamCodec<ByteBuf, LogicGateType> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /**
     * 计算输出。
     *
     * @param outputDir 输出面方向
     * @param inputs    输入面输入信号（仅含标记为 {@link #INPUT} 的方向）
     */
    public int calculate(Direction outputDir, Map<Direction, Integer> inputs) {
        return switch (this) {
            case NONE, INPUT -> 0;
            case NOT_GATE -> {
                // 非门：输出面（outputDir）的输入来自其反面（outputDir.getOpposite()）
                Direction inputDir = outputDir.getOpposite();
                if (!inputs.containsKey(inputDir)) yield 0;
                int input = inputs.get(inputDir);
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
            case OR_GATE, OUTPUT -> {
                if (inputs.isEmpty()) yield 0;
                int max = 0;
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    max = Math.max(max, face.getValue());
                }
                yield max;
            }
        };
    }
}