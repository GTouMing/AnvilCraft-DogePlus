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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 镶嵌条目：存储单个镶嵌材料的完整数据
 * <p>
 * 包含材料 ID 和额外数据列表（如药水效果、附魔等）
 */
public record InlayEntry(ResourceLocation id, List<ResourceLocation> extra, List<ResourceLocation> attributes) {

    // ==================== 编解码器 ====================

    public static final Codec<InlayEntry> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("id").forGetter(InlayEntry::id),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("extra", List.of()).forGetter(InlayEntry::extra),
                    ResourceLocation.CODEC.listOf().optionalFieldOf("attributes", List.of()).forGetter(InlayEntry::attributes)
            ).apply(instance, InlayEntry::new)
    );

    public static final StreamCodec<ByteBuf, InlayEntry> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            InlayEntry::id,
            ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC),
            InlayEntry::extra,
            ByteBufCodecs.collection(ArrayList::new, ResourceLocation.STREAM_CODEC),
            InlayEntry::attributes,
            InlayEntry::new
    );

    // ==================== 工厂方法 ====================

    /**
     * 从 ItemStack 创建 InlayEntry
     */
    public static InlayEntry fromItemStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return nulls();
        }

        MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(stack);
        if (material == null) return nulls();

        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        List<ResourceLocation> extra = new ArrayList<>();
        List<ResourceLocation> attributes = material.properties().stream().map(InlayProperty::id).toList();

        // ===== 药水效果提取 =====
        // 从 ItemStack 的 DataComponents 中提取药水效果
        if (!attributes.contains(InlayProperty.EFFECT.id())) return new InlayEntry(id, extra, attributes);
        var potionContents = stack.get(DataComponents.POTION_CONTENTS);
        if (potionContents == null) return new InlayEntry(id, extra, attributes);

        potionContents.potion().ifPresent(potionHolder -> {
            ResourceLocation potionId = BuiltInRegistries.POTION.getKey(potionHolder.value());
            if (potionId == null) return;
            extra.add(potionId);
        });

        return new InlayEntry(id, extra, attributes);
    }

    // ==================== 转换方法 ====================

    /**
     * 将 InlayEntry 转换为 ItemStack
     * <p>
     * 根据 id 创建基础物品，然后应用 extra 中的药水效果
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

        if (!containsAttributes(InlayProperty.EFFECT)) {
            return stack;
        }

        // 没有额外数据，直接返回
        if (extra == null || extra.isEmpty()) {
            return stack;
        }

        // 查找药水效果
        ResourceLocation potionId = null;
        for (ResourceLocation extraId : extra) {
            if (BuiltInRegistries.POTION.containsKey(extraId)) {
                potionId = extraId;
                break;
            }
        }

        // 没有药水效果，直接返回
        if (potionId == null) {
            return stack;
        }

        // 应用药水效果
        var holder = BuiltInRegistries.POTION.getHolder(potionId).orElse(null);
        if (holder == null) {
            return stack;
        }

        var contents = new PotionContents(
                Optional.of(holder),
                Optional.empty(),
                List.of()
        );
        stack.set(DataComponents.POTION_CONTENTS, contents);

        return stack;
    }

    /**
     * 空数据对象
     */
    public static InlayEntry nulls() {
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