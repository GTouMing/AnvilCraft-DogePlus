package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import lombok.Getter;
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

import java.util.Arrays;
import java.util.List;

import static dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager.hasSocket;

/**
 * 镶嵌台方块实体：4 个槽位（镶嵌物/被镶嵌物/产品/旧镶嵌物）。
 *
 * <p>铁砧砸击时执行镶嵌：查找匹配的 {@link InlayRecipe}，消耗 1 个材料 + 1 个基材，
 * 产出镶嵌后的物品。基材镶孔数（数据驱动）决定可镶嵌次数：
 * 未满时追加镶嵌，满镶时替换最旧镶嵌并将旧材料弹出到旧镶嵌物槽。
 * 支持一次铁砧批量镶嵌。</p>
 */
public class InlayTableBlockEntity extends BlockEntity {

    // ==================== 槽位常量 ====================

    public static final int SLOT_BASE = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_PRODUCT = 2;
    public static final int SLOT_OLD_MATERIAL = 3;
    public static final int SLOT_COUNT = 4;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];

    // ==================== IItemHandler 实现 ====================

    @Getter
    private final IItemHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            return isValidSlot(slot) ? slots[slot] : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isValidSlot(slot)) return stack.copy();
            if (slot != SLOT_MATERIAL && slot != SLOT_BASE) return stack.copy();
            if (slot == SLOT_BASE && !hasSocket(stack)) return stack.copy();
            return insertStack(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!isValidSlot(slot)) return ItemStack.EMPTY;
            if (slot != SLOT_PRODUCT && slot != SLOT_OLD_MATERIAL) return ItemStack.EMPTY;

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
            return slot == SLOT_MATERIAL || slot == SLOT_BASE;
        }

        @Override
        public int getSlots() {
            return SLOT_COUNT;
        }
    };

    // ==================== 构造 ====================

    public InlayTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(slots, ItemStack.EMPTY);
    }
    // ==================== 内部槽位操作 ====================

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < SLOT_COUNT;
    }

    private ItemStack insertStack(int slot, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) return ItemStack.EMPTY;

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

    public void setStackInSlot(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) return;
        slots[slot] = stack.copy();
        syncToClient();
    }

    private boolean canAcceptSlot(int slot, ItemStack stack) {
        ItemStack existing = slots[slot];
        if (existing.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(existing, stack)
                && existing.getCount() + stack.getCount() <= existing.getMaxStackSize();
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

    // ==================== Tick：收集掉落物 ====================

    public void tick() {
        if (level == null || level.isClientSide) return;

        boolean changed = false;
        AABB box = new AABB(getBlockPos()).setMaxY(getBlockPos().getY() + 1.1);

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.isRemoved() || entity.getItem().isEmpty()) continue;

            ItemStack item = entity.getItem();
            for (int i = 0; i < SLOT_COUNT; i++) {
                ItemStack result = itemHandler.insertItem(i, item, false);
                if (result.getCount() == item.getCount()) continue;

                changed = true;
                if (result.isEmpty()) {
                    entity.discard();
                } else {
                    entity.setItem(result);
                }
                break;
            }
        }

        if (changed) syncToClient();
    }

    // ==================== 核心：镶嵌处理 ====================

    /**
     * 铁砧砸击处理：批量执行镶嵌。
     *
     * <p>基材的镶孔数（数据驱动，见 {@link MaterialManager}）决定可镶嵌次数：
     * 未满时追加镶嵌，满镶时替换最旧镶嵌并将旧材料弹出到旧镶嵌物槽。
     * 每次消耗 1 个材料 + 1 个基材，产出 1 个镶嵌后的物品。</p>
     *
     * <p>当材料槽为空时：依次取下已镶嵌的材料（从最旧到最新），
     * 每次砸击取下最旧的一个镶嵌物，放入旧镶嵌物槽。</p>
     *
     * @return 是否至少完成了一次镶嵌或取下操作
     */
    public boolean processInlay(Level level, float fallDistance) {
        if (level.isClientSide) return false;

        ItemStack material = slots[SLOT_MATERIAL];
        ItemStack base = slots[SLOT_BASE];
        int processed = 0;

        // ========== 模式 1：材料槽有材料 → 执行镶嵌 ==========
        if (!material.isEmpty() && !base.isEmpty()) {
            processed = processAddInlay(material, base, fallDistance);
        }

        // ========== 模式 2：材料槽为空 → 取下已镶嵌材料 ==========
        if (processed == 0 && material.isEmpty() && !base.isEmpty()) {
            processed = processRemoveInlay(base, fallDistance);
        }

        // ========== 完成处理 ==========
        if (processed > 0) {
            syncToClient();
            playEffects(level);
        }
        return processed > 0;
    }

    private int processAddInlay(ItemStack inlay, ItemStack base, float fallDistance) {
        int processed = 0;
        ItemStack currentBase = base;

        while (processed < 64 && !inlay.isEmpty() && !currentBase.isEmpty()) {
            InlayRecipe recipe = findRecipe(inlay, currentBase);
            if (recipe == null) break;

            InlayEntry entry = InlayEntry.fromItemStack(inlay);
            int sockets = MaterialManager.getSocketCount(currentBase);
            int inlayCount = InlayUtil.getInlayCount(currentBase);
            boolean full = inlayCount >= sockets;

            // 满镶时准备旧材料
            ItemStack oldStack = ItemStack.EMPTY;
            int slotToReplace = -1;

            if (full) {
                // ===== 根据下落高度计算要替换的槽位 =====
                // fallDistance 0→0, 1→0, 2→1, 3→2, 4→3 ... 限制在 0 ~ (inlayCount-1)
                slotToReplace = Math.min((int) Math.floor(fallDistance), inlayCount - 1);
                if (slotToReplace < 0) break;

                InlayEntry oldEntry = InlayUtil.getInlayAt(currentBase, slotToReplace);
                if (oldEntry.isEmpty()) break;

                oldStack = oldEntry.toItemStack();
                if (oldEntry.containsAttributes(InlayProperty.ENCHANT)) {
                    oldStack = InlayUtil.extractFirstEnchantment(currentBase, oldStack);
                }

                if (!canAcceptSlot(SLOT_OLD_MATERIAL, oldStack)) break;
            }

            // 执行镶嵌
            ItemStack result = full
                    ? InlayUtil.withReplacedAt(currentBase, slotToReplace, entry)
                    : InlayUtil.withAddedInlay(currentBase, entry);

            if (entry.containsAttributes(InlayProperty.ENCHANT)) {
                InlayUtil.transferEnchantments(result, inlay);
            }

            if (!canAcceptSlot(SLOT_PRODUCT, result)) break;

            // 提交
            inlay.shrink(1);
            currentBase.shrink(1);
            if (!oldStack.isEmpty()) insertStack(SLOT_OLD_MATERIAL, oldStack, false);
            insertStack(SLOT_PRODUCT, result, false);
            processed++;

            currentBase = slots[SLOT_BASE];
        }

        return processed;
    }

    private int processRemoveInlay(ItemStack base, float fallDistance) {
        // 如果高度 < 0，不移除
        if (fallDistance < 0) return 0;

        int processed = 0;
        ItemStack currentBase = base;

        while (processed < 64 && !currentBase.isEmpty()) {
            int inlayCount = InlayUtil.getInlayCount(currentBase);
            if (inlayCount <= 0) break;

            // ===== 根据下落高度计算目标槽位 =====
            // fallDistance 0→0, 1→0, 2→1, 3→2 ... 限制在 0 ~ (inlayCount-1)
            int slot = Math.min((int) Math.floor(fallDistance), inlayCount - 1);
            if (slot < 0) break;

            InlayEntry targetEntry = InlayUtil.getInlayAt(currentBase, slot);
            if (targetEntry.isEmpty()) break;

            ItemStack removedStack = targetEntry.toItemStack();

            if (targetEntry.containsAttributes(InlayProperty.ENCHANT)) {
                removedStack = InlayUtil.extractFirstEnchantment(currentBase, removedStack);
            }

            if (!canAcceptSlot(SLOT_OLD_MATERIAL, removedStack)) break;

            ItemStack result = InlayUtil.withRemovedAt(currentBase, slot);
            if (result.isEmpty()) break;

            if (!canAcceptSlot(SLOT_PRODUCT, result)) break;

            currentBase.shrink(1);
            insertStack(SLOT_OLD_MATERIAL, removedStack, false);
            insertStack(SLOT_PRODUCT, result, false);
            processed++;

            currentBase = slots[SLOT_BASE];
        }

        return processed;
    }

    // ==================== 辅助方法 ====================

    @Nullable
    private InlayRecipe findRecipe(ItemStack material, ItemStack base) {
        if (level == null) return null;
        List<RecipeHolder<InlayRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get());

        for (RecipeHolder<InlayRecipe> holder : recipes) {
            if (holder.value().matches(material, base)) {
                return holder.value();
            }
        }
        return null;
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