package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * 材料（material）定义构建器。
 *
 * <p>{@code ingredient} 字段本身就是一个 vanilla {@code Ingredient} 的 JSON 表达，
 * 这里仿照其设计把「匹配条件」建模为可级联的 builder 方法：</p>
 * <ul>
 *   <li>{@link #name(String)}：文件键（{@code material/base|<inlay>/<name>.json}）；</li>
 *   <li>{@link #item(String)} / {@link #tag(String)} / {@link #dataComponent(String, JsonObject)}：
 *       依次追加匹配条件（可多个，对应 ingredient 数组的多个元素）；</li>
 *   <li>{@link #sockets(int)} / {@link #attributes(String...)}：基材镶孔数 / 材料属性；</li>
 *   <li>{@link #buildBase()} / {@link #buildInlay()}：校验并关闭构造器。</li>
 * </ul>
 *
 * <p>产出 JSON 与原先手写文件逐字等价（含 effect 药水系列的多条件数组）。</p>
 */
public class MaterialBuilder {

    private String name;
    private final List<JsonObject> conditions = new ArrayList<>();
    private int sockets = -1;
    private final List<String> attributes = new ArrayList<>();

    private MaterialBuilder() {
    }

    /** 打开一个材料构建器。 */
    public static MaterialBuilder builder() {
        return new MaterialBuilder();
    }

    /** 定义文件键（{@code material/<base|inlay>/<name>.json}，不含 {@code .json}）。 */
    public MaterialBuilder name(String name) {
        this.name = name;
        return this;
    }

    /** 追加一条「匹配某物品」条件。 */
    public MaterialBuilder item(String itemId) {
        // ResourceLocation.parse 会校验非法字符，保证写入的 id 合法
        ResourceLocation.parse(itemId);
        JsonObject condition = new JsonObject();
        condition.addProperty("item", itemId);
        this.conditions.add(condition);
        return this;
    }

    /** 追加一条「匹配某物品标签」条件。 */
    public MaterialBuilder tag(String tagId) {
        ResourceLocation.parse(tagId);
        JsonObject condition = new JsonObject();
        condition.addProperty("tag", tagId);
        this.conditions.add(condition);
        return this;
    }

    /**
     * 追加一条「匹配带数据组件的物品」条件（NeoForge 扩展 ingredient 格式）。
     *
     * @param itemId     物品 id（如 {@code minecraft:potion}）
     * @param components 组件内容（如 {@code {"minecraft:potion_contents": {"potion": "minecraft:water"}}}）
     */
    public MaterialBuilder dataComponent(String itemId, JsonObject components) {
        ResourceLocation.parse(itemId);
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:components");
        JsonArray items = new JsonArray();
        items.add(itemId);
        condition.add("items", items);
        condition.add("components", components);
        this.conditions.add(condition);
        return this;
    }

    /** 设置基材镶孔数。 */
    public MaterialBuilder sockets(int sockets) {
        this.sockets = sockets;
        return this;
    }

    /** 追加材料属性（可多个）。 */
    public MaterialBuilder attributes(String... attributes) {
        this.attributes.addAll(List.of(attributes));
        return this;
    }

    /** 关闭构造器，产出基材定义。 */
    public BaseMaterialData buildBase() {
        if (name == null) {
            throw new IllegalStateException("Base material name must not be null");
        }
        if (conditions.isEmpty()) {
            throw new IllegalStateException("Base material '" + name + "' must have at least one ingredient condition");
        }
        if (sockets <= 0) {
            throw new IllegalStateException("Base material '" + name + "' must have positive sockets");
        }
        return new BaseMaterialData(name, sockets, ingredient());
    }

    /** 关闭构造器，产出镶嵌材料定义。 */
    public InlayMaterialData buildInlay() {
        if (name == null) {
            throw new IllegalStateException("Inlay material name must not be null");
        }
        if (conditions.isEmpty()) {
            throw new IllegalStateException("Inlay material '" + name + "' must have at least one ingredient condition");
        }
        if (attributes.isEmpty()) {
            throw new IllegalStateException("Inlay material '" + name + "' must have at least one attribute");
        }
        return new InlayMaterialData(name, ingredient(), attributes.toArray(String[]::new));
    }

    /** 组装 ingredient 数组（与原手写一致：即使单条也以数组形式输出）。 */
    private JsonArray ingredient() {
        JsonArray array = new JsonArray();
        for (JsonObject condition : conditions) {
            array.add(condition);
        }
        return array;
    }
}
