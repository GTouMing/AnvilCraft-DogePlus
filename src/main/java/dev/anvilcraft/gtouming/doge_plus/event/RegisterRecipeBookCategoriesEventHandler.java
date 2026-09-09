package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import net.minecraft.client.RecipeBookCategories;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterRecipeBookCategoriesEvent;

/**
 * 为自定义配方类型注册配方书分类，避免 {@code ClientRecipeBook} 对未知类型逐条告警。
 *
 * <p>镶合/机器类配方不进原版合成书（由 JEI 承担查看），统一归入
 * {@link RecipeBookCategories#UNKNOWN}（不渲染在任意标签页）即可消除告警。
 * 该监听在客户端 mod 事件总线上注册（见 {@code AnvilCraftDogePlusClient}）。</p>
 */
public class RegisterRecipeBookCategoriesEventHandler {

    @SubscribeEvent
    public static void onRegisterRecipeBookCategories(RegisterRecipeBookCategoriesEvent event) {
        event.registerRecipeCategoryFinder(
                ModRecipeTypes.INLAY_CRAFTING_TYPE.get(),
                holder -> RecipeBookCategories.UNKNOWN);
    }
}

