package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

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
}