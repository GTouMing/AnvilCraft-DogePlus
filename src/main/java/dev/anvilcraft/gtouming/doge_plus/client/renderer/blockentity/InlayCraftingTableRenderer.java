package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCraftingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;


/**
 * 渲染镶合台各槽位物品：基材居中，材料按当前启用的材料槽数量以骰子点数布局
 * （1 居中、2 分开、3 对角、4 正方、5 分岔、6 两列）。
 */
public class InlayCraftingTableRenderer implements BlockEntityRenderer<InlayCraftingTableBlockEntity> {

    /** 骰子点数布局（x, z）；下标即材料槽序号。 */
    private static final float[][] DICE_1 = {{0.5F, 0.5F}};
    private static final float[][] DICE_2 = {{0.34F, 0.34F}, {0.66F, 0.66F}};
    private static final float[][] DICE_3 = {{0.3F, 0.3F}, {0.5F, 0.5F}, {0.7F, 0.7F}};
    private static final float[][] DICE_4 = {{0.3F, 0.3F}, {0.7F, 0.3F}, {0.3F, 0.7F}, {0.7F, 0.7F}};
    private static final float[][] DICE_5 = {
            {0.3F, 0.3F}, {0.7F, 0.3F}, {0.5F, 0.5F}, {0.3F, 0.7F}, {0.7F, 0.7F}
    };
    private static final float[][] DICE_6 = {
            {0.3F, 0.3F}, {0.3F, 0.5F}, {0.3F, 0.7F},
            {0.7F, 0.3F}, {0.7F, 0.5F}, {0.7F, 0.7F}
    };

    private static float[] materialPos(int count, int index) {
        float[][] layout = switch (count) {
            case 1 -> DICE_1;
            case 2 -> DICE_2;
            case 3 -> DICE_3;
            case 4 -> DICE_4;
            case 5 -> DICE_5;
            default -> DICE_6;
        };
        return layout[Math.min(index, layout.length - 1)];
    }

    public InlayCraftingTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            InlayCraftingTableBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        int activeMaterials = Math.min(
                blockEntity.materialSlotCount(), InlayCraftingTableBlockEntity.MATERIAL_SLOTS);
        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            ItemStack stack = blockEntity.getItemHandler().getStackInSlot(slot);
            if (stack.isEmpty()) continue;

            float x = 0.5F;
            float z = 0.5F;
            if (slot != InlayCraftingTableBlockEntity.SLOT_BASE) {
                int index = slot - InlayCraftingTableBlockEntity.SLOT_MATERIAL_START;
                // 当前过滤下未启用的材料槽不渲染（物品仍在槽里，可空手取回）。
                if (index >= activeMaterials) continue;
                float[] pos = materialPos(activeMaterials, index);
                x = pos[0];
                z = pos[1];
            }

            boolean isBlock = false;
            float multi = 1F;
            if (stack.getItem() instanceof BlockItem item) {
                isBlock = true;
                if (item.getBlock() instanceof IMultiPartBlock)
                    multi = 1/3F;
            }

            float region = isBlock
                    ? (slot == InlayCraftingTableBlockEntity.SLOT_BASE ? 0.60F : 0.82F)
                    : (slot == InlayCraftingTableBlockEntity.SLOT_BASE ? 0.75F : 0.86F);
            float scale = isBlock
                    ? (slot == InlayCraftingTableBlockEntity.SLOT_BASE ? 0.375F : 0.1F) * multi
                    : (slot == InlayCraftingTableBlockEntity.SLOT_BASE ? 0.75F : 0.2F);

            poseStack.pushPose();
            poseStack.translate(x, region, z);
            poseStack.scale(scale, scale, scale);
            if (!isBlock) poseStack.mulPose(Axis.XN.rotation((float) (Math.PI / 2)));
            renderItem(stack, packedLight, packedOverlay, poseStack, bufferSource, blockEntity.getLevel(), (int) (blockEntity.getBlockPos().asLong() + slot));
            poseStack.popPose();
        }

        FilterIconRenderer.renderOnHorizontalSides(
                poseStack, bufferSource, blockEntity.getLevel(), blockEntity.getBlockPos(),
                blockEntity.getRecipeFilter(), packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(InlayCraftingTableBlockEntity blockEntity) {
        // 过滤图标贴在四个水平侧面外侧，包围盒略外扩避免被视锥剔除。
        return new AABB(blockEntity.getBlockPos()).inflate(0.5);
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
