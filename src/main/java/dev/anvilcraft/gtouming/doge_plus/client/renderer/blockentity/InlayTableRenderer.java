package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;


/**
 * 渲染镶嵌台各槽位物品：基材与镶嵌材料平躺居中于平台（基材占满平台、材料缩小至中央孔洞）。
 */
public class InlayTableRenderer implements BlockEntityRenderer<InlayTableBlockEntity> {

    private static final float[] ITEM_Y = {
            0.75F,   // SLOT_BASE 平台中央
            0.90F    // SLOT_MATERIAL 平台中央孔洞
    };
    private static final float[] BLOCK_Y = {
            0.60F,   // SLOT_BASE 平台中央
            0.90F    // SLOT_MATERIAL 平台中央孔洞
    };

    /** 各槽位的缩放（NONE 变换无内置缩放，此处即最终大小）。 */
    private static final float[] BLOCK_SCALE = {
            0.375F,   // SLOT_BASE 占满平台
            0.25F     // SLOT_MATERIAL 缩小至中央孔洞
    };

    /** 各槽位的缩放（NONE 变换无内置缩放，此处即最终大小）。 */
    private static final float[] ITEM_SCALE = {
            0.75F,   // SLOT_BASE 占满平台
            0.25F    // SLOT_MATERIAL 缩小至中央孔洞
    };

    public InlayTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            InlayTableBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        for (int slot = 0; slot < InlayTableBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty()) continue;
            boolean isBlock = false;
            float multi = 1F;
            if (stack.getItem() instanceof BlockItem item) {
                isBlock = true;
                if (item.getBlock() instanceof IMultiPartBlock)
                    multi = 1/3F;
            }

            float region = isBlock ? BLOCK_Y[slot] : ITEM_Y[slot];
            float scale = isBlock ? BLOCK_SCALE[slot] * multi :  ITEM_SCALE[slot];

            poseStack.pushPose();
            poseStack.translate(0.5F, region, 0.5F);
            poseStack.scale(scale, scale, scale);
            if (!isBlock && slot == 0) poseStack.mulPose(Axis.XN.rotation((float) (Math.PI / 2)));
            renderItem(stack, packedLight, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), (int) blockEntity.getBlockPos().asLong());
            poseStack.popPose();
        }
    }

    private void renderItem(ItemStack stack, int packedLight, int packedOverlay,
                            PoseStack poseStack, MultiBufferSource bufferSource, @Nullable Level level, int seed) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.NONE,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                level,
                seed);
    }
}
