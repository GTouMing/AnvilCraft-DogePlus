package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.gtouming.doge_plus.block.entity.AbstractProcessingTableBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * 镶嵌台 / 镶合台渲染器的共用部分：物品渲染、过滤图标和略外扩的包围盒。
 *
 * <p>槽位布局与缩放由子类在 {@code render} 里决定。</p>
 */
public abstract class AbstractTableRenderer<T extends AbstractProcessingTableBlockEntity>
        implements BlockEntityRenderer<T> {

    protected AbstractTableRenderer(BlockEntityRendererProvider.Context context) {
    }

    /** 在给定姿态下渲染一个槽位物品。 */
    protected final void renderItem(
            ItemStack stack,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            @Nullable Level level,
            int seed) {
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

    /** 在四个水平侧面绘制配方过滤图标。 */
    protected final void renderFilter(
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            T blockEntity,
            int packedLight,
            int packedOverlay) {
        FilterIconRenderer.renderOnHorizontalSides(
                poseStack, bufferSource, blockEntity.getLevel(), blockEntity.getBlockPos(),
                blockEntity.getRecipeFilter(), packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(T blockEntity) {
        // 过滤图标贴在四个水平侧面外侧，包围盒略外扩避免被视锥剔除。
        return new AABB(blockEntity.getBlockPos()).inflate(0.5);
    }
}
