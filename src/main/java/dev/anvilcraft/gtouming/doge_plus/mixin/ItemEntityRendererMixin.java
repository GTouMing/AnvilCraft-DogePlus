package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 被 Doge 节点捕获的掉落物不按普通物品实体渲染：渲染已移交
 * {@code DogeNodeEntityRenderer}（节点渲染器统一按鱼缸式绘制），此处仅取消默认渲染。
 */
@Mixin(ItemEntityRenderer.class)
public class ItemEntityRendererMixin {

    @Inject(method = "render*", at = @At("HEAD"), cancellable = true)
    private void doge_plus$cancelCapturedRender(ItemEntity entity, float entityYaw, float partialTick,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, CallbackInfo ci) {
        if (entity instanceof ICaptured iCaptured && iCaptured.doge_plus$isCaptured()) {
            ci.cancel();
        }
    }
}
