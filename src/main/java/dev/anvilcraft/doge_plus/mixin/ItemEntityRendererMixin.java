package dev.anvilcraft.doge_plus.mixin;

import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.anvilcraft.doge_plus.entity.NodeSupportEntity.TAG_CAPTURED;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(
            ItemEntity entity,
            float entityYaw,
            float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poseStack,
            net.minecraft.client.renderer.MultiBufferSource bufferSource,
            int packedLight,
            CallbackInfo ci
    ) {
        if (entity.getPersistentData().getBoolean(TAG_CAPTURED)) {
        }
        ci.cancel();
    }
}
