package dev.anvilcraft.gtouming.doge_plus.event;

import com.mojang.datafixers.util.Either;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.api.tooltip.InlayTooltipComponent;
import dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip.ClientInlayTooltip;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;

import java.util.List;

/**
 * 镶嵌 tooltip 追加渲染。
 * <p>
 * 事件总线由 NeoForge 按事件类型自动路由：
 * <ul>
 *   <li>MOD 总线：注册 {@link InlayTooltipComponent} 的客户端渲染工厂；</li>
 *   <li>GAME 总线：通过 {@link RenderTooltipEvent.GatherComponents} 把镶嵌内容追加到原版内容之后，
 *       顺序为「原版内容 → 镶孔图像 → 属性/提示文本」，交由原版排版引擎布局与定位，
 *       因此不会覆盖原版内容、不会越界、不会产生叠加顺序错误。</li>
 * </ul>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public class ClientTooltipEvent {

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(InlayTooltipComponent.class, ClientInlayTooltip::new);
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

        // 1. 材料物品：性质描述行（追加为普通文本行）
        MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(stack);
        if (material != null) {
            elements.add(Either.left(Component.translatable("tooltip.anvilcraft_doge_plus.material_attributes")
                    .withStyle(ChatFormatting.GRAY)));
            for (InlayProperty property : material.properties()) {
                elements.add(Either.left(property.getTooltip()));
            }
        }

        // 2. 基材/已镶嵌物品：镶孔图像 + 属性文本
        List<InlayEntry> inlays = InlayUtil.getInlays(stack);
        if (inlays.isEmpty() && !MaterialManager.hasSocket(stack)) return;
        if (Screen.hasShiftDown()) {
            List<ItemStack> materialStacks = inlays.stream().map(InlayEntry::toItemStack).toList();
            int sockets = Math.max(materialStacks.size(), MaterialManager.getSocketCount(stack));
            boolean hasDirection = InlayUtil.hasProperty(stack, InlayProperty.DIRECTION);

            // 2.1 镶孔图像组件（已镶嵌画材料图标、空镶孔画中括号、必要时在下方标注方位）
            elements.add(Either.right(new InlayTooltipComponent(materialStacks, sockets, hasDirection)));

            // 2.2 属性/提示文本
            boolean resonance = InlayUtil.hasProperty(stack, InlayProperty.RESONANCE);
            for (InlayEntry entry : inlays) {
                for (ResourceLocation id : entry.attributes()) {
                    InlayProperty property = InlayProperty.get(id);
                    if (property == null) continue;
                    Component component = resonance ? enhancedTooltip(property) : property.getTooltip();
                    if (elements.contains(Either.left(component))) continue;
                    elements.add(Either.left(component));
                }
            }

            // 高温数值
            if (InlayUtil.hasProperty(stack, InlayProperty.HIGH_TEMP)) {
                int heat = stack.getOrDefault(ModDataComponentTypes.HEAT, 0);
                if (heat > 0) {
                    elements.add(Either.left(Component.translatable(
                                    "tooltip.anvilcraft_doge_plus.inlay_property.high_temp_amount", heat)
                            .withStyle(ChatFormatting.RED)));
                }
            }
        } else {
            elements.add(Either.left(Component.translatable("tooltip.anvilcraft_doge_plus.inlay_details")
                    .withStyle(ChatFormatting.GRAY)));
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private static Component enhancedTooltip(InlayProperty property) {
        String key = resonanceKey(property);
        if (key == null) return property.getTooltip();
        return Component.translatable(key)
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(property.getColor())));
    }

    private static String resonanceKey(InlayProperty property) {
        return switch (property.id().getPath()) {
            case "cold_forged", "high_temp", "nirvana", "defense", "life", "attack", "enchant" ->
                    "tooltip.anvilcraft_doge_plus.inlay_property.resonance." + property.id().getPath();
            default -> null;
        };
    }
}
