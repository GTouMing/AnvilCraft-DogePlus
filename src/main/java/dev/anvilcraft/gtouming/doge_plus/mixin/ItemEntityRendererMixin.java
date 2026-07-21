package dev.anvilcraft.gtouming.doge_plus.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.api.ICaptured;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.client.renderer.entity.ItemEntityRenderer.getSeedForItemStack;
import static net.minecraft.client.renderer.entity.ItemEntityRenderer.renderMultipleFromCount;

@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {
    @Unique
    private final RandomSource doge_plus$random = RandomSource.create();

    @Inject(method = "render*", at = @At("HEAD"), cancellable = true)
    private void anvilcraftDoge$onRender(ItemEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, CallbackInfo ci) {
        if (!((entity instanceof ICaptured iCaptured) && iCaptured.doge_plus$isCaptured())) return;
        ci.cancel();
        poseStack.pushPose();

        // 8 等分圆周位置
        float angle = (float) (iCaptured.doge_plus$getIndex() * Math.PI * 2 / 8);
        float radius = 0.2F;
        float offsetX = Mth.cos(angle) * radius;
        float offsetZ = Mth.sin(angle) * radius;

        ItemStack itemstack = entity.getItem();
        doge_plus$random.setSeed(getSeedForItemStack(itemstack));
        BakedModel bakedmodel = Minecraft.getInstance().getItemRenderer()
                .getModel(itemstack, entity.level(), null, entity.getId());
        boolean flag = bakedmodel.isGui3d();

        poseStack.translate(offsetX, 0, offsetZ);

        // 面向中心
        poseStack.mulPose(Axis.YN.rotation((float) (angle + Math.PI / 2)));
        // 向外倾斜 75°
        poseStack.mulPose(Axis.XN.rotation((float) Math.toRadians(75)));

        renderMultipleFromCount(
                Minecraft.getInstance().getItemRenderer(),
                poseStack, bufferSource, packedLight,
                itemstack, bakedmodel, flag, doge_plus$random
        );

        poseStack.popPose();
    }
}
