package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayOutput;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * inlay_crafting（镶合）配方输出：{@code data/anvilcraft_doge_plus/recipe/<kind.folder()>/<id>.json}。
 *
 * <p>JSON 结构：{@code type}/{@code base}/{@code inlays[]}/{@code results[]}（/{@code kind}），
 * 与运行时 {@code InlayCraftingRecipe.Serializer} 的编解码器一致。{@code results} 由
 * {@link InlayOutput#CODEC} 编码，支持固定 / 概率产物与动态产物条目。这类配方包含物品与
 * 镶孔顺序，不属于 vanilla {@code RecipeProvider} 可表达范围，故直接构造 {@link JsonObject} 写出。</p>
 */
public class InlayCraftingRecipeProvider implements DataProvider {

    private final PackOutput packOutput;
    private final List<InlayCraftingData> recipes;

    public InlayCraftingRecipeProvider(PackOutput packOutput, List<InlayCraftingData> recipes) {
        this.packOutput = packOutput;
        this.recipes = recipes;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path dataPath = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK);
        return CompletableFuture.allOf(this.recipes.stream().map(entry -> {
            Path target = dataPath.resolve(
                    "anvilcraft_doge_plus/recipe/" + entry.kind().folder() + "/" + entry.id() + ".json");
            return DataProvider.saveStable(cache, toJson(entry), target);
        }).toArray(CompletableFuture[]::new));
    }

    private static JsonObject toJson(InlayCraftingData entry) {
        JsonObject json = new JsonObject();
        json.addProperty("type", AnvilCraftDogePlus.MOD_ID + ":inlay_crafting");
        json.addProperty("base", BuiltInRegistries.ITEM.getKey(entry.base()).toString());
        json.add("inlays", InlayMatcher.CODEC.listOf()
                .encodeStart(JsonOps.INSTANCE, entry.inlays())
                .getOrThrow());
        json.add("results", InlayOutput.CODEC.listOf()
                .encodeStart(JsonOps.INSTANCE, entry.results())
                .getOrThrow());
        // 类别（缺省 crafting）；每类产物（含返还的模具）均已在 results 中显式声明
        if (entry.kind() != InlayCraftingRecipe.Kind.CRAFTING) {
            json.addProperty("kind", entry.kind().getSerializedName());
        }
        return json;
    }

    @Override
    public String getName() {
        return "DogePlus inlay_crafting recipes (" + AnvilCraftDogePlus.MOD_ID + ")";
    }
}
