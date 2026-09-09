package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SuperHeatingRecipe;
import net.minecraft.world.item.Items;

/**
 * 复用前置 AnvilCraft 配方数据生成器（与前置自身 {@code *RecipeLoader} 同款 builder 写法）
 * 生成「本 mod 的 AnvilCraft 类型配方」：super_heating（Doge 钢熔炼）。
 * <p>巨型 Doge 砧的 multiblock_conversion 配方不在此处生成，改为手写资源
 * {@code data/anvilcraft_doge_plus/recipe/multiblock_conversion/giant_doge_anvil.json}。</p>
 */
public class ModAnvilRecipeHandler {

    private ModAnvilRecipeHandler() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        superHeatingRecipes(provider);
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
}
