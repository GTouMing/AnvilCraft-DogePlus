package dev.anvilcraft.gtouming.doge_plus.logic;

import net.minecraft.core.Direction;

import java.util.HashMap;
import java.util.Map;

/**
 * 压缩存储 6 个方向的信号 (每个方向 4 bits)
 */
public class DirectionalSignals {
    private int packed;  // 24 bits: 6 * 4

    int getSignal(Direction direction) {
        int shift = direction.ordinal() * 4;
        return (packed >> shift) & 0xF;
    }

    void setSignal(Direction direction, int signal) {
        int shift = direction.ordinal() * 4;
        packed = (packed & ~(0xF << shift)) | ((signal & 0xF) << shift);
    }

    public Map<Direction, Integer> toMap() {
        Map<Direction, Integer> map = new HashMap<>();
        for (Direction dir : Direction.values()) {
            map.put(dir, getSignal(dir));
        }
        return map;
    }

    boolean isEmpty() {
        return packed == 0;
    }

    int getPacked() {return packed;}

    void setPacked(int packed) {
        this.packed = packed;
    }
}
