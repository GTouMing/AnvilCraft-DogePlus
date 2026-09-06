package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import dev.dubhe.anvilcraft.recipe.multiblock.BlockPredicateWithState;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import net.minecraft.world.item.Items;

/**
 * 复用前置 AnvilCraft 配方数据生成器（与前置自身 {@code *RecipeLoader} 同款 builder 写法）
 * 生成「本 mod 的 AnvilCraft 类型配方」：super_heating（Doge 钢熔炼）与
 * multiblock_conversion（Doge 钢块 3×3×3 → 巨型 Doge 砧）。
 */
public class ModAnvilRecipeHandler {

    private ModAnvilRecipeHandler() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        superHeatingRecipes(provider);
        giantDogeAnvilMultiblock(provider);
    }

    // ==================== super_heating ====================

    private static void superHeatingRecipes(RegistrumRecipeProvider provider) {
        SuperHeatingRecipe.builder()
                .requires(Items.IRON_INGOT)
                .requires(Items.BONE_MEAL)
                .result(ModItems.DOGE_STEEL_INGOT.get())
                .save(provider, AnvilCraftDogePlus.of("super_heating/doge_steel_ingot"));

        SuperHeatingRecipe.builder()
                .requires(Items.IRON_BLOCK)
                .requires(Items.BONE_BLOCK)
                .result(ModBlocks.DOGE_STEEL_BLOCK.get())
                .save(provider, AnvilCraftDogePlus.of("super_heating/doge_steel_block"));
    }

    // ==================== multiblock_conversion：巨型 Doge 砧 ====================

    private static void giantDogeAnvilMultiblock(RegistrumRecipeProvider provider) {
        MultiblockConversionRecipe.builder()
                .inputLayer("DDD", "DDD", "DDD")
                .inputLayer("   ", " D ", "   ")
                .inputLayer("DDD", "DDD", "DDD")
                .inputSymbol('D', ModBlocks.DOGE_STEEL_BLOCK.get())
                .outputLayer("ABC", "DEF", "GHI")
                .outputLayer("JKL", "MNO", "PQR")
                .outputLayer("STU", "VWX", "YZ[")
                .outputSymbol('A', part("bottom_wn"))
                .outputSymbol('B', part("bottom_n"))
                .outputSymbol('C', part("bottom_en"))
                .outputSymbol('D', part("bottom_w"))
                .outputSymbol('E', part("bottom_center"))
                .outputSymbol('F', part("bottom_e"))
                .outputSymbol('G', part("bottom_ws"))
                .outputSymbol('H', part("bottom_s"))
                .outputSymbol('I', part("bottom_es"))
                .outputSymbol('J', part("mid_wn"))
                .outputSymbol('K', part("mid_n"))
                .outputSymbol('L', part("mid_en"))
                .outputSymbol('M', part("mid_w"))
                .outputSymbol('N', centerPart("mid_center"))
                .outputSymbol('O', part("mid_e"))
                .outputSymbol('P', part("mid_ws"))
                .outputSymbol('Q', part("mid_s"))
                .outputSymbol('R', part("mid_es"))
                .outputSymbol('S', part("top_wn"))
                .outputSymbol('T', part("top_n"))
                .outputSymbol('U', part("top_en"))
                .outputSymbol('V', part("top_w"))
                .outputSymbol('W', part("top_center"))
                .outputSymbol('X', part("top_e"))
                .outputSymbol('Y', part("top_ws"))
                .outputSymbol('Z', part("top_s"))
                .outputSymbol('[', part("top_es"))
                .save(provider, AnvilCraftDogePlus.of("multiblock_conversion/giant_doge_anvil"));
    }

    /** 边缘/角落部件（cube=corner）。 */
    private static BlockPredicateWithState part(String half) {
        return BlockPredicateWithState.of(ModBlocks.GIANT_DOGE_ANVIL.get())
                .hasState("cube", "corner")
                .hasState("half", half);
    }

    /** 中心部件（cube=center）。 */
    private static BlockPredicateWithState centerPart(String half) {
        return BlockPredicateWithState.of(ModBlocks.GIANT_DOGE_ANVIL.get())
                .hasState("cube", "center")
                .hasState("half", half);
    }
}
