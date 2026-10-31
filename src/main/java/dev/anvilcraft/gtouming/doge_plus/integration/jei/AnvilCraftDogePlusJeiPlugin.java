package dev.anvilcraft.gtouming.doge_plus.integration.jei;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.AbstractChuteScreen;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.category.InlayCraftingRecipeCategory;
import dev.anvilcraft.gtouming.doge_plus.integration.jei.category.InlayRecipeCategory;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.handlers.GhostIngredientHandler;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * JEI 集成入口：注册镶嵌配方类别、配方与催化方块（镶嵌台）。
 * <p>实现 {@link IModPlugin} 即可被 JEI 自动发现，无需额外声明。</p>
 */
@JeiPlugin
public class AnvilCraftDogePlusJeiPlugin implements IModPlugin {

    public static final RecipeType<RecipeHolder<InlayRecipe>> INLAY =
            RecipeType.createRecipeHolderType(AnvilCraftDogePlus.of("inlay"));
    public static final RecipeType<RecipeHolder<InlayCraftingRecipe>> INLAY_CRAFTING =
            RecipeType.createRecipeHolderType(AnvilCraftDogePlus.of("inlay_crafting"));
    /** 镶嵌锻造：来自前置锻造配方，模具随产物一并消耗。 */
    public static final RecipeType<RecipeHolder<InlayCraftingRecipe>> INLAY_SMITHING =
            RecipeType.createRecipeHolderType(AnvilCraftDogePlus.of("inlay_smithing"));
    /** 镶嵌复制：来自前置珠宝复制配方，返还模具并产出复制品。 */
    public static final RecipeType<RecipeHolder<InlayCraftingRecipe>> INLAY_COPYING =
            RecipeType.createRecipeHolderType(AnvilCraftDogePlus.of("inlay_copying"));

    @Override
    public ResourceLocation getPluginUid() {
        return AnvilCraftDogePlus.of("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new InlayRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new InlayCraftingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new InlayCraftingRecipeCategory(registration.getJeiHelpers().getGuiHelper(),
                        INLAY_SMITHING, "gui.anvilcraft_doge_plus.jei.inlay_smithing"),
                new InlayCraftingRecipeCategory(registration.getJeiHelpers().getGuiHelper(),
                        INLAY_COPYING, "gui.anvilcraft_doge_plus.jei.inlay_copying"));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        registration.addRecipes(INLAY, level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get()));
        // 三类镶合共用同一个配方类型，按 kind 分到三个分类里
        List<RecipeHolder<InlayCraftingRecipe>> inlayCrafting = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get());
        registration.addRecipes(INLAY_CRAFTING, byKind(inlayCrafting, InlayCraftingRecipe.Kind.CRAFTING));
        registration.addRecipes(INLAY_SMITHING, byKind(inlayCrafting, InlayCraftingRecipe.Kind.SMITHING));
        registration.addRecipes(INLAY_COPYING, byKind(inlayCrafting, InlayCraftingRecipe.Kind.COPYING));
    }

    /** 按镶合类别筛选配方。 */
    private static List<RecipeHolder<InlayCraftingRecipe>> byKind(
            List<RecipeHolder<InlayCraftingRecipe>> recipes, InlayCraftingRecipe.Kind kind) {
        return recipes.stream().filter(holder -> holder.value().kind() == kind).toList();
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // AE2 的样板编码终端：JEI「+」直接写入处理样板（AE2 自身在 19.2.18 没有 JEI 集成）。
        if (!ModList.get().isLoaded("ae2")) return;
        Ae2PatternTransfer.register(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.INLAY_TABLE.get(), INLAY);
        // 镶合配方需要先在镶嵌台镶满基材，再放入镶合台砸砧；三类共用同一张台子
        registration.addRecipeCatalyst(ModBlocks.INLAY_TABLE.get(), INLAY_CRAFTING);
        registration.addRecipeCatalyst(ModBlocks.INLAY_CRAFTING_TABLE.get(), INLAY_CRAFTING);
        registration.addRecipeCatalyst(ModBlocks.INLAY_TABLE.get(), INLAY_SMITHING);
        registration.addRecipeCatalyst(ModBlocks.INLAY_CRAFTING_TABLE.get(), INLAY_SMITHING);
        registration.addRecipeCatalyst(ModBlocks.INLAY_TABLE.get(), INLAY_COPYING);
        registration.addRecipeCatalyst(ModBlocks.INLAY_CRAFTING_TABLE.get(), INLAY_COPYING);
        // 巨狗砧是这两条配方（多方块转化 / 多方块合成）的产物，登记成对应分类的催化方块后，
        // 对着它按 U（用途）就能翻到这两页配方；按 R（来源）则由产物槽给出。
        registration.addRecipeCatalyst(ModBlocks.GIANT_DOGE_ANVIL.get(), AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
        registration.addRecipeCatalyst(ModBlocks.GIANT_DOGE_ANVIL.get(), AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGhostIngredientHandler(
            AbstractChuteScreen.class,
            new GhostIngredientHandler<>()
        );
    }
}
