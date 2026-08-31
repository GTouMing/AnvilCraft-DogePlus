package dev.anvilcraft.gtouming.doge_plus.client.markdown.recipe;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * 镶嵌配方渲染组件：展示「基材 + 材料 → 镶嵌后的产物」。
 *
 * <p>对应 Markdown 扩展标签：{@code <recipe id="anvilcraft_doge_plus:inlay/..."/>}。</p>
 * <p>布局：左侧为基材（被镶嵌物），中间为镶嵌材料，右侧为镶嵌产物。</p>
 */
public class MDInlayRecipeComponent extends MDRecipeComponent {
    /** 配方底图（复用 AnvilCraft 的 256×128 底图）。 */
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/ageratum/256back.png");

    public static final int BASE_X = 40;
    public static final int BASE_Y = 46;
    public static final int INLAY_X = 100;
    public static final int INLAY_Y = 46;
    public static final int OUTPUT_X = 194;
    public static final int OUTPUT_Y = 46;
    public static final int ARROW_X = 138;
    public static final int ARROW_Y = 40;

    private final InlayRecipe recipe;

    public MDInlayRecipeComponent(InlayRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 256, 128, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        MaterialManager.InlayMaterial inlayMaterial = recipe.getInlayMaterial();
        MaterialManager.BaseMaterial baseMaterial = recipe.getBaseMaterial();
        if (inlayMaterial == null || baseMaterial == null) {
            return;
        }

        GuiGraphics guiGraphics = context.graphics();

        // 基材（被镶嵌物）——循环展示所有候选物品
        List<ItemStack> bases = Arrays.asList(baseMaterial.ingredient().getItems());
        if (!bases.isEmpty()) {
            ItemStack base = bases.get((int) ((System.currentTimeMillis() / 1000) % bases.size()));
            AgeratumUtil.renderItem(context, base, mouseX, mouseY, BASE_X, BASE_Y);
        }

        // 镶嵌材料
        List<ItemStack> inlays = Arrays.asList(inlayMaterial.ingredient().getItems());
        if (!inlays.isEmpty()) {
            ItemStack inlay = inlays.get((int) ((System.currentTimeMillis() / 1000) % inlays.size()));
            AgeratumUtil.renderItem(context, inlay, mouseX, mouseY, INLAY_X, INLAY_Y);
        }

        // 产物：镶嵌后的基材（首个候选）
        if (!bases.isEmpty() && !inlays.isEmpty()) {
            ItemStack base = bases.getFirst();
            ItemStack inlay = inlays.getFirst();
            ItemStack result = InlayUtil.withAddedInlay(base, InlayEntry.fromItemStack(inlay));
            AgeratumUtil.renderArrow(guiGraphics, ARROW_X, ARROW_Y);
            AgeratumUtil.renderItem(context, result, mouseX, mouseY, OUTPUT_X, OUTPUT_Y);
        }
    }
}
