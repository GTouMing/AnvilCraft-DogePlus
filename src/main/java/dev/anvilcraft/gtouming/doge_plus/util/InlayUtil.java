package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 镶嵌工具：读写 {@code INLAY} 组件（材料 ID 列表）、查询材料性质。
 */
public final class InlayUtil {

    private InlayUtil() {}

    // ==================== INLAY 组件读写 ====================

    public static ArrayList<InlayEntry> getInlays(ItemStack stack) {
        return new ArrayList<>(stack.getOrDefault(ModDataComponentTypes.INLAY, new ArrayList<>()));
    }

    public static void setInlays(ItemStack stack, ArrayList<InlayEntry> list) {
        stack.set(ModDataComponentTypes.INLAY, list);
    }

    public static int getInlayCount(ItemStack stack) {
        return getInlays(stack).size();
    }

    @Nullable
    public static MaterialManager.InlayMaterial getMaterial(InlayEntry entry) {
        return MaterialManager.getInlayMaterial(BuiltInRegistries.ITEM.get(entry.id()).getDefaultInstance());
    }

    // ==================== 属性查询 ====================

    public static boolean hasProperty(ItemStack stack, InlayProperty property) {
        for (InlayEntry entry : getInlays(stack)) {
            if (entry.containsAttributes(property))
                return true;
        }
        return false;
    }

    public static int countProperty(ItemStack stack, InlayProperty property) {
        int count = 0;
        for (InlayEntry entry : getInlays(stack)) {
            if (entry.containsAttributes(property)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 镶嵌操作 ====================

    public static ItemStack withAddedInlay(ItemStack base, InlayEntry inlay) {
        List<InlayEntry> inlays = new ArrayList<>(getInlays(base));
        if (inlays.contains(InlayEntry.nulls()))
            inlays.set(inlays.indexOf(InlayEntry.nulls()), inlay);
        else
            inlays.add(inlay);
        return withInlays(base, inlays);
    }

    /**
     * 替换指定槽位的镶嵌
     */
    public static ItemStack withReplacedAt(ItemStack base, int slot, InlayEntry newEntry) {
        List<InlayEntry> inlays = new ArrayList<>(getInlays(base));
        if (slot < 0 || slot >= inlays.size()) return ItemStack.EMPTY;
        inlays.set(slot, newEntry);
        return withInlays(base, inlays);
    }

    /**
     * 移除指定槽位的镶嵌
     */
    public static ItemStack withRemovedAt(ItemStack base, int slot) {
        List<InlayEntry> inlays = new ArrayList<>(getInlays(base));
        if (slot < 0 || slot >= inlays.size()) return ItemStack.EMPTY;
        inlays.set(slot, InlayEntry.nulls());
        return withInlays(base, inlays);
    }

    /**
     * 获取指定槽位的镶嵌
     */
    public static InlayEntry getInlayAt(ItemStack stack, int slot) {
        List<InlayEntry> inlays = getInlays(stack);
        if (slot < 0 || slot >= inlays.size()) return InlayEntry.nulls();
        return inlays.get(slot);
    }

    private static ItemStack withInlays(ItemStack base, List<InlayEntry> inlays) {
        ItemStack result = base.copy();
        result.setCount(1);
        if (inlays.isEmpty()) {
            result.remove(ModDataComponentTypes.INLAY);
        } else {
            result.set(ModDataComponentTypes.INLAY, inlays);
        }
        return reapplyAttributeModifiers(result);
    }

    // ==================== 属性修饰器 ====================

    public static ItemAttributeModifiers getCurrentModifiers(ItemStack stack) {
        ItemAttributeModifiers fromComponent = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (fromComponent != null && fromComponent != ItemAttributeModifiers.EMPTY) {
            return fromComponent;
        }

        Item item = stack.getItem();
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getDefaultAttributeModifiers();
        }
        return ItemAttributeModifiers.EMPTY;
    }

    public static ItemStack reapplyAttributeModifiers(ItemStack stack) {
        int defense = countProperty(stack, InlayProperty.DEFENSE);
        int life = countProperty(stack, InlayProperty.LIFE);
        int attack = countProperty(stack, InlayProperty.ATTACK);

        ItemAttributeModifiers modifiers = getCurrentModifiers(stack);
        EquipmentSlotGroup slotGroup = getSlotGroupForItem(stack);
        double base = hasProperty(stack, InlayProperty.RESONANCE) ? 4.0 : 2.0;

        modifiers = applyModifier(modifiers, Attributes.ARMOR, defense, "inlay_defense", base, slotGroup);
        modifiers = applyModifier(modifiers, Attributes.MAX_HEALTH, life, "inlay_life", base, slotGroup);
        modifiers = applyModifier(modifiers, Attributes.ATTACK_DAMAGE, attack, "inlay_attack", base, EquipmentSlotGroup.MAINHAND);

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
        return stack;
    }

    private static ItemAttributeModifiers applyModifier(ItemAttributeModifiers modifiers,
                                                        Holder<Attribute> attribute,
                                                        int count,
                                                        String key,
                                                        double base,
                                                        EquipmentSlotGroup slot) {
        ResourceLocation id = AnvilCraftDogePlus.of(key);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // 复制所有原有的修饰器（排除同名）
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            if (!entry.modifier().is(id)) {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        if (count > 0) {
            builder.add(
                    attribute,
                    new AttributeModifier(id, base * count, AttributeModifier.Operation.ADD_VALUE),
                    slot
            );
        }

        return builder.build();
    }

    private static EquipmentSlotGroup getSlotGroupForItem(ItemStack stack) {
        Item item = stack.getItem();
        return switch (item) {
            case ArmorItem armor -> switch (armor.getEquipmentSlot()) {
                case HEAD -> EquipmentSlotGroup.HEAD;
                case CHEST -> EquipmentSlotGroup.CHEST;
                case LEGS -> EquipmentSlotGroup.LEGS;
                case FEET -> EquipmentSlotGroup.FEET;
                default -> EquipmentSlotGroup.ANY;
            };
            case ShieldItem ignored -> EquipmentSlotGroup.OFFHAND;
            case TieredItem ignored -> EquipmentSlotGroup.MAINHAND;
            default -> EquipmentSlotGroup.ANY;
        };
    }

    // ==================== 附魔操作 ====================

    public static void transferEnchantments(ItemStack result, ItemStack material) {
        ItemEnchantments matEnch = getEnchants(material);
        if (matEnch.isEmpty()) {
            return;
        }

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(getEnchants(result));
        boolean resonance = hasProperty(result, InlayProperty.RESONANCE);

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : matEnch.entrySet()) {
            Holder<Enchantment> ench = entry.getKey();
            int level = entry.getIntValue();
            int existing = mutable.getLevel(ench);
            int newLevel = calculateNewLevel(ench, level, existing, resonance);
            mutable.set(ench, newLevel);
        }

        result.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    private static int calculateNewLevel(Holder<Enchantment> ench, int level, int existing, boolean resonance) {
        int newLevel;
        if (existing > 0) {
            newLevel = existing == level
                    ? Math.min(existing + 1, ench.value().getMaxLevel())
                    : Math.max(existing, level);
        } else {
            newLevel = level;
        }
        if (resonance && Math.random() < 0.5) {
            newLevel = Math.min(newLevel + 1, ench.value().getMaxLevel());
        }
        return newLevel;
    }

    public static ItemStack extractFirstEnchantment(ItemStack base, ItemStack oldStack) {
        ItemEnchantments ench = getEnchants(base);
        if (ench.isEmpty() || hasProperty(base, InlayProperty.RESONANCE) && Math.random() < 0.5) {
            return oldStack.is(Items.ENCHANTED_BOOK) ? new ItemStack(Items.BOOK) : oldStack;
        }

        Object2IntMap.Entry<Holder<Enchantment>> first = ench.entrySet().iterator().next();
        Holder<Enchantment> enchHolder = first.getKey();
        int level = first.getIntValue();

        removeEnchantmentFromStack(base, enchHolder);
        return toEnchantedBook(oldStack, enchHolder, level);
    }

    private static void removeEnchantmentFromStack(ItemStack stack, Holder<Enchantment> ench) {
        ItemEnchantments current = getEnchants(stack);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(current);
        mutable.removeIf(holder -> holder == ench);
        stack.set(getEnchantmentComponent(stack), mutable.toImmutable());
    }

    private static ItemStack toEnchantedBook(ItemStack oldStack, Holder<Enchantment> ench, int level) {
        // 如果旧物品是普通书，转换为附魔书
        ItemStack book = oldStack.is(Items.BOOK) ? new ItemStack(Items.ENCHANTED_BOOK) : oldStack;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(getEnchants(book));
        mutable.set(ench, level);
        book.set(getEnchantmentComponent(book), mutable.toImmutable());
        return book;
    }

    public static ItemEnchantments getEnchants(ItemStack stack) {
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        if (stored != null && !stored.isEmpty()) {
            return stored;
        }
        return stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
    }

    private static DataComponentType<ItemEnchantments> getEnchantmentComponent(ItemStack stack) {
        return stack.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
    }
}