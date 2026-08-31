package dev.anvilcraft.gtouming.doge_plus.client;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.client.markdown.DogePlusRecipeComponentFactories;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * Doge+ 客户端入口。
 *
 * <p>负责注册仅客户端可用的内容，如 Ageratum 手册的配方渲染组件。</p>
 */
@Mod(value = AnvilCraftDogePlus.MOD_ID, dist = Dist.CLIENT)
public class AnvilCraftDogePlusClient {
    public AnvilCraftDogePlusClient(IEventBus modEventBus) {
        // 向 Ageratum 注册镶嵌配方渲染组件，使手册中的 inlay 配方可显示
        DogePlusRecipeComponentFactories.RECIPE_COMPONENT_FACTORIES.register(modEventBus);
    }
}
