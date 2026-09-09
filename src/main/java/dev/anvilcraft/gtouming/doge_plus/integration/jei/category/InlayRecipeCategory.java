package dev.anvilcraft.gtouming.doge_plus.integration.jei.category;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.AnvilCraftDogePlusJeiPlugin;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.AbstractProgressCategory;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 镶嵌配方 JEI 界面：继承 {@link AbstractProgressCategory} 使用铁砧工艺通用布局，
 * 参考物品冲压（Stamping）安排界面。
 *
 * <p>一个数据包配方一页：材料/基材为标签时，匹配物品在两个输入槽中循环展示；
 * 输出逐帧跟随「当前展示的材料 × 基材」现算（display override），与镶合/锻造页同机制。</p>
 */
public class InlayRecipeCategory extends AbstractProgressCategory<InlayRecipe> {

    public InlayRecipeCategory(IGuiHelper helper) {
        super(helper,
                new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), ModBlocks.INLAY_TABLE.getDefaultState()),
                Component.translatable("gui.anvilcraft_doge_plus.jei.inlay"));
    }

    @Override
    public RecipeType<RecipeHolder<InlayRecipe>> getRecipeType() {
        return AnvilCraftDogePlusJeiPlugin.INLAY;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<InlayRecipe> recipeHolder, IFocusGroup focuses) {
        InlayRecipe recipe = recipeHolder.value();

        // 材料槽 - 传入材料列表
        builder.addInputSlot(JeiSlotUtil.INPUT_X - JeiSlotUtil.OFFSET / 2, JeiSlotUtil.DEFAULT_Y)
                .addItemStacks(getInlays(focuses, recipe));

        // 基材槽 - 传入基材列表
        builder.addInputSlot(JeiSlotUtil.INPUT_X + JeiSlotUtil.OFFSET / 2, JeiSlotUtil.DEFAULT_Y)
                .addItemStacks(getBases(focuses, recipe));

        // 输出槽 - 传入产品列表
        builder.addOutputSlot(JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y)
                .addItemStacks( getResults(focuses, recipe));
    }
    
    /**
     * 输出逐帧跟随：按「当前展示的材料 × 基材」现算镶嵌结果并覆盖输出槽，
     * 聚焦输出时从输出物品反推基材（同锻造/镶合页机制）。
     */
    @Override
    public void onDisplayedIngredientsUpdate(
            RecipeHolder<InlayRecipe> recipeHolder,
            List<IRecipeSlotDrawable> recipeSlots,
            IFocusGroup focuses) {
        if (recipeSlots.size() < 3) return;
        InlayRecipe recipe = recipeHolder.value();

        IRecipeSlotDrawable inlaySlot = recipeSlots.get(0);
        IRecipeSlotDrawable baseSlot = recipeSlots.get(1);
        IRecipeSlotDrawable outputSlot = recipeSlots.get(2);

        ItemStack inlay = inlaySlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
        ItemStack base = baseSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);

        // 聚焦输出：以输出物品作为基材展示，保证页面稳定
        if (focuses.getFocuses(RecipeIngredientRole.OUTPUT).findAny().isPresent()) {
            ItemStack displayed = outputSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
            if (displayed.isEmpty()) return;
            base = new ItemStack(displayed.getItem());
            baseSlot.createDisplayOverrides().addItemStack(base);
        }
        if (inlay.isEmpty() || base.isEmpty()) return;

        ItemStack output = inlayResult(recipe, inlay, base);
        if (output.isEmpty()) return;
        outputSlot.createDisplayOverrides().addItemStack(output);
    }

    /** 现算「基材 + 材料 → 基材追加该镶嵌」，属性取自配方引用的材料定义。 */
    private static ItemStack inlayResult(InlayRecipe recipe, ItemStack inlay, ItemStack base) {
        MaterialManager.InlayMaterial def = recipe.getInlayMaterial();
        InlayEntry entry = def != null
                ? InlayEntry.fromItemStack(inlay, def)
                : InlayEntry.fromItemStack(inlay);
        if (entry.isEmpty()) return ItemStack.EMPTY;
        return InlayUtil.withAddedInlay(base, entry);
    }

    public List<ItemStack> getResults(IFocusGroup focuses, InlayRecipe recipe) {
        MaterialManager.InlayMaterial inlayMaterial = recipe.getInlayMaterial();
        MaterialManager.BaseMaterial baseMaterial = recipe.getBaseMaterial();
        if (inlayMaterial == null || baseMaterial == null) return List.of();

        List<ItemStack> inlays = getInlays(focuses, recipe);
        List<ItemStack> bases = getBases(focuses, recipe);
        if (inlays.isEmpty() || bases.isEmpty()) return List.of();

        // 同步递增穷举：第 i 项 = (inlays[i % A], bases[i % B])，形如 11 22 13 21 12 23 …
        // JEI 各槽共享同一全局帧 n（材料槽显示 inlays[n % A]、基材槽 bases[n % B]），
        // 故输出槽第 n 项恰好等于两个输入槽当前帧的组合，三槽逐帧一一对应。
        int total = inlays.size() * bases.size();
        List<ItemStack> results = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            int a = i % inlays.size();
            int b = i % bases.size();
            results.add(InlayUtil.withAddedInlay(bases.get(b), InlayEntry.fromItemStack(inlays.get(a))));
        }

        return results;
    }

    public List<ItemStack> getInlays(IFocusGroup focuses, InlayRecipe recipe) {
        MaterialManager.InlayMaterial inlayMaterial = recipe.getInlayMaterial();
        if (inlayMaterial == null) return List.of();

        List<ItemStack> inlays = Arrays.asList(inlayMaterial.ingredient().getItems());
        List<ItemStack> focused = focuses.getItemStackFocuses().map(f -> f.getTypedValue().getIngredient()).toList();
        List<ItemStack> filterInlays = inlays.stream().filter(i -> focused.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p,i))).toList();
        return filterInlays.isEmpty() ? inlays : filterInlays;

    }

    public List<ItemStack> getBases(IFocusGroup focuses, InlayRecipe recipe) {
        MaterialManager.BaseMaterial baseMaterial = recipe.getBaseMaterial();
        if (baseMaterial == null) return List.of();

        List<ItemStack> bases = Arrays.asList(baseMaterial.ingredient().getItems());
        List<ItemStack> focused = focuses.getItemStackFocuses().map(f -> f.getTypedValue().getIngredient()).toList();
        List<ItemStack> filterBases = bases.stream().filter(i -> focused.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p,i))).toList();
        return filterBases.isEmpty()? bases : filterBases;
    }

    @Override
    public void draw(RecipeHolder<InlayRecipe> recipe, IRecipeSlotsView slotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
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
                guiGraphics, ModBlocks.INLAY_TABLE.getDefaultState(), 81, 40, 0, 12, RenderSupport.SINGLE_BLOCK);


        this.arrowIn.draw(guiGraphics, 54, 30);
        this.arrowOutputFromBelow.draw(guiGraphics, 92, 29);
        JeiSlotUtil.drawDefaultInputSlots(guiGraphics, this.slotDefault, 2);
        JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, this.slotDefault, 1);
    }
}