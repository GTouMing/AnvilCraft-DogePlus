package dev.anvilcraft.gtouming.doge_plus.mixin.ae2;

import appeng.api.crafting.PatternDetailsTooltip;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AE2 编码样板的 tooltip 只展示嵌套 {@link GenericStack} 的物品图标，不会触发我们
 * 对顶层 ItemStack 的镶嵌 tooltip 追加。这里在输入 / 输出加入时补一行镶嵌信息。
 */
@Mixin(targets = "appeng.api.crafting.PatternDetailsTooltip", remap = false)
public abstract class PatternDetailsTooltipMixin {

    @Inject(method = "addInput(Lappeng/api/stacks/AEKey;J)V", at = @At("TAIL"))
    private void doge$addInputKey(AEKey what, long amount, CallbackInfo ci) {
        doge$appendInlay(what);
    }

    @Inject(method = "addInput(Lappeng/api/stacks/GenericStack;)V", at = @At("TAIL"))
    private void doge$addInputStack(GenericStack stack, CallbackInfo ci) {
        if (stack != null) doge$appendInlay(stack.what());
    }

    @Inject(method = "addOutput(Lappeng/api/stacks/AEKey;J)V", at = @At("TAIL"))
    private void doge$addOutputKey(AEKey what, long amount, CallbackInfo ci) {
        doge$appendInlay(what);
    }

    @Inject(method = "addOutput(Lappeng/api/stacks/GenericStack;)V", at = @At("TAIL"))
    private void doge$addOutputStack(GenericStack stack, CallbackInfo ci) {
        if (stack != null) doge$appendInlay(stack.what());
    }

    private void doge$appendInlay(AEKey what) {
        if (!(what instanceof AEItemKey key)) return;
        List<InlayEntry> inlays = key.get(ModDataComponentTypes.INLAY.get());
        if (inlays == null || inlays.isEmpty()) return;

        String names = inlays.stream()
                .filter(entry -> !entry.isEmpty())
                .map(entry -> new ItemStack(BuiltInRegistries.ITEM.get(entry.id())).getHoverName().getString())
                .collect(Collectors.joining("、"));
        if (names.isEmpty()) return;

        ((PatternDetailsTooltip) (Object) this)
                .addProperty(Component.translatable("tooltip.anvilcraft_doge_plus.pattern_inlay", names));
    }
}
