package dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶合配方：当基材上镶嵌的全部材料与本配方声明的 {@code inlays} 一一匹配时，
 * 消耗 1 个基材，产出产物与一个「空镶嵌」的基材（原基材去掉所有镶嵌）。
 *
 * <p>典型用法是复刻锻造等原版合成：基材充当可复用的「模具」，镶孔内嵌材料
 * 与装备，配方匹配后返回模具并给出成品。JSON 位于
 * {@code data/<ns>/recipe/inlay_crafting/*.json}：</p>
 * <pre>{@code
 * {
 *   "type": "anvilcraft_doge_plus:inlay_crafting",
 *   "base": "minecraft:netherite_upgrade_smithing_template",
 *   "inlays": [
 *     {"item": "minecraft:netherite_ingot"},
 *     {"item": "minecraft:diamond_axe"}
 *   ],
 *   "result": "minecraft:netherite_axe"
 * }
 * }</pre>
 *
 * <p>匹配规则：基材物品一致 + 有效镶孔与 {@code inlays} 存在一一匹配
 * （<b>忽略镶孔顺序</b>，空占位、多余或无法匹配的镶嵌视为不匹配）；
 * 每个 {@code inlays} 可用物品或标签表达（如纹饰锻造的
 * {@code #minecraft:trimmable_armor}）。{@code result} 可缺省：缺省表示产物为
 * 「模具基材自身 + 施加纹饰组件的镶孔装备」这类需在加工时按实际输入推导的结果。</p>
 *
 * <p>{@code curse_of_vanishing} 为真时产物附加 1 级消失诅咒（前置珠宝合成台的复制语义：
 * 复制品带诅咒，模具不受影响）。{@code result} 支持原版多产物写法
 * （{@code {"id": ..., "count": n}}），单产物时也可直接写物品 id。</p>
 *
 * @param base              基材物品 ID（充当模具的物品）
 * @param inlays            期望的镶孔内容集合（顺序无关）
 * @param result            合成产物（含数量）；为 {@code null} 时产物在加工时按基材/镶孔推导（如纹饰）
 * @param curseOfVanishing  产物是否附加 1 级消失诅咒
 */
public record InlayCraftingRecipe(
        ResourceLocation base,
        List<Ingredient> inlays,
        @Nullable ItemStack result,
        boolean curseOfVanishing
) implements Recipe<InlayCraftingRecipe.Input> {

    public InlayCraftingRecipe {
        inlays = List.copyOf(inlays);
    }

    /** 是否在加工时按输入推导产物（无固定 result，如纹饰锻造）。 */
    public boolean derivesResult() {
        return result == null;
    }

    // ==================== 匹配 ====================

    /**
     * 基材是否满足本配方：物品匹配，且每个有效镶孔能被某一个 {@code inlays} 命中、
     * 每个 {@code inlays} 恰好对应一个镶孔（忽略顺序的完美匹配）。
     */
    public boolean matches(ItemStack baseStack) {
        if (baseStack.isEmpty() || !baseStack.is(getBaseItem())) return false;

        List<ItemStack> filled = new ArrayList<>();
        for (InlayEntry entry : InlayUtil.getInlays(baseStack)) {
            if (!entry.isEmpty()) {
                ItemStack stack = toStack(entry);
                if (stack.isEmpty()) return false;
                filled.add(stack);
            }
        }
        if (filled.size() != inlays.size()) return false;
        if (filled.isEmpty()) return false;

        return matchIngredient(0, filled, new boolean[filled.size()]);
    }

    /** 递归为每个 {@code inlays} 分配一个互不重复的镶孔（完美匹配）。 */
    private boolean matchIngredient(int index, List<ItemStack> filled, boolean[] used) {
        if (index == inlays.size()) return true;
        for (int i = 0; i < filled.size(); i++) {
            if (used[i]) continue;
            if (inlays.get(index).test(filled.get(i))) {
                used[i] = true;
                if (matchIngredient(index + 1, filled, used)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    /** 生成空镶嵌基材（原基材去掉全部镶嵌，属性修饰器一并清理）。 */
    public ItemStack emptyBaseOf(ItemStack baseStack) {
        return InlayUtil.withInlaysRemoved(baseStack);
    }

    // ==================== 物品解析 ====================

    /** 解析基材物品；未注册时为空气（配合 {@code baseStack.is(...)} 自然不匹配）。 */
    public Item getBaseItem() {
        return BuiltInRegistries.ITEM.get(base);
    }

    /** 配方声明的产物（含数量）；无固定产物（纹饰类）返回空。 */
    public ItemStack getResultStack() {
        return result == null ? ItemStack.EMPTY : result.copy();
    }

    /** 按镶孔条目重建用于匹配的物品（只比较物品本身，不携带药水/附魔等额外数据）。 */
    private static ItemStack toStack(InlayEntry entry) {
        Item item = BuiltInRegistries.ITEM.get(entry.id());
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    // ==================== Recipe 接口 ====================

    @Override
    public boolean matches(Input input, Level level) {
        return matches(input.base());
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack stack = getResultStack();
        if (stack.isEmpty()) return ItemStack.EMPTY;

        if (curseOfVanishing) {
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(registries.holderOrThrow(Enchantments.VANISHING_CURSE), 1);
            stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
        }
        return stack;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        list.addAll(inlays);
        return list;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public RecipeSerializer<InlayCraftingRecipe> getSerializer() {
        return ModRecipeTypes.INLAY_CRAFTING_SERIALIZER.get();
    }

    @Override
    public RecipeType<InlayCraftingRecipe> getType() {
        return ModRecipeTypes.INLAY_CRAFTING_TYPE.get();
    }

    /** 单槽输入：槽 0 为基材（含其全部镶嵌组件）。 */
    public record Input(ItemStack base) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? base : ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 1;
        }
    }

    // ==================== 序列化 ====================

    public static class Serializer implements RecipeSerializer<InlayCraftingRecipe> {

        /**
         * 产物编解码：兼容 {@code "result": "minecraft:xxx"}（单产物简写）与
         * {@code "result": {"id": "minecraft:xxx", "count": n}}（原版多产物写法）。
         */
        private static final Codec<ItemStack> RESULT_CODEC = Codec.either(
                ResourceLocation.CODEC,
                ItemStack.CODEC
        ).xmap(
                either -> either.map(id -> new ItemStack(BuiltInRegistries.ITEM.get(id)), stack -> stack),
                stack -> stack.getCount() > 1
                        ? Either.right(stack)
                        : Either.left(BuiltInRegistries.ITEM.getKey(stack.getItem()))
        );

        public static final MapCodec<InlayCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("base").forGetter(InlayCraftingRecipe::base),
                Ingredient.CODEC.listOf().fieldOf("inlays").forGetter(InlayCraftingRecipe::inlays),
                RESULT_CODEC.optionalFieldOf("result").forGetter(r -> Optional.ofNullable(r.result())),
                Codec.BOOL.optionalFieldOf("curse_of_vanishing", false)
                        .forGetter(InlayCraftingRecipe::curseOfVanishing)
        ).apply(instance, (base, inlays, result, curseOfVanishing) ->
                new InlayCraftingRecipe(base, inlays, result.orElse(null), curseOfVanishing)));

        public static final StreamCodec<RegistryFriendlyByteBuf, InlayCraftingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        private static void toNetwork(RegistryFriendlyByteBuf buffer, InlayCraftingRecipe recipe) {
            ResourceLocation.STREAM_CODEC.encode(buffer, recipe.base());
            buffer.writeVarInt(recipe.inlays().size());
            for (Ingredient ingredient : recipe.inlays()) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
            ItemStack.OPTIONAL_STREAM_CODEC.encode(
                    buffer, recipe.result() == null ? ItemStack.EMPTY : recipe.result());
            buffer.writeBoolean(recipe.curseOfVanishing());
        }

        private static InlayCraftingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            ResourceLocation base = ResourceLocation.STREAM_CODEC.decode(buffer);
            int size = buffer.readVarInt();
            List<Ingredient> inlays = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                inlays.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            ItemStack result = ItemStack.OPTIONAL_STREAM_CODEC.decode(buffer);
            boolean curseOfVanishing = buffer.readBoolean();
            return new InlayCraftingRecipe(base, inlays, result.isEmpty() ? null : result, curseOfVanishing);
        }

        @Override
        public MapCodec<InlayCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InlayCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
