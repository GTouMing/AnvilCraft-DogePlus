package dev.anvilcraft.gtouming.doge_plus.client.markdown.recipe;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayOutput;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * 镶合配方渲染组件：展示「已镶满的基材 →（铁砧砸在镶合台上）→ 产物（+ 返还的空基材）」。
 *
 * <p>对应 Markdown 扩展标签 {@code <recipe id="anvilcraft_doge_plus:inlay_crafting/..."/>}；
 * 三类镶合（合成 / 锻造 / 复制）共用同一个配方类型，因此也都由本组件渲染。</p>
 *
 * <p>布局沿用高温熔炼那套 anvil 家族写法：输入物品（左）→ 铁砧 + 工作方块（中）→ 产物（右），
 * 物品组交给 {@link AgeratumUtil#renderItems} 排布。输入是各镶孔代表物品现拼出来的带镶嵌基材，
 * 不能用前置基类要求的 {@code ItemIngredientPredicate} 表达，因此没有直接继承它。</p>
 *
 * <p>产物取配方声明的全部产物（含返还的模具、复制类的消失诅咒、按输入推导的中子锭 / 纹饰），
 * 由 {@link InlayCraftingRecipe#previewFor} 列出所有可能产物。</p>
 */
public class MDInlayCraftingRecipeComponent extends MDRecipeComponent {
    /** 与镶嵌组件共用同一张 256×128 底图。 */
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

    private final InlayCraftingRecipe recipe;

    public MDInlayCraftingRecipeComponent(InlayCraftingRecipe recipe, boolean enableAlignCenter) {
        super(TEXTURE, 256, 128, enableAlignCenter);
        this.recipe = recipe;
    }

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {
        ItemStack inlaidBase = inlaidBase();
        if (inlaidBase.isEmpty()) return;

        GuiGraphics g = context.graphics();

        // 输入：已镶满的基材
        AgeratumUtil.renderItems(context, List.of(inlaidBase), mouseX, mouseY, INPUT_X, INPUT_Y);
        AgeratumUtil.renderArrow(g, INPUT_ARROW_X, ARROW_Y);

        // 加工环境：铁砧砸在镶合台上
        AgeratumUtil.renderBlock(
                context,
                Blocks.ANVIL.defaultBlockState(),
                mouseX,
                mouseY,
                BLOCK_X,
                BLOCK_Y - 2 * AgeratumUtil.BLOCK_SIZE,
                100);
        AgeratumUtil.renderBlock(
                context, ModBlocks.INLAY_CRAFTING_TABLE.getDefaultState(), mouseX, mouseY, BLOCK_X, BLOCK_Y, 10);

        AgeratumUtil.renderArrow(g, OUTPUT_ARROW_X, ARROW_Y);

        // 产物 + 返还的空基材
        AgeratumUtil.renderItems(context, outputs(inlaidBase), mouseX, mouseY, OUTPUT_X, OUTPUT_Y);
    }

    /** 预览用的「已镶满的基材」：基材 + 每个镶孔各取一个代表物品。 */
    private ItemStack inlaidBase() {
        ItemStack base = new ItemStack(this.recipe.getBaseItem());
        for (InlayMatcher matcher : this.recipe.inlays()) {
            List<ItemStack> candidates = matcher.candidates();
            if (candidates.isEmpty()) continue;
            base = InlayUtil.withAddedInlay(base, InlayEntry.fromItemStack(candidates.getFirst()));
        }
        return base;
    }

    /** 产物列表：配方声明的全部产物（含返还的模具、复制类的诅咒、推导产物）。 */
    private List<ItemStack> outputs(ItemStack inlaidBase) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        List<ItemStack> outputs = new ArrayList<>();
        for (InlayOutput.Preview preview : this.recipe.previewFor(inlaidBase, level.registryAccess())) {
            if (!preview.stack().isEmpty()) outputs.add(preview.stack());
        }
        return outputs;
    }
}
