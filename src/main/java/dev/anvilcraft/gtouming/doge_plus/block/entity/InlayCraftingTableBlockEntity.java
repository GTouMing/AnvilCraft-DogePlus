package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.gtouming.doge_plus.util.RecipeFilterUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 镶合台方块实体：一个基材槽 + 按基材剩余空镶孔开放的材料槽（基材为空时为 0 槽，
 * 有目标产物过滤时再受配方缺失镶孔数限制，最多 9 个）。
 *
 * <p>铁砧砸击时先把材料槽里的静默镶嵌追加到基材，再按 {@link InlayCraftingRecipe} 匹配整件基材
 * （物品 + 全部镶孔）；命中则一次耗尽整叠：按份生成配方声明的全部产物（含返还的模具），
 * 均落在台体下方，供漏斗/溜槽等自动化收集；产物区域位于收集区之外，不会被自身 tick 吸回。</p>
 */
public class InlayCraftingTableBlockEntity extends AbstractProcessingTableBlockEntity {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_MATERIAL_START = 1;
    /** 可放入的静默镶嵌材料数（内置镶合配方最大 9：多用途工具的九个镶孔）。 */
    public static final int MATERIAL_SLOTS = 9;
    public static final int SLOT_COUNT = SLOT_MATERIAL_START + MATERIAL_SLOTS;

    public InlayCraftingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    /** 该槽位是否接受该物品（基材槽按产物过滤；材料槽按过滤决定的数量与对应 matcher）。 */
    @Override
    public boolean accepts(int slot, ItemStack stack) {
        if (slot == SLOT_BASE) {
            return RecipeFilterUtil.acceptsCraftingTable(recipeFilter, stack, level);
        }
        if (slot >= SLOT_MATERIAL_START && slot < SLOT_COUNT) {
            return RecipeFilterUtil.acceptsCraftingTableMaterial(
                    recipeFilter, slots[SLOT_BASE], slot - SLOT_MATERIAL_START, stack, level);
        }
        return false;
    }

    /**
     * 自动化插入：基材尚未到时允许先把材料暂存进材料槽，避免样板供应器按任意顺序
     * 模拟插入时因“基材优先”整批失败。玩家右键仍由 {@link #canPlace} 的基材优先门禁控制。
     */
    @Override
    protected boolean canInsert(int slot, ItemStack stack) {
        if (slot >= SLOT_MATERIAL_START && slot < SLOT_COUNT && slots[SLOT_BASE].isEmpty()) {
            return RecipeFilterUtil.acceptsCraftingTableMaterialForInsertion(
                    recipeFilter, ItemStack.EMPTY, slot - SLOT_MATERIAL_START, stack, level);
        }
        return super.canInsert(slot, stack);
    }

    /** 当前基材剩余空镶孔数；基材为空或已镶满时为 0。 */
    public int materialSlotCount() {
        return RecipeFilterUtil.craftingTableMaterialSlotCount(recipeFilter, slots[SLOT_BASE], level);
    }

    /**
     * 材料槽数量上限：每个材料槽最多放「基材数量」个，保证一个槽对应每个基材的一份材料。
     * 例如 16 个空心磁铁块时，每个材料槽最多 16 个铁锭，64 个铁锭会顺延到 4 个材料槽。
     */
    @Override
    protected int slotLimit(int slot) {
        if (slot >= SLOT_MATERIAL_START && slot < SLOT_COUNT) {
            if (slots[SLOT_BASE].isEmpty()) return 64;
            if (materialSlotCount() <= 0) return 0;
            return Math.min(64, Math.max(1, slots[SLOT_BASE].getCount()));
        }
        return super.slotLimit(slot);
    }

    /** 收集掉落物：按槽位顺序依次插入，直到装不下。 */
    @Override
    protected ItemStack collectInto(ItemStack stack, ItemEntity entity) {
        ItemStack remaining = stack;
        for (int slot = 0; slot < SLOT_COUNT && !remaining.isEmpty(); slot++) {
            if (!accepts(slot, remaining)) continue;
            remaining = getInput().insertItem(slot, remaining, false);
        }
        return remaining;
    }

    @Override
    public boolean canSetFilter(ItemStack stack, Level level) {
        return RecipeFilterUtil.canSetCraftingTableFilter(stack, level);
    }

    @Override
    public boolean onAnvilStrike(Level level, BlockPos pos, float fallDistance) {
        return processCrafting();
    }

    // ==================== 核心：镶合处理 ====================

    /**
     * 铁砧砸击处理：先把材料槽里的静默镶嵌全部追加到基材，再按 {@link InlayCraftingRecipe}
     * 匹配整件基材；命中后一次耗尽整叠，按份数在台体下方生成全部产物。
     *
     * <p>每份基材独立判定一次概率产物并展开多产物，再把整叠结果按物品合并、按堆叠上限
     * 掉落——这样概率产物按份独立出现，多产物 / 多数量都准确生成。</p>
     *
     * @return 是否完成了一次批量合成
     */
    public boolean processCrafting() {
        if (level == null || level.isClientSide) return false;

        ItemStack base = slots[SLOT_BASE];
        if (base.isEmpty()) return false;

        // 有材料时一次砸击产出一份；每个槽先各取 1 个，未匹配时再用整叠补齐同类材料的重复镶孔。
        // 无材料时（基材已镶满）沿用整叠镶合。
        int activeMaterialSlots = materialSlotCount();
        int materialEnd = SLOT_MATERIAL_START + activeMaterialSlots;
        boolean hasMaterials = false;
        for (int slot = SLOT_MATERIAL_START; slot < materialEnd; slot++) {
            if (!slots[slot].isEmpty()) {
                hasMaterials = true;
                break;
            }
        }

        ItemStack work = base.copyWithCount(1);
        int crafts;
        int[] used = new int[SLOT_COUNT];
        InlayCraftingRecipe recipe;
        if (hasMaterials) {
            int applied = 0;
            // 第一轮：每个有材料的槽各取 1 个，保证不同材料都能进入镶孔。
            // 不能直接按整叠铺满，否则第一个槽的整叠（创造模式常见 64 个）会占满所有镶孔。
            for (int slot = SLOT_MATERIAL_START; slot < materialEnd; slot++) {
                ItemStack material = slots[slot];
                if (material.isEmpty()) continue;
                ItemStack next = InlayUtil.applySilentInlay(work, material);
                if (next.isEmpty()) break; // 没有空镶孔
                work = next;
                used[slot]++;
                applied++;
            }
            if (applied == 0) return false;

            // 自动化（AE 样板供应器）常把一个材料堆叠集中在一个槽里：按
            // 「材料总数 / 基材数」补足每个基材应镶的份数，避免 16 基材 + 64 铁锭只命中 1 铁锭配方。
            int baseCount = Math.max(1, base.getCount());
            int nonEmptySlots = 0;
            int totalMaterials = 0;
            for (int slot = SLOT_MATERIAL_START; slot < materialEnd; slot++) {
                if (slots[slot].isEmpty()) continue;
                nonEmptySlots++;
                totalMaterials += slots[slot].getCount();
            }
            int target = Math.min(activeMaterialSlots,
                    Math.max(nonEmptySlots, totalMaterials / baseCount));

            boolean progress = true;
            while (applied < target && progress) {
                progress = false;
                for (int slot = SLOT_MATERIAL_START; slot < materialEnd && applied < target; slot++) {
                    ItemStack material = slots[slot];
                    if (used[slot] >= material.getCount()) continue;
                    ItemStack next = InlayUtil.applySilentInlay(work, material);
                    if (next.isEmpty()) continue;
                    work = next;
                    used[slot]++;
                    applied++;
                    progress = true;
                }
            }

            recipe = findRecipe(work);
            // 仍没匹配时按整叠补齐，保留「一个槽的整叠对应多个同种镶孔」的用法。
            if (recipe == null) {
                for (int slot = SLOT_MATERIAL_START; slot < materialEnd; slot++) {
                    ItemStack material = slots[slot];
                    for (int n = used[slot]; n < material.getCount(); n++) {
                        ItemStack next = InlayUtil.applySilentInlay(work, material);
                        if (next.isEmpty()) break;
                        work = next;
                        used[slot]++;
                        applied++;
                    }
                }
                recipe = findRecipe(work);
            }
        } else {
            recipe = findRecipe(work);
        }

        if (recipe == null) return false;

        // 合成次数 = 基材与各材料槽能支持的最小值；每次消耗 used[slot] 个材料。
        // 基材/材料叠上限都是 64，因此单次砸击最多合成一叠。
        crafts = base.getCount();
        if (hasMaterials) {
            for (int slot = SLOT_MATERIAL_START; slot < materialEnd; slot++) {
                if (used[slot] <= 0) continue;
                crafts = Math.min(crafts, slots[slot].getCount() / used[slot]);
            }
        }
        if (crafts <= 0) return false;

        List<ItemStack> totals = new ArrayList<>();
        for (int i = 0; i < crafts; i++) {
            for (ItemStack result : recipe.resultsFor(work, level.random, level.registryAccess())) {
                accumulate(totals, result);
            }
        }
        if (totals.isEmpty()) return false;

        // 全部校验通过后才消耗，避免失败时吞材料。used[slot] 是每次合成的消耗量。
        slots[SLOT_BASE] = base.copyWithCount(base.getCount() - crafts);
        for (int slot = SLOT_MATERIAL_START; slot < materialEnd; slot++) {
            if (used[slot] <= 0) continue;
            slots[slot] = slots[slot].copyWithCount(slots[slot].getCount() - used[slot] * crafts);
        }

        for (ItemStack total : totals) {
            dropItem(total, total.getCount());
        }

        syncToClient();
        playEffects(level);
        return true;
    }

    /** 把一份产物并入累加器：相同物品与数据组件则叠加数量，否则新增一项。 */
    private static void accumulate(List<ItemStack> totals, ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() <= 0) return;
        for (ItemStack existing : totals) {
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                existing.grow(stack.getCount());
                return;
            }
        }
        totals.add(stack.copy());
    }

    @Nullable
    private InlayCraftingRecipe findRecipe(ItemStack base) {
        if (level == null) return null;
        List<RecipeHolder<InlayCraftingRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get());

        // 目标产物过滤：必须在产出该产物的配方里选，否则「同一基材+同一材料、不同产物」
        // （如九种下界合金升级）会总是命中第一条。
        if (!recipeFilter.isEmpty() && !RecipeFilterUtil.isFilterItem(recipeFilter)) {
            for (RecipeHolder<InlayCraftingRecipe> holder : recipes) {
                InlayCraftingRecipe recipe = holder.value();
                if (!RecipeFilterUtil.produces(recipe, recipeFilter)) continue;
                if (recipe.getBaseItem() != base.getItem()) continue;
                if (recipe.matches(base)) return recipe;
            }
            return null;
        }

        for (RecipeHolder<InlayCraftingRecipe> holder : recipes) {
            InlayCraftingRecipe recipe = holder.value();
            if (recipe.matches(base)) return recipe;
        }
        return null;
    }
}
