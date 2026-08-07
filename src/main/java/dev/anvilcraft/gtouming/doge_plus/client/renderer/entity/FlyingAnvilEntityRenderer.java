package dev.anvilcraft.gtouming.doge_plus.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.gtouming.doge_plus.entity.FlyingAnvilEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;

/**
 * 将 {@link FlyingAnvilEntity} 渲染为铁砧方块。
 */
public class FlyingAnvilEntityRenderer extends EntityRenderer<FlyingAnvilEntity> {

    public FlyingAnvilEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(FlyingAnvilEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                entity.getAnvilBlockState(),
                poseStack,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
        );
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(FlyingAnvilEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
