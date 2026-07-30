package dev.anvilcraft.gtouming.doge_plus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

public class CuriosRenderer implements ICurioRenderer {

    @Override
    public <T extends LivingEntity, M extends EntityModel<T>> void render(
            ItemStack stack, SlotContext slotContext, PoseStack poseStack,
            RenderLayerParent<T, M> renderLayerParent, MultiBufferSource buffer,
            int light, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch) {

        if (!(stack.getItem() instanceof MobileSilencer)) return;

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer itemRenderer = mc.getItemRenderer();
        LivingEntity wearer = slotContext.entity();
        M model = renderLayerParent.getModel();

        poseStack.pushPose();


        if (model instanceof HumanoidModel<?> humanoidModel) {
            /* ── HumanoidModel 的标准头部渲染 ──
             *
             * 直接调用 Minecraft 原生的 ModelPart API，不依赖 Curios 的 helper。
             *
             * head.translateAndRotate(poseStack) 流程：
             *   a. translate(0, 0, 0)              ← head 部件位置偏移，为 (0,0,0)
             *   b. translate(0, 1.5, 0)             ← 移动到颈部 pivot (24/16=1.5)
             *   c. rotate(Y) → rotate(X)           ← 应用头部偏航/俯仰
             *   d. translate(0, -1.5, 0)            ← 移回原点
             * 执行后 poseStack 位于 (0,0,0)，头部旋转已生效
             */
            humanoidModel.head.translateAndRotate(poseStack);
            poseStack.rotateAround(Axis.ZP.rotation((float) Math.PI), 0F, 0F, 0F);
            poseStack.translate(0, 4 / 16.0F, 0); // 继续到头顶
        } else {
            /* ── 非 HumanoidModel 实体的 Fallback ──
             * 无法获取 head ModelPart，使用眼高近似 + 手动旋转
             */
            poseStack.translate(0, wearer.getEyeHeight(), 0);
            poseStack.mulPose(Axis.YN.rotationDegrees(netHeadYaw));
            poseStack.mulPose(Axis.XN.rotationDegrees(headPitch));
        }

        // 缩放至合适大小
        poseStack.scale(0.6f, 0.6f, 0.6f);

        // 使用 HEAD display context 渲染（从模型 JSON 的 display.head 读取变换）
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.HEAD,
                light,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                wearer.level(),
                0
        );

        poseStack.popPose();
    }
}
