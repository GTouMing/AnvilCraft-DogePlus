package dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 镶合配方的一条产物。
 *
 * <ul>
 *   <li>{@link Fixed}：物品 + 数量 + 可选概率 / 消失诅咒。数量与概率都是 {@code 基数 × 乘数}
 *       （见 {@link InlayValue}），基数可为固定值或附魔数量。JSON 可写成物品 id 字符串，或
 *       {@code {"item": "id", "count": 4, "chance": {"base": "enchants_count", "multiplier": 0.1}}}。</li>
 *   <li>{@link ArmorTrim}：由基材模板与镶孔内的装备/材料推导盔甲纹饰产物，
 *       JSON 为 {@code {"type": "...:armor_trim"}}。</li>
 * </ul>
 *
 * @see InlayCraftingRecipe#resultsFor(ItemStack, RandomSource, RegistryAccess)
 */
public sealed interface InlayOutput permits InlayOutput.Fixed, InlayOutput.ArmorTrim {

    /**
     * 实际产出：按概率判定，未命中的产物不产出（返回空列表）。每份基材独立判定一次。
     *
     * @param baseStack  已镶满的基材（含全部镶孔）
     * @param random     随机源
     * @param registries 注册表访问（附魔/纹饰推导用）
     */
    List<ItemStack> roll(ItemStack baseStack, RandomSource random, RegistryAccess registries);

    /**
     * JEI / 文档展示：列出所有可能产物，不判定概率。
     * 数量与概率以表达式形式给出（见 {@link InlayValue}），供界面标注。
     */
    List<Preview> preview(ItemStack baseStack, RegistryAccess registries);

    /** 该产物是否为概率产出（可能不产出：概率低于 100% 或随附魔数变化）。 */
    boolean isChance();

    /** 展示用产物：物品 + 数量表达式 + 概率表达式。 */
    record Preview(ItemStack stack, InlayValue count, InlayValue chance) {
    }

    // ==================== 编解码 ====================

    Codec<InlayOutput> CODEC = Codec.either(Fixed.CODEC, ArmorTrim.CODEC).xmap(
            either -> either.left().<InlayOutput>map(fixed -> fixed)
                    .orElseGet(() -> either.right().orElseThrow()),
            output -> output instanceof Fixed fixed
                    ? Either.left(fixed)
                    : Either.right((ArmorTrim) output));

    StreamCodec<RegistryFriendlyByteBuf, InlayOutput> STREAM_CODEC =
            StreamCodec.of(InlayOutput::write, InlayOutput::read);

    private static void write(RegistryFriendlyByteBuf buffer, InlayOutput output) {
        switch (output) {
            case Fixed fixed -> {
                buffer.writeUtf("item");
                ResourceLocation.STREAM_CODEC.encode(buffer, BuiltInRegistries.ITEM.getKey(fixed.item()));
                InlayValue.STREAM_CODEC.encode(buffer, fixed.count());
                InlayValue.STREAM_CODEC.encode(buffer, fixed.chance());
                buffer.writeBoolean(fixed.curse());
            }
            case ArmorTrim ignored -> buffer.writeUtf("armor_trim");
        }
    }

    private static InlayOutput read(RegistryFriendlyByteBuf buffer) {
        return switch (buffer.readUtf()) {
            case "armor_trim" -> new ArmorTrim();
            case "item" -> new Fixed(
                    BuiltInRegistries.ITEM.get(ResourceLocation.STREAM_CODEC.decode(buffer)),
                    InlayValue.STREAM_CODEC.decode(buffer),
                    InlayValue.STREAM_CODEC.decode(buffer),
                    buffer.readBoolean());
            default -> throw new IllegalStateException("Unknown inlay output type in network buffer");
        };
    }

    /** 只接受指定 id 的 {@code "type"} 字段；不匹配时让 {@link Codec#either} 落到下一种形态。 */
    private static Codec<ResourceLocation> typeLiteral(ResourceLocation expected) {
        return ResourceLocation.CODEC.flatXmap(
                id -> id.equals(expected)
                        ? DataResult.success(id)
                        : DataResult.error(() -> "Expected inlay output type " + expected + " but got " + id),
                DataResult::success);
    }

    // ==================== 固定产物 ====================

    /**
     * 固定产物：物品 + 数量，可选概率与消失诅咒。
     *
     * @param item   产物物品
     * @param count  每份基材的产出数量（可为 {@code 倍率 × 附魔数量} 表达式）
     * @param chance 出现概率（可为 {@code 倍率 × 附魔数量} 表达式；1 表示必定出现）
     * @param curse  是否附加 1 级消失诅咒（前置珠宝复制的复制品语义）
     */
    record Fixed(Item item, InlayValue count, InlayValue chance, boolean curse) implements InlayOutput {

        private static final InlayValue ONE = InlayValue.ONE;

        /** 简写：单个物品 id 字符串，数量 1、必定出现、无诅咒。 */
        private static final Codec<Fixed> BARE_CODEC = ResourceLocation.CODEC.xmap(
                id -> new Fixed(BuiltInRegistries.ITEM.get(id), ONE, ONE, false),
                fixed -> BuiltInRegistries.ITEM.getKey(fixed.item()));

        private static final MapCodec<Fixed> OBJECT_CODEC = new MapCodec<>() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.of(
                        ops.createString("item"),
                        ops.createString("id"),
                        ops.createString("count"),
                        ops.createString("chance"),
                        ops.createString("curse"));
            }

            @Override
            public <T> DataResult<Fixed> decode(DynamicOps<T> ops, MapLike<T> input) {
                T itemTag = input.get("item") != null ? input.get("item") : input.get("id");
                if (itemTag == null) {
                    return DataResult.error(() -> "Inlay result requires an \"item\"");
                }
                return ops.getStringValue(itemTag).flatMap(raw -> {
                    ResourceLocation id = ResourceLocation.tryParse(raw);
                    if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
                        return DataResult.error(() -> "Unknown item in inlay result: " + raw);
                    }
                    Item item = BuiltInRegistries.ITEM.get(id);
                    return field(ops, input, "count", InlayValue.COUNT_CODEC, ONE).flatMap(
                            count -> field(ops, input, "chance", InlayValue.CHANCE_CODEC, ONE).flatMap(
                                    chance -> field(ops, input, "curse", Codec.BOOL, false).map(
                                            curse -> new Fixed(item, count, chance, curse))));
                });
            }

            @Override
            public <T> RecordBuilder<T> encode(Fixed input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                prefix.add("item", ops.createString(BuiltInRegistries.ITEM.getKey(input.item()).toString()));
                if (!isOne(input.count())) {
                    prefix.add("count", InlayValue.COUNT_CODEC.encodeStart(ops, input.count()).getOrThrow());
                }
                if (!isOne(input.chance())) {
                    prefix.add("chance", InlayValue.CHANCE_CODEC.encodeStart(ops, input.chance()).getOrThrow());
                }
                if (input.curse()) {
                    prefix.add("curse", ops.createBoolean(true));
                }
                return prefix;
            }

            private static <T, A> DataResult<A> field(
                    DynamicOps<T> ops, MapLike<T> input, String key, Codec<A> codec, A fallback) {
                T tag = input.get(key);
                return tag == null ? DataResult.success(fallback) : codec.parse(ops, tag);
            }
        };

        public static final Codec<Fixed> CODEC = Codec.either(BARE_CODEC, OBJECT_CODEC.codec()).xmap(
                either -> either.left().orElseGet(() -> either.right().orElseThrow()),
                fixed -> isSimple(fixed)
                        ? Either.<Fixed, Fixed>left(fixed)
                        : Either.<Fixed, Fixed>right(fixed));

        private static boolean isSimple(Fixed fixed) {
            return isOne(fixed.count()) && isOne(fixed.chance()) && !fixed.curse();
        }

        private static boolean isOne(InlayValue value) {
            return value.base() instanceof InlayValue.Base.Constant constant
                    && constant.value() == 1.0
                    && value.multiplier() == 1.0;
        }

        @Override
        public List<ItemStack> roll(ItemStack baseStack, RandomSource random, RegistryAccess registries) {
            if (item == Items.AIR) return List.of();
            int enchantsCount = InlayCraftingRecipe.enchantmentCount(baseStack);
            int amount = (int) Math.round(count.evaluate(enchantsCount));
            if (amount <= 0) return List.of();
            double chanceValue = chance.evaluate(enchantsCount);
            if (chanceValue < 1.0 && random.nextDouble() >= chanceValue) return List.of();
            return List.of(withCurse(new ItemStack(item, amount), registries));
        }

        @Override
        public List<Preview> preview(ItemStack baseStack, RegistryAccess registries) {
            if (item == Items.AIR) return List.of();
            int enchantsCount = InlayCraftingRecipe.enchantmentCount(baseStack);
            int amount = (int) Math.round(count.evaluate(enchantsCount));
            // 随附魔数变化的数量无法给出定值，图标只显示 1 件，具体数量在 tooltip 标注。
            int iconCount = count.isEnchantScaled() ? 1 : Math.max(1, amount);
            ItemStack stack = withCurse(new ItemStack(item, iconCount), registries);
            return List.of(new Preview(stack, count, chance));
        }

        @Override
        public boolean isChance() {
            return chance.isEnchantScaled() || chance.evaluate(0) < 1.0;
        }

        private ItemStack withCurse(ItemStack stack, RegistryAccess registries) {
            if (!curse) return stack;
            ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            enchantments.set(registries.holderOrThrow(Enchantments.VANISHING_CURSE), 1);
            stack.set(DataComponents.ENCHANTMENTS, enchantments.toImmutable());
            return stack;
        }
    }

    // ==================== 盔甲纹饰 ====================

    /**
     * 盔甲纹饰推导：给镶孔内的「可纹饰装备」施加由基材模板与纹饰材料推导出的纹饰组件，
     * 产物 = 装备副本（+ 纹饰）。合并了原先分散在方块实体与 JEI 里的两份推导逻辑。
     */
    record ArmorTrim() implements InlayOutput {

        public static final ResourceLocation TYPE = AnvilCraftDogePlus.of("armor_trim");

        public static final MapCodec<ArmorTrim> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                typeLiteral(TYPE).fieldOf("type").forGetter(trim -> TYPE)
        ).apply(instance, id -> new ArmorTrim()));

        public static final Codec<ArmorTrim> CODEC = MAP_CODEC.codec();

        private static final TagKey<Item> TRIM_ARMOR =
                TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trimmable_armor"));
        private static final TagKey<Item> TRIM_MATERIALS =
                TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trim_materials"));

        @Override
        public List<ItemStack> roll(ItemStack baseStack, RandomSource random, RegistryAccess registries) {
            ItemStack result = derive(baseStack, registries);
            return result.isEmpty() ? List.of() : List.of(result);
        }

        @Override
        public List<Preview> preview(ItemStack baseStack, RegistryAccess registries) {
            ItemStack result = derive(baseStack, registries);
            return result.isEmpty()
                    ? List.of()
                    : List.of(new Preview(result, InlayValue.ONE, InlayValue.ONE));
        }

        @Override
        public boolean isChance() {
            return false;
        }

        private static ItemStack derive(ItemStack template, RegistryAccess registries) {
            ItemStack armor = ItemStack.EMPTY;
            ItemStack material = ItemStack.EMPTY;
            for (InlayEntry entry : InlayUtil.getInlays(template)) {
                if (entry.isEmpty()) continue;
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.id()));
                if (stack.isEmpty()) return ItemStack.EMPTY;
                if (stack.is(TRIM_ARMOR) && armor.isEmpty()) {
                    armor = stack;
                } else if (stack.is(TRIM_MATERIALS)) {
                    material = stack;
                }
            }
            if (armor.isEmpty() || material.isEmpty()) return ItemStack.EMPTY;

            Optional<Holder.Reference<TrimPattern>> pattern = TrimPatterns.getFromTemplate(registries, template);
            Optional<Holder.Reference<TrimMaterial>> trimMaterial = TrimMaterials.getFromIngredient(registries, material);
            if (pattern.isEmpty() || trimMaterial.isEmpty()) return ItemStack.EMPTY;

            ItemStack result = armor.copyWithCount(1);
            result.set(DataComponents.TRIM,
                    new net.minecraft.world.item.armortrim.ArmorTrim(trimMaterial.get(), pattern.get()));
            return result;
        }
    }
}
