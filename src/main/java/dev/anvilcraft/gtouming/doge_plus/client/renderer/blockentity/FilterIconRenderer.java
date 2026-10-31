package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 把「配方过滤」物品画在方块水平侧面上。
 *
 * <p>算法与物流载体过滤图标（{@code InlayCarrierRenderer#renderFilterItem}）一致：用 {@code GUI}
 * 显示变换，普通物品沿图标法线压扁成平面；自带 {@code BlockEntityWithoutLevelRenderer} 的物品
 * （最关键的是 {@code anvilcraft:filter}）交给它自己的自定义渲染，才能画出轮播物品与拒绝屏障。</p>
 *
 * <p>光照与载体一样：不走物品渲染管线的全局光照 uniform，而是逐面按世界朝向取
 * {@link Level#getShade} 烘进顶点色。</p>
 */
public final class FilterIconRenderer {

    /** 过滤物品图标的缩放（相对一格）。 */
    private static final float FILTER_ITEM_SCALE = 0.3F;

    /** 大小归一化之后再缩 1/3。 */
    private static final float FILTER_ITEM_SHRINK = 1.0F / 3.0F;

    /** 沿图标法线压扁的比例：把 GUI 的三维图标压成平面。 */
    private static final float FILTER_ITEM_FLATTEN = 0.05F;

    /** 贴面偏移：略高于方块表面，避免与方块模型 z-fighting。 */
    private static final float SURFACE_OFFSET = 0.502F;

    /** 图标在侧面上的竖直中心（13/16 格高，落在方块顶部侧沿上）。 */
    private static final float ICON_CENTER_Y = 13.0F / 16.0F;

    /** 四角选择框：半边长、每段臂长、线宽（半个方块像素）、离面偏移（局部 +Z 朝外）。 */
    private static final float BRACKET_HALF_SIZE = 1.5F / 16.0F;
    private static final float BRACKET_ARM = 1.0F / 16.0F;
    private static final float BRACKET_THICKNESS = 0.5F / 16.0F;
    private static final float BRACKET_Z = 0.001F;

    /** 选择框颜色。 */
    private static final int BRACKET_R = 230;
    private static final int BRACKET_G = 245;
    private static final int BRACKET_B = 255;
    private static final int BRACKET_A = 220;

    private static final RenderType PART_RENDER_TYPE = RenderType.cutout();
    private static final RandomSource RANDOM = RandomSource.create();

    private FilterIconRenderer() {
    }

    /** 在方块东/南/西/北四个水平侧面上各画一份过滤物品图标；四角选择框只在准星选中该方块时画。 */
    public static void renderOnHorizontalSides(
            PoseStack poseStack,
            MultiBufferSource buffer,
            @Nullable Level level,
            BlockPos pos,
            ItemStack filter,
            int packedLight,
            int packedOverlay) {
        if (filter.isEmpty() || level == null) return;
        boolean selected = isBlockSelected(pos);
        for (Direction face : Direction.Plane.HORIZONTAL) {
            renderOnFace(poseStack, buffer, level, face, filter, selected, packedLight, packedOverlay);
        }
    }

    /** 在一个侧面上画过滤物品图标，图标平面朝外。 */
    public static void renderOnFace(
            PoseStack poseStack,
            MultiBufferSource buffer,
            Level level,
            Direction face,
            ItemStack filter,
            boolean showBrackets,
            int packedLight,
            int packedOverlay) {
        if (filter.isEmpty()) return;

        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(filter, level, null, 0);
        Vector3f guiScale = model.getTransforms().getTransform(ItemDisplayContext.GUI).scale;
        // 多方块巨构（如加速环）的物品模型是整座结构，额外缩 1/3 才能放进过滤图标。
        float multiPartShrink = filter.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof IMultiPartBlock ? 1.0F / 3.0F : 1.0F;
        float scale = FILTER_ITEM_SCALE * FILTER_ITEM_SHRINK * multiPartShrink
                / Math.max(guiScale.x(), 0.01F);

        // 让图标正面朝向该面：right = up × normal，(right, up, normal) 构成右手正交基。
        Vector3f normal = axis(face);
        Vector3f up = new Vector3f(0.0F, 1.0F, 0.0F);
        Vector3f right = new Vector3f(up).cross(normal);
        Quaternionf rotation = new Matrix3f(
                right.x(), right.y(), right.z(),
                up.x(), up.y(), up.z(),
                normal.x(), normal.y(), normal.z()
        ).getNormalizedRotation(new Quaternionf());

        poseStack.pushPose();
        poseStack.translate(
                0.5F + normal.x() * SURFACE_OFFSET,
                ICON_CENTER_Y + normal.y() * SURFACE_OFFSET,
                0.5F + normal.z() * SURFACE_OFFSET);
        Vector3f center = poseStack.last().pose().getTranslation(new Vector3f());
        poseStack.mulPose(rotation);
        // 四角选择框画在面的局部平面里（块坐标），先于物品的 GUI 缩放/显示变换；只在选中方块时显示。
        if (showBrackets) renderCornerBrackets(poseStack, buffer);
        poseStack.scale(scale, scale, scale);
        BakedModel icon = ClientHooks.handleCameraTransforms(
                poseStack, model, ItemDisplayContext.GUI, false);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        if (icon.isCustomRenderer()) {
            IClientItemExtensions.of(filter).getCustomRenderer().renderByItem(
                    filter, ItemDisplayContext.GUI, flattenedStack(poseStack, center, face),
                    buffer, packedLight, packedOverlay);
        } else {
            VertexConsumer consumer = buffer.getBuffer(PART_RENDER_TYPE);
            renderShaded(flattened(poseStack, center, face), consumer, icon, packedLight, packedOverlay, level);
        }
        poseStack.popPose();
    }

    /**
     * 在图标外的四个角画 L 形选择框。
     *
     * <p>当前姿态已平移面中心并按面朝向旋转，因此局部 XY 平面即该侧面、+Z 朝外。线宽要按世界空间
     * 取半个方块像素，而 {@link RenderType#lines()} 的线宽固定是 1 屏幕像素，所以这里用四边形
     * （{@link RenderType#debugQuads()}，纯颜色、无贴图）拼出每段线，厚度在块坐标里精确可控。</p>
     */
    private static void renderCornerBrackets(PoseStack poseStack, MultiBufferSource buffer) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();
        float half = BRACKET_HALF_SIZE;
        float arm = BRACKET_ARM;
        float halfThickness = BRACKET_THICKNESS / 2.0F;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                float x = sx * half;
                float y = sy * half;
                float innerX = sx * (half - arm);
                float innerY = sy * (half - arm);
                // 水平臂：X 从角点向内，Y 方向取线宽。
                quad(consumer, pose,
                        Math.min(x, innerX), y - halfThickness,
                        Math.max(x, innerX), y + halfThickness);
                // 竖直臂：Y 从角点向内，X 方向取线宽。
                quad(consumer, pose,
                        x - halfThickness, Math.min(y, innerY),
                        x + halfThickness, Math.max(y, innerY));
            }
        }
    }

    /** 以 (x0,y0)-(x1,y1) 为对角、位于面局部平面上的实心四边形（顶点顺时针朝向 +Z）。 */
    private static void quad(VertexConsumer consumer, PoseStack.Pose pose,
                             float x0, float y0, float x1, float y1) {
        consumer.addVertex(pose, x0, y0, BRACKET_Z)
                .setColor(BRACKET_R, BRACKET_G, BRACKET_B, BRACKET_A);
        consumer.addVertex(pose, x1, y0, BRACKET_Z)
                .setColor(BRACKET_R, BRACKET_G, BRACKET_B, BRACKET_A);
        consumer.addVertex(pose, x1, y1, BRACKET_Z)
                .setColor(BRACKET_R, BRACKET_G, BRACKET_B, BRACKET_A);
        consumer.addVertex(pose, x0, y1, BRACKET_Z)
                .setColor(BRACKET_R, BRACKET_G, BRACKET_B, BRACKET_A);
    }

    /** 准星是否正选中该方块（与原生方块描边一致：命中的是方块且坐标相符）。 */
    private static boolean isBlockSelected(BlockPos pos) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit == null || hit.getType() != HitResult.Type.BLOCK) return false;
        return hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos);
    }

    /** 把当前姿态沿 {@code normal} 压扁成一张平面，返回用于渲染的姿态。 */
    private static PoseStack.Pose flattened(PoseStack poseStack, Vector3f center, Direction normal) {
        Matrix4f squash = new Matrix4f()
                .translate(center.x, center.y, center.z)
                .scale(
                        normal.getStepX() != 0 ? FILTER_ITEM_FLATTEN : 1.0F,
                        normal.getStepY() != 0 ? FILTER_ITEM_FLATTEN : 1.0F,
                        normal.getStepZ() != 0 ? FILTER_ITEM_FLATTEN : 1.0F)
                .translate(-center.x, -center.y, -center.z);
        PoseStack flat = new PoseStack();
        flat.last().pose().set(squash).mul(poseStack.last().pose());
        flat.last().normal().set(poseStack.last().normal());
        return flat.last();
    }

    /** 与 {@link #flattened} 相同的压扁姿态，包成新的 PoseStack，供自带 BEWLR 的物品也按压扁后的姿态渲染。 */
    private static PoseStack flattenedStack(PoseStack poseStack, Vector3f center, Direction normal) {
        PoseStack.Pose flat = flattened(poseStack, center, normal);
        PoseStack stack = new PoseStack();
        stack.last().pose().set(flat.pose());
        stack.last().normal().set(flat.normal());
        return stack;
    }

    private static void renderShaded(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            BakedModel model,
            int packedLight,
            int packedOverlay,
            Level level) {
        for (Direction cull : Direction.values()) {
            RANDOM.setSeed(42L);
            renderQuads(pose, consumer, model.getQuads(null, cull, RANDOM), packedLight, packedOverlay, level);
        }
        RANDOM.setSeed(42L);
        renderQuads(pose, consumer, model.getQuads(null, null, RANDOM), packedLight, packedOverlay, level);
    }

    private static void renderQuads(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            int packedLight,
            int packedOverlay,
            Level level) {
        for (BakedQuad quad : quads) {
            Direction local = quad.getDirection();
            Vector3f n = new Vector3f(local.getStepX(), local.getStepY(), local.getStepZ());
            n.mul(pose.normal());
            Direction worldDir = Direction.getNearest(n.x(), n.y(), n.z());
            float shade = level.getShade(worldDir, quad.isShade());
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f,
                    bakedLight(quad, packedLight), packedOverlay);
        }
    }

    /** quad 顶点里烘的 {@code neoforge_data} 光照（UV2 槽）；未写入时回退到方块自身光照。 */
    private static int bakedLight(BakedQuad quad, int fallback) {
        int[] vertices = quad.getVertices();
        return vertices.length > 6 && vertices[6] != 0 ? vertices[6] : fallback;
    }

    private static Vector3f axis(Direction face) {
        return new Vector3f(face.getStepX(), face.getStepY(), face.getStepZ());
    }
}
