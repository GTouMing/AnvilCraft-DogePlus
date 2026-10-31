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
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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

    /**
     * AE2 可选兼容：用反射持有解包函数，AE2 缺席时返回 identity。
     * 这样本类不会在类加载/校验阶段直接引用 AE2 相关类。
     */
    private static final Function<ItemStack, ItemStack> AE2_UNWRAP = createAe2Unwrap();

    private static Function<ItemStack, ItemStack> createAe2Unwrap() {
        try {
            if (!ModList.get().isLoaded("ae2")) return Function.identity();
            Class<?> compat = Class.forName("dev.anvilcraft.gtouming.doge_plus.client.Ae2TooltipCompat");
            Method unwrap = compat.getMethod("unwrap", ItemStack.class);
            return stack -> {
                try {
                    return (ItemStack) unwrap.invoke(null, stack);
                } catch (ReflectiveOperationException e) {
                    return stack;
                }
            };
        } catch (Throwable ignored) {
            return Function.identity();
        }
    }

    @SubscribeEvent
    public static void registerTooltipComponents(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(InlayTooltipComponent.class, ClientInlayTooltip::new);
    }

    @SubscribeEvent
    public static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        // AE2 的虚拟槽 / 样板会把 GenericStack 包装成物品；先解包再读取 INLAY。
        stack = AE2_UNWRAP.apply(stack);
        List<Either<FormattedText, TooltipComponent>> elements = event.getTooltipElements();

        // 1. 材料物品：性质描述行（追加为普通文本行）
        MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(stack);
        if (material != null && !material.properties().isEmpty()) {
            elements.add(Either.left(Component.translatable("tooltip.anvilcraft_doge_plus.material_attributes")
                    .withStyle(ChatFormatting.GRAY)));
            for (InlayProperty property : material.properties()) {
                elements.add(Either.left(property.getTooltip()));
            }
        }

        // 2. 基材/已镶嵌物品：按住 Shift 展开镶孔图像与属性文本；
        //    AE2 虚拟槽 / 样板 tooltip 同样走这里。
        List<InlayEntry> inlays = InlayUtil.getInlays(stack);
        if (inlays.isEmpty() && !MaterialManager.hasSocket(stack)) return;

        if (Screen.hasShiftDown()) {
            List<ItemStack> materialStacks = inlays.stream().map(InlayEntry::toItemStack).toList();
            int sockets = Math.max(materialStacks.size(), MaterialManager.getSocketCount(stack));

            // 2.1 镶孔图像组件（每个镶孔画槽位贴图，已镶嵌叠材料图标）
            elements.add(Either.right(new InlayTooltipComponent(materialStacks, sockets)));

            // 2.1.1 镶孔数由数据包定义，可能远超图像上限：补一行说明。
            if (sockets > ClientInlayTooltip.MAX_VISIBLE_SOCKETS) {
                elements.add(Either.left(Component.translatable(
                                "tooltip.anvilcraft_doge_plus.inlay_sockets_truncated",
                                sockets - ClientInlayTooltip.MAX_VISIBLE_SOCKETS, sockets)
                        .withStyle(ChatFormatting.GRAY)));
            }

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

    /**
     * 物品描述行：仿前置 {@code ItemTooltipManager#addTooltip}，为物品补一行
     * {@code tooltip.<命名空间>.item.<路径>} 描述（灰色，插在物品名之后）。
     *
     * <p>与前置一致地只处理后置物：只处理本模组命名空间的物品，且仅在存在对应翻译时追加
     * （{@link I18n#exists}）——描述完全由语言文件决定，不必在代码里再维护一张物品清单。</p>
     */
    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!AnvilCraftDogePlus.MOD_ID.equals(id.getNamespace())) return;
        String key = "tooltip.%s.item.%s".formatted(id.getNamespace(), id.getPath());
        if (!I18n.exists(key)) return;
        // 支持 \n 多行：倒序插到下标 1，保持原有顺序。
        List<Component> lines = new ArrayList<>();
        for (String line : Component.translatable(key).getString().split("\n")) {
            lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
        }
        for (int i = lines.size() - 1; i >= 0; i--) {
            event.getToolTip().add(1, lines.get(i));
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
