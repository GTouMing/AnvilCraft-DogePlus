package dev.anvilcraft.gtouming.doge_plus.logic;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

import java.util.List;
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
     * @param inputs     各方向输入信号
     * @param outputDir  本门输出方向
     * @param inputFaces 输入面方向列表（INPUT 门标记的面，按 DirectionsOrder 从本门槽位之后查找）
     */
    public int calculate(Map<Direction, Integer> inputs, Direction outputDir, List<Direction> inputFaces) {
        return switch (this) {
            case NONE, INPUT -> 0;
            case NOT_GATE -> {
                if (inputFaces.isEmpty()) yield 0;
                int input = inputs.getOrDefault(inputFaces.getFirst(), 0);
                yield input > 0 ? 0 : 15;
            }
            case AND_GATE -> {
                // 与运算需至少两个输入面，否则不满足条件，永远返回 0
                if (inputFaces.size() < 2) yield 0;
                int min = 15;
                for (Direction face : inputFaces) {
                    min = Math.min(min, inputs.getOrDefault(face, 0));
                }
                yield min;
            }
            case OR_GATE, OUTPUT -> {
                if (inputFaces.isEmpty()) yield 0;
                int max = 0;
                for (Direction face : inputFaces) {
                    max = Math.max(max, inputs.getOrDefault(face, 0));
                }
                yield max;
            }
        };
    }
}