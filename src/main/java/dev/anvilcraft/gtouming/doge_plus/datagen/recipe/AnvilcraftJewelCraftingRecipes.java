package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 前置（AnvilCraft）珠宝复制配方的数据源读取器。
 *
 * <p>读取 {@code data/anvilcraft/recipe/jewel_crafting/*.json}（{@code anvilcraft:jewel_crafting}），
 * 解析出「被复制物 + 所需材料」。珠宝合成台以 source 槽的被复制物充当可复用模具
 * （不消耗、配方完成后原样留在槽内），与镶合配方的「基材（模具）」语义一致，
 * 故沿用锻造的派生方式：基材 = 被复制物（同时是配方产物），成分 = {@code ingredients}
 * （重复项各占一个镶孔）。</p>
 *
 * <p>只镜像材料数小于 {@link #INGREDIENT_LIMIT} 的配方：镶合配方的每个镶孔只能承载一件
 * 材料，猫/狗护符等动辄三四十件材料的配方需要等量镶孔，无法实用。</p>
 *
 * <p>珠宝复制产物带 1 级消失诅咒（与前置珠宝合成台一致），由镶合配方的
 * {@code curse_of_vanishing} 表达。</p>
 */
public final class AnvilcraftJewelCraftingRecipes {

    private AnvilcraftJewelCraftingRecipes() {
    }

    private static final String ANVILCRAFT = "anvilcraft";
    /** 珠宝复制配方所在目录（相对 {@code recipe/}）。 */
    private static final String FOLDER = "jewel_crafting";
    private static final String TYPE = ANVILCRAFT + ":jewel_crafting";

    /** 材料数上限（不含）：材料数小于该值的配方才派生镶合配方。 */
    public static final int INGREDIENT_LIMIT = 5;

    /**
     * 读取全部珠宝复制配方，仅保留材料数小于 {@link #INGREDIENT_LIMIT} 者；
     * 无资源管理器或读取失败时返回已成功解析的部分。
     *
     * <p>返回条目的 {@code template} 为被复制物（同时是产物）。</p>
     */
    public static List<AnvilcraftSmithingRecipes.Entry> read(@Nullable ResourceManager resourceManager) {
        List<AnvilcraftSmithingRecipes.Entry> entries = new ArrayList<>();
        if (resourceManager == null) {
            AnvilCraftDogePlus.LOGGER.warn("No datagen ResourceManager; AnvilCraft jewel crafting recipes skipped");
            return entries;
        }
        Map<ResourceLocation, Resource> found = resourceManager.listResources(
                "recipe/" + FOLDER, id -> id.getNamespace().equals(ANVILCRAFT));
        List<ResourceLocation> ids = new ArrayList<>(found.keySet());
        // 排序保证生成顺序确定（资源列表本身无序）
        ids.sort(Comparator.comparing(ResourceLocation::toString));
        for (ResourceLocation id : ids) {
            try (Reader reader = new InputStreamReader(found.get(id).open(), StandardCharsets.UTF_8)) {
                AnvilcraftSmithingRecipes.Entry entry = parse(id, GsonHelper.parse(reader));
                if (entry != null) entries.add(entry);
            } catch (Exception exception) {
                AnvilCraftDogePlus.LOGGER.error(
                        "Failed to read AnvilCraft jewel crafting recipe {}: {}", id, exception.getMessage());
            }
        }
        AnvilCraftDogePlus.LOGGER.info(
                "Read {} AnvilCraft jewel crafting recipes for inlay datagen", entries.size());
        return entries;
    }

    @Nullable
    private static AnvilcraftSmithingRecipes.Entry parse(ResourceLocation id, JsonObject json) {
        if (!TYPE.equals(GsonHelper.getAsString(json, "type", ""))) return null;

        List<AnvilcraftSmithingRecipes.Spec> ingredients = new ArrayList<>();
        for (JsonElement element : GsonHelper.getAsJsonArray(json, "ingredients")) {
            AnvilcraftSmithingRecipes.Spec spec =
                    AnvilcraftSmithingRecipes.ingredientSpec(element.getAsJsonObject());
            if (spec != null) ingredients.add(spec);
        }
        if (ingredients.isEmpty() || ingredients.size() >= INGREDIENT_LIMIT) return null;

        JsonObject result = GsonHelper.getAsJsonObject(json, "result");
        Item resultItem = AnvilcraftSmithingRecipes.item(GsonHelper.getAsString(result, "id"));
        if (resultItem == null) return null;

        // 被复制物既是模具（基材）也是产物
        return new AnvilcraftSmithingRecipes.Entry(id, resultItem, ingredients, resultItem);
    }
}
