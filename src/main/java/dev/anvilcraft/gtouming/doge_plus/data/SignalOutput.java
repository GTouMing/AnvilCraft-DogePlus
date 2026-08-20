package dev.anvilcraft.gtouming.doge_plus.data;

import net.minecraft.core.Direction;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * 信号输出数据：持有所有方向的信号强度。
 */
public record SignalOutput(Map<Direction, Integer> signals) {


    public static SignalOutput defaults() {
        EnumMap<Direction, Integer> map = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            map.put(direction, 0);
        }
        return new SignalOutput(map);
    }

    public static SignalOutput of(Direction direction, int signal) {
        Map<Direction, Integer> map = new EnumMap<>(Direction.class);
        map.put(direction, Math.clamp(signal, 0, 15));
        return new SignalOutput(map);
    }

    public static SignalOutput of(Map<Direction, Integer> signals) {
        EnumMap<Direction, Integer> map = new EnumMap<>(Direction.class);
        for (Map.Entry<Direction, Integer> entry : signals.entrySet()) {
            map.put(entry.getKey(), Math.clamp(entry.getValue(), 0, 15));
        }
        return new SignalOutput(map);
    }

    public int getSignal(Direction direction) {
        return signals.getOrDefault(direction, 0);
    }

    public SignalOutput withSignal(Direction direction, int signal) {
        EnumMap<Direction, Integer> newSignals = new EnumMap<>(Direction.class);
        newSignals.putAll(signals);
        newSignals.put(direction, Math.clamp(signal, 0, 15));
        return new SignalOutput(newSignals);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SignalOutput that = (SignalOutput) o;
        return Objects.equals(signals, that.signals);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(signals);
    }

    @Override
    public String toString() {
        return signals.toString();
    }
}