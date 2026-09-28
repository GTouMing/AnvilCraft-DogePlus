package dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶合配方：当基材上镶嵌的全部材料与本配方声明的 {@code inlays} 一一匹配时，
 * 消耗整叠基材，按 {@code results} 声明的产物逐份产出。
 *
 * <p>匹配与产物都是数据驱动的：{@code inlays} 里的每条 {@link InlayMatcher} 可以按
 * 材料属性（{@code "properties"}）或物品/标签 + 附魔条数范围（{@code "enchants_count"}）匹配；
 * {@code results} 里的每条 {@link InlayOutput} 可以固定、带概率，数量和概率还能用
 * {@code {"base": …, "multiplier": …}}（基数 × 乘数，基数可为附魔数量）缩放（见 {@link InlayValue}）。JSON 位于
 * {@code data/<ns>/recipe/<kind.folder()>/*.json}：</p>
 *
 * <pre>{@code
 * {
 *   "type": "anvilcraft_doge_plus:inlay_crafting",
 *   "base": "anvilcraft:overheated_ember_metal_block",
 *   "inlays": [
 *     { "item": "anvilcraft:charged_neutronium_ingot", "enchants_count": { "min": 16 } }
 *   ],
 *   "results": [
 *     { "item": "anvilcraft:neutronium_ingot" },
 *     { "item": "anvilcraft:transcendium_block" },
 *     { "item": "anvilcraft:transcendium_nugget", "count": { "base": "enchants_count", "multiplier": 1 } }
 *   ]
 * }
 * }</pre>
 *
 * <p>匹配规则：基材物品一致 + 有效镶孔与 {@code inlays} 存在一一匹配
 * （<b>忽略镶孔顺序</b>，空占位、多余或无法匹配的镶嵌视为不匹配）。</p>
 *
 * @param base    基材物品 ID（充当模具的物品）
 * @param inlays  期望的镶孔要求集合（顺序无关）
 * @param results 产物列表（可多件、可带概率、可动态推导）
 * @param kind    类别（只决定界面分类与数据包目录）
 */
public record InlayCraftingRecipe(
        ResourceLocation base,
        List<InlayMatcher> inlays,
        List<InlayOutput> results,
        Kind kind
) implements Recipe<InlayCraftingRecipe.Input> {

    /**
     * 镶合类别：只决定界面分类与数据包目录。
     *
     * <ul>
     *   <li>{@link #CRAFTING} 镶嵌合成——本 mod 手写配方；</li>
     *   <li>{@link #SMITHING} 镶嵌锻造——来自锻造类配方（模板作为产物返还）；</li>
     *   <li>{@link #COPYING} 镶嵌复制——来自前置珠宝复制配方。</li>
     * </ul>
     */
    public enum Kind implements StringRepresentable {
        CRAFTING("crafting"),
        SMITHING("smithing"),
        COPYING("copying");

        public static final Codec<Kind> CODEC = StringRepresentable.fromEnum(Kind::values);

        private final String name;

        Kind(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** 该类别的数据包目录名（同时用作 JEI 分类名的后缀）。 */
        public String folder() {
            return switch (this) {
                case SMITHING -> "inlay_smithing";
                case COPYING -> "inlay_copying";
                case CRAFTING -> "inlay_crafting";
            };
        }
    }

    public InlayCraftingRecipe {
        inlays = List.copyOf(inlays);
        results = List.copyOf(results);
    }

    // ==================== 匹配 ====================

    /**
     * 基材是否满足本配方：物品匹配，且每个有效镶孔能被某一条 {@code inlays} 命中、
     * 每条 {@code inlays} 恰好对应一个镶孔（忽略顺序的完美匹配）。
     */
    public boolean matches(ItemStack baseStack) {
        if (baseStack.isEmpty() || !baseStack.is(getBaseItem())) return false;

        List<InlayEntry> filled = new ArrayList<>();
        for (InlayEntry entry : InlayUtil.getInlays(baseStack)) {
            if (!entry.isEmpty()) filled.add(entry);
        }
        if (filled.size() != inlays.size()) return false;
        if (filled.isEmpty()) return false;

        return match(0, filled, new boolean[filled.size()]);
    }

    /** 递归为每条 {@code inlays} 分配一个互不重复的镶孔（完美匹配）。 */
    private boolean match(int index, List<InlayEntry> filled, boolean[] used) {
        if (index == inlays.size()) return true;
        for (int i = 0; i < filled.size(); i++) {
            if (used[i]) continue;
            if (inlays.get(index).test(filled.get(i))) {
                used[i] = true;
                if (match(index + 1, filled, used)) return true;
                used[i] = false;
            }
        }
        return false;
    }

    /** 基材孔内材料的附魔条数（取第一个非空镶孔；供产物数量/概率表达式求值）。 */
    public static int enchantmentCount(ItemStack baseStack) {
        for (InlayEntry entry : InlayUtil.getInlays(baseStack)) {
            if (!entry.isEmpty()) return entry.enchantmentCount();
        }
        return 0;
    }

    /** 配方限定的附魔条数范围（某条镶孔要求带 {@code enchants_count} 时）；未限定返回空。 */
    public Optional<InlayMatcher.EnchantRange> enchantRange() {
        for (InlayMatcher matcher : inlays) {
            if (matcher instanceof InlayMatcher.ByItem byItem
                    && (byItem.enchantsCount().min().isPresent() || byItem.enchantsCount().max().isPresent())) {
                return Optional.of(byItem.enchantsCount());
            }
        }
        return Optional.empty();
    }

    // ==================== 产物 ====================

    /** 实际产出：逐个产物按概率判定并展开（每份基材调用一次）。 */
    public List<ItemStack> resultsFor(ItemStack baseStack, RandomSource random, RegistryAccess registries) {
        List<ItemStack> outputs = new ArrayList<>();
        for (InlayOutput output : results) {
            for (ItemStack stack : output.roll(baseStack, random, registries)) {
                if (!stack.isEmpty()) outputs.add(stack);
            }
        }
        return outputs;
    }

    /** 展示用产物：列出所有可能产物，不判定概率（JEI / 文档）。 */
    public List<InlayOutput.Preview> previewFor(ItemStack baseStack, RegistryAccess registries) {
        List<InlayOutput.Preview> previews = new ArrayList<>();
        for (InlayOutput output : results) {
            previews.addAll(output.preview(baseStack, registries));
        }
        return previews;
    }

    /** 是否含「按输入推导」的产物（JEI 需要逐帧重算输出槽）。 */
    public boolean hasDynamicResults() {
        for (InlayOutput output : results) {
            if (output instanceof InlayOutput.ArmorTrim) return true;
        }
        return false;
    }

    // ==================== 物品解析 ====================

    /** 解析基材物品；未注册时为空气（配合 {@code baseStack.is(...)} 自然不匹配）。 */
    public Item getBaseItem() {
        return BuiltInRegistries.ITEM.get(base);
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

    /**
     * 单产物回退（供配方书等通用消费者）：返回第一条固定产物的物品（数量 1）；
     * 没有固定产物时返回空。真正展示走 {@link #previewFor}。
     */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        for (InlayOutput output : results) {
            if (output instanceof InlayOutput.Fixed fixed && fixed.item() != net.minecraft.world.item.Items.AIR) {
                return new ItemStack(fixed.item());
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (InlayMatcher matcher : inlays) {
            List<ItemStack> candidates = matcher.candidates();
            list.add(candidates.isEmpty()
                    ? Ingredient.of()
                    : Ingredient.of(candidates.toArray(ItemStack[]::new)));
        }
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

        public static final MapCodec<InlayCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("base").forGetter(InlayCraftingRecipe::base),
                InlayMatcher.CODEC.listOf().fieldOf("inlays").forGetter(InlayCraftingRecipe::inlays),
                InlayOutput.CODEC.listOf().fieldOf("results").forGetter(InlayCraftingRecipe::results),
                Kind.CODEC.optionalFieldOf("kind", Kind.CRAFTING).forGetter(InlayCraftingRecipe::kind)
        ).apply(instance, InlayCraftingRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, InlayCraftingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        private static void toNetwork(RegistryFriendlyByteBuf buffer, InlayCraftingRecipe recipe) {
            ResourceLocation.STREAM_CODEC.encode(buffer, recipe.base());
            buffer.writeVarInt(recipe.inlays().size());
            for (InlayMatcher matcher : recipe.inlays()) {
                InlayMatcher.STREAM_CODEC.encode(buffer, matcher);
            }
            buffer.writeVarInt(recipe.results().size());
            for (InlayOutput output : recipe.results()) {
                InlayOutput.STREAM_CODEC.encode(buffer, output);
            }
            buffer.writeUtf(recipe.kind().getSerializedName());
        }

        private static InlayCraftingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            ResourceLocation base = ResourceLocation.STREAM_CODEC.decode(buffer);
            int inlayCount = buffer.readVarInt();
            List<InlayMatcher> inlays = new ArrayList<>(inlayCount);
            for (int i = 0; i < inlayCount; i++) {
                inlays.add(InlayMatcher.STREAM_CODEC.decode(buffer));
            }
            int resultCount = buffer.readVarInt();
            List<InlayOutput> results = new ArrayList<>(resultCount);
            for (int i = 0; i < resultCount; i++) {
                results.add(InlayOutput.STREAM_CODEC.decode(buffer));
            }
            Kind kind = switch (buffer.readUtf()) {
                case "smithing" -> Kind.SMITHING;
                case "copying" -> Kind.COPYING;
                default -> Kind.CRAFTING;
            };
            return new InlayCraftingRecipe(base, inlays, results, kind);
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
