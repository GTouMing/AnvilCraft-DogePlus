package dev.anvilcraft.gtouming.doge_plus.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.GiantDogeAnvilRenderer;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.anvilcraft.gtouming.doge_plus.init.ModItems;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {

    @ModifyVariable(
            method = "render",
            at = @At(value = "HEAD"),
            argsOnly = true,
            index = 8
    )
    public BakedModel doge_plus$modifyModel(
            BakedModel originalModel,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int combinedLight,
            int combinedOverlay
    ) {
        if (!stack.is(ModItems.MOBILE_SILENCER.get())) {
            return originalModel;
        }

        if (displayContext != ItemDisplayContext.HEAD) return originalModel;

        return Minecraft.getInstance().getModelManager()
                .getModel(ModelResourceLocation.standalone(AnvilCraftDogePlus.of("item/mobile_silencer_3d")));
    }

    /**
     * 巨型 Doge 砧方块物品也补绘 {@code light} 组呼吸灯。
     *
     * <p>物品模型 {@code item/giant_doge_anvil} 直接继承方块模型，方块模型里已移除 {@code light} 组，
     * 所以物品同样缺少发光核心。这里在物品模型绘制完成后、姿态弹栈前，
     * 以同一姿态（已套用物品的 display 变换）补绘一次 light 组呼吸灯，与世界/下落实体共用同一套逻辑。</p>
     */
    @Inject(
            method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
            )
    )
    private void doge_plus$renderGiantDogeAnvilLight(
            ItemStack stack, ItemDisplayContext displayContext, boolean leftHand,
            PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int combinedOverlay,
            BakedModel model, CallbackInfo ci
    ) {
        if (stack.getItem() != ModBlocks.GIANT_DOGE_ANVIL.get().asItem()) return;
        GiantDogeAnvilRenderer.renderBreathingLight(poseStack, bufferSource);
    }
}
