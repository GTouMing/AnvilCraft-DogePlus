package dev.anvilcraft.gtouming.doge_plus.integration.jei;

import appeng.api.stacks.GenericStack;
import appeng.integration.modules.itemlists.EncodingHelper;
import appeng.menu.me.items.PatternEncodingTermMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JEI「+」→ AE2 样板编码终端。
 *
 * <p>AE2 19.2.18 没有 JEI 集成，但它的 {@link EncodingHelper#encodeProcessingRecipe} 是公开的，
 * 正是 EMI/REI 写入处理样板用的路径：设置处理模式、按网络优先级选料、合并同类输入、把内容写进
 * 处理输入/输出槽。这里直接复用它，并把 JEI 当前显示的精确堆叠（含 {@code INLAY} 组件）传进去。</p>
 *
 * <p>本类引用 AE2，只在 AE2 已加载时由 JEI 插件调用。</p>
 */
final class Ae2PatternTransfer {

    private Ae2PatternTransfer() {
    }

    static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
                new PatternTransfer<>(AnvilCraftDogePlusJeiPlugin.INLAY), AnvilCraftDogePlusJeiPlugin.INLAY);
        registration.addRecipeTransferHandler(
                new PatternTransfer<>(AnvilCraftDogePlusJeiPlugin.INLAY_CRAFTING),
                AnvilCraftDogePlusJeiPlugin.INLAY_CRAFTING);
        registration.addRecipeTransferHandler(
                new PatternTransfer<>(AnvilCraftDogePlusJeiPlugin.INLAY_SMITHING),
                AnvilCraftDogePlusJeiPlugin.INLAY_SMITHING);
        registration.addRecipeTransferHandler(
                new PatternTransfer<>(AnvilCraftDogePlusJeiPlugin.INLAY_COPYING),
                AnvilCraftDogePlusJeiPlugin.INLAY_COPYING);
    }

    private static final class PatternTransfer<R>
            implements IRecipeTransferHandler<PatternEncodingTermMenu, R> {

        private final RecipeType<R> recipeType;

        private PatternTransfer(RecipeType<R> recipeType) {
            this.recipeType = recipeType;
        }

        @Override
        public Class<? extends PatternEncodingTermMenu> getContainerClass() {
            return PatternEncodingTermMenu.class;
        }

        @Override
        public Optional<MenuType<PatternEncodingTermMenu>> getMenuType() {
            return Optional.of(PatternEncodingTermMenu.TYPE);
        }

        @Override
        public RecipeType<R> getRecipeType() {
            return this.recipeType;
        }

        @Nullable
        @Override
        public IRecipeTransferError transferRecipe(
                PatternEncodingTermMenu menu,
                R recipe,
                IRecipeSlotsView recipeSlots,
                Player player,
                boolean maxTransfer,
                boolean doTransfer) {
            if (!doTransfer) {
                return null; // 允许显示「+」，真正写入在 doTransfer 时执行
            }

            List<List<GenericStack>> inputs = new ArrayList<>();
            for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
                ItemStack stack = slot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
                if (stack.isEmpty()) continue;
                GenericStack generic = GenericStack.fromItemStack(stack);
                if (generic != null) inputs.add(List.of(generic));
            }

            List<GenericStack> outputs = new ArrayList<>();
            for (IRecipeSlotView slot : recipeSlots.getSlotViews(RecipeIngredientRole.OUTPUT)) {
                ItemStack stack = slot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
                if (stack.isEmpty()) continue;
                GenericStack generic = GenericStack.fromItemStack(stack);
                if (generic != null) outputs.add(generic);
            }

            if (inputs.isEmpty() || outputs.isEmpty()) {
                return null;
            }

            EncodingHelper.encodeProcessingRecipe(menu, inputs, outputs);
            return null;
        }
    }
}
