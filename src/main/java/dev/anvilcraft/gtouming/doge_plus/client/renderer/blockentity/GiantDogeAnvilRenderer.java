package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.entity.GiantDogeAnvilBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class GiantDogeAnvilRenderer implements BlockEntityRenderer<GiantDogeAnvilBlockEntity> {
    public static final ModelResourceLocation RED = ModelResourceLocation.standalone(
            AnvilCraftDogePlus.of("block/anvil_core")
    );
    public static final ModelResourceLocation GREEN = ModelResourceLocation.standalone(
            AnvilCraftDogePlus.of("block/anvil_core1")
    );
    public static final ModelResourceLocation BLUE = ModelResourceLocation.standalone(
            AnvilCraftDogePlus.of("block/anvil_core2")
    );

    public GiantDogeAnvilRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GiantDogeAnvilBlockEntity blockEntity,
                       float partialTick,
                       PoseStack poseStack,
                       MultiBufferSource bufferSource,
                       int packedLight,
                       int packedOverlay) {
        float rotation = blockEntity.getRotationAngle();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        // 整体偏移 (模型位置调整)
        poseStack.translate(0F, -1.3F, 0F);

        // 绕 X 轴旋转 (红色)
        renderRotatedModel(poseStack, vertexConsumer, packedOverlay, RED, Axis.XP, rotation);

        // 绕 Y 轴旋转 (绿色)
        renderRotatedModel(poseStack, vertexConsumer, packedOverlay, GREEN, Axis.YP, rotation);

        // 绕 Z 轴旋转 (蓝色)
        renderRotatedModel(poseStack, vertexConsumer, packedOverlay, BLUE, Axis.ZP, rotation);

        poseStack.popPose();
    }

    /**
     * 在指定轴向上旋转并渲染模型
     */
    private void renderRotatedModel(PoseStack poseStack,
                                    VertexConsumer vertexConsumer,
                                    int packedOverlay,
                                    ModelResourceLocation model,
                                    Axis axis,
                                    float rotation) {
        poseStack.pushPose();
        // 平移到中心 → 旋转 → 平移回原点
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(0.8F, 0.8F, 0.8F);
        poseStack.mulPose(axis.rotationDegrees(rotation));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        renderModel(poseStack, vertexConsumer, packedOverlay, model);
        poseStack.popPose();
    }

    /**
     * 实际渲染模型
     */
    private void renderModel(PoseStack poseStack,
                             VertexConsumer vertexConsumer,
                             int packedOverlay,
                             ModelResourceLocation model) {
        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        vertexConsumer,
                        null,
                        Minecraft.getInstance().getModelManager().getModel(model),
                        1.0F, 1.0F, 1.0F,
                        LightTexture.FULL_BLOCK,
                        packedOverlay
                );
    }

//        float rotation = blockEntity.getRotationAngle();
//
//        poseStack.pushPose();
//        poseStack.translate(0.5, -0.8, 0.5);
//
//        VertexConsumer consumer = bufferSource.getBuffer(RenderType..lightning());
//
//        // 绕 X 轴旋转
//        poseStack.pushPose();
//        poseStack.mulPose(Axis.XP.rotationDegrees(rotation));
//        drawBox(poseStack.last(), consumer,
//                -0.4, -0.4, -0.4, 0.8, 0.8, 0.8,
//                RED, packedLight, packedOverlay);
//        poseStack.popPose();
//
//        // 绕 Y 轴旋转
//        poseStack.pushPose();
//        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));
//        drawBox(poseStack.last(), consumer,
//                -0.4, -0.4, -0.4, 0.8, 0.8, 0.8,
//                GREEN, packedLight, packedOverlay);
//        poseStack.popPose();
//
//        // 绕 Z 轴旋转
//        poseStack.pushPose();
//        poseStack.mulPose(Axis.ZP.rotationDegrees(rotation));
//        drawBox(poseStack.last(), consumer,
//                -0.4, -0.4, -0.4, 0.8, 0.8, 0.8,
//                BLUE, packedLight, packedOverlay);
//        poseStack.popPose();
//
//        poseStack.popPose();
//    }

    @Override
    public AABB getRenderBoundingBox(GiantDogeAnvilBlockEntity entity) {
        BlockPos pos = entity.getBlockPos();
        return AABB.ofSize(Vec3.atCenterOf(pos), 3, 3, 3);
    }

    private void drawBox(PoseStack.Pose pose, VertexConsumer consumer,
                         double x, double y, double z,
                         double w, double h, double d,
                         int color, int packedLight, int packedOverlay) {

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        float fr = r / 255f;
        float fg = g / 255f;
        float fb = b / 255f;

        float minX = (float) x;
        float minY = (float) y;
        float minZ = (float) z;
        float maxX = (float) (x + w);
        float maxY = (float) (y + h);
        float maxZ = (float) (z + d);

// ===== 顶面 (Y+) 法线朝上 =====
// 法线朝上 (0,1,0)，从上方看逆时针
        vertex(consumer, pose, minX, maxY, minZ, fr, fg, fb, 0, 1, packedLight, packedOverlay, 0, 1, 0);
        vertex(consumer, pose, minX, maxY, maxZ, fr, fg, fb, 1, 1, packedLight, packedOverlay, 0, 1, 0);
        vertex(consumer, pose, maxX, maxY, maxZ, fr, fg, fb, 1, 0, packedLight, packedOverlay, 0, 1, 0);
        vertex(consumer, pose, maxX, maxY, minZ, fr, fg, fb, 0, 0, packedLight, packedOverlay, 0, 1, 0);

// ===== 底面 (Y-) 法线朝下 =====
// 法线朝下 (0,-1,0)，从上方看必须是顺时针
        vertex(consumer, pose, minX, minY, minZ, fr, fg, fb, 0, 1, packedLight, packedOverlay, 0, -1, 0);
        vertex(consumer, pose, maxX, minY, minZ, fr, fg, fb, 1, 1, packedLight, packedOverlay, 0, -1, 0);
        vertex(consumer, pose, maxX, minY, maxZ, fr, fg, fb, 1, 0, packedLight, packedOverlay, 0, -1, 0);
        vertex(consumer, pose, minX, minY, maxZ, fr, fg, fb, 0, 0, packedLight, packedOverlay, 0, -1, 0);

        // ===== 前面 (Z+) 法线朝前 =====
        // 从前方看：左下 → 右下 → 右上 → 左上 (逆时针)
        vertex(consumer, pose, minX, minY, maxZ, fr, fg, fb, 0, 1, packedLight, packedOverlay, 0, 0, 1);
        vertex(consumer, pose, maxX, minY, maxZ, fr, fg, fb, 1, 1, packedLight, packedOverlay, 0, 0, 1);
        vertex(consumer, pose, maxX, maxY, maxZ, fr, fg, fb, 1, 0, packedLight, packedOverlay, 0, 0, 1);
        vertex(consumer, pose, minX, maxY, maxZ, fr, fg, fb, 0, 0, packedLight, packedOverlay, 0, 0, 1);

        // ===== 后面 (Z-) 法线朝后 =====
        // 从后方看：左下 → 右下 → 右上 → 左上 (逆时针)
        // 但从前方看是顺时针，所以后面可见
        vertex(consumer, pose, minX, minY, minZ, fr, fg, fb, 0, 1, packedLight, packedOverlay, 0, 0, -1);
        vertex(consumer, pose, minX, maxY, minZ, fr, fg, fb, 1, 1, packedLight, packedOverlay, 0, 0, -1);
        vertex(consumer, pose, maxX, maxY, minZ, fr, fg, fb, 1, 0, packedLight, packedOverlay, 0, 0, -1);
        vertex(consumer, pose, maxX, minY, minZ, fr, fg, fb, 0, 0, packedLight, packedOverlay, 0, 0, -1);

        // ===== 右面 (X+) 法线朝右 =====
        // 从右方看：左下 → 右下 → 右上 → 左上 (逆时针)
        vertex(consumer, pose, maxX, minY, minZ, fr, fg, fb, 0, 1, packedLight, packedOverlay, 1, 0, 0);
        vertex(consumer, pose, maxX, maxY, minZ, fr, fg, fb, 1, 1, packedLight, packedOverlay, 1, 0, 0);
        vertex(consumer, pose, maxX, maxY, maxZ, fr, fg, fb, 1, 0, packedLight, packedOverlay, 1, 0, 0);
        vertex(consumer, pose, maxX, minY, maxZ, fr, fg, fb, 0, 0, packedLight, packedOverlay, 1, 0, 0);

        // ===== 左面 (X-) 法线朝左 =====
        // 从左方看：左下 → 右下 → 右上 → 左上 (逆时针)
        // 但从右方看是顺时针，所以左面可见
        vertex(consumer, pose, minX, minY, minZ, fr, fg, fb, 0, 1, packedLight, packedOverlay, -1, 0, 0);
        vertex(consumer, pose, minX, minY, maxZ, fr, fg, fb, 1, 1, packedLight, packedOverlay, -1, 0, 0);
        vertex(consumer, pose, minX, maxY, maxZ, fr, fg, fb, 1, 0, packedLight, packedOverlay, -1, 0, 0);
        vertex(consumer, pose, minX, maxY, minZ, fr, fg, fb, 0, 0, packedLight, packedOverlay, -1, 0, 0);
    }

    private void vertex(VertexConsumer consumer, PoseStack.Pose pose,
                        float x, float y, float z,
                        float r, float g, float b,
                        float u, float v,
                        int packedLight, int packedOverlay,
                        float nx, float ny, float nz) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, 1.0f)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, nx, ny, nz);
    }
}