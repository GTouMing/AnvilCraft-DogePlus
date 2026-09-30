package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 镶合台方块实体：单个物品槽（镶满的基材）。
 *
 * <p>铁砧砸击时按 {@link InlayCraftingRecipe} 匹配整件基材（物品 + 全部镶孔按序一致），
 * 命中则一次耗尽整叠：按份生成配方声明的全部产物（含返还的模具），均落在台体下方，
 * 供漏斗/溜槽等自动化收集；产物区域位于收集区之外，不会被自身 tick 吸回。</p>
 */
public class InlayCraftingTableBlockEntity extends BlockEntity implements IItemHandlerHolder {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_COUNT = 1;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];

    // ==================== IItemHandler 实现 ====================

    /**
     * 内部存储：读写抽取都正常，供本方块自身与玩家交互使用。
     *
     * <p>与前置加工台 {@code ProcessingTableBlockEntity} 一样，真正存放材料的是这份内部
     * handler，对外只暴露下面的 {@link #proxy}——「存储输入材料、不存储输出产物」，
     * 产物由镶合流程从台体下方掉出。</p>
     */
    private final ItemStackHandler input = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return isValidSlot(slot) ? slots[slot] : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isValidSlot(slot) || stack.isEmpty()) return stack.copy();

            ItemStack existing = slots[slot];
            ItemStack remaining = stack.copy();
            if (existing.isEmpty()) {
                if (!simulate) {
                    slots[slot] = remaining;
                    syncToClient();
                }
                return ItemStack.EMPTY;
            }

            if (!ItemStack.isSameItemSameComponents(existing, stack)) {
                return remaining;
            }
            int space = existing.getMaxStackSize() - existing.getCount();
            int toMove = Math.min(space, stack.getCount());
            remaining.shrink(toMove);

            if (!simulate) {
                existing.grow(toMove);
                syncToClient();
            }
            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isValidSlot(slot)) return ItemStack.EMPTY;
            ItemStack existing = slots[slot];
            int extracted = Math.min(amount, existing.getCount());
            ItemStack result = existing.copyWithCount(extracted);
            if (!simulate) {
                existing.shrink(extracted);
                syncToClient();
            }
            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidSlot(slot) && !stack.isEmpty();
        }

        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }
    };

    /**
     * 对外暴露给漏斗 / 溜槽 / 物流的代理（同前置加工台的 {@code proxy}）：可以放入基材，
     * 但**一律拒绝抽取**——存储的基材只由镶合流程消耗或玩家手动取用，因此溜槽只会吸到
     * 从台下方掉出的产物，不会把材料槽里的基材吸走。
     */
    private final ItemStackHandler proxy = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return input.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return input.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return input.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return input.isItemValid(slot, stack);
        }

        @Override
        public void setSize(int size) {
        }

        @Override
        public void setStackInSlot(int slot, ItemStack stack) {
            input.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return input.getSlots();
        }
    };

    /** 对外（能力）暴露的代理：只进不出。 */
    @Override
    public IItemHandler getItemHandler() {
        return proxy;
    }

    /** 内部存储，供本方块自身读写。 */
    public IItemHandler getInput() {
        return input;
    }

    // ==================== 构造 ====================

    public InlayCraftingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) return;
        slots[slot] = stack.copy();
        syncToClient();
    }

    // ==================== 客户端同步 ====================

    private void syncToClient() {
        setChanged();
        if (level == null || level.isClientSide) return;
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== Tick：收集投入的物品 ====================

    public void tick() {
        if (level == null || level.isClientSide) return;

        // 仅收集投进顶部凹槽内（水平 2..14/16、高度 10..16 像素）的掉落物；
        // 产物生成在台体下方（见 dropItem），位于此 AABB 之外，不会被吸回。
        AABB box = new AABB(
                getBlockPos().getX() + 0.125, getBlockPos().getY() + 0.625, getBlockPos().getZ() + 0.125,
                getBlockPos().getX() + 0.875, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.875);

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.isRemoved() || entity.getItem().isEmpty()) continue;

            ItemStack item = entity.getItem();
            ItemStack result = input.insertItem(SLOT_BASE, item, false);
            if (result.getCount() == item.getCount()) continue;

            if (result.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(result);
            }
            syncToClient();
        }
    }

    // ==================== 核心：镶合处理 ====================

    /**
     * 铁砧砸击处理：单槽基材匹配 {@link InlayCraftingRecipe} 后一次耗尽整叠，
     * 按份数在台体下方生成全部产物（同镶嵌台整叠加工语义）。
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

        InlayCraftingRecipe recipe = findRecipe(base);
        if (recipe == null) return false;

        int crafts = base.getCount();
        List<ItemStack> totals = new ArrayList<>();
        for (int i = 0; i < crafts; i++) {
            for (ItemStack result : recipe.resultsFor(base, level.random, level.registryAccess())) {
                accumulate(totals, result);
            }
        }
        if (totals.isEmpty()) return false;

        slots[SLOT_BASE] = ItemStack.EMPTY;
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
        for (RecipeHolder<InlayCraftingRecipe> holder : recipes) {
            InlayCraftingRecipe recipe = holder.value();
            if (recipe.matches(base)) return recipe;
        }
        return null;
    }

    // ==================== 产物输出 ====================

    /** 在台体正下方生成 count 份掉落物；超出单堆上限时按堆叠上限分堆。 */
    private void dropItem(ItemStack stack, int count) {
        if (level == null || level.isClientSide || stack.isEmpty() || count <= 0) return;
        int max = stack.getMaxStackSize();
        while (count > 0) {
            int batch = Math.min(count, max);
            dropItem(stack.copyWithCount(batch));
            count -= batch;
        }
    }

    /** 在台体正下方生成一个掉落物（位于顶部收集 AABB 外，不会被自身 tick 吸回）。 */
    private void dropItem(ItemStack stack) {
        if (level == null || level.isClientSide || stack.isEmpty()) return;
        BlockPos pos = getBlockPos();
        ItemEntity item = new ItemEntity(
                level,
                pos.getX() + 0.5,
                pos.getY(),
                pos.getZ() + 0.5,
                stack,
                0.0, 0.0, 0.0);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
    }

    private void playEffects(Level level) {
        level.playSound(null, getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
        if (level instanceof ServerLevel server) {
            server.sendParticles(
                    ParticleTypes.CRIT,
                    getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 1.0,
                    getBlockPos().getZ() + 0.5,
                    12, 0.3, 0.2, 0.3, 0.05
            );
        }
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack slot : slots) {
            list.add(slot.saveOptional(registries));
        }
        tag.put("Slots", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), slots.length); i++) {
            slots[i] = ItemStack.parseOptional(registries, list.getCompound(i));
        }
    }
}
