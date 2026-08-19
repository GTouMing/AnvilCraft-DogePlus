package dev.anvilcraft.gtouming.doge_plus.integration.jei.category;

import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.AnvilCraftDogePlusJeiPlugin;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.category.anvil.AbstractProgressCategory;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

/**
 * 镶嵌配方 JEI 界面：继承 {@link AbstractProgressCategory} 使用铁砧工艺通用布局，
 * 参考物品冲压（Stamping）安排界面。
 *
 * <p>一个数据包配方一页：材料/基材为标签时，匹配物品在槽位中循环展示；
 * 输出为所有「材料 × 基材」组合，合并到一个输出槽循环展示。</p>
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
                .addItemStacks(recipe.getInlays(focuses));

        // 基材槽 - 传入基材列表
        builder.addInputSlot(JeiSlotUtil.INPUT_X + JeiSlotUtil.OFFSET / 2, JeiSlotUtil.DEFAULT_Y)
                .addItemStacks(recipe.getBases(focuses));

        // 输出槽 - 传入产品列表
        builder.addOutputSlot(JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y)
                .addItemStacks( recipe.getResults(focuses));
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