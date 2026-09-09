package dev.anvilcraft.gtouming.doge_plus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

/**
 * Doge 节点渲染器。
 *
 * <ul>
 *   <li>节点本体：渲染为一个 1×1×1 旋转小方块，渲染类型为 {@code lighting}
 *       （方块模型经 {@code renderSingleBlock} 以光照渲染）；</li>
 *   <li>捕获物品：几乎完全参照前置鱼缸 {@code drawItemsInTank}——整体随机偏转、
 *       绕环排布、按 count 分层堆叠、每层随机微偏，并用 {@link ItemRenderer#renderStatic}
 *       静态渲染（无流体，因此无流体浮动）。</li>
 * </ul>
 */
public class DogeNodeEntityRenderer extends EntityRenderer<DogeNodeEntity> {

    /** 本体缩放：1×1×1 单位方块缩放到节点实际尺寸（1/16 格 = 1 像素）。 */
    private static final float SCALE = 1 / 16F;
    /** 本体自转速度（度/ tick）。 */
    private static final float ROTATION_SPEED = 3.0F;
    /** 捕获物环绕半径（鱼缸 drawItemsInTank 用 0.125）。 */
    private static final float ORBIT_RADIUS = 1 / 8F;

    public DogeNodeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(DogeNodeEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        // ===== 节点本体：1×1×1 旋转小方块（lighting 渲染） =====
        poseStack.pushPose();
        float angle = (entity.tickCount + partialTick) * ROTATION_SPEED;
        poseStack.mulPose(Axis.YP.rotationDegrees(angle));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-0.5, 0, -0.5);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                Blocks.WHITE_CONCRETE.defaultBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                RenderType.solid()
        );
        poseStack.popPose();

        // ===== 捕获物品：参照鱼缸 drawItemsInTank =====
        List<ItemEntity> captured = entity.getRenderItems();
        if (!captured.isEmpty()) {
            List<ItemStack> stacks = new ArrayList<>(captured.size());
            for (ItemEntity item : captured) {
                if (!item.isRemoved() && !item.getItem().isEmpty()) stacks.add(item.getItem());
            }
            drawCapturedItems(entity, stacks, poseStack, buffer, packedLight);
        }

        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    /**
     * 在节点中心绘制捕获物品（移植自鱼缸 {@code FishTankBlockEntityRenderer#drawItemsInTank}）：
     * 整体随机偏转一次；物品绕 Y 均匀环状排布（单物品置于中心）；每物品带正弦浮动；
     * 按堆叠数量分层（每 8 个一层），每层在 1/16 半径内随机微偏；使用静态渲染。
     */
    private void drawCapturedItems(DogeNodeEntity node, List<ItemStack> items,
                                   PoseStack pose, MultiBufferSource source, int light) {
        if (items.isEmpty()) return;
        RandomSource random = RandomSource.create(node.getUUID().hashCode());
        final float randomOffsetDeg = random.nextIntBetweenInclusive(0, 50) - 25;

        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(randomOffsetDeg));

        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        int itemCount = items.size();
        float partAngleDeg = 360F / itemCount;
        // 与鱼缸一致：单物品置于中心，多物品沿 0.125 半径圆环排布（进入循环前按总数量判定一次）
        float ringRadius = itemCount == 1 ? 0 : ORBIT_RADIUS;
        for (ItemStack stack : items) {
            pose.pushPose();

            float angle = Mth.DEG_TO_RAD * (partAngleDeg * itemCount);
            double sin = Mth.sin(angle);
            double cos = Mth.cos(angle);
            pose.translate(ringRadius * cos, 0, -ringRadius * sin);
            pose.mulPose(new Quaternionf()
                    .rotateY(Mth.DEG_TO_RAD * (partAngleDeg * itemCount + 35))
                    .rotateX(Mth.DEG_TO_RAD * 65));
            // 数量分层：每 8 个物品堆叠一层（同鱼缸），层间随机微偏
            for (int i = 0; i <= stack.getCount() / 8; i++) {
                pose.pushPose();
                float radius = 1 / 16F;
                pose.translate(
                        (random.nextFloat() - 0.5F) * 2 * radius,
                        (random.nextFloat() - 0.5F) * 2 * radius,
                        (random.nextFloat() - 0.5F) * 2 * radius);
                renderer.renderStatic(stack, ItemDisplayContext.GROUND, light,
                        OverlayTexture.NO_OVERLAY, pose, source, node.level(), 0);
                pose.popPose();
            }
            pose.popPose();
            itemCount--;
        }
        pose.popPose();
        if (source instanceof MultiBufferSource.BufferSource buffer) buffer.endBatch();
    }

    @Override
    public ResourceLocation getTextureLocation(DogeNodeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
