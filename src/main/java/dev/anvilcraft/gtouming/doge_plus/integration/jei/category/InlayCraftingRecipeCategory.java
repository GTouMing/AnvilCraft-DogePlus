package dev.anvilcraft.gtouming.doge_plus.integration.jei.category;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.AnvilCraftDogePlusJeiPlugin;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayOutput;
import dev.anvilcraft.gtouming.doge_plus.util.InlayRecipeTooltips;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 镶合配方 JEI 界面：仿照 {@link InlayRecipeCategory} 的铁砧工艺布局，
 * 展示「已镶满的基材（单输入槽）→ 全部产物」。
 *
 * <p>输入槽为配方 base 施加全部 {@code inlays} 后的成品基材
 * （含 INLAY 组件；多物品/标签材料取笛卡尔组合循环展示）。
 * 输出槽由 {@link InlayCraftingRecipe#previewFor} 动态生成，因此多产物、概率产物、
 * 按输入推导的产物（中子锭行为表 / 盔甲纹饰）都能完整列出；概率产物在 tooltip 标注百分比。
 * 催化方块为镶嵌台与镶合台。</p>
 */
public class InlayCraftingRecipeCategory implements IRecipeCategory<RecipeHolder<InlayCraftingRecipe>> {

    private static final int WIDTH = 162;
    private static final int HEIGHT = 64;

    private final Component title;
    private final IDrawable icon;
    private final RecipeType<RecipeHolder<InlayCraftingRecipe>> recipeType;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final IDrawable arrowIn;
    private final IDrawable arrowOutputFromBelow;
    private final ITickTimer timer;

    public InlayCraftingRecipeCategory(IGuiHelper helper) {
        this(helper, AnvilCraftDogePlusJeiPlugin.INLAY_CRAFTING,
                "gui.anvilcraft_doge_plus.jei.inlay_crafting");
    }

    /**
     * 三类镶合（合成 / 锻造 / 复制）共用同一套界面，只是分类与标题不同。
     */
    public InlayCraftingRecipeCategory(
            IGuiHelper helper,
            RecipeType<RecipeHolder<InlayCraftingRecipe>> recipeType,
            String titleKey) {
        this.recipeType = recipeType;
        this.title = Component.translatable(titleKey);
        this.icon = new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                ModBlocks.INLAY_CRAFTING_TABLE.getDefaultState());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOutputFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
        this.timer = helper.createTickTimer(30, 60, true);
    }

    @Override
    public RecipeType<RecipeHolder<InlayCraftingRecipe>> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
            IRecipeLayoutBuilder builder,
            RecipeHolder<InlayCraftingRecipe> recipeHolder,
            IFocusGroup focuses) {
        InlayCraftingRecipe recipe = recipeHolder.value();

        List<ItemStack> inlaidBases = buildInlaidBases(recipe);
        ItemStack sampleBase = inlaidBases.isEmpty()
                ? new ItemStack(recipe.getBaseItem())
                : inlaidBases.getFirst();
        List<InlayOutput.Preview> previews = previews(recipe, sampleBase);

        // 输入：已镶满的基材（base + 全部 inlays），单槽
        List<ItemStack> displayedInputs = filterFocused(inlaidBases, focuses);
        if (!displayedInputs.isEmpty()) {
            builder.addInputSlot(JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
                    .addItemStacks(displayedInputs)
                    .addRichTooltipCallback((view, tooltip) -> {
                        InlayRecipeTooltips.propertyRequirements(recipe.inlays()).forEach(tooltip::add);
                        // 超限合金配方：标明本条配方要求的附魔条数（同前置，金色）
                        recipe.enchantRange().ifPresent(range ->
                                tooltip.add(InlayRecipeTooltips.transcendiumEnchantments(range)));
                    });
        }

        // 输出：配方声明的全部产物（多产物 / 概率产物 / 推导产物），逐格标注数量与概率
        int total = previews.size();
        for (int i = 0; i < total; i++) {
            InlayOutput.Preview preview = previews.get(i);
            int[] pos = gridPos(i, total);
            builder.addOutputSlot(pos[0], pos[1])
                    .addItemStack(preview.stack())
                    .addRichTooltipCallback((view, tooltip) -> outputTooltip(preview, tooltip));
        }
    }

    /**
     * 产物 tooltip：数量 / 概率随附魔条数变化时按前置文案给出说明（金色），
     * 如「数量为魔咒数量 × 3」「概率：(10 × (魔咒数量)) / 100」；
     * 固定概率低于 100% 时给百分数。
     */
    private static void outputTooltip(InlayOutput.Preview preview, ITooltipBuilder tooltip) {
        if (preview.count().isEnchantScaled()) {
            tooltip.add(InlayRecipeTooltips.transcendiumAmount(preview.count().multiplier()));
        }
        if (preview.chance().isEnchantScaled()) {
            tooltip.add(InlayRecipeTooltips.transcendiumChance(preview.chance().multiplier()));
        } else if (preview.chance().evaluate(0) < 1.0) {
            tooltip.add(InlayRecipeTooltips.chance(preview.chance()));
        }
    }

    /** 计算配方的展示用产物（所有可能产物，不判定概率）。 */
    private static List<InlayOutput.Preview> previews(InlayCraftingRecipe recipe, ItemStack baseStack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return List.of();
        return recipe.previewFor(baseStack, level.registryAccess());
    }

    /** 每个镶孔在 JEI 中最多展示的候选物品数（避免纹饰 tag 组合爆炸）。 */
    private static final int MAX_INLAY_CHOICES = 8;

    /**
     * 输入槽最多逐帧展示多少种「已镶满的基材」。
     *
     * <p>帧数是各孔候选数的乘积：两个镶孔时最多 8×8=64（就是现在的上限），
     * 但逻辑载体有 6 个镶孔，不封顶会到 8⁶ 帧。封顶不影响已有配方。</p>
     */
    private static final int MAX_FRAMES = 64;

    /**
     * 构造「已镶满」的基材（与 JEI 帧同步）：不展开笛卡尔积，而是按全局帧 i 从各镶孔
     * 候选列表同步取 {@code choices[slot][i % choices[slot].size()]}，相邻帧的镶孔组合
     * 交错变化（两镶孔各 2/3 候选时形如 11,22,13,21,12,23），避免步进时组合单调堆积。
     * 条目不含属性/额外数据（展示用）。
     */
    private static List<ItemStack> buildInlaidBases(InlayCraftingRecipe recipe) {
        List<List<ItemStack>> choices = new ArrayList<>();
        for (InlayMatcher matcher : recipe.inlays()) {
            List<ItemStack> items = new ArrayList<>();
            for (ItemStack stack : matcher.candidates()) {
                if (items.size() >= MAX_INLAY_CHOICES) break;
                if (stack.isEmpty() || items.stream().anyMatch(e -> e.is(stack.getItem()))) continue;
                items.add(stack);
            }
            if (items.isEmpty()) return List.of();
            choices.add(items);
        }

        int frames = 1;
        for (List<ItemStack> choice : choices) {
            // 边乘边封顶，既避免 6 镶孔时的指数爆炸，也不会溢出。
            frames = Math.min(MAX_FRAMES, frames * choice.size());
        }

        List<ItemStack> results = new ArrayList<>(frames);
        for (int i = 0; i < frames; i++) {
            ItemStack base = new ItemStack(recipe.getBaseItem());
            for (List<ItemStack> choice : choices) {
                base = InlayUtil.withAddedInlay(base, entryOf(choice.get(i % choice.size())));
            }
            results.add(base);
        }
        return results;
    }

    /** 由物品生成无属性镶孔条目（镶合展示用，与「锻造材料无需属性」语义一致）。 */
    private static InlayEntry entryOf(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id.equals(BuiltInRegistries.ITEM.getDefaultKey()) || stack.getItem() == Items.AIR) {
            return InlayEntry.empty();
        }
        return new InlayEntry(id, List.of(), List.of());
    }

    /** 与 {@link JeiSlotUtil} 输出网格一致的排布，保证图标与槽框对齐（总数决定网格大小）。 */
    private static int[] gridPos(int index, int total) {
        if (total <= 0) return new int[] {JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y};
        int cols = (int) Math.ceil(Math.sqrt(total));
        int rows = Math.ceilDiv(total, cols);
        int startX = JeiSlotUtil.OUTPUT_X - (cols - 1) * JeiSlotUtil.OFFSET / 2;
        int startY = JeiSlotUtil.DEFAULT_Y - (rows - 1) * JeiSlotUtil.OFFSET / 2;
        return new int[] {startX + (index % cols) * JeiSlotUtil.OFFSET, startY + (index / cols) * JeiSlotUtil.OFFSET};
    }

    /** JEI 聚焦过滤：与当前聚焦物品匹配时只保留命中项，否则显示全部。 */
    private static List<ItemStack> filterFocused(List<ItemStack> stacks, IFocusGroup focuses) {
        List<ItemStack> focused = focuses.getItemStackFocuses()
                .map(f -> f.getTypedValue().getIngredient())
                .toList();
        if (focused.isEmpty()) return stacks;
        List<ItemStack> filtered = stacks.stream()
                .filter(i -> focused.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p, i)))
                .toList();
        return filtered.isEmpty() ? stacks : filtered;
    }

    /**
     * 含推导产物（盔甲纹饰 / 中子锭行为表）的配方按当前展示的基材逐帧重算输出槽，
     * 使输出与输入帧对应；纯固定产物的配方无需重算。
     */
    @Override
    public void onDisplayedIngredientsUpdate(
            RecipeHolder<InlayCraftingRecipe> recipeHolder,
            List<IRecipeSlotDrawable> recipeSlots,
            IFocusGroup focuses) {
        InlayCraftingRecipe recipe = recipeHolder.value();
        if (!recipe.hasDynamicResults()) return;
        // 聚焦输出时保留 JEI 默认行为
        if (focuses.getFocuses(RecipeIngredientRole.OUTPUT).findAny().isPresent()) return;
        if (recipeSlots.size() < 2) return;

        ItemStack inlaid = recipeSlots.getFirst().getDisplayedItemStack().orElse(ItemStack.EMPTY);
        if (inlaid.isEmpty()) return;
        List<InlayOutput.Preview> previews = previews(recipe, inlaid);
        for (int i = 0; i < previews.size() && i + 1 < recipeSlots.size(); i++) {
            recipeSlots.get(i + 1).createDisplayOverrides().addItemStack(previews.get(i).stack());
        }
    }

    @Override
    public void draw(
            RecipeHolder<InlayCraftingRecipe> recipeHolder,
            IRecipeSlotsView slotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                81,
                22 + anvilYOffset,
                20,
                12,
                RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
                guiGraphics,
                ModBlocks.INLAY_CRAFTING_TABLE.getDefaultState(),
                81,
                40,
                0,
                12,
                RenderSupport.SINGLE_BLOCK);

        this.arrowIn.draw(guiGraphics, 54, 30);
        this.arrowOutputFromBelow.draw(guiGraphics, 92, 29);
        JeiSlotUtil.drawDefaultInputSlots(guiGraphics, this.slotDefault, 1);
        // 输出槽框数量与 setRecipe 的 gridPos 网格一致（按产物数量决定网格大小），
        // 物品坐标 +1 偏移与输入槽约定一致。
        int outputs = slotsView.getSlotViews(RecipeIngredientRole.OUTPUT).size();
        List<InlayOutput> results = recipeHolder.value().results();
        for (int i = 0; i < outputs; i++) {
            int[] pos = gridPos(i, outputs);
            // 概率产出用前置的虚影槽贴图（与 AnvilCraft 的概率产物约定一致）
            boolean chance = i < results.size() && results.get(i).isChance();
            (chance ? this.slotProbability : this.slotDefault).draw(guiGraphics, pos[0] - 1, pos[1] - 1);
        }
    }
}
