package dev.anvilcraft.gtouming.doge_plus.integration.jei;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.AbstractChuteScreen;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.category.InlayRecipeCategory;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.dubhe.anvilcraft.integration.jei.handlers.GhostIngredientHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;

/**
 * JEI 集成入口：注册镶嵌配方类别、配方与催化方块（镶嵌台）。
 * <p>实现 {@link IModPlugin} 即可被 JEI 自动发现，无需额外声明。</p>
 */
@JeiPlugin
public class AnvilCraftDogePlusJeiPlugin implements IModPlugin {

    public static final RecipeType<RecipeHolder<InlayRecipe>> INLAY =
            RecipeType.createRecipeHolderType(AnvilCraftDogePlus.of("inlay"));

    @Override
    public ResourceLocation getPluginUid() {
        return AnvilCraftDogePlus.of("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new InlayRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        registration.addRecipes(INLAY, level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.INLAY_TABLE.get(), INLAY);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
            AbstractChuteScreen.class,
            new GhostIngredientHandler<>()
        );
    }
}
