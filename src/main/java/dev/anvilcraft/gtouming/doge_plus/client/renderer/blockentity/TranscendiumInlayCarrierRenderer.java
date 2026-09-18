package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.gtouming.doge_plus.block.entity.TranscendiumInlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * 超限镶嵌载体渲染器：在每面 6×6 像素网格上点亮已镶嵌槽位。
 *
 * <p>槽位下标 i → 面 {@code Direction.values()[i % 6]}，像素 {@code i / 6}；
 * 颜色取自该镶嵌条目第一个性质的 {@link InlayProperty#getColor()}，以全亮实色呈现。</p>
 */
public class TranscendiumInlayCarrierRenderer implements BlockEntityRenderer<TranscendiumInlayCarrierBlockEntity> {

    private static final float EPS = 0.001F;
    /** 每面边长（像素网格 6×6）。 */
    private static final int GRID = 6;
    private static final float CELL = 1.0F / GRID;

    public TranscendiumInlayCarrierRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            TranscendiumInlayCarrierBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        List<InlayEntry> inlays = blockEntity.getInlays();
        if (inlays.isEmpty()) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());
        PoseStack.Pose pose = poseStack.last();
        for (int slot = 0; slot < inlays.size(); slot++) {
            InlayEntry entry = inlays.get(slot);
            if (entry.isEmpty()) continue;
            int color = colorOf(entry);
            if (color < 0) continue;
            Direction face = TranscendiumInlayCarrierBlockEntity.faceOf(slot);
            int pixel = TranscendiumInlayCarrierBlockEntity.pixelOf(slot);
            drawPixel(pose, consumer, face, pixel % GRID, pixel / GRID, color);
        }
    }

    @Override
    public AABB getRenderBoundingBox(TranscendiumInlayCarrierBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(0.01);
    }

    /** 条目第一个已注册性质的 RGB 颜色；无性质返回 -1（不点亮）。 */
    private static int colorOf(InlayEntry entry) {
        for (ResourceLocation attribute : entry.attributes()) {
            InlayProperty property = InlayProperty.get(attribute);
            if (property != null) return property.getColor();
        }
        return -1;
    }

    private static void drawPixel(PoseStack.Pose pose, VertexConsumer consumer, Direction face, int col, int row, int color) {
        float u0 = col * CELL;
        float u1 = u0 + CELL;
        float v0 = row * CELL;
        float v1 = v0 + CELL;
        // 竖直方向自上而下：行 0 在面顶部
        float yTop = 1.0F - v0;
        float yBottom = 1.0F - v1;

        float[][] quad = switch (face) {
            case DOWN -> new float[][]{
                    {u0, -EPS, v0}, {u1, -EPS, v0}, {u1, -EPS, v1}, {u0, -EPS, v1}};
            case UP -> new float[][]{
                    {u0, 1.0F + EPS, v0}, {u1, 1.0F + EPS, v0}, {u1, 1.0F + EPS, v1}, {u0, 1.0F + EPS, v1}};
            case NORTH -> new float[][]{
                    {u0, yBottom, -EPS}, {u1, yBottom, -EPS}, {u1, yTop, -EPS}, {u0, yTop, -EPS}};
            case SOUTH -> new float[][]{
                    {u0, yBottom, 1.0F + EPS}, {u1, yBottom, 1.0F + EPS}, {u1, yTop, 1.0F + EPS}, {u0, yTop, 1.0F + EPS}};
            case WEST -> new float[][]{
                    {-EPS, yBottom, u0}, {-EPS, yBottom, u1}, {-EPS, yTop, u1}, {-EPS, yTop, u0}};
            case EAST -> new float[][]{
                    {1.0F + EPS, yBottom, u0}, {1.0F + EPS, yBottom, u1}, {1.0F + EPS, yTop, u1}, {1.0F + EPS, yTop, u0}};
        };

        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;
        for (float[] vertex : quad) {
            consumer.addVertex(pose, vertex[0], vertex[1], vertex[2]).setColor(r, g, b, 1.0F);
        }
    }
}
