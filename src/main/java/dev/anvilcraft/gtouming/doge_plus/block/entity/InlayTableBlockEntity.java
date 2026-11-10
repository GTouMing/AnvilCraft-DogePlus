package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.anvilcraft.gtouming.doge_plus.util.RecipeFilterUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager.hasSocket;

/**
 * 镶嵌台方块实体：2 个槽位（镶嵌物/被镶嵌物），只处理**显式镶嵌配方**；
 * 静默镶嵌由镶合台负责。
 *
 * <p>铁砧砸击时执行镶嵌：查找匹配的 {@link InlayRecipe}，消耗 1 个材料 + 1 个基材，
 * 产出镶嵌后的物品。基材镶孔数（数据驱动）决定可镶嵌次数：
 * 未满时追加镶嵌，满镶时替换指定槽位。产物与被替换的旧材料均以掉落物形式生成。</p>
 */
public class InlayTableBlockEntity extends AbstractProcessingTableBlockEntity {

    // ==================== 槽位常量 ====================

    public static final int SLOT_BASE = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_COUNT = 2;

    public InlayTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SLOT_COUNT);
    }

    /** 该槽位是否接受该物品（受配方过滤约束）。 */
    @Override
    public boolean accepts(int slot, ItemStack stack) {
        return RecipeFilterUtil.acceptsInlayTable(slot, recipeFilter, stack, level);
    }

    /** 基材槽额外要求物品带镶孔；材料槽沿用 {@link #accepts}。 */
    @Override
    protected boolean canInsert(int slot, ItemStack stack) {
        return (slot != SLOT_BASE || hasSocket(stack)) && super.canInsert(slot, stack);
    }

    /** 收集掉落物：按槽位顺序插入第一个接受的槽。 */
    @Override
    protected ItemStack collectInto(ItemStack stack, ItemEntity entity) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!accepts(slot, stack)) continue;
            ItemStack remaining = getInput().insertItem(slot, stack, false);
            if (remaining.getCount() == stack.getCount()) continue;
            return remaining;
        }
        return stack;
    }

    @Override
    public boolean canSetFilter(ItemStack stack, Level level) {
        return RecipeFilterUtil.canSetInlayTableFilter(stack, level);
    }

    @Override
    public boolean onAnvilStrike(Level level, BlockPos pos, float fallDistance) {
        return processInlay(level, fallDistance);
    }

    // ==================== 核心：镶嵌处理 ====================

    /**
     * 铁砧砸击处理：批量执行镶嵌。
     *
     * <p>基材的镶孔数（数据驱动，见 {@link MaterialManager}）决定可镶嵌次数：
     * 未满时追加镶嵌，满镶时替换指定槽位并将旧材料掉落。
     * 每次消耗 1 个材料 + 1 个基材，产物与旧材料在本次砸击结束后统一生成在台下方。</p>
     *
     * <p>当材料槽为空时：取下已镶嵌的材料，取下的镶嵌物与取下后的基材均以掉落物生成。
     * 砸击高度定位到的槽位若是空占位（已取走过），直接中止本次操作，不顺延到其它槽。</p>
     *
     * @return 是否至少完成了一次镶嵌或取下操作
     */
    public boolean processInlay(Level level, float fallDistance) {
        if (level.isClientSide) return false;

        ItemStack material = slots[SLOT_MATERIAL];
        ItemStack base = slots[SLOT_BASE];

        int processed;
        if (base.isEmpty()) return false;
        if (!material.isEmpty()) {
            // 材料槽有材料 → 执行镶嵌
            processed = processAddInlay(material, base, fallDistance);
        } else {
            // 材料槽为空 → 取下已镶嵌材料
            processed = processRemoveInlay(base, fallDistance);
        }

        if (processed > 0) {
            syncToClient();
            playEffects(level);
        }
        return processed > 0;
    }

    private int processAddInlay(ItemStack inlay, ItemStack base, float fallDistance) {
        // 镶嵌台只处理显式镶嵌配方；静默镶嵌由镶合台负责。
        InlayRecipe recipe = findRecipe(inlay, base);
        if (recipe == null) return 0;
        MaterialManager.InlayMaterial inlayMaterial = recipe.getInlayMaterial();
        if (inlayMaterial == null) return 0;

        // 一次砸击消耗 min(材料,基材) 份，多出的材料/基材保留在槽位中；产物按份数生成。
        int count = Math.min(inlay.getCount(), base.getCount());

        // 潜影嵌套计数：镶嵌台看得到基材，在这里累加好（其它材料算出来是全零，不记）
        var nesting = InlayUtil.nesting(inlay, base);
        InlayEntry entry = InlayEntry.fromItemStack(inlay, inlayMaterial, nesting);
        int sockets = MaterialManager.getSocketCount(base);

        // 镶嵌操作在基材副本上执行：补空占位、抽取附魔等都不应污染未被消耗的剩余基材。
        ItemStack workBase = base.copyWithCount(1);

        // 槽位列表：取出过的槽位为空占位，列表长度即物理槽位数
        List<InlayEntry> existing = InlayUtil.getInlays(workBase);
        if (existing.isEmpty() && sockets > 0) {
            // 基材尚无镶嵌组件（全新基材，或全部镶孔被取出后组件被移除）：
            // 先按镶孔数补满 empty() 占位再执行镶嵌，保证槽位下标稳定，
            // 也避免空镶孔在工具提示中消失。
            existing = new ArrayList<>(sockets);
            for (int i = 0; i < sockets; i++) existing.add(InlayEntry.empty());
            InlayUtil.setInlays(workBase, new ArrayList<>(existing));
        }
        boolean full = !hasEmptySlot(existing) && existing.size() >= sockets;

        // 满镶时准备旧材料（整叠基材同一槽位换下同一种旧材料）
        ItemStack oldStack = ItemStack.EMPTY;
        ItemStack result;
        if (full) {
            // 仅满镶时按下落高度定位替换槽位；未满（含全新基材，列表为空）不进入此处，
            // 避免 existing.size() - 1 为负而误中止追加镶嵌
            int slotToReplace = Math.min((int) Math.floor(fallDistance), existing.size() - 1);
            if (slotToReplace < 0) return 0;

            InlayEntry oldEntry = InlayUtil.getInlayAt(workBase, slotToReplace);
            // 命中已取走的空占位：直接中止本次操作，不消耗、不顺延到其它槽
            if (oldEntry.isEmpty()) return 0;

            oldStack = oldEntry.toItemStack();
            if (oldEntry.containsAttributes(InlayProperty.ENCHANT)) {
                oldStack = InlayUtil.extractFirstEnchantment(workBase, oldStack);
            }
            result = InlayUtil.withReplacedAt(workBase, slotToReplace, entry);
        } else {
            // 未满：追加到空占位或列表末尾
            result = InlayUtil.withAddedInlay(workBase, entry);
        }
        if (result.isEmpty()) return 0;

        if (entry.containsAttributes(InlayProperty.ENCHANT)) {
            InlayUtil.transferEnchantments(result, inlay);
        }

        // 仅消耗已镶嵌的份数，剩余基材留在槽位中等待下次砸击
        inlay.shrink(count);
        base.shrink(count);

        dropItem(result, count);
        dropItem(oldStack, count);
        return count;
    }

    private int processRemoveInlay(ItemStack base, float fallDistance) {
        if (fallDistance < 0) return 0;

        List<InlayEntry> inlays = InlayUtil.getInlays(base);
        if (inlays.isEmpty()) return 0;

        int slotIndex = Math.min((int) Math.floor(fallDistance), inlays.size() - 1);
        if (slotIndex < 0) return 0;

        // 命中已取走的空占位：直接中止本次操作，不消耗、不顺延到其它槽
        InlayEntry targetEntry = inlays.get(slotIndex);
        if (targetEntry.isEmpty()) return 0;

        ItemStack removedInlay = targetEntry.toItemStack();
        if (targetEntry.containsAttributes(InlayProperty.ENCHANT)) {
            removedInlay = InlayUtil.extractFirstEnchantment(base, removedInlay);
        }

        // 取下：基材上的该镶嵌物掉落，基材本身(去掉该镶嵌)以掉落物生成。
        // 整叠基材统一去掉该槽位镶嵌，按总份数生成。
        ItemStack result = InlayUtil.withRemovedAt(base, slotIndex);
        if (result.isEmpty()) return 0;

        int count = base.getCount();
        base.setCount(0);

        dropItem(result, count);
        dropItem(removedInlay, count);
        return count;
    }

    /** 槽位列表中是否存在空占位（取出过的物理槽位）。 */
    private static boolean hasEmptySlot(List<InlayEntry> inlays) {
        for (InlayEntry e : inlays) {
            if (e.isEmpty()) return true;
        }
        return false;
    }

    @Nullable
    private InlayRecipe findRecipe(ItemStack material, ItemStack base) {
        if (level == null) return null;
        return InlayRecipe.find(level, material, base);
    }
}
