package dev.anvilcraft.gtouming.doge_plus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import dev.anvilcraft.gtouming.doge_plus.entity.NodeSupportEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static net.minecraft.client.renderer.entity.ItemEntityRenderer.getSeedForItemStack;
import static net.minecraft.client.renderer.entity.ItemEntityRenderer.renderMultipleFromCount;

public class NodeSupportEntityRenderer extends EntityRenderer<NodeSupportEntity> {
    final EntityRendererProvider.Context context;
    final RandomSource random = RandomSource.create();
    public NodeSupportEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public void render(
            @NotNull NodeSupportEntity entity,
            float entityYaw,
            float partialTick,
            @NotNull PoseStack pose,
            @NotNull MultiBufferSource bufferSource,
            int packedLight
    ) {

        List<ItemEntity> items = entity.getCapturedItemEntities();
        for (ItemEntity itemEntity : items) {
            if (itemEntity == null) continue;

            pose.pushPose();

            // 计算当前物品在圆周上的位置（8等分）
            float angle = (float) (items.indexOf(itemEntity) * Math.PI * 2 / 8);
            float radius = 0.2F;
            float offsetX = Mth.cos(angle) * radius;
            float offsetZ = Mth.sin(angle) * radius;

            ItemStack itemstack = itemEntity.getItem();
            this.random.setSeed(getSeedForItemStack(itemstack));
            BakedModel bakedmodel = Minecraft.getInstance().getItemRenderer().getModel(itemstack, entity.level(), null, itemEntity.getId());
            boolean flag = bakedmodel.isGui3d();

            float scaleY = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
            pose.translate(offsetX, 0.25F * scaleY, offsetZ);

            // 面向中心：绕Y轴旋转
            pose.mulPose(Axis.YN.rotation((float) (angle + Math.PI / 2)));

            // 向外倾倒75度：绕X轴旋转
            float tiltAngle = (float) Math.toRadians(75);
            pose.mulPose(Axis.XN.rotation(tiltAngle));
            // 渲染物品
            renderMultipleFromCount(Minecraft.getInstance().getItemRenderer(), pose, bufferSource, packedLight, itemstack, bakedmodel, flag, this.random);
            pose.popPose();
        }
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull NodeSupportEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    }
}