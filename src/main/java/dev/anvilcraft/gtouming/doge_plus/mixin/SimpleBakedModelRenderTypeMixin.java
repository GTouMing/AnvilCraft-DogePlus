package dev.anvilcraft.gtouming.doge_plus.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

/**
 * 让带 {@code render_type} 的 {@link SimpleBakedModel} 真正按渲染类型过滤 quad。
 *
 * <p>Sodium 的 multipart 渲染会对「所有子模型渲染类型的并集」逐层循环，每层都通过
 * Forgified Fabric API 直接发射<b>每个</b>子模型的全部 quad。{@code SimpleBakedModel} 没有
 * 重写 NeoForge 的 5 参 {@code getQuads}，走的是默认实现——它忽略 {@code renderType}，返回全部
 * quad。于是 translucent 子模型也会在 cutout 那一趟被画一次：cutout 只做 0.1 的 alpha 裁剪，
 * 半透明像素（例如 0.33）会被当成不透明保留，之后 translucent 再混合也盖不回半透明。</p>
 *
 * <p>这里在 {@code renderType} 与子模型自身的 {@code blockRenderTypes} 不匹配时返回空表，
 * 把每个子模型限制在自己的渲染层里。没有 {@code render_type} 的模型
 * （{@code blockRenderTypes == null}）行为不变，仍返回全部 quad。</p>
 */
@Mixin(SimpleBakedModel.class)
public abstract class SimpleBakedModelRenderTypeMixin {

    @Shadow
    @Final
    protected ChunkRenderTypeSet blockRenderTypes;

    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource random,
            ModelData modelData,
            @Nullable RenderType renderType) {
        if (renderType != null && this.blockRenderTypes != null && !this.blockRenderTypes.contains(renderType)) {
            return List.of();
        }
        return ((BakedModel) (Object) this).getQuads(state, side, random);
    }
}
