package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.client.selection.DogeBlockSelection;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.dubhe.anvilcraft.client.selection.ModelBlockSelection;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 让前置的「多方块放置预览」也能描出本模组方块的轮廓。
 *
 * <p>前置 {@code LargeBlockPlacePreviewEventListener} 在 OUTLINE 预览模式下先问
 * {@link ModelBlockSelection#multipartOutline}；但那份快照只烘了前置自己命名空间的方块，
 * 本模组的方块查不到，于是退回虚影（鬼影）。这里在方法开头补上本模组自己的整套轮廓
 * （见 {@link DogeBlockSelection#outline}），预览便自动走轮廓分支——颜色、透明度、放置失败的
 * 闪烁动画全都沿用前置那套，无需另写渲染器。</p>
 *
 * <p>非本模组的状态原样返回空表，交回前置自己的逻辑。</p>
 */
@Mixin(value = ModelBlockSelection.class, remap = false)
public abstract class ModelBlockSelectionMixin {
    @Inject(method = "multipartOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private static void doge_plus$dogeOutline(BlockState state, CallbackInfoReturnable<List<SelectionPart>> cir) {
        List<SelectionPart> outline = DogeBlockSelection.outline(state);
        if (!outline.isEmpty()) cir.setReturnValue(outline);
    }
}
