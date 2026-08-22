package dev.anvilcraft.gtouming.doge_plus.recipe.inlay;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
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
public class InlayUtil {

    /** 读取物品已镶嵌的材料 ID 列表（长度 = 已占用的镶孔数）。 */
    public static ArrayList<ResourceLocation> getInlays(ItemStack stack) {
        return new ArrayList<>(stack.getOrDefault(ModDataComponentTypes.INLAY, new ArrayList<>()));
    }

    public static void setInlays(ItemStack stack, ArrayList<ResourceLocation> list) {
        stack.set(ModDataComponentTypes.INLAY, list);
    }

    /** 已镶嵌次数。 */
    public static int getInlayCount(ItemStack stack) {
        return getInlays(stack).size();
    }

    /** 最旧的镶嵌材料 ID（满镶替换时弹出），无镶嵌返回 null。 */
    @Nullable
    public static ResourceLocation getFirstInlay(ItemStack stack) {
        List<ResourceLocation> inlays = getInlays(stack);
        return inlays.isEmpty() ? null : inlays.getFirst();
    }

    @Nullable
    public static MaterialManager.InlayMaterial getMaterial(ResourceLocation id) {
        return MaterialManager.getInlay(BuiltInRegistries.ITEM.get(id).getDefaultInstance());
    }

    /** 物品是否携带指定性质（任一镶嵌材料带该性质即视为携带）。 */
    public static boolean hasProperty(ItemStack stack, InlayProperty property) {
        return countProperty(stack, property) > 0;
    }

    /** 携带指定性质的镶嵌材料数量。 */
    public static int countProperty(ItemStack stack, InlayProperty property) {
        int count = 0;
        for (ResourceLocation id : getInlays(stack)) {
            MaterialManager.InlayMaterial material = getMaterial(id);
            if (material != null && material.has(property)) count++;
        }
        return count;
    }

    /** 在基材副本上追加一个镶嵌（填入一个空镶孔）。 */
    public static ItemStack withAddedInlay(ItemStack base, ResourceLocation materialId) {
        List<ResourceLocation> inlays = new ArrayList<>(getInlays(base));
        inlays.add(materialId);
        return withInlays(base, inlays);
    }

    /** 在基材副本上替换最旧镶嵌（满镶时替换）。 */
    public static ItemStack withReplacedOldestInlay(ItemStack base, ResourceLocation materialId) {
        List<ResourceLocation> inlays = new ArrayList<>(getInlays(base));
        if (!inlays.isEmpty()) inlays.removeFirst();
        inlays.add(materialId);
        return withInlays(base, inlays);
    }

    /**
     * 从基材中移除最旧的镶嵌物（FIFO）
     *
     * @param base 基材物品
     * @return 移除最旧镶嵌物后的基材，如果无法移除则返回 null
     */
    public static ItemStack withRemovedOldestInlay(ItemStack base) {
        List<ResourceLocation> inlays = new ArrayList<>(getInlays(base));
        if (!inlays.isEmpty()) inlays.removeFirst();
        return withInlays(base, inlays);
    }

    private static ItemStack withInlays(ItemStack base, List<ResourceLocation> inlays) {
        ItemStack result = base.copy();
        result.setCount(1);
        if (inlays.isEmpty()) {
            result.remove(ModDataComponentTypes.INLAY);
        } else {
            result.set(ModDataComponentTypes.INLAY, inlays);
        }
        return reapplyAttributeModifiers(result);
    }

    public static ItemAttributeModifiers getCurrentModifiers(ItemStack stack) {
        Item item = stack.getItem();

        // 优先从组件获取
        ItemAttributeModifiers fromComponent = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (fromComponent != null && fromComponent != ItemAttributeModifiers.EMPTY) {
            return fromComponent;
        }

        // 如果组件为空，从 Item 获取默认修饰器
        if (item instanceof ArmorItem armorItem) {
            return armorItem.getDefaultAttributeModifiers();
        }

        return ItemAttributeModifiers.EMPTY;
    }

    /**
     * 按已镶嵌材料的防御/生命/攻击性质，重算物品的属性修饰组件（保留物品默认修饰，数量可叠加）。
     * 供镶嵌台产出与镶嵌方块破坏后重建物品时调用。
     */
    public static ItemStack reapplyAttributeModifiers(ItemStack stack) {
        int defense = countProperty(stack, InlayProperty.DEFENSE);
        int life = countProperty(stack, InlayProperty.LIFE);
        int attack = countProperty(stack, InlayProperty.ATTACK);
        if (defense == 0 && life == 0 && attack == 0) return stack;

        // ⭐ 获取当前所有修饰器（包括默认的）
        ItemAttributeModifiers modifiers = getCurrentModifiers(stack);

        EquipmentSlotGroup slotGroup = getSlotGroupForItem(stack);

        if (defense > 0) {
            modifiers = modifiers.withModifierAdded(
                    Attributes.ARMOR,
                    new AttributeModifier(AnvilCraftDogePlus.of("inlay_defense"), 2.0 * defense, AttributeModifier.Operation.ADD_VALUE),
                    slotGroup
            );
        }
        if (life > 0) {
            modifiers = modifiers.withModifierAdded(
                    Attributes.MAX_HEALTH,
                    new AttributeModifier(AnvilCraftDogePlus.of("inlay_life"), 2.0 * life, AttributeModifier.Operation.ADD_VALUE),
                    slotGroup
            );
        }
        if (attack > 0) {
            modifiers = modifiers.withModifierAdded(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(AnvilCraftDogePlus.of("inlay_attack"), 2.0 * attack, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, modifiers);
        return stack;
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

    /**
     * 把嵌材附魔转移到基材（同等级附魔合并 +1，上限为附魔最大等级）。
     */
    public static void transferEnchantments(ItemStack result, ItemStack material) {
        ItemEnchantments matEnch = getEnchants(material);
        if (matEnch.isEmpty()) return;

        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(getEnchants(result));
        for (Object2IntMap.Entry<Holder<Enchantment>> e : matEnch.entrySet()) {
            Holder<Enchantment> ench = e.getKey();
            int level = e.getIntValue();
            int existing = mutable.getLevel(ench);
            int newLevel;
            if (existing > 0) {
                newLevel = existing == level
                        ? Math.min(existing + 1, ench.value().getMaxLevel())
                        : Math.max(existing, level);
            } else {
                newLevel = level;
            }
            mutable.set(ench, newLevel);
        }
        result.set(DataComponents.ENCHANTMENTS, mutable.toImmutable());
    }

    /** 提取基材的第一个附魔到旧材料（附魔书），并从基材移除该附魔。 */
    public static ItemStack extractFirstEnchantment(ItemStack base, ItemStack oldStack) {
        ItemEnchantments ench = getEnchants(base);
        if (ench.isEmpty()) return oldStack;

        Object2IntMap.Entry<Holder<Enchantment>> first = ench.entrySet().iterator().next();
        // 从基材移除第一个附魔
        ItemEnchantments.Mutable baseMutable = new ItemEnchantments.Mutable(ench);
        baseMutable.removeIf(holder -> holder == first.getKey());
        base.set(base.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, baseMutable.toImmutable());

        // 应用到旧材料（附魔书）
        ItemStack book = oldStack;
        if (book.isEmpty()) book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable bookMutable = new ItemEnchantments.Mutable(getEnchants(oldStack));
        bookMutable.set(first.getKey(), first.getIntValue());
        book.set(book.is(Items.ENCHANTED_BOOK) ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, bookMutable.toImmutable());
        return book;
    }

    public static ItemEnchantments getEnchants(ItemStack stack) {
        return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY));
    }
}
