package dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶合配方的一个镶孔要求。
 *
 * <p>两种写法：</p>
 * <ul>
 *   <li>{@link ByProperty}：{@code {"properties": "id"}} 或 {@code {"properties": ["a","b"]}}，
 *       只要材料携带其中<b>任一</b>属性即命中（如逻辑载体六孔各要一种门）；</li>
 *   <li>{@link ByItem}：{@code {"item": "id"}} / {@code {"item": ["a","b"]}} / {@code {"tag": "id"}}，
 *       可再附加 {@code "enchants_count": {"min": 16}} 限定材料镶入时的附魔条数范围
 *       （如超温余烬按中子锭附魔条数出产物）。</li>
 * </ul>
 *
 * <p>匹配对象是基材上的 {@link InlayEntry}（额外数据里记着附魔条数），因此
 * {@code enchants_count} 这类要求才成立；{@link #test(ItemStack)} 供「静默镶嵌」
 * 用实时物品做前置校验。</p>
 */
public sealed interface InlayMatcher permits InlayMatcher.ByProperty, InlayMatcher.ByItem {

    /** 是否命中某个已记录的镶孔条目。 */
    boolean test(InlayEntry entry);

    /** 是否命中一个实时物品（静默镶嵌用；附魔条数取物品自身）。 */
    boolean test(ItemStack stack);

    /** 展示用候选物品（JEI / 文档构造「已镶满的基材」）。 */
    List<ItemStack> candidates();

    /** 该要求点名的属性（非属性要求返回空），供 tooltip 说明。 */
    List<InlayProperty> properties();

    // ==================== 工厂 ====================

    static InlayMatcher byProperty(List<InlayProperty> properties) {
        return new ByProperty(properties);
    }

    /** 物品要求（不限附魔条数）。 */
    static InlayMatcher byItem(Item... items) {
        return new ByItem(List.of(items), Optional.empty(), EnchantRange.ANY);
    }

    /** 物品要求 + 附魔条数范围。 */
    static InlayMatcher byItem(EnchantRange enchantsCount, Item... items) {
        return new ByItem(List.of(items), Optional.empty(), enchantsCount);
    }

    /** 标签要求。 */
    static InlayMatcher byTag(TagKey<Item> tag) {
        return new ByItem(List.of(), Optional.of(tag), EnchantRange.ANY);
    }

    // ==================== 编解码 ====================

    Codec<InlayMatcher> CODEC = Codec.either(ByProperty.CODEC, ByItem.CODEC).xmap(
            either -> either.left().<InlayMatcher>map(matcher -> matcher)
                    .orElseGet(() -> either.right().orElseThrow()),
            matcher -> matcher instanceof ByProperty byProperty
                    ? Either.left(byProperty)
                    : Either.right((ByItem) matcher));

    StreamCodec<RegistryFriendlyByteBuf, InlayMatcher> STREAM_CODEC =
            StreamCodec.of(InlayMatcher::write, InlayMatcher::read);

    private static void write(RegistryFriendlyByteBuf buffer, InlayMatcher matcher) {
        switch (matcher) {
            case ByProperty byProperty -> {
                buffer.writeUtf("properties");
                buffer.writeVarInt(byProperty.properties().size());
                for (InlayProperty property : byProperty.properties()) {
                    ResourceLocation.STREAM_CODEC.encode(buffer, property.id());
                }
            }
            case ByItem byItem -> {
                buffer.writeUtf("item");
                buffer.writeVarInt(byItem.items().size());
                for (Item item : byItem.items()) {
                    ResourceLocation.STREAM_CODEC.encode(buffer, BuiltInRegistries.ITEM.getKey(item));
                }
                if (byItem.tag().isPresent()) {
                    buffer.writeBoolean(true);
                    ResourceLocation.STREAM_CODEC.encode(buffer, byItem.tag().get().location());
                } else {
                    buffer.writeBoolean(false);
                }
                EnchantRange range = byItem.enchantsCount();
                buffer.writeBoolean(range.min().isPresent());
                range.min().ifPresent(buffer::writeVarInt);
                buffer.writeBoolean(range.max().isPresent());
                range.max().ifPresent(buffer::writeVarInt);
            }
        }
    }

    private static InlayMatcher read(RegistryFriendlyByteBuf buffer) {
        String kind = buffer.readUtf();
        if (kind.equals("properties")) {
            int size = buffer.readVarInt();
            List<InlayProperty> properties = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                properties.add(InlayProperty.get(ResourceLocation.STREAM_CODEC.decode(buffer)));
            }
            return new ByProperty(properties);
        }
        if (kind.equals("item")) {
            int count = buffer.readVarInt();
            List<Item> items = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                items.add(BuiltInRegistries.ITEM.get(ResourceLocation.STREAM_CODEC.decode(buffer)));
            }
            Optional<TagKey<Item>> tag = buffer.readBoolean()
                    ? Optional.of(TagKey.create(Registries.ITEM, ResourceLocation.STREAM_CODEC.decode(buffer)))
                    : Optional.empty();
            Optional<Integer> min = buffer.readBoolean() ? Optional.of(buffer.readVarInt()) : Optional.empty();
            Optional<Integer> max = buffer.readBoolean() ? Optional.of(buffer.readVarInt()) : Optional.empty();
            return new ByItem(items, tag, new EnchantRange(min, max));
        }
        throw new IllegalStateException("Unknown inlay matcher kind in network buffer: " + kind);
    }

    // ==================== 按属性 ====================

    /** 命中材料携带的任一属性。 */
    record ByProperty(List<InlayProperty> properties) implements InlayMatcher {

        private static final Codec<List<InlayProperty>> PROPERTIES_CODEC =
                Codec.either(InlayProperty.CODEC, InlayProperty.CODEC.listOf()).xmap(
                        either -> either.left().map(List::of).orElseGet(() -> either.right().orElseThrow()),
                        list -> list.size() == 1
                                ? Either.left(list.getFirst())
                                : Either.right(list));

        public static final Codec<ByProperty> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                PROPERTIES_CODEC.fieldOf("properties").forGetter(ByProperty::properties)
        ).apply(instance, ByProperty::new));

        @Override
        public boolean test(InlayEntry entry) {
            for (InlayProperty property : properties) {
                if (entry.attributes().contains(property.id())) return true;
            }
            return false;
        }

        @Override
        public boolean test(ItemStack stack) {
            MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(stack);
            if (material == null) return false;
            for (InlayProperty property : properties) {
                if (material.properties().contains(property)) return true;
            }
            return false;
        }

        @Override
        public List<ItemStack> candidates() {
            return MaterialManager.itemsWithAnyOf(properties).stream().map(ItemStack::new).toList();
        }
    }

    // ==================== 按物品 / 标签 ====================

    /** 命中指定物品（或标签）且附魔条数落在范围内。 */
    record ByItem(List<Item> items, Optional<TagKey<Item>> tag, EnchantRange enchantsCount)
            implements InlayMatcher {

        private static final Codec<List<Item>> ITEMS_CODEC =
                Codec.either(BuiltInRegistries.ITEM.byNameCodec(), BuiltInRegistries.ITEM.byNameCodec().listOf()).xmap(
                        either -> either.left().map(List::of).orElseGet(() -> either.right().orElseThrow()),
                        list -> list.size() == 1
                                ? Either.left(list.getFirst())
                                : Either.right(list));

        public static final Codec<ByItem> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ITEMS_CODEC.optionalFieldOf("item", List.of()).forGetter(ByItem::items),
                TagKey.codec(Registries.ITEM).optionalFieldOf("tag").forGetter(ByItem::tag),
                EnchantRange.CODEC.optionalFieldOf("enchants_count", EnchantRange.ANY)
                        .forGetter(ByItem::enchantsCount)
        ).apply(instance, ByItem::new));

        @Override
        public boolean test(InlayEntry entry) {
            Item item = BuiltInRegistries.ITEM.get(entry.id());
            return matches(item) && enchantsCount.test(entry.enchantmentCount());
        }

        @Override
        public boolean test(ItemStack stack) {
            return !stack.isEmpty()
                    && matches(stack.getItem())
                    && enchantsCount.test(InlayEntry.countEnchantments(stack));
        }

        private boolean matches(Item item) {
            if (item == Items.AIR) return false;
            if (items.contains(item)) return true;
            return tag.map(value -> new ItemStack(item).is(value)).orElse(false);
        }

        @Override
        public List<ItemStack> candidates() {
            List<ItemStack> candidates = new ArrayList<>();
            for (Item item : items) {
                if (item != Items.AIR) candidates.add(new ItemStack(item));
            }
            tag.ifPresent(value -> BuiltInRegistries.ITEM.getTag(value)
                    .ifPresent(set -> set.forEach(holder -> candidates.add(new ItemStack(holder.value())))));
            return candidates;
        }

        @Override
        public List<InlayProperty> properties() {
            return List.of();
        }
    }

    // ==================== 附魔条数范围 ====================

    /** 附魔条数范围：{@code {"min": n, "max": m}}，两端皆可省略。 */
    record EnchantRange(Optional<Integer> min, Optional<Integer> max) {

        public static final EnchantRange ANY = new EnchantRange(Optional.empty(), Optional.empty());

        public static final Codec<EnchantRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("min").forGetter(EnchantRange::min),
                Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf("max").forGetter(EnchantRange::max)
        ).apply(instance, EnchantRange::new));

        public boolean test(int count) {
            if (min.isPresent() && count < min.get()) return false;
            return max.isEmpty() || count <= max.get();
        }
    }
}
