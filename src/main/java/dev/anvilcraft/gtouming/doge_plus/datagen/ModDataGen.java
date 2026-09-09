package dev.anvilcraft.gtouming.doge_plus.datagen;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.datagen.lang.EnUsLangHandler;
import dev.anvilcraft.gtouming.doge_plus.datagen.lang.ZhCnLanguageProvider;
import dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialJsonProvider;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.InlayCraftingRecipeProvider;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.InlayRecipeProvider;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.ModAnvilRecipeHandler;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.ModRecipeHandler;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;
import net.neoforged.neoforge.data.event.GatherDataEvent;

/**
 * 数据生成入口。仿照前置 {@code AnvilCraftDatagen}：
 * <ul>
 *   <li>在 mod 构造末尾调用 {@link #init()}：把各类数据生成回调注册到 {@code REGISTRUM}（仅 datagen 环境生效）；</li>
 *   <li>{@link ModGatherDataEvents} 监听 {@link GatherDataEvent}：添加不走 Registrum 的
 *       vanilla / 自定义 provider（zh_cn 语言、material、super_heating / multiblock / 跨 mod 配方）。</li>
 * </ul>
 */
public class ModDataGen {

    private ModDataGen() {
    }

    /**
     * 注册所有基于 Registrum 的 provider 回调。需在 mod 构造期间调用（与前置一致）。
     */
    public static void init() {
        AnvilCraftDogePlus.REGISTRUM.addDataGenerator(
                ProviderType.LANG,
                EnUsLangHandler::init);

        AnvilCraftDogePlus.REGISTRUM.addDataGenerator(
                ProviderType.RECIPE,
                ModRecipeHandler::init);

        // AnvilCraft 类型配方：复用前置 builder（super_heating / multiblock_conversion）
        AnvilCraftDogePlus.REGISTRUM.addDataGenerator(
                ProviderType.RECIPE,
                ModAnvilRecipeHandler::init);
    }

    /**
     * {@link GatherDataEvent} 监听：添加仅走 vanilla {@code DataProvider} 的生成器。
     * 由 {@link AnvilCraftDogePlus} 构造中 {@code modEventBus.addListener(...)} 注册。
     */
    public static class ModGatherDataEvents {

        private ModGatherDataEvents() {
        }

        public static void gatherData(GatherDataEvent event) {
            var generator = event.getGenerator();
            var packOutput = generator.getPackOutput();

            // zh_cn：vanilla LanguageProvider（RegistrumLangProvider 只输出 en_us/en_ud）
            generator.addProvider(event.includeClient(), new ZhCnLanguageProvider(packOutput));

            // material（data/<ns>/material/base|inlay）
            generator.addProvider(event.includeServer(), new MaterialJsonProvider(packOutput));

            // inlay（镶嵌）配方 —— 本 mod 自有配方类型，自写 provider 输出
            generator.addProvider(event.includeServer(), new InlayRecipeProvider(packOutput));

            // inlay_crafting（镶合）配方 —— 同上，路径 recipe/inlay_crafting/
            generator.addProvider(event.includeServer(), new InlayCraftingRecipeProvider(packOutput));
        }
    }
}
