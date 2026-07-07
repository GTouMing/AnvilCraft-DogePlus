package dev.anvilcraft.doge_plus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import dev.anvilcraft.doge_plus.entity.NodeSupportEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

import java.util.Random;

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
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        ItemStack itemstack = entity.getDisplayItem();
        BakedModel bakedmodel = context.getItemRenderer().getModel(itemstack, entity.level(), null, entity.getId());
        boolean flag = bakedmodel.isGui3d();
        boolean shouldBob = IClientItemExtensions.of(itemstack).shouldBobAsEntity(itemstack);
        float f1 = shouldBob ? Mth.sin((partialTick) / 10.0F) * 0.1F + 0.1F : 0.0F;
        float f2 = bakedmodel.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        poseStack.translate(0.0F, f1 + 0.25F * f2, 0.0F);
        renderMultipleFromCount(context.getItemRenderer(), poseStack, bufferSource, packedLight, itemstack, bakedmodel, flag, random);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    @NotNull
    public ResourceLocation getTextureLocation(@NotNull NodeSupportEntity entity) {
        return ResourceLocation.withDefaultNamespace("textures/misc/underwater.png");
    }    public static void renderMultipleFromCount(ItemRenderer itemRenderer, PoseStack poseStack, MultiBufferSource buffer, int packedLight, ItemStack item, BakedModel model, boolean isGui3d, RandomSource random) {
        int i = getRenderedAmount(item.getCount());
        float f = model.getTransforms().ground.scale.x();
        float f1 = model.getTransforms().ground.scale.y();
        float f2 = model.getTransforms().ground.scale.z();
        if (!isGui3d) {
            float f3 = -0.0F * (float)(i - 1) * 0.5F * f;
            float f4 = -0.0F * (float)(i - 1) * 0.5F * f1;
            float f5 = -0.09375F * (float)(i - 1) * 0.5F * f2;
            poseStack.translate(f3, f4, f5);
        }

        boolean shouldSpread = IClientItemExtensions.of(item).shouldSpreadAsEntity(item);

        for(int j = 0; j < i; ++j) {
            poseStack.pushPose();
            if (j > 0 && shouldSpread) {
                if (isGui3d) {
                    float f7 = 0.15F;
                    float f9 = 0.15F;
                    float f6 = 0.15F;
                    poseStack.translate(f7, f9, f6);
                } else {
                    float f8 = (2.0F - 1.0F) * 0.15F * 0.5F;
                    float f10 = (2.0F - 1.0F) * 0.15F * 0.5F;
                    poseStack.translate(f8, f10, 0.0F);
                }
            }

            itemRenderer.render(item, ItemDisplayContext.GROUND, false, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY, model);
            poseStack.popPose();
            if (!isGui3d) {
                poseStack.translate(0.0F * f, 0.0F * f1, 0.09375F * f2);
            }
        }

    }
    static int getRenderedAmount(int count) {
        if (count <= 1) {
            return 1;
        } else if (count <= 16) {
            return 2;
        } else if (count <= 32) {
            return 3;
        } else {
            return count <= 48 ? 4 : 5;
        }
    }
}