package dev.anvilcraft.gtouming.doge_plus.client;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物品图标左上角的镶嵌属性像素点阵列。
 *
 * <p>每个已镶嵌材料占用一行（竖排）或一列（横排），行/列数严格等于已镶嵌材料数；
 * 同一材料带来的属性在同一行/列内紧邻排列。无属性的材料也占一个点，颜色取该物品/方块
 * 模型材质（粒子图标）中距中心最近的不透明像素，作为最近邻采样的代表色。</p>
 *
 * <p>由客户端配置 {@code showInlayDots} 控制，默认关闭。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class InlayIconDecorator implements IItemDecorator {

    /** 距图标左上角的偏移（留 1px 边距）。 */
    private static final int ORIGIN = 1;
    /** 不同材料行/列之间的间距：1px 点 + 1px 间隔。 */
    private static final int MATERIAL_PITCH = 2;
    /** 同一材料属性之间的间距：紧邻。 */
    private static final int PROPERTY_PITCH = 1;
    /** 16×16 图标内最多容纳的材料行/列数。 */
    private static final int MAX_MATERIALS = 8;
    /** 16×16 图标内最多容纳的属性数。 */
    private static final int MAX_PROPERTIES = 8;
    private static final int ALPHA = 0xFF000000;
    private static final int FALLBACK_COLOR = 0xFFFFFFFF;

    /** 按材料物品缓存的材质代表色（ARGB）。 */
    private static final Map<ResourceLocation, Integer> MATERIAL_COLORS = new HashMap<>();

    private static final InlayIconDecorator INSTANCE = new InlayIconDecorator();

    private InlayIconDecorator() {
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            event.register(item, INSTANCE);
        }
    }

    @Override
    public boolean render(GuiGraphics graphics, Font font, ItemStack stack, int x, int y) {
        if (!AnvilCraftDogePlus.CLIENT_CONFIG.showInlayDots) return false;

        List<InlayEntry> materials = new ArrayList<>();
        for (InlayEntry entry : InlayUtil.getInlays(stack)) {
            if (!entry.isEmpty()) materials.add(entry);
        }
        if (materials.isEmpty()) return false;

        boolean vertical = AnvilCraftDogePlus.CLIENT_CONFIG.verticalInlayDots;
        int materialLimit = Math.min(materials.size(), MAX_MATERIALS);
        boolean drew = false;

        for (int materialIndex = 0; materialIndex < materialLimit; materialIndex++) {
            InlayEntry entry = materials.get(materialIndex);
            List<ResourceLocation> attributes = entry.attributes();
            int dotIndex = 0;

            if (attributes.isEmpty()) {
                // 无属性镶嵌：用材料本身的物品/方块材质代表色。
                int color = materialColor(entry);
                int pixelX = vertical
                        ? x + ORIGIN + dotIndex * PROPERTY_PITCH
                        : x + ORIGIN + materialIndex * MATERIAL_PITCH;
                int pixelY = vertical
                        ? y + ORIGIN + materialIndex * MATERIAL_PITCH
                        : y + ORIGIN + dotIndex * PROPERTY_PITCH;
                graphics.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, color);
                drew = true;
                continue;
            }

            for (int attributeIndex = 0;
                 attributeIndex < attributes.size() && dotIndex < MAX_PROPERTIES;
                 attributeIndex++) {
                InlayProperty property = InlayProperty.get(attributes.get(attributeIndex));
                if (property == null) continue;

                int pixelX = vertical
                        ? x + ORIGIN + dotIndex * PROPERTY_PITCH
                        : x + ORIGIN + materialIndex * MATERIAL_PITCH;
                int pixelY = vertical
                        ? y + ORIGIN + materialIndex * MATERIAL_PITCH
                        : y + ORIGIN + dotIndex * PROPERTY_PITCH;

                graphics.fill(pixelX, pixelY, pixelX + 1, pixelY + 1, ALPHA | property.getColor());
                drew = true;
                dotIndex++;
            }
        }

        return drew;
    }

    /**
     * 材质代表色：取物品模型粒子图标中距中心最近的不透明像素，相当于把整张贴图
     * 最近邻缩放到 1px 后得到的颜色。
     */
    private static int materialColor(InlayEntry entry) {
        return MATERIAL_COLORS.computeIfAbsent(entry.id(), InlayIconDecorator::sampleMaterialColor);
    }

    private static int sampleMaterialColor(ResourceLocation id) {
        ItemStack material = new ItemStack(BuiltInRegistries.ITEM.get(id));
        if (material.isEmpty()) return FALLBACK_COLOR;

        Minecraft minecraft = Minecraft.getInstance();
        BakedModel model = minecraft.getItemRenderer().getModel(
                material, minecraft.level, minecraft.player, 0);
        TextureAtlasSprite sprite = model.getParticleIcon(ModelData.EMPTY);
        int width = sprite.contents().width();
        int height = sprite.contents().height();
        if (width <= 0 || height <= 0) return FALLBACK_COLOR;

        int centerX = width / 2;
        int centerY = height / 2;
        int nearest = FALLBACK_COLOR;
        int nearestDistance = Integer.MAX_VALUE;

        for (int py = 0; py < height; py++) {
            for (int px = 0; px < width; px++) {
                int abgr = sprite.getPixelRGBA(0, px, py);
                if ((abgr >>> 24) == 0) continue;

                int dx = px - centerX;
                int dy = py - centerY;
                int distance = dx * dx + dy * dy;
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = toArgb(abgr);
                }
            }
        }

        return nearest;
    }

    private static int toArgb(int abgr) {
        int a = (abgr >>> 24) & 0xFF;
        int b = (abgr >>> 16) & 0xFF;
        int g = (abgr >>> 8) & 0xFF;
        int r = abgr & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
