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

import java.awt.Color;
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

    /** 从图标左上角开始绘制。 */
    private static final int ORIGIN = 0;
    /** 每个像素点的边长。 */
    private static final int DOT_SIZE = 2;
    /** 不同材料行/列之间的间隔。 */
    private static final int GAP = 1;
    /** 不同材料行/列之间的步距：2px 点 + 1px 间隔。 */
    private static final int MATERIAL_PITCH = DOT_SIZE + GAP;
    /** 同一材料的属性紧邻：步距等于点边长，不留间隔。 */
    private static final int PROPERTY_PITCH = DOT_SIZE;
    /** 18×18 槽位内最多容纳的材料行/列数（6 × 3 = 18）。 */
    private static final int MAX_MATERIALS = 6;
    /** 18×18 槽位内最多容纳的属性数。 */
    private static final int MAX_PROPERTIES = 6;
    private static final int ALPHA = 0xFF000000;
    private static final int FALLBACK_COLOR = 0xFFFFFFFF;
    /** 物品模型/数量/耐久在更高的 z 上；装饰点需再抬一层，否则会被物品遮挡。 */
    private static final float OVERLAY_Z = 300.0F;

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

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, OVERLAY_Z);
        try {
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
                    fillDot(graphics, pixelX, pixelY, color);
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

                    fillDot(graphics, pixelX, pixelY, ALPHA | property.getColor());
                    drew = true;
                    dotIndex++;
                }
            }
        } finally {
            graphics.pose().popPose();
        }

        return drew;
    }

    /**
     * 2×2 点拆成四个 1px 象限：左上高光、右下阴影，形成轻微立体质感。
     * 只调明度与饱和度，保留属性色/材质色的色相。
     */
    private static void fillDot(GuiGraphics graphics, int x, int y, int baseColor) {
        graphics.fill(x, y, x + 1, y + 1, shade(baseColor, 1.24F, 0.82F));
        graphics.fill(x + 1, y, x + 2, y + 1, shade(baseColor, 1.08F, 0.96F));
        graphics.fill(x, y + 1, x + 1, y + 2, shade(baseColor, 0.88F, 1.06F));
        graphics.fill(x + 1, y + 1, x + 2, y + 2, shade(baseColor, 0.70F, 1.16F));
    }

    /** 以 HSB 调整明度/饱和度，保留 alpha 与色相。 */
    private static int shade(int argb, float brightness, float saturation) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        float[] hsb = Color.RGBtoHSB(r, g, b, null);
        hsb[1] = Math.clamp(hsb[1] * saturation, 0.0F, 1.0F);
        hsb[2] = Math.clamp(hsb[2] * brightness, 0.0F, 1.0F);
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]) & 0xFFFFFF;
        return (a << 24) | rgb;
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
