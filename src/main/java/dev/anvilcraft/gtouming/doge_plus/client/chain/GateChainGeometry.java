package dev.anvilcraft.gtouming.doge_plus.client.chain;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 门链预览的纯几何推导：由链首、已固定的拐点与当前准星算出相邻的方块序列。
 *
 * <p>链按拐点分段，每段以最新拐点（没有拐点时是链首）为锚点，按以下优先级确定本段：</p>
 * <ol>
 *   <li>准星命中方块：以「命中面外侧那一格」（要连到的那一格，等于原版放置位）为目标，
 *       方向取「锚点 → 该格」偏移量中绝对值最大的轴，长度取该分量的大小；</li>
 *   <li>准星未命中：把视线延长「最大门链长度 − 已存在的预览门链长度」，延长线命中方块时仍按第 1 条取；</li>
 *   <li>延长线仍未命中：看它与锚点 6 向射线是否相交，取锚点到最近交点的轴向坐标；</li>
 *   <li>否则取视线远端（方向取视线最贴近的轴，长度取视线远端在该轴上的投影）。</li>
 * </ol>
 *
 * <p>主轴并列时取与命中面法线更同向的轴，而不是按视线方向取舍；被点击方块所在的那条轴一律不参与
 * 走位（朝它第一步必然撞上那个方块）。</p>
 */
public final class GateChainGeometry {

    /** 判定两个分量是否并列的容差。 */
    private static final double TIE_EPSILON = 1.0E-6;

    /** 判定「延长线与某条轴向射线相交」的距离容差（格）。 */
    private static final double INTERSECT_TOLERANCE = 1.0;

    private GateChainGeometry() {
    }

    /** 沿一条线段做方块射线追踪，返回第一个命中的结果（方块 + 命中面）；未命中返回 {@code null}。 */
    @FunctionalInterface
    public interface RayCaster {
        @Nullable
        BlockHitResult cast(Vec3 from, Vec3 to);
    }

    /**
     * 某一格能否纳入链。
     *
     * <p>带上前一格与走来的方位，是因为「能不能收」有时取决于两格之间的关系而不只是这一格本身：
     * 移除模式要求两格确实按信号方向接着，而放置模式只看这一格可否替换。</p>
     *
     * @param from 链里上一格；链首（{@link #build} 的 {@code start}）为 {@code null}
     * @param step 从 {@code from} 走到 {@code next} 的方位；链首为 {@code null}
     * @param next 待判定的位置
     */
    @FunctionalInterface
    public interface Candidate {
        boolean test(@Nullable BlockPos from, @Nullable Direction step, BlockPos next);
    }

    /**
     * 瞄准信息。
     *
     * @param crosshair 准星（默认触及距离）的命中结果（方块 + 命中面）；未命中为 {@code null}
     * @param eye       射线起点（眼位）
     * @param look      视线方向（单位向量）
     * @param raycaster 用于「延长视线」后再次追踪的射线追踪器
     */
    public record Aim(@Nullable BlockHitResult crosshair, Vec3 eye, Vec3 look, RayCaster raycaster) {
    }

    /**
     * @param start      链首方块
     * @param waypoints  已固定的拐点（按点击顺序）
     * @param clickedPos 被点击的方块；它相对锚点的那条轴不参与走位（不相邻时无影响）
     * @param aim        当前瞄准信息
     * @param maxSteps   单段最大延伸格数
     * @param maxLength  整条链的最大方块数
     * @param accept     某一格能否纳入链（放置：可替换；移除：逻辑载体且与上一格按信号方向接着）
     */
    public static List<BlockPos> build(
            BlockPos start,
            List<BlockPos> waypoints,
            BlockPos clickedPos,
            Aim aim,
            int maxSteps,
            int maxLength,
            Candidate accept) {
        List<BlockPos> chain = new ArrayList<>();
        if (!accept.test(null, null, start)) return chain;
        chain.add(start);
        // 整条链共用一个去重集合：任何一段都不许走回链上已有的格子。放置时服务端的 sanitize 与
        // 移除时的逐格校验都会在重复处截断，预览在这里停住才能与实际生效的那一段一致。
        Set<BlockPos> seen = new HashSet<>();
        seen.add(start);

        for (BlockPos waypoint : waypoints) {
            walk(chain, seen, chain.getLast(), waypoint, maxLength, accept, null, null);
            if (chain.size() >= maxLength) return chain;
        }

        BlockPos anchor = chain.getLast();
        Direction blocked = directionBetween(anchor, clickedPos);
        extend(chain, seen, anchor, blocked, aim, maxSteps, maxLength, accept);
        return chain;
    }

    /** 本段：按上面的优先级走出去，命中方块时再补足到命中面外侧那一格。 */
    private static void extend(
            List<BlockPos> chain,
            Set<BlockPos> seen,
            BlockPos anchor,
            @Nullable Direction blocked,
            Aim aim,
            int maxSteps,
            int maxLength,
            Candidate accept) {
        BlockHitResult hit = aim.crosshair();
        Vec3 far = null;
        if (hit == null) {
            far = aim.eye().add(aim.look().scale(Math.max(1, maxLength - chain.size())));
            hit = aim.raycaster().cast(aim.eye(), far);
        }

        if (hit != null) {
            Segment segment = towardHit(anchor, hit, blocked, maxSteps);
            if (segment.length() > 0) {
                walk(chain, seen, anchor, anchor.relative(segment.direction(), segment.length()), maxLength, accept, blocked, hit.getDirection());
            }
            return;
        }

        // 4. 延长线与锚点 6 向射线的最近交点；否则第 5 条：视线远端。
        Segment crossing = towardNearestAxisCrossing(anchor, aim.eye(), far, maxSteps);
        Segment segment = crossing != null ? crossing : towardFar(anchor, aim.look(), far, maxSteps);
        if (segment.length() > 0) {
            walk(chain, seen, anchor, anchor.relative(segment.direction(), segment.length()), maxLength, accept, blocked, null);
        }
    }

    /** 锚点 → 目标格（命中面外侧那一格）：方向取偏移量主轴，长度取该分量的大小。 */
    private static Segment towardHit(
            BlockPos anchor, BlockHitResult hit, @Nullable Direction blocked, int maxSteps) {
        Direction hitFace = hit.getDirection();
        Vec3 offset = Vec3.atCenterOf(hit.getBlockPos().relative(hitFace)).subtract(Vec3.atCenterOf(anchor));
        Direction direction = dominantAxis(offset, hitFace, blocked);
        return new Segment(direction, clampLength(offset.dot(axis(direction)), maxSteps));
    }

    /** 锚点 → 视线远端：方向取视线最贴近的轴，长度取视线远端在该轴上的投影。 */
    private static Segment towardFar(BlockPos anchor, Vec3 look, Vec3 far, int maxSteps) {
        Direction direction = Direction.getNearest(look.x, look.y, look.z);
        double extent = far.subtract(Vec3.atCenterOf(anchor)).dot(axis(direction));
        return new Segment(direction, clampLength(extent, maxSteps));
    }

    /** 延长线与锚点 6 向射线的交点：取最近的一个（轴向坐标最小者），其坐标即本段长度。 */
    @Nullable
    private static Segment towardNearestAxisCrossing(BlockPos anchor, Vec3 eye, Vec3 far, int maxSteps) {
        Vec3 anchorCenter = Vec3.atCenterOf(anchor);
        Vec3 sight = far.subtract(eye);
        double sightLength = sight.length();
        if (sightLength < TIE_EPSILON) return null;
        Vec3 sightDirection = sight.scale(1.0 / sightLength);
        Vec3 between = eye.subtract(anchorCenter);

        Direction best = null;
        double bestExtent = Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            Vec3 axis = axis(direction);
            double alignment = sightDirection.dot(axis);
            double denominator = 1.0 - alignment * alignment;
            // 与视线近乎平行时交点不确定，交给第 5 条处理。
            if (denominator < TIE_EPSILON) continue;

            // 锚点轴向射线与延长视线的最近点，取其轴向坐标。
            double extent = (between.dot(axis) - alignment * between.dot(sightDirection)) / denominator;
            if (extent <= 0.0) continue;

            Vec3 onAxis = anchorCenter.add(axis.scale(extent));
            double onSight = Math.clamp(onAxis.subtract(eye).dot(sightDirection), 0.0, sightLength);
            double distance = onAxis.distanceTo(eye.add(sightDirection.scale(onSight)));
            if (distance > INTERSECT_TOLERANCE) continue;

            if (extent < bestExtent) {
                bestExtent = extent;
                best = direction;
            }
        }
        return best == null ? null : new Segment(best, clampLength(bestExtent, maxSteps));
    }

    /**
     * 偏移量的主轴：分量绝对值最大者；并列时取<b>与命中面法线更同向</b>的一条，而不是按视线取舍。
     *
     * <p>命中面法线朝玩家，链条正是从锚点一侧接近该面，和它同向的轴才顺路；像「锚点 (0,1,0)、
     * 命中 (1,0,0) 的顶面」这种 X/Y 并列的情形，按视线取舍会不稳定地落到被 (0,0,0) 挡住的 Y 轴。</p>
     *
     * <p>{@code blocked}（被点击方块所在的方位）根本不参与选择：朝那个方向第一步必然撞上被点击方块。</p>
     */
    private static Direction dominantAxis(Vec3 offset, @Nullable Direction hitFace, @Nullable Direction blocked) {
        Direction best = null;
        double bestExtent = 0.0;
        double bestAlignment = -Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (direction == blocked) continue;
            double extent = offset.dot(axis(direction));
            if (extent <= 0.0) continue; // 只取沿该轴正向的分量，方向与偏移一致
            double alignment = hitFace == null ? 0.0 : axis(direction).dot(axis(hitFace));
            if (extent > bestExtent + TIE_EPSILON
                    || (Math.abs(extent - bestExtent) <= TIE_EPSILON && alignment > bestAlignment)) {
                bestExtent = extent;
                bestAlignment = alignment;
                best = direction;
            }
        }
        // 偏移量为零时（无正向分量）方向不影响长度，退回命中面或朝下即可。
        return best != null ? best : (hitFace != null ? hitFace : Direction.DOWN);
    }

    /** {@code from} 到 {@code to} 相差一格时的方位；不相邻返回 {@code null}。 */
    @Nullable
    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (Direction direction : Direction.values()) {
            if (from.relative(direction).equals(to)) return direction;
        }
        return null;
    }

    /**
     * 下一步走向：剩余偏移绝对值最大的轴；被点击方块所在的轴排除，并列时优先与命中面法线更同向的轴。
     *
     * @param faceAxis 命中面法线；无命中面（看向天空等）时为 {@code null}
     */
    @Nullable
    private static Direction nextStep(BlockPos from, BlockPos to, @Nullable Direction blocked, @Nullable Direction faceAxis) {
        Direction best = null;
        int bestDelta = 0;
        double bestAlignment = -Double.MAX_VALUE;
        for (Direction direction : Direction.values()) {
            if (direction == blocked) continue;
            int delta = switch (direction.getAxis()) {
                case X -> (to.getX() - from.getX()) * direction.getStepX();
                case Y -> (to.getY() - from.getY()) * direction.getStepY();
                case Z -> (to.getZ() - from.getZ()) * direction.getStepZ();
            };
            if (delta <= 0) continue;
            double alignment = faceAxis == null ? 0.0 : axis(direction).dot(axis(faceAxis));
            if (delta > bestDelta || (delta == bestDelta && alignment > bestAlignment)) {
                bestDelta = delta;
                bestAlignment = alignment;
                best = direction;
            }
        }
        return best;
    }

    private static int clampLength(double extent, int maxSteps) {
        return (int) Math.clamp(Math.round(extent), 0L, (long) maxSteps);
    }

    private static Vec3 axis(Direction direction) {
        return new Vec3(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    /** 一段门链：方向 + 长度（格）。 */
    private record Segment(Direction direction, int length) {
    }

    /**
     * 从 {@code from} 逐格走向 {@code to}（每次只推进一个轴，保证相邻）。
     *
     * <p>遇到「已经在链上」的格子就停：多段走位（拐点、回拖）可能把某一段折回前面走过的格子上，
     * 而服务端会在这里截断，预览必须停在同一处。随后再看不接受的位置，同样停住。</p>
     */
    private static void walk(
            List<BlockPos> out,
            Set<BlockPos> seen,
            BlockPos from,
            BlockPos to,
            int limit,
            Candidate accept,
            @Nullable Direction blocked,
            @Nullable Direction faceAxis) {
        int x = from.getX();
        int y = from.getY();
        int z = from.getZ();
        while (out.size() < limit && (x != to.getX() || y != to.getY() || z != to.getZ())) {
            BlockPos current = new BlockPos(x, y, z);
            Direction step = nextStep(current, to, blocked, faceAxis);
            if (step == null) return;
            x += step.getStepX();
            y += step.getStepY();
            z += step.getStepZ();
            BlockPos next = new BlockPos(x, y, z);
            if (seen.contains(next)) return;
            if (!accept.test(current, step, next)) return;
            seen.add(next);
            out.add(next);
        }
    }
}
