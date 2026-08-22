package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶嵌 tooltip：
 * <ul>
 *   <li>材料物品：始终显示各性质描述行。</li>
 *   <li>基材/已镶嵌物品：图像横向渲染镶孔（材料图标或空镶孔 {@code []}）；文本行显示
 *       「按详情键查看」提示，按住详情键（默认 Shift，可改）时依次渲染各材料属性与描述。</li>
 * </ul>
 */
@Mixin(Item.class)
public abstract class ItemTooltipMixin {

    /** 材料：始终显示性质描述；基材/已镶嵌：提示或详情。 */
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void doge_plus$appendInlayTooltip(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltipComponents,
            TooltipFlag flag,
            CallbackInfo ci) {
        // 材料物品：始终显示性质描述
        MaterialManager.InlayMaterial material = MaterialManager.getInlay(stack);
        if (material != null) {
            for (InlayProperty property : material.properties()) {
                if (!tooltipComponents.contains(Component.translatable("tooltip.anvilcraft_doge_plus.material_attributes").withStyle(ChatFormatting.GRAY)))
                    tooltipComponents.add(Component.translatable("tooltip.anvilcraft_doge_plus.material_attributes").withStyle(ChatFormatting.GRAY));
                tooltipComponents.add(property.getTooltip());
            }
        }

        // 仅已镶嵌物品显示提示/详情
        if (InlayUtil.getInlays(stack).isEmpty()) return;
        boolean showDetails = flag.hasShiftDown() || flag.isCreative();
        if (showDetails) {
            // 依次渲染各镶嵌材料属性与描述
            for (ResourceLocation id : InlayUtil.getInlays(stack)) {
               MaterialManager.InlayMaterial inlay = MaterialManager.getInlay(BuiltInRegistries.ITEM.get(id).getDefaultInstance());
                if (inlay == null) continue;
                for (InlayProperty property : inlay.properties()) {
                    if (tooltipComponents.contains(property.getTooltip())) continue;
                    tooltipComponents.add(property.getTooltip());
                }
            }
            // 高温：当前累加伤害
            if (InlayUtil.hasProperty(stack, InlayProperty.HIGH_TEMP)) {
                int heat = stack.getOrDefault(ModDataComponentTypes.HEAT, 0);
                if (heat > 0) {
                    tooltipComponents.add(Component.translatable(
                            "tooltip.anvilcraft_doge_plus.inlay_property.high_temp_amount", heat)
                            .withStyle(ChatFormatting.RED));
                }
            }
        }
        else {
            // 提示：按住详情键查看
            if (!flag.isAdvanced()) return;
            tooltipComponents.add(Component.translatable("tooltip.anvilcraft_doge_plus.inlay_details")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /** 基材/已镶嵌物品：返回横向渲染镶孔的 tooltip 图像组件（材料图标 + 空镶孔 []）。 */
    @Inject(method = "getTooltipImage", at = @At("RETURN"), cancellable = true)
    private void doge_plus$inlayTooltipImage(
            ItemStack stack,
            CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        // 保留原版返回（若有物品分类等图像组件），仅在为空时追加镶嵌图像
        if (cir.getReturnValue().isPresent()) return;

        List<ResourceLocation> inlays = InlayUtil.getInlays(stack);
        if (inlays.isEmpty() && !MaterialManager.hasSocket(stack)) return;

        List<ItemStack> materialStacks = new ArrayList<>();
        for (ResourceLocation id : inlays) {
            Item materialItem = BuiltInRegistries.ITEM.get(id);
            if (materialItem == Items.AIR) continue;
            materialStacks.add(new ItemStack(materialItem));
        }
        int sockets = Math.max(materialStacks.size(), MaterialManager.getSocketCount(stack));
        boolean hasDirection = InlayUtil.hasProperty(stack, InlayProperty.DIRECTION);
        cir.setReturnValue(Optional.of(new InlayTooltipComponent(materialStacks, sockets, hasDirection)));
    }
}
