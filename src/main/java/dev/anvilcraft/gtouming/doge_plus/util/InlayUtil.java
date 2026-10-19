package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.data.InlayExtra;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

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

    // ==================== 潜影嵌套计数 ====================

    /** 该物品自身记录的潜影嵌套计数（取其全部镶孔条目里的最大值；无则全零）。 */
    public static InlayExtra.Nesting nestingOf(ItemStack stack) {
        InlayExtra.Nesting best = InlayExtra.Nesting.EMPTY;
        for (InlayEntry entry : getInlays(stack)) {
            if (entry.isEmpty()) continue;
            InlayExtra.Nesting nesting = entry.nesting();
            if (nesting.shulkers() + nesting.voidMatter() > best.shulkers() + best.voidMatter()) {
                best = nesting;
            }
        }
        return best;
    }

    /**
     * 镶嵌台在基材上镶入 {@code inlay} 后，新条目应记录的潜影嵌套计数：
     * 基材已记录的 + 被镶物品自身记录的 + 这次镶入的（潜影盒 +1 / 虚空物质块 +1）。
     *
     * <p>不区分先后、也不看最后一次镶的是哪个：逐层把盒镶进盒、或往同一个盒里连镶，计数都照样累加，
     * 于是「已镶 3 个潜影盒 + 1 个虚空物质块」这一状态与镶入顺序无关。</p>
     */
    public static InlayExtra.Nesting nesting(ItemStack inlay, ItemStack base) {
        InlayExtra.Nesting fromBase = nestingOf(base);
        InlayExtra.Nesting fromInlay = nestingOf(inlay);
        int shulkers = fromBase.shulkers() + fromInlay.shulkers() + (inlay.is(Items.SHULKER_BOX) ? 1 : 0);
        int voidMatter = fromBase.voidMatter() + fromInlay.voidMatter()
                + (inlay.is(ModBlocks.VOID_MATTER_BLOCK.asItem()) ? 1 : 0);
        return new InlayExtra.Nesting(shulkers, voidMatter);
    }

    // ==================== 镶嵌操作 ====================

    public static ItemStack withAddedInlay(ItemStack base, InlayEntry inlay) {
        List<InlayEntry> inlays = new ArrayList<>(getInlays(base));
        if (inlays.contains(InlayEntry.empty()))
            inlays.set(inlays.indexOf(InlayEntry.empty()), inlay);
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
        inlays.set(slot, InlayEntry.empty());
        return withInlays(base, inlays);
    }

    /**
     * 获取指定槽位的镶嵌
     */
    public static InlayEntry getInlayAt(ItemStack stack, int slot) {
        List<InlayEntry> inlays = getInlays(stack);
        if (slot < 0 || slot >= inlays.size()) return InlayEntry.empty();
        return inlays.get(slot);
    }

    private static ItemStack withInlays(ItemStack base, List<InlayEntry> inlays) {
        ItemStack result = base.copy();
        result.setCount(1);
        // 仅当不再含有效镶嵌（全部为空占位）时移除组件
        boolean hasValid = false;
        for (InlayEntry entry : inlays) {
            if (!entry.isEmpty()) {
                hasValid = true;
                break;
            }
        }
        if (!hasValid) {
            result.remove(ModDataComponentTypes.INLAY);
        } else {
            result.set(ModDataComponentTypes.INLAY, inlays);
        }
        return result;
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