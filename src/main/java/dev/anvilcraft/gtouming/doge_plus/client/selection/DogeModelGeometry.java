package dev.anvilcraft.gtouming.doge_plus.client.selection;

import dev.anvilcraft.lib.v2.cube.client.model.CubeModelDecoder;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.resources.model.BlockModelRotation;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 把方块模型元素解码成 anvillib 的凸体（{@link ConvexShape} 可任意旋转，所以 45° 斜面是真斜面）。
 *
 * <p>做法与前置 {@code ModelCubeGeometry} 一致：先把局部坐标放大 {@link #SCALE} 倍以保留薄面，
 * 再由 {@code SelectionPart} 的 {@code 1 / SCALE} 变换还原成方块坐标。</p>
 */
public final class DogeModelGeometry {
    public static final float SCALE = 128.0F;
    private static final float MIN_THICKNESS = 0.0016F;

    private DogeModelGeometry() {
    }

    public static List<ConvexShape> decode(List<BlockElement> elements) {
        if (elements.isEmpty()) return List.of();
        boolean hasVolume = elements.stream().anyMatch(element -> !isPlane(element));
        List<BlockElement> scaled = new ArrayList<>(elements.size());
        for (BlockElement element : elements) {
            // 与体积件混用的纯平面（特效面、指针等）不参与选区，避免多出无意义的薄片。
            if (hasVolume && isPlane(element)) continue;
            Vector3f from = new Vector3f(element.from);
            Vector3f to = new Vector3f(element.to);
            for (int axis = 0; axis < 3; axis++) {
                if (Math.abs(to.get(axis) - from.get(axis)) < MIN_THICKNESS) {
                    float center = (to.get(axis) + from.get(axis)) * 0.5F;
                    from.setComponent(axis, center - MIN_THICKNESS * 0.5F);
                    to.setComponent(axis, center + MIN_THICKNESS * 0.5F);
                }
            }
            BlockElementRotation rotation = element.rotation;
            if (rotation != null) {
                rotation = new BlockElementRotation(
                    new Vector3f(rotation.origin()).mul(SCALE), rotation.axis(), rotation.angle(), rotation.rescale());
            }
            scaled.add(new BlockElement(from.mul(SCALE), to.mul(SCALE), element.faces, rotation, element.shade));
        }
        return CubeModelDecoder.decode(scaled, BlockModelRotation.X0_Y0);
    }

    private static boolean isPlane(BlockElement element) {
        return element.from.x == element.to.x || element.from.y == element.to.y || element.from.z == element.to.z;
    }
}
