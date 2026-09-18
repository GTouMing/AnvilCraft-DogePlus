package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import dev.anvilcraft.gtouming.doge_plus.datagen.material.BaseMaterialData;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
 * 加工时推导（如盔甲纹饰：给可纹饰装备施加模板与材料推导的纹饰组件）。
 * {@code curseOfVanishing} 为真时写出 {@code "curse_of_vanishing": true}，
 * 产物附加 1 级消失诅咒（前置珠宝复制的产物语义）。
 * {@code result} 为多数量时按原版写法写出 {@code {"id": ..., "count": n}}，
 * 单数量时写出物品 id 字符串。</p>
 *
 * @param id               配方文件 id（不含路径与扩展名）
 * @param base             基材物品（模具，需要预先开镶孔）
 * @param inlays           期望的镶孔内容集合（顺序无关）
 * @param result           产物（含数量）；{@code null} 表示按输入推导
 * @param curseOfVanishing 产物是否附加 1 级消失诅咒
 */
public record InlayCraftingData(
        String id,
        Item base,
        List<Ingredient> inlays,
        @Nullable ItemStack result,
        boolean curseOfVanishing
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

    /** 本 mod 内置镶合配方（9 下界合金升级 + 18 盔甲纹饰）。 */
    private static final List<InlayCraftingData> VANILLA = new ArrayList<>();
    static {
        // ===== 原版锻造：下界合金升级（固定产物）=====
        for (Item[] pair : NETHERITE_UPGRADES) {
            Item diamond = pair[0];
            Item netherite = pair[1];
            String name = itemName(netherite);
            VANILLA.add(new InlayCraftingData(
                    name + "_upgrade",
                    Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE,
                    List.of(
                            Ingredient.of(Items.NETHERITE_INGOT),
                            Ingredient.of(diamond)
                    ),
                    new ItemStack(netherite),
                    false
            ));
        }

        // ===== 原版盔甲纹饰：模板 + [可纹饰装备, 纹饰材料] → 动态产物（无 result）=====
        for (Item template : BaseMaterialData.TRIM_TEMPLATES) {
            String itemName = itemName(template);
            String shortName = itemName.endsWith(TRIM_ID_SUFFIX)
                    ? itemName.substring(0, itemName.length() - TRIM_ID_SUFFIX.length())
                    : itemName;
            VANILLA.add(new InlayCraftingData(
                    shortName + "_trim",
                    template,
                    List.of(
                            Ingredient.of(TRIM_ARMOR),
                            Ingredient.of(TRIM_MATERIAL)
                    ),
                    null,
                    false
            ));
        }

        // ===== 空心磁铁块：镶入 n 个铁锭即镶合出 n 个磁铁锭（n = 1..镶孔数）=====
        for (int count = 1; count <= BaseMaterialData.HOLLOW_MAGNET_BLOCK_SOCKETS; count++) {
            List<Ingredient> inlays = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                inlays.add(Ingredient.of(Items.IRON_INGOT));
            }
            VANILLA.add(new InlayCraftingData(
                    "magnet_ingot_from_hollow_magnet_block_" + count,
                    ModBlocks.HOLLOW_MAGNET_BLOCK.asItem(),
                    inlays,
                    new ItemStack(ModItems.MAGNET_INGOT.get(), count),
                    false
            ));
        }
    }

    /**
     * 全部镶合配方：内置 + 前置（AnvilCraft）锻造配方 / 珠宝复制配方派生的镶合配方。
     *
     * <p>由 {@link AnvilcraftSmithingRecipes.Entry} 派生：基材 + 全部成分 → 产物；
     * 配方 id 镜像前置配方的路径（见 {@link AnvilcraftSmithingRecipes#craftingId}）。
     * 珠宝复制配方以被复制物为基材与产物，复制品带消失诅咒。</p>
     *
     * @param smithing 前置锻造配方
     * @param jewel    前置珠宝复制配方（材料数已由读取器筛选）
     */
    public static List<InlayCraftingData> all(
            List<AnvilcraftSmithingRecipes.Entry> smithing,
            List<AnvilcraftSmithingRecipes.Entry> jewel) {
        List<InlayCraftingData> list = new ArrayList<>(VANILLA);
        for (AnvilcraftSmithingRecipes.Entry entry : smithing) {
            list.add(of(entry, false));
        }
        // 珠宝复制的复制品带消失诅咒（与前置珠宝合成台一致），模具不受影响
        for (AnvilcraftSmithingRecipes.Entry entry : jewel) {
            list.add(of(entry, true));
        }
        return list;
    }

    private static InlayCraftingData of(AnvilcraftSmithingRecipes.Entry entry, boolean curseOfVanishing) {
        List<Ingredient> inlays = entry.ingredients().stream()
                .map(AnvilcraftSmithingRecipes.Spec::ingredient)
                .toList();
        return new InlayCraftingData(
                AnvilcraftSmithingRecipes.craftingId(entry.source()),
                entry.template(),
                inlays,
                entry.result() == null ? null : new ItemStack(entry.result()),
                curseOfVanishing
        );
    }

    private static String itemName(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath();
    }
}
