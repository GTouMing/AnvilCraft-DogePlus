package dev.anvilcraft.gtouming.doge_plus.client.markdown;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.markdown.recipe.MDInlayRecipeComponent;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.client.registries.AgeratumRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Doge+ 自定义配方组件工厂注册。
 *
 * <p>向 Ageratum 注册 {@code inlay}（镶嵌）配方类型的渲染组件，
 * 使手册中的 {@code <recipe id="anvilcraft_doge_plus:inlay/..."/>} 可以正常显示。</p>
 */
@SuppressWarnings("unused")
public class DogePlusRecipeComponentFactories {
    /** 配方组件工厂注册表（Ageratum 自定义注册表）。 */
    public static final DeferredRegister<MDRecipeComponent.RecipeComponentFactory<?>> RECIPE_COMPONENT_FACTORIES =
            DeferredRegister.create(
                    AgeratumRegistries.RECIPE_COMPONENT_FACTORY_REGISTRY_KEY,
                    AnvilCraftDogePlus.MOD_ID
            );

    /** 镶嵌配方渲染组件。 */
    public static final DeferredHolder<MDRecipeComponent.RecipeComponentFactory<?>, MDRecipeComponent.RecipeComponentFactory<?>> INLAY =
            RECIPE_COMPONENT_FACTORIES.register(
                    "inlay",
                    () -> MDRecipeComponent.RecipeComponentFactory.create(
                            ModRecipeTypes.INLAY_TYPE.get(),
                            MDInlayRecipeComponent::new
                    )
            );

    private DogePlusRecipeComponentFactories() {
    }
}
