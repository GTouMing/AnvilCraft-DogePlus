package dev.anvilcraft.gtouming.doge_plus.client.markdown.recipe;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 镶嵌配方渲染组件：展示「基材 + 镶嵌材料 →（铁砧砸在镶嵌台上）→ 镶嵌后的基材」。
 *
 * <p>对应 Markdown 扩展标签：{@code <recipe id="anvilcraft_doge_plus:inlay/..."/>}。</p>
 *
 * <p>布局沿用高温熔炼那套 anvil 家族写法：输入物品（左）→ 铁砧 + 工作方块（中）→ 产物（右），
 * 物品组交给 {@link AgeratumUtil#renderItems} 排布。</p>
 *
 * <p>没有直接继承前置的 {@code MDBaseAnvilRecipeComponent}：基类只接受
 * {@code ItemIngredientPredicate}，而这里的产物是「基材 + 材料」现拼出来的带镶嵌物品，
 * 用它表达会把镶嵌内容画丢。</p>
 */
public class MDInlayRecipeComponent extends MDRecipeComponent {
    /** 与前置 anvil 家族共用同一张 256×128 底图。 */
    public static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("anvilcraft", "textures/gui/ageratum/256back.png");

    /** 输入 / 产物的物品组起点，以及中间那组铁砧 + 工作方块的位置（同前置 anvil 家族布局）。 */
    private static final int INPUT_X = 40;
    private static final int INPUT_Y = 46;
    private static final int INPUT_ARROW_X = 86;
    private static final int BLOCK_X = 128;
    private static final int BLOCK_Y = 64;
    private static final int OUTPUT_ARROW_X = 138;
    private static final int OUTPUT_X = 194;
    private static final int ARROW_Y = 40;
    private static final int OUTPUT_Y = 46;

    private final InlayRecipe recipe;

    public MDInlayRecipeComponent(InlayRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 256, 128, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        MaterialManager.InlayMaterial inlayMaterial = recipe.getInlayMaterial();
        MaterialManager.BaseMaterial baseMaterial = recipe.getBaseMaterial();
        if (inlayMaterial == null || baseMaterial == null) return;

        ItemStack base = displayOf(baseMaterial.ingredient());
        ItemStack inlay = displayOf(inlayMaterial.ingredient());
        if (base.isEmpty() || inlay.isEmpty()) return;

        GuiGraphics g = context.graphics();

        // 输入：基材 + 镶嵌材料
        AgeratumUtil.renderItems(context, List.of(base, inlay), mouseX, mouseY, INPUT_X, INPUT_Y);
        AgeratumUtil.renderArrow(g, INPUT_ARROW_X, ARROW_Y);

        // 加工环境：铁砧砸在镶嵌台上
        AgeratumUtil.renderBlock(
                context,
                Blocks.ANVIL.defaultBlockState(),
                mouseX,
                mouseY,
                BLOCK_X,
                BLOCK_Y - 2 * AgeratumUtil.BLOCK_SIZE,
                100);
        AgeratumUtil.renderBlock(
                context, ModBlocks.INLAY_TABLE.getDefaultState(), mouseX, mouseY, BLOCK_X, BLOCK_Y, 10);

        AgeratumUtil.renderArrow(g, OUTPUT_ARROW_X, ARROW_Y);

        // 产物：镶嵌后的基材
        ItemStack result = InlayUtil.withAddedInlay(base, InlayEntry.fromItemStack(inlay));
        AgeratumUtil.renderItems(context, List.of(result), mouseX, mouseY, OUTPUT_X, OUTPUT_Y);
    }

    /** 候选物品里按时间轮换展示一个（与前置 anvil 家族一致）。 */
    private static ItemStack displayOf(Ingredient ingredient) {
        ItemStack[] items = ingredient.getItems();
        return items.length == 0 ? ItemStack.EMPTY : items[RecipeUtil.getDisplayIndex(items.length)];
    }
}
