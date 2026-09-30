package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayPropertyIngredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 自定义 ingredient 注册（NeoForge 的 {@code IngredientType}）。
 *
 * <p>注册后即可在任何原版/模组配方的 {@code ingredient} 位置按 {@code "type"} 引用，
 * 例如镶合配方的 {@code inlays} 数组。</p>
 */
public class ModIngredients {

    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, AnvilCraftDogePlus.MOD_ID);

    /**
     * 按镶嵌属性匹配材料：
     * <pre>{@code
     * { "type": "anvilcraft_doge_plus:inlay_property", "properties": ["and_gate", "counter_gate"] }
     * }</pre>
     */
    public static final DeferredHolder<IngredientType<?>, IngredientType<InlayPropertyIngredient>> INLAY_PROPERTY =
            INGREDIENT_TYPES.register(
                    "inlay_property", () -> new IngredientType<>(InlayPropertyIngredient.CODEC));

    public static void register(IEventBus bus) {
        INGREDIENT_TYPES.register(bus);
    }
}
