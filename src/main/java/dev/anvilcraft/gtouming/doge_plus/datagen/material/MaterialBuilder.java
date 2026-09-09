package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * 材料（material）定义构建器。
 *
 * <p>{@code ingredient} 字段本身就是一个 vanilla {@code Ingredient} 的 JSON 表达，
 * 这里仿照其设计把「匹配条件」建模为可级联的 builder 方法：</p>
 * <ul>
 *   <li>{@link #name(String)}：文件键（{@code material/base|<inlay>/<name>.json}）；</li>
 *   <li>{@link #item(ItemLike)} / {@link #tag(TagKey)} / {@link #dataComponent(ItemLike, JsonObject)}：
 *       依次追加匹配条件（可多个，对应 ingredient 数组的多个元素）；</li>
 *   <li>{@link #sockets(int)} / {@link #attributes(String...)}：基材镶孔数 / 材料属性；</li>
 *   <li>{@link #buildBase()} / {@link #buildInlay()}：校验并关闭构造器。</li>
 * </ul>
 *
 * <p>物品/标签统一以对象引用（{@link ItemLike} / {@link TagKey}）传入，内部经注册表解析为
 * id 字符串，编译期即可追踪条目改名，避免手写字面量拼错。方块条目（{@code BlockEntry} 等
 * 实现了 {@link ItemLike}）会经 {@code Block.asItem()} 关联到其同名物品。</p>
 *
 * <p>产出 JSON 与原手写文件逐字等价（含 effect 药水系列的多条件数组）。</p>
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

    /**
     * 追加一条「匹配某物品」条件。
     *
     * @param item 物品/方块条目（vanilla {@code Items.XXX}、Registrum {@code ItemEntry}/{@code BlockEntry}
     *             或 AnvilCraft 的 entry 常量均实现 {@link ItemLike}）；方块会自动关联其同名物品
     */
    public MaterialBuilder item(ItemLike item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalArgumentException("Item " + item + " is not registered in the item registry");
        }
        JsonObject condition = new JsonObject();
        condition.addProperty("item", id.toString());
        this.conditions.add(condition);
        return this;
    }

    /**
     * 追加一条「匹配某物品标签」条件。
     *
     * @param tag 标签键；路径型子标签（如 {@code c:tools/melee_weapon}）由 {@link TagKey} 的
     *            location 天然表达，无需额外建模
     */
    public MaterialBuilder tag(TagKey<?> tag) {
        ResourceLocation id = tag.location();
        JsonObject condition = new JsonObject();
        condition.addProperty("tag", id.toString());
        this.conditions.add(condition);
        return this;
    }

    /**
     * 追加一条「匹配带数据组件的物品」条件（NeoForge 扩展 ingredient 格式）。
     *
     * @param item      物品条目（见 {@link #item(ItemLike)}）
     * @param components 组件内容（如 {@code {"minecraft:potion_contents": {"potion": "minecraft:water"}}}）
     */
    public MaterialBuilder dataComponent(ItemLike item, JsonObject components) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item.asItem());
        if (id.equals(BuiltInRegistries.ITEM.getDefaultKey())) {
            throw new IllegalArgumentException("Item " + item + " is not registered in the item registry");
        }
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:components");
        JsonArray items = new JsonArray();
        items.add(id.toString());
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

    /**
     * 追加材料属性（可多个），以性质对象引用。
     *
     * <p>与 {@link #attributes(String...)} 序列化格式一致：本模组命名空间下的性质写出
     * 裸路径（{@code "fire_proof"}），其它命名空间写出完整 ID（{@code ns:path}），
     * 与 {@link InlayProperty#CODEC} 的解析规则（裸串按本模组命名空间解析）互逆。</p>
     */
    public MaterialBuilder attributes(InlayProperty... properties) {
        for (InlayProperty property : properties) {
            ResourceLocation id = property.id();
            this.attributes.add(id.getNamespace().equals(AnvilCraftDogePlus.MOD_ID) ? id.getPath() : id.toString());
        }
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

    /**
     * 关闭构造器，产出镶嵌材料定义。
     *
     * <p>允许属性为空：锻造材料 / 珠宝复制材料等只需被镶入参与合成、不需要赋予任何性质，
     * 定义文件写出 {@code "attributes": []}。</p>
     */
    public InlayMaterialData buildInlay() {
        if (name == null) {
            throw new IllegalStateException("Inlay material name must not be null");
        }
        if (conditions.isEmpty()) {
            throw new IllegalStateException("Inlay material '" + name + "' must have at least one ingredient condition");
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
