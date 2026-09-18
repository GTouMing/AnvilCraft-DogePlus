package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * inlay_crafting（镶合）配方输出：{@code data/anvilcraft_doge_plus/recipe/inlay_crafting/<id>.json}。
 *
 * <p>JSON 结构：{@code type}/{@code base}/{@code inlays[]}/{@code result}，与运行时
 * {@code InlayCraftingRecipe.Serializer} 的编解码器一致。这类配方包含物品与镶孔顺序，
 * 不属于 vanilla {@code RecipeProvider} 可表达范围，故直接构造 {@link JsonObject} 写出。</p>
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
                    "anvilcraft_doge_plus/recipe/inlay_crafting/" + entry.id() + ".json");
            return DataProvider.saveStable(cache, toJson(entry), target);
        }).toArray(CompletableFuture[]::new));
    }

    private static JsonObject toJson(InlayCraftingData entry) {
        JsonObject json = new JsonObject();
        json.addProperty("type", AnvilCraftDogePlus.MOD_ID + ":inlay_crafting");
        json.addProperty("base", BuiltInRegistries.ITEM.getKey(entry.base()).toString());
        JsonArray inlays = new JsonArray();
        for (Ingredient ingredient : entry.inlays()) {
            JsonElement element = Ingredient.CODEC.encodeStart(JsonOps.INSTANCE, ingredient).getOrThrow();
            inlays.add(element);
        }
        json.add("inlays", inlays);
        // result 缺省表示产物在加工时按输入推导（如盔甲纹饰）；
        // 多数量产物按原版写法写出 {"id": ..., "count": n}，单数量时写物品 id 字符串
        ItemStack result = entry.result();
        if (result != null) {
            if (result.getCount() > 1) {
                json.add("result", ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, result).getOrThrow());
            } else {
                json.addProperty("result", BuiltInRegistries.ITEM.getKey(result.getItem()).toString());
            }
        }
        // 缺省表示产物不带诅咒；珠宝复制的复制品带消失诅咒
        if (entry.curseOfVanishing()) {
            json.addProperty("curse_of_vanishing", true);
        }
        return json;
    }

    @Override
    public String getName() {
        return "DogePlus inlay_crafting recipes (" + AnvilCraftDogePlus.MOD_ID + ")";
    }
}
