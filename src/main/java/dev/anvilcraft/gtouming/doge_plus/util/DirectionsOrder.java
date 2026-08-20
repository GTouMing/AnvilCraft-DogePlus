package dev.anvilcraft.gtouming.doge_plus.util;

import net.minecraft.core.Direction;

import java.util.List;

public class DirectionsOrder {
    public static List<Direction> getOrder() {
        return  List.of(
                        Direction.EAST,// 0
                        Direction.SOUTH,  // 1
                        Direction.UP,     // 2
                        Direction.WEST,   // 3
                        Direction.NORTH,  // 4
                        Direction.DOWN    // 5
                );
    }

    public static Direction getNextDirection(Direction current) {
        List<Direction> order = getOrder();
        int index = order.indexOf(current);
        if (index == -1) return order.getFirst();
        return order.get((index + 1) % order.size());
    }
}
