package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * inlay_crafting（镶合）配方的源数据。
 *
 * <p>基材（模具物品）+ 镶孔内容（顺序无关）+ 可选产物；JSON 与运行时
 * {@code InlayCraftingRecipe} 的字段一一对应，配方输出路径为
 * {@code data/anvilcraft_doge_plus/recipe/inlay_crafting/<id>.json}。
 * {@code result} 为 {@code null} 时 JSON 省略 {@code result} 字段，表示产物在
 * 加工时推导（如盔甲纹饰：给可纹饰装备施加模板与材料推导的纹饰组件）。</p>
 *
 * @param id     配方文件 id（不含路径与扩展名）
 * @param base   基材物品（模具，需要预先开镶孔）
 * @param inlays 期望的镶孔内容集合（顺序无关）
 * @param result 产物物品；{@code null} 表示按输入推导
 */
public record InlayCraftingData(
        String id,
        Item base,
        List<Ingredient> inlays,
        @Nullable Item result
) {

    /** 原版「可纹饰装备」物品标签。 */
    private static final TagKey<Item> TRIM_ARMOR =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trimmable_armor"));
    /** 原版「纹饰材料」物品标签。 */
    private static final TagKey<Item> TRIM_MATERIAL =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trim_materials"));

    /** 原版锻造：钻石装备 → 对应下界合金装备（钻石物品在前）。 */
    private static final List<Item[]> NETHERITE_UPGRADES = List.of(
            new Item[] {Items.DIAMOND_SWORD, Items.NETHERITE_SWORD},
            new Item[] {Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE},
            new Item[] {Items.DIAMOND_AXE, Items.NETHERITE_AXE},
            new Item[] {Items.DIAMOND_SHOVEL, Items.NETHERITE_SHOVEL},
            new Item[] {Items.DIAMOND_HOE, Items.NETHERITE_HOE},
            new Item[] {Items.DIAMOND_HELMET, Items.NETHERITE_HELMET},
            new Item[] {Items.DIAMOND_CHESTPLATE, Items.NETHERITE_CHESTPLATE},
            new Item[] {Items.DIAMOND_LEGGINGS, Items.NETHERITE_LEGGINGS},
            new Item[] {Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS}
    );

    private static final String TRIM_ID_SUFFIX = "_armor_trim_smithing_template";

    /** 生成全部镶合配方（9 下界合金升级 + 18 盔甲纹饰）。 */
    public static final List<InlayCraftingData> ALL = new ArrayList<>();
    static {
        // ===== 原版锻造：下界合金升级（固定产物）=====
        for (Item[] pair : NETHERITE_UPGRADES) {
            Item diamond = pair[0];
            Item netherite = pair[1];
            String name = itemName(netherite);
            ALL.add(new InlayCraftingData(
                    name + "_upgrade",
                    Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                    List.of(
                            Ingredient.of(Items.NETHERITE_INGOT),
                            Ingredient.of(diamond)
                    ),
                    netherite
            ));
        }

        // ===== 原版盔甲纹饰：模板 + [可纹饰装备, 纹饰材料] → 动态产物（无 result）=====
        for (Item template : BaseMaterialData.TRIM_TEMPLATES) {
            String itemName = itemName(template);
            String shortName = itemName.endsWith(TRIM_ID_SUFFIX)
                    ? itemName.substring(0, itemName.length() - TRIM_ID_SUFFIX.length())
                    : itemName;
            ALL.add(new InlayCraftingData(
                    shortName + "_trim",
                    template,
                    List.of(
                            Ingredient.of(TRIM_ARMOR),
                            Ingredient.of(TRIM_MATERIAL)
                    ),
                    null
            ));
        }

        // ===== 前置（AnvilCraft）锻造兼容：见 AnvilcraftSmithingCompat =====
        ALL.addAll(AnvilcraftSmithingCompat.craftingRecipes());
    }

    private static String itemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
