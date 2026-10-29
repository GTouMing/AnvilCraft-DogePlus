package dev.anvilcraft.gtouming.doge_plus.transfer;

import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.FilterItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 「合成存入」的合成部分。
 *
 * <p>从该「存入」面可达的<b>多个取出源容器</b>里按材料池匹配原版合成配方（3×3，只看材料、不看是否有序），
 * 把过滤物品当成目标产物：多个源合计有足够材料时合成并插入目标容器；否则返回 0，由调用方回退普通存入。</p>
 *
 * <p>材料分配按「近源优先」：源列表本身是反向 BFS 的先近后远顺序，贪心分配会优先用靠前的源，
 * 凑不齐再向后面的源借材料。容器物品（空桶等）通过 {@link CraftingRecipe#getRemainingItems} 取回，
 * <b>还回提供该材料的那个源容器</b>。只在服务端调用。</p>
 */
final class CraftInsertHelper {

    /** 单轮最多尝试的次数上限（吞吐量本身已 ≤ 64，这里只防呆）。 */
    private static final int MAX_CRAFTS = 64;

    /** 一个可选材料来源：容器处理器 + 容器位置（掉落兜底用）。 */
    record SourceRef(IItemHandler handler, BlockPos pos) {
    }

    private CraftInsertHelper() {
    }

    /** 构建「产物物品 → 候选合成配方」索引；跳过特殊配方与空产物。 */
    static Map<Item, List<RecipeHolder<CraftingRecipe>>> buildIndex(Level level) {
        Map<Item, List<RecipeHolder<CraftingRecipe>>> index = new HashMap<>();
        for (RecipeHolder<CraftingRecipe> holder : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            CraftingRecipe recipe = holder.value();
            if (recipe.isSpecial()) continue;
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            index.computeIfAbsent(result.getItem(), key -> new ArrayList<>()).add(holder);
        }
        return index;
    }

    /**
     * 尝试用多个取出源的材料合成过滤物品并插入目标。
     *
     * @param throughput 该面的物流量：本轮产物物品数不得超过它
     * @param chain      沿途「存入」面的过滤（产物也要逐级通过）
     * @return 实际插入目标的产物物品数；0 表示没能合成
     */
    static int craft(
            Level level,
            List<SourceRef> sources,
            IItemHandler target,
            ItemStack filter,
            int throughput,
            List<ItemStack> chain,
            Map<Item, List<RecipeHolder<CraftingRecipe>>> index) {
        if (level.isClientSide || filter.isEmpty() || filter.is(ModItems.FILTER) || sources.isEmpty()) return 0;
        List<RecipeHolder<CraftingRecipe>> candidates = index.get(filter.getItem());
        if (candidates == null) return 0;

        for (RecipeHolder<CraftingRecipe> holder : candidates) {
            CraftingRecipe recipe = holder.value();
            ItemStack result = recipe.getResultItem(level.registryAccess());
            if (result.isEmpty()) continue;
            if (!ItemStack.isSameItemSameComponents(result, filter)) continue;
            if (!passesChain(result, chain)) continue;

            int resultCount = result.getCount();
            if (resultCount <= 0) continue;
            // 产物数 ≤ 物流量：整次合成都要放得下这么多产物才做。
            int maxCrafts = throughput / resultCount;
            if (maxCrafts <= 0) continue;

            List<Ingredient> ingredients = nonEmptyIngredients(recipe);
            if (ingredients.isEmpty()) continue;

            int available = availableCrafts(sources, ingredients, maxCrafts);
            if (available <= 0) continue;

            int crafted = cook(level, sources, target, recipe, ingredients, Math.min(maxCrafts, available));
            if (crafted > 0) return crafted;
        }
        return 0;
    }

    private static boolean passesChain(ItemStack product, List<ItemStack> chain) {
        for (ItemStack filter : chain) {
            if (!filter.isEmpty() && !FilterItem.filter(filter, product)) return false;
        }
        return true;
    }

    private static List<Ingredient> nonEmptyIngredients(CraftingRecipe recipe) {
        List<Ingredient> result = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (!ingredient.isEmpty()) result.add(ingredient);
        }
        return result;
    }

    /** 每个源各槽位的当前数量快照。 */
    private static int[][] snapshot(List<SourceRef> sources) {
        int[][] counts = new int[sources.size()][];
        for (int s = 0; s < sources.size(); s++) {
            IItemHandler handler = sources.get(s).handler();
            counts[s] = new int[handler.getSlots()];
            for (int slot = 0; slot < counts[s].length; slot++) {
                counts[s][slot] = handler.getStackInSlot(slot).getCount();
            }
        }
        return counts;
    }

    /** 在全局材料池里找一个匹配该材料的槽位并扣 1；返回是否成功。近源优先。 */
    private static boolean consumeOne(List<SourceRef> sources, int[][] remaining, Ingredient ingredient) {
        for (int s = 0; s < sources.size(); s++) {
            IItemHandler handler = sources.get(s).handler();
            for (int slot = 0; slot < remaining[s].length; slot++) {
                if (remaining[s][slot] <= 0) continue;
                if (!ingredient.test(handler.getStackInSlot(slot))) continue;
                remaining[s][slot]--;
                return true;
            }
        }
        return false;
    }

    /** 按跨源材料多重集计算最多能合成几次（不超过 {@code cap}）。 */
    private static int availableCrafts(List<SourceRef> sources, List<Ingredient> ingredients, int cap) {
        int[][] remaining = snapshot(sources);
        int crafts = 0;
        while (crafts < cap) {
            boolean ok = true;
            for (Ingredient ingredient : ingredients) {
                if (!consumeOne(sources, remaining, ingredient)) {
                    ok = false;
                    break;
                }
            }
            if (!ok) break;
            crafts++;
        }
        return crafts;
    }

    /**
     * 逐次合成并插入，最多 {@code maxCrafts} 次。每次重新按当前多源状态分配材料，
     * 以便正确处理跨源借料、同一槽位被多个材料格共享、以及空桶回填后的状态。
     */
    private static int cook(
            Level level,
            List<SourceRef> sources,
            IItemHandler target,
            CraftingRecipe recipe,
            List<Ingredient> ingredients,
            int maxCrafts) {
        int inserted = 0;
        for (int n = 0; n < maxCrafts && n < MAX_CRAFTS; n++) {
            int width = 3;
            int height = 3;
            if (recipe instanceof ShapedRecipe shaped) {
                width = shaped.getWidth();
                height = shaped.getHeight();
            }
            int gridSize = width * height;

            // 每个材料格 -> (源下标, 槽位)，-1 表示空位。
            int[] sourceOf = new int[gridSize];
            int[] slotOf = new int[gridSize];
            Arrays.fill(sourceOf, -1);
            ItemStack[] grid = new ItemStack[gridSize];
            Arrays.fill(grid, ItemStack.EMPTY);
            int[][] remaining = snapshot(sources);

            boolean built = true;
            List<Ingredient> recipeIngredients = recipe.getIngredients();
            for (int i = 0; i < recipeIngredients.size() && i < gridSize; i++) {
                Ingredient ingredient = recipeIngredients.get(i);
                if (ingredient.isEmpty()) continue;
                boolean matched = false;
                for (int s = 0; s < sources.size() && !matched; s++) {
                    IItemHandler handler = sources.get(s).handler();
                    for (int slot = 0; slot < remaining[s].length; slot++) {
                        if (remaining[s][slot] <= 0) continue;
                        if (!ingredient.test(handler.getStackInSlot(slot))) continue;
                        remaining[s][slot]--;
                        sourceOf[i] = s;
                        slotOf[i] = slot;
                        grid[i] = handler.getStackInSlot(slot).copyWithCount(1);
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    built = false;
                    break;
                }
            }
            if (!built) break;

            CraftingInput input = CraftingInput.of(width, height, Arrays.asList(grid));
            ItemStack result = recipe.assemble(input, level.registryAccess()).copy();
            if (result.isEmpty()) break;

            // 目标收不下完整产物就不消耗材料。
            ItemStack simulated = ItemHandlerUtil.insertItem(target, result, true);
            if (simulated.getCount() >= result.getCount()) break;

            NonNullList<ItemStack> remainders = recipe.getRemainingItems(input);

            // 按 (源, 槽) 汇总需要扣的数量。
            int[][] need = new int[sources.size()][];
            for (int s = 0; s < sources.size(); s++) {
                need[s] = new int[sources.get(s).handler().getSlots()];
            }
            for (int i = 0; i < gridSize; i++) {
                if (sourceOf[i] >= 0) need[sourceOf[i]][slotOf[i]]++;
            }

            // 扣材料；任一源取不满就整体回滚。
            List<ItemStack> takenStacks = new ArrayList<>();
            List<Integer> takenSources = new ArrayList<>();
            boolean extracted = true;
            for (int s = 0; s < sources.size() && extracted; s++) {
                IItemHandler handler = sources.get(s).handler();
                for (int slot = 0; slot < need[s].length; slot++) {
                    if (need[s][slot] <= 0) continue;
                    ItemStack taken = handler.extractItem(slot, need[s][slot], false);
                    if (!taken.isEmpty()) {
                        takenStacks.add(taken);
                        takenSources.add(s);
                    }
                    if (taken.getCount() != need[s][slot]) {
                        for (int k = 0; k < takenStacks.size(); k++) {
                            SourceRef ref = sources.get(takenSources.get(k));
                            ItemStack left = ItemHandlerUtil.insertItem(ref.handler(), takenStacks.get(k), false);
                            if (!left.isEmpty()) drop(level, ref.pos(), left);
                        }
                        extracted = false;
                        break;
                    }
                }
            }
            if (!extracted) break;

            // 容器物品（空桶等）还回提供该材料的源。
            for (int i = 0; i < gridSize; i++) {
                ItemStack remainder = remainders.get(i);
                if (remainder.isEmpty()) continue;
                int s = Math.max(0, sourceOf[i]);
                SourceRef ref = sources.get(s);
                ItemStack left = ItemHandlerUtil.insertItem(ref.handler(), remainder.copy(), false);
                if (!left.isEmpty()) left = ItemHandlerUtil.insertItem(target, left, false);
                if (!left.isEmpty()) drop(level, ref.pos(), left);
            }

            // 插入产物；模拟/实际不一致的余量退回第一个源，最后才掉落。
            ItemStack leftover = ItemHandlerUtil.insertItem(target, result, false);
            inserted += result.getCount() - leftover.getCount();
            if (!leftover.isEmpty()) {
                SourceRef ref = sources.get(0);
                ItemStack toSource = ItemHandlerUtil.insertItem(ref.handler(), leftover, false);
                if (!toSource.isEmpty()) drop(level, ref.pos(), toSource);
                break;
            }
        }
        return inserted;
    }

    private static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }
}
