package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import com.google.gson.JsonObject;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * inlay（镶嵌）配方输出：{@code data/anvilcraft_doge_plus/recipe/inlay/<name>.json}。
 *
 * <p>inlay 是 {@link InlayRecipeData} 中记录的一对文件名键（材料定义 + 基材定义），
 * JSON 只含 {@code type}/{@code inlay}/{@code base} 三个字段，运行时经序列化器
 * 解析后由 {@code MaterialManager} 查具体物品定义。这类配方不属于 vanilla
 * {@code RecipeProvider} 可表达范围，故这里直接构造 JsonObject 写出。</p>
 */
public class InlayRecipeProvider implements DataProvider {

    private final PackOutput packOutput;

    public InlayRecipeProvider(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path dataPath = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK);
        return CompletableFuture.allOf(InlayRecipeData.ALL.stream().map(entry -> {
            Path target = dataPath.resolve("anvilcraft_doge_plus/recipe/inlay/" + entry.fileName() + ".json");
            JsonObject json = new JsonObject();
            json.addProperty("type", AnvilCraftDogePlus.MOD_ID + ":inlay");
            json.addProperty("inlay", entry.inlay());
            json.addProperty("base", entry.base());
            return DataProvider.saveStable(cache, json, target);
        }).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "DogePlus inlay recipes (" + AnvilCraftDogePlus.MOD_ID + ")";
    }
}
