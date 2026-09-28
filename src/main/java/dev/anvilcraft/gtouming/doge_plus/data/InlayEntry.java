package dev.anvilcraft.gtouming.doge_plus.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶嵌条目：存储单个镶嵌材料的完整数据。
 *
 * <p>包含材料 ID、额外数据（{@link InlayExtra} 列表，如药水效果、附魔条数）与属性列表。</p>
 */
public record InlayEntry(ResourceLocation id, List<InlayExtra> extra, List<ResourceLocation> attributes) {

    // ==================== 编解码器 ====================

    public static final Codec<InlayEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(InlayEntry::id),
                    InlayExtra.LIST_CODEC.optionalFieldOf("extra", List.of()).forGetter(InlayEntry::extra),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(InlayEntry::attributes)
            ).apply(instance, InlayEntry::new)
    );

    public static final StreamCodec<ByteBuf, InlayEntry> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            InlayEntry::id,
            InlayExtra.LIST_STREAM_CODEC,
            InlayEntry::extra,
            ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC),
            InlayEntry::attributes,
            InlayEntry::new
    );

    // ==================== 工厂方法 ====================

    /**
     * 从 ItemStack 创建 InlayEntry（属性取自第一个匹配该物品的材料定义）。
     */
    public static InlayEntry fromItemStack(ItemStack stack) {
        return fromItemStack(stack, MaterialManager.getInlayMaterial(stack));
    }

    /**
     * 从 ItemStack 创建 InlayEntry，属性显式取自指定的材料定义。
     *
     * <p>同一物品可能匹配多个材料定义（如下界合金锭同时匹配 defense/fire_proof 等），
     * 镶嵌时应以实际命中的配方所引用的材料定义为准，保证「无属性锻造材料」的定义
     * 不会被其它定义的属性污染。</p>
     */
    public static InlayEntry fromItemStack(ItemStack stack, @Nullable MaterialManager.InlayMaterial material) {
        if (stack.isEmpty() || material == null) {
            return empty();
        }

        List<ResourceLocation> attributes = material.properties().stream().map(InlayProperty::id).toList();
        return build(stack, attributes);
    }

    /**
     * 按物品直接构造镶孔条目（不查材料定义，属性为空）。
     *
     * <p>供「静默镶嵌」使用：材料没有任何属性定义时（如两种中子锭，只作为镶合条件里的材料），
     * 仍然要记下物品 id 与额外数据（药水、附魔条数）。</p>
     */
    public static InlayEntry ofItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return empty();
        }
        return build(stack, List.of());
    }

    /** 按物品与属性列表构造条目，并抽取额外数据。 */
    private static InlayEntry build(ItemStack stack, List<ResourceLocation> attributes) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<InlayExtra> extra = new ArrayList<>();

        // 药水效果：只有带「效果」属性的材料才提取，镶嵌后仍能还原出对应药水
        if (attributes.contains(InlayProperty.EFFECT.id())) {
            PotionContents potionContents = stack.get(DataComponents.POTION_CONTENTS);
            if (potionContents != null) {
                potionContents.potion().ifPresent(potionHolder -> {
                    ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potionHolder.value());
                    if (potionId != null) extra.add(new InlayExtra.Potion(potionId));
                });
            }
        }

        // 附魔条数：材料本身会被消耗掉，条数要留下来（如中子锭按条数决定镶合产物）
        int enchantments = countEnchantments(stack);
        if (enchantments > 0) {
            extra.add(new InlayExtra.Enchantments(enchantments));
        }

        return new InlayEntry(id, extra, attributes);
    }

    /**
     * 统计物品携带的附魔条数（附魔书取 {@code STORED_ENCHANTMENTS}）。
     */
    public static int countEnchantments(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) return stored.size();
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).size();
    }

    // ==================== 额外数据查询 ====================

    /** 本条目的附魔条数（无此项时为 0）。 */
    public int enchantmentCount() {
        for (InlayExtra data : extra) {
            if (data instanceof InlayExtra.Enchantments(int count)) {
                return count;
            }
        }
        return 0;
    }

    /** 本条目的药水效果（无此项时为空）。 */
    public Optional<ResourceLocation> potion() {
        for (InlayExtra data : extra) {
            if (data instanceof InlayExtra.Potion(ResourceLocation potion1)) {
                return Optional.of(potion1);
            }
        }
        return Optional.empty();
    }

    // ==================== 转换方法 ====================

    /**
     * 将 InlayEntry 转换为 ItemStack：按 id 创建基础物品，再还原额外数据（目前为药水效果）。
     */
    public ItemStack toItemStack() {
        if (isEmpty()) {
            return ItemStack.EMPTY;
        }

        var item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item);

        Optional<ResourceLocation> potionId = potion();
        if (potionId.isEmpty()) {
            return stack;
        }

        var holder = BuiltInRegistries.POTION.getHolder(potionId.get()).orElse(null);
        if (holder == null) {
            return stack;
        }

        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.of(holder), Optional.empty(), List.of()));
        return stack;
    }

    /**
     * 空数据对象
     */
    public static InlayEntry empty() {
        return new InlayEntry(
                ResourceLocation.withDefaultNamespace("air"),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public boolean containsAttributes(InlayProperty property) {
        return attributes.contains(property.id());
    }

    /**
     * 检查是否为空数据
     */
    public boolean isEmpty() {
        return id.equals(ResourceLocation.withDefaultNamespace("air"));
    }

    // ==================== 工具方法 ====================

    @Override
    public String toString() {
        return "InlayEntry{id=" + id + ", extra=" + extra + "attributes=" + attributes + "}";
    }
}
