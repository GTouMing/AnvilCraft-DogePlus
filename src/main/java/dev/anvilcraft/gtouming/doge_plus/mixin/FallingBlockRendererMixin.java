package dev.anvilcraft.gtouming.doge_plus.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.gtouming.doge_plus.block.GiantDogeAnvil;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.GiantDogeAnvilRenderer;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.FallingBlockRenderer;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 巨型 Doge 砧下落实体（前置 {@code FallingGiantAnvilEntity}）沿用原版 {@link FallingBlockRenderer}
 * 渲染（见前置 {@code ModEntities.FALLING_GIANT_ANVIL} 的渲染器注册）。
 *
 * <p>{@code light} 组已从方块模型 {@code giant_doge_anvil} 中移除，方块形态由
 * {@link GiantDogeAnvilRenderer} 在世界阶段事件里绘制。坠落时方块模型由本渲染器绘制，
 * light 组会一并缺失，这里在方块模型绘制完成后、姿态弹栈前补绘一次。</p>
 *
 * <p>下落实体持有的是 {@code cube=center} 的模型承载状态，原版在此姿态下先
 * {@code translate(-0.5, 0, -0.5)} 再绘制，方块模型原点即中心格原点，与世界阶段
 * {@code translate(pos)} 后的局部空间一致，因此直接复用同一套呼吸灯逻辑即可对齐。</p>
 */
@Mixin(FallingBlockRenderer.class)
public class FallingBlockRendererMixin {

    @Inject(
            method = "render(Lnet/minecraft/world/entity/item/FallingBlockEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"
            )
    )
    private void doge_plus$renderGiantDogeAnvilLight(
            FallingBlockEntity entity, float entityYaw, float partialTicks,
            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci
    ) {
        BlockState state = entity.getBlockState();
        if (!state.is(ModBlocks.GIANT_DOGE_ANVIL.get())) return;
        if (state.getValue(GiantDogeAnvil.CUBE) != GiantAnvilCube.CENTER) return;
        GiantDogeAnvilRenderer.renderBreathingLight(poseStack, buffer, partialTicks);
    }
}
