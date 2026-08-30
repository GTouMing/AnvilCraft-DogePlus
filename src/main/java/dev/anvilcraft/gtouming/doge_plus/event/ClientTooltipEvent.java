package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip.ClientInlayTooltip;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

/**
 * 注册镶嵌 tooltip 图像组件的客户端工厂。
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public class ClientTooltipEvent {

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(InlayTooltipComponent.class, ClientInlayTooltip::new);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();
        var flag = event.getFlags();

        // ===== 1. 材料物品：始终显示性质描述 =====
        MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(stack);
        if (material != null) {
            tooltip.add(Component.translatable("tooltip.anvilcraft_doge_plus.material_attributes")
                    .withStyle(ChatFormatting.GRAY));
            for (InlayProperty property : material.properties()) {
                tooltip.add(property.getTooltip());
            }
        }

        // ===== 2. 已镶嵌物品：显示提示/详情 =====
        if (InlayUtil.getInlays(stack).isEmpty()) return;

        boolean showDetails = flag.hasShiftDown() || flag.isCreative();

        if (showDetails) {
            // 共鸣：其他镶孔的材料属性描述增强
            boolean resonance = InlayUtil.hasProperty(stack, InlayProperty.RESONANCE);

            for (InlayEntry entry : InlayUtil.getInlays(stack)) {
                for (ResourceLocation id : entry.attributes()) {
                    InlayProperty property = InlayProperty.get(id);
                    if (property == null) continue;

                    Component line = resonance ? enhancedTooltip(property) : property.getTooltip();
                    if (tooltip.contains(line)) continue;
                    tooltip.add(line);
                }
            }

            // 高温：当前累加伤害
            if (InlayUtil.hasProperty(stack, InlayProperty.HIGH_TEMP)) {
                int heat = stack.getOrDefault(ModDataComponentTypes.HEAT, 0);
                if (heat > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.anvilcraft_doge_plus.inlay_property.high_temp_amount", heat)
                            .withStyle(ChatFormatting.RED));
                }
            }
        } else {
            // 提示：按住 Shift 查看详情
            if (!flag.isAdvanced()) return;
            tooltip.add(Component.translatable("tooltip.anvilcraft_doge_plus.inlay_details")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * 增强的 Tooltip（共鸣时使用）
     */
    private static Component enhancedTooltip(InlayProperty property) {
        Component original = property.getTooltip();
        // 添加共鸣标记
        return Component.literal("✦ ")
                .withStyle(ChatFormatting.GOLD)
                .append(original.copy().withStyle(ChatFormatting.WHITE));
    }
}
