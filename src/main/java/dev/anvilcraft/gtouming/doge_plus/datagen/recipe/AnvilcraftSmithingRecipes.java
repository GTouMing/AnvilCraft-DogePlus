package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 前置（AnvilCraft）锻造配方的数据源读取器。
 *
 * <p>不再硬编码配方表：直接从 datagen 的 {@link ResourceManager} 读取 AnvilCraft 已生成的
 * 锻造配方 JSON（{@code data/anvilcraft/recipe/}），解析出「模板 + 非模板成分 + 产物」。
 * 覆盖两种配方类型：</p>
 * <ul>
 *   <li>原版 {@code minecraft:smithing_transform}（模板 / 基材 / 追加材料 / 产物）——
 *       位于 {@code recipe/smithing/}；</li>
 *   <li>AnvilCraft {@code anvilcraft:*_to_one_smithing}（模板 / 材料 / 多个输入 / 产物）——
 *       位于 {@code recipe/{two,four,eight}_to_one_smithing/}。</li>
 * </ul>
 *
 * <p>据此派生出镶合四件套：模板基材（镶孔数 = 非模板成分数）、镶入材料定义、
 * 材料 × 模板镶入配方、以及镶合配方（见各 Data 类的 {@code all(...)} 工厂）。</p>
 */
public final class AnvilcraftSmithingRecipes {

    private AnvilcraftSmithingRecipes() {
    }

    private static final String ANVILCRAFT = "anvilcraft";
    /** AnvilCraft 锻造配方所在目录（相对 {@code recipe/}）。 */
    private static final List<String> FOLDERS = List.of(
            "smithing",
            "two_to_one_smithing",
            "four_to_one_smithing",
            "eight_to_one_smithing"
    );

    private static final String SMITHING_TRANSFORM = "minecraft:smithing_transform";
    private static final String MULTIPLE_TO_ONE_SUFFIX = "_to_one_smithing";

    /** 单个镶入成分：物品或标签，外加用于定义文件键的名称。 */
    public record Spec(@Nullable Item item, @Nullable TagKey<Item> tag, String name) {

        public Ingredient ingredient() {
            return this.item != null ? Ingredient.of(this.item) : Ingredient.of(this.tag);
        }
    }

    /**
     * 一条前置（AnvilCraft）配方：来源 id + 基材（锻造配方为模板，珠宝复制配方为被复制物）
     * + 成分（顺序与配方一致）+ 产物。
     */
    public record Entry(ResourceLocation source, Item template, List<Spec> ingredients, Item result) {
    }

    // ==================== 读取 ====================

    /** 读取全部 AnvilCraft 锻造配方；无资源管理器或读取失败时返回已成功解析的部分。 */
    public static List<Entry> read(@Nullable ResourceManager resourceManager) {
        List<Entry> entries = new ArrayList<>();
        if (resourceManager == null) {
            AnvilCraftDogePlus.LOGGER.warn("No datagen ResourceManager; AnvilCraft smithing recipes skipped");
            return entries;
        }
        for (String folder : FOLDERS) {
            Map<ResourceLocation, Resource> found = resourceManager.listResources(
                    "recipe/" + folder, id -> id.getNamespace().equals(ANVILCRAFT));
            List<ResourceLocation> ids = new ArrayList<>(found.keySet());
            // 排序保证生成顺序确定（资源列表本身无序）
            ids.sort(Comparator.comparing(ResourceLocation::toString));
            for (ResourceLocation id : ids) {
                try (Reader reader = new InputStreamReader(found.get(id).open(), StandardCharsets.UTF_8)) {
                    Entry entry = parse(id, GsonHelper.parse(reader));
                    if (entry != null) entries.add(entry);
                } catch (Exception exception) {
                    AnvilCraftDogePlus.LOGGER.error(
                            "Failed to read AnvilCraft smithing recipe {}: {}", id, exception.getMessage());
                }
            }
        }
        AnvilCraftDogePlus.LOGGER.info("Read {} AnvilCraft smithing recipes for inlay datagen", entries.size());
        return entries;
    }

    @Nullable
    private static Entry parse(ResourceLocation id, JsonObject json) {
        String type = GsonHelper.getAsString(json, "type", "");
        if (SMITHING_TRANSFORM.equals(type)) return parseSmithingTransform(id, json);
        if (type.startsWith(ANVILCRAFT + ":") && type.endsWith(MULTIPLE_TO_ONE_SUFFIX)) {
            return parseMultipleToOne(id, json);
        }
        return null;
    }

    /** 原版锻造：追加材料在前、基材在后（与旧的硬编码顺序一致）。 */
    @Nullable
    private static Entry parseSmithingTransform(ResourceLocation id, JsonObject json) {
        Spec template = ingredientSpec(GsonHelper.getAsJsonObject(json, "template"));
        if (template == null || template.item() == null) return null;

        List<Spec> ingredients = new ArrayList<>();
        collectSpec(ingredients, GsonHelper.getAsJsonObject(json, "addition"));
        collectSpec(ingredients, GsonHelper.getAsJsonObject(json, "base"));

        JsonObject result = GsonHelper.getAsJsonObject(json, "result");
        Item resultItem = item(GsonHelper.getAsString(result, "id"));
        if (resultItem == null || ingredients.isEmpty()) return null;
        return new Entry(id, template.item(), ingredients, resultItem);
    }

    /** AnvilCraft 多合一锻造：材料在前、各输入在后。 */
    @Nullable
    private static Entry parseMultipleToOne(ResourceLocation id, JsonObject json) {
        List<Spec> templates = predicateSpecs(GsonHelper.getAsJsonObject(json, "template"));
        if (templates.isEmpty() || templates.getFirst().item() == null) return null;

        List<Spec> ingredients = new ArrayList<>();
        collectSpecs(ingredients, GsonHelper.getAsJsonObject(json, "material"));
        JsonArray inputs = GsonHelper.getAsJsonArray(json, "inputs");
        for (JsonElement input : inputs) {
            collectSpecs(ingredients, input.getAsJsonObject());
        }

        Item resultItem = item(GsonHelper.getAsString(json, "result"));
        if (resultItem == null || ingredients.isEmpty()) return null;
        return new Entry(id, templates.getFirst().item(), ingredients, resultItem);
    }

    // ==================== JSON 解析辅助 ====================

    /**
     * 解析单个 ingredient（{@code {"item": ...}} / {@code {"tag": ...}} / 物品谓词）。
     *
     * <p>解析规则由锻造与珠宝复制两个读取器共用。</p>
     */
    @Nullable
    public static Spec ingredientSpec(JsonObject object) {
        if (object.has("item")) return itemSpec(GsonHelper.getAsString(object, "item"));
        if (object.has("tag")) return tagSpec(GsonHelper.getAsString(object, "tag"));
        List<Spec> specs = predicateSpecs(object);
        return specs.isEmpty() ? null : specs.getFirst();
    }

    /** 解析物品谓词（{@code {"items": "id" | "#tag" | [...]}}）为若干成分。 */
    private static List<Spec> predicateSpecs(JsonObject object) {
        List<Spec> specs = new ArrayList<>();
        JsonElement items = object.get("items");
        if (items == null) return specs;
        if (items.isJsonArray()) {
            for (JsonElement element : items.getAsJsonArray()) {
                addRawSpec(specs, element.getAsString());
            }
        } else {
            addRawSpec(specs, items.getAsString());
        }
        return specs;
    }

    private static void collectSpec(List<Spec> target, JsonObject object) {
        Spec spec = ingredientSpec(object);
        if (spec != null) target.add(spec);
    }

    private static void collectSpecs(List<Spec> target, JsonObject object) {
        target.addAll(predicateSpecs(object));
    }

    /** {@code "#tag"} 视为标签，否则视为物品。 */
    private static void addRawSpec(List<Spec> target, String raw) {
        Spec spec = raw.startsWith("#") ? tagSpec(raw.substring(1)) : itemSpec(raw);
        if (spec != null) target.add(spec);
    }

    @Nullable
    private static Spec itemSpec(String raw) {
        Item item = item(raw);
        return item == null ? null : new Spec(item, null, itemPath(item));
    }

    private static Spec tagSpec(String raw) {
        ResourceLocation location = ResourceLocation.parse(raw);
        TagKey<Item> tag = TagKey.create(Registries.ITEM, location);
        return new Spec(null, tag, lastSegment(location.getPath()));
    }

    /** 解析物品 id；未注册或为空气时返回 {@code null}。 */
    @Nullable
    public static Item item(String raw) {
        ResourceLocation location = ResourceLocation.tryParse(raw);
        if (location == null || !BuiltInRegistries.ITEM.containsKey(location)) return null;
        Item item = BuiltInRegistries.ITEM.get(location);
        return item == Items.AIR ? null : item;
    }

    // ==================== 命名 ====================

    /** 物品注册路径（如 {@code royal_steel_ingot}）。 */
    public static String itemPath(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }

    /** 标签路径末段（如 {@code c:tools/mace} → {@code mace}）。 */
    private static String lastSegment(String path) {
        int index = path.lastIndexOf('/');
        return index < 0 ? path : path.substring(index + 1);
    }

    /**
     * 镶合配方 id：镜像 AnvilCraft 的配方路径，去掉 {@code recipe/} 前缀与 {@code .json} 后缀、
     * 去掉 {@code _smithing} 目录后缀并展平。
     * 例如 {@code smithing/royal_steel_pickaxe} → {@code royal_steel_pickaxe}，
     * {@code two_to_one_smithing/transcendence_anvil} → {@code two_to_one_transcendence_anvil}。
     */
    public static String craftingId(ResourceLocation source) {
        // 资源管理器返回的路径包含目录前缀与扩展名，如 recipe/two_to_one_smithing/xxx.json
        String path = source.getPath();
        if (path.startsWith("recipe/")) path = path.substring("recipe/".length());
        if (path.endsWith(".json")) path = path.substring(0, path.length() - ".json".length());

        int slash = path.lastIndexOf('/');
        if (slash < 0) return path;
        String folder = path.substring(0, slash);
        String file = path.substring(slash + 1);
        // 目录名去掉 smithing 后缀（smithing / two_to_one_smithing → 前缀 "" / "two_to_one"）
        String prefix = folder.endsWith("smithing")
                ? folder.substring(0, folder.length() - "smithing".length())
                : folder;
        if (prefix.endsWith("_")) prefix = prefix.substring(0, prefix.length() - 1);
        return prefix.isEmpty() ? file : prefix + "_" + file;
    }
}
