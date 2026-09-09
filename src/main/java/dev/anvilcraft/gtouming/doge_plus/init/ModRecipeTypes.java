package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 配方类型与序列化器注册（数据驱动）。
 */
public class ModRecipeTypes {
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, AnvilCraftDogePlus.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, AnvilCraftDogePlus.MOD_ID);

    /** 镶嵌配方类型。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<InlayRecipe>> INLAY_TYPE =
            RECIPE_TYPES.register(
                    "inlay", () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return AnvilCraftDogePlus.of("inlay").toString();
                        }
                    }
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InlayRecipe>> INLAY_SERIALIZER =
            RECIPE_SERIALIZERS.register("inlay", InlayRecipe.Serializer::new);

    /** 镶合配方类型：基材全部镶孔按序匹配 → 产物 + 空镶嵌基材。 */
    public static final DeferredHolder<RecipeType<?>, RecipeType<InlayCraftingRecipe>> INLAY_CRAFTING_TYPE =
            RECIPE_TYPES.register(
                    "inlay_crafting", () -> new RecipeType<>() {
                        @Override
                        public String toString() {
                            return AnvilCraftDogePlus.of("inlay_crafting").toString();
                        }
                    }
            );
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<InlayCraftingRecipe>> INLAY_CRAFTING_SERIALIZER =
            RECIPE_SERIALIZERS.register("inlay_crafting", InlayCraftingRecipe.Serializer::new);

    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}
