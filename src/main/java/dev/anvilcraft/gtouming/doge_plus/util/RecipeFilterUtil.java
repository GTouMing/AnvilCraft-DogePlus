package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayOutput;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.FilterItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 镶嵌台 / 镶合台「配方过滤」的推导与槽位接受判定。
 *
 * <p>方块实体只存一个副手设定的过滤物品（{@code recipeFilter}），这里把该物品解释成配方约束：</p>
 * <ul>
 *   <li><b>镶嵌台</b>：过滤物品是基材（可带已有镶嵌）。基材槽要求同一基材定义；材料槽在基材已带镶嵌时
 *       只接受现有镶嵌物品，否则接受该基材全部配方中的材料。</li>
 *   <li><b>镶合台</b>：过滤物品是目标产物。基材槽只接受能产出该物品的镶合配方所需的完整基材。</li>
 *   <li>若过滤物品是 {@code anvilcraft:filter}，两个槽都直接交给 {@link FilterItem#filter} 按过滤器内容判定。</li>
 * </ul>
 */
public final class RecipeFilterUtil {

    private RecipeFilterUtil() {
    }

    /** 是否为 AnvilCraft 的过滤器物品。 */
    public static boolean isFilterItem(ItemStack stack) {
        return stack.is(ModItems.FILTER);
    }

    // ==================== 运行时接受判定 ====================

    /** 镶嵌台某槽位是否接受该物品。{@code filter} 为空表示不过滤。 */
    public static boolean acceptsInlayTable(int slot, ItemStack filter, ItemStack stack, @Nullable Level level) {
        if (filter.isEmpty()) return true;
        if (stack.isEmpty()) return false;

        if (slot == InlayTableBlockEntity.SLOT_BASE) {
            // 基材槽始终要求有镶孔（过滤器物品也不能把普通物品放进基材槽）。
            if (!MaterialManager.hasSocket(stack)) return false;
            return isFilterItem(filter)
                    ? FilterItem.filter(filter, stack)
                    : MaterialManager.hasSameBase(filter, stack);
        }
        if (slot == InlayTableBlockEntity.SLOT_MATERIAL) {
            return isFilterItem(filter)
                    ? FilterItem.filter(filter, stack)
                    : acceptsInlayMaterial(filter, stack, level);
        }
        return false;
    }

    /** 镶合台槽位是否接受该物品。{@code filter} 为空表示不过滤。 */
    public static boolean acceptsCraftingTable(ItemStack filter, ItemStack stack, @Nullable Level level) {
        if (filter.isEmpty()) return true;
        if (stack.isEmpty()) return false;
        if (isFilterItem(filter)) return FilterItem.filter(filter, stack);
        if (level == null || level.isClientSide) return true;
        return matchesRecipeProducing(filter, stack, level);
    }

    private static boolean acceptsInlayMaterial(ItemStack filterBase, ItemStack stack, @Nullable Level level) {
        // 副手是已镶基材：只接受基材上已有的镶嵌物品。
        boolean hasInlay = false;
        for (InlayEntry entry : InlayUtil.getInlays(filterBase)) {
            if (entry.isEmpty()) continue;
            hasInlay = true;
            if (ItemStack.isSameItem(entry.toItemStack(), stack)) return true;
        }
        if (hasInlay) return false;

        // 客户端拿不到完整配方表，放行以避免误拒；服务端权威。
        if (level == null || level.isClientSide) return true;

        boolean resolved = false;
        for (RecipeHolder<InlayRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get())) {
            InlayRecipe recipe = holder.value();
            MaterialManager.BaseMaterial baseMaterial = recipe.getBaseMaterial();
            if (baseMaterial == null || !baseMaterial.ingredient().test(filterBase)) continue;
            resolved = true;
            MaterialManager.InlayMaterial inlayMaterial = recipe.getInlayMaterial();
            if (inlayMaterial != null && inlayMaterial.ingredient().test(stack)) return true;
        }
        // 静默镶嵌：镶合配方里声明的材料同样可以镶进该基材。
        for (RecipeHolder<InlayCraftingRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get())) {
            InlayCraftingRecipe recipe = holder.value();
            if (!filterBase.is(recipe.getBaseItem())) continue;
            resolved = true;
            for (InlayMatcher matcher : recipe.inlays()) {
                if (matcher.test(stack)) return true;
            }
        }
        // 一条配方都解析不到时不锁死：放行（设定时已做合法性校验）。
        return !resolved;
    }

    private static boolean matchesRecipeProducing(ItemStack output, ItemStack stack, Level level) {
        for (RecipeHolder<InlayCraftingRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get())) {
            InlayCraftingRecipe recipe = holder.value();
            if (produces(recipe, output) && recipe.matches(stack)) return true;
        }
        return false;
    }

    private static boolean produces(InlayCraftingRecipe recipe, ItemStack output) {
        for (InlayOutput result : recipe.results()) {
            if (result instanceof InlayOutput.Fixed fixed && fixed.item() == output.getItem()) {
                return true;
            }
        }
        return false;
    }

    // ==================== 设定时的合法性 ====================

    /** 镶嵌台过滤是否可以设定：空（清除）/ 过滤器物品恒可；普通物品须是能解析出配方的基材。 */
    public static boolean canSetInlayTableFilter(ItemStack filter, Level level) {
        if (filter.isEmpty() || isFilterItem(filter)) return true;
        if (!MaterialManager.hasSocket(filter)) return false;
        // 已镶基材：直接可以约束材料槽
        for (InlayEntry entry : InlayUtil.getInlays(filter)) {
            if (!entry.isEmpty()) return true;
        }
        for (RecipeHolder<InlayRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get())) {
            MaterialManager.BaseMaterial base = holder.value().getBaseMaterial();
            if (base != null && base.ingredient().test(filter)) return true;
        }
        for (RecipeHolder<InlayCraftingRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get())) {
            if (filter.is(holder.value().getBaseItem())) return true;
        }
        return false;
    }

    /** 镶合台过滤是否可以设定：空（清除）/ 过滤器物品恒可；普通物品须是某条镶合配方的目标产物。 */
    public static boolean canSetCraftingTableFilter(ItemStack filter, Level level) {
        if (filter.isEmpty() || isFilterItem(filter)) return true;
        for (RecipeHolder<InlayCraftingRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get())) {
            if (produces(holder.value(), filter)) return true;
        }
        return false;
    }
}
