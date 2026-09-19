package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumRecipeProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.Objects;

/**
 * 标准合成配方生成（crafting_shaped / crafting_shapeless），由 {@code ProviderType.RECIPE} 驱动。
 */
public class ModRecipeHandler {

    private ModRecipeHandler() {
    }

    public static void init(RegistrumRecipeProvider provider) {
        shapedRecipes(provider);
        shapelessRecipes(provider);
    }

    // ==================== 有序合成 ====================

    private static void shapedRecipes(RegistrumRecipeProvider provider) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOGE_ANVIL.get())
                .pattern("DDD")
                .pattern(" I ")
                .pattern("III")
                .define('D', ModBlocks.DOGE_STEEL_BLOCK.get())
                .define('I', ModItems.DOGE_STEEL_INGOT.get())
                .unlockedBy("has_doge_steel_block", RegistrumRecipeProvider.has(ModBlocks.DOGE_STEEL_BLOCK.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shaped/doge_anvil"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.DOGE_MAGNET.get())
                .pattern(" E ")
                .pattern("DRD")
                .pattern(" E ")
                .define('E', Items.ENDER_PEARL)
                .define('D', ModItems.DOGE_STEEL_INGOT.get())
                .define('R', Items.REDSTONE)
                .unlockedBy("has_doge_steel_ingot", RegistrumRecipeProvider.has(ModItems.DOGE_STEEL_INGOT.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shaped/doge_magnet"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.DOGE_STEEL_BLOCK.get())
                .pattern("DDD")
                .pattern("DDD")
                .pattern("DDD")
                .define('D', ModItems.DOGE_STEEL_INGOT.get())
                .unlockedBy("has_doge_steel_ingot", RegistrumRecipeProvider.has(ModItems.DOGE_STEEL_INGOT.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shaped/doge_steel_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INLAY_TABLE.get())
                .pattern("III")
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.DOGE_STEEL_INGOT.get())
                .unlockedBy("has_doge_steel_ingot", RegistrumRecipeProvider.has(ModItems.DOGE_STEEL_INGOT.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shaped/inlay_table"));

        // ===== 两种镶嵌载体：4 红石导线围 1 Doge 钢锭 =====
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.INLAY_CARRIER.get(), 4)
                .pattern(" R ")
                .pattern("RIR")
                .pattern(" R ")
                .define('R', anvilcraftItem("redstone_wire"))
                .define('I', ModItems.DOGE_STEEL_INGOT.get())
                .unlockedBy("has_doge_steel_ingot", RegistrumRecipeProvider.has(ModItems.DOGE_STEEL_INGOT.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shaped/inlay_carrier_block"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.TRANSCENDIUM_INLAY_CARRIER.get())
                .pattern(" T ")
                .pattern("ICI")
                .pattern(" T ")
                .define('T', anvilcraftItem("transcendium_ingot"))
                .define('I', ModItems.DOGE_STEEL_INGOT.get())
                .define('C', ModBlocks.INLAY_CARRIER.get())
                .unlockedBy("has_inlay_carrier_block", RegistrumRecipeProvider.has(ModBlocks.INLAY_CARRIER.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shaped/transcendium_inlay_carrier_block"));
    }

    // ==================== 无序合成 ====================

    private static void shapelessRecipes(RegistrumRecipeProvider provider) {
        // ===== 镶嵌台 / 镶合台：1:1 无序互转 =====
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.INLAY_CRAFTING_TABLE.get())
                .requires(ModBlocks.INLAY_TABLE.get())
                .unlockedBy("has_inlay_table", RegistrumRecipeProvider.has(ModBlocks.INLAY_TABLE.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shapeless/inlay_crafting_table_from_inlay_table"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModBlocks.INLAY_TABLE.get())
                .requires(ModBlocks.INLAY_CRAFTING_TABLE.get())
                .unlockedBy("has_inlay_crafting_table", RegistrumRecipeProvider.has(ModBlocks.INLAY_CRAFTING_TABLE.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shapeless/inlay_table_from_inlay_crafting_table"));

        shapeless(provider, "chute_dispenser", List.of(anvilcraftItem("chute"), Items.DISPENSER), ModBlocks.CHUTE_DISPENSER.get());
        shapeless(provider, "chute_dropper", List.of(anvilcraftItem("chute"), Items.DROPPER), ModBlocks.CHUTE_DROPPER.get());
        shapeless(provider, "magnetic_chute_dispenser", List.of(anvilcraftItem("magnetic_chute"), Items.DISPENSER), ModBlocks.MAGNETIC_CHUTE_DISPENSER.get());
        shapeless(provider, "magnetic_chute_dropper", List.of(anvilcraftItem("magnetic_chute"), Items.DROPPER), ModBlocks.MAGNETIC_CHUTE_DROPPER.get());

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.DOGE_STEEL_INGOT.get(), 9)
                .requires(ModBlocks.DOGE_STEEL_BLOCK.get())
                .unlockedBy("has_doge_steel_block", RegistrumRecipeProvider.has(ModBlocks.DOGE_STEEL_BLOCK.get()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shapeless/doge_steel_ingot"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MOBILE_SILENCER.get())
                .requires(anvilcraftItem("active_silencer"))
                .unlockedBy("has_active_silencer", RegistrumRecipeProvider.has(anvilcraftItem("active_silencer")))
                .save(provider, AnvilCraftDogePlus.of("crafting_shapeless/mobile_silencer"));
    }

    private static void shapeless(
            RegistrumRecipeProvider provider,
            String name,
            List<ItemLike> inputs,
            ItemLike result) {
        var builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result);
        for (ItemLike input : inputs) {
            builder.requires(input);
        }
        builder.unlockedBy("has_" + name, RegistrumRecipeProvider.has(inputs.getFirst()))
                .save(provider, AnvilCraftDogePlus.of("crafting_shapeless/" + name));
    }

    // ==================== 辅助 ====================

    private static ItemLike anvilcraftItem(String path) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("anvilcraft", path));
        return Objects.requireNonNull(item, "missing anvilcraft item: " + path);
    }
}
