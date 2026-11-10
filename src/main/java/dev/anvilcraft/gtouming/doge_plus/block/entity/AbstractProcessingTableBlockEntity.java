package dev.anvilcraft.gtouming.doge_plus.block.entity;

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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.Arrays;

/**
 * 镶嵌台 / 镶合台共用的加工台方块实体。
 *
 * <p>负责槽位数组、内部输入 handler 与「只进不出」的对外代理、配方过滤、客户端同步、
 * 顶部掉落物收集、产物掉落、音效粒子与 NBT 持久化。具体槽位规则与加工流程由子类实现。</p>
 */
public abstract class AbstractProcessingTableBlockEntity extends BlockEntity
        implements IItemHandlerHolder, RecipeFilterHost, AnvilStrikeProcessor {

    protected final ItemStack[] slots;
    protected ItemStack recipeFilter = ItemStack.EMPTY;

    private final ItemStackHandler input;
    private final ItemStackHandler proxy;

    protected AbstractProcessingTableBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.slots = new ItemStack[slotCount];
        Arrays.fill(this.slots, ItemStack.EMPTY);

        this.input = new ItemStackHandler(slotCount) {
            @Override
            public ItemStack getStackInSlot(int slot) {
                return isValidSlot(slot) ? slots[slot] : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isValidSlot(slot) || stack.isEmpty()) return stack.copy();
                if (!canInsert(slot, stack)) return stack.copy();

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
                return isValidSlot(slot) && !stack.isEmpty() && accepts(slot, stack);
            }

            @Override
            public int getSlots() {
                return slots.length;
            }
        };

        this.proxy = new ItemStackHandler(slotCount) {
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
    }

    /** 该槽位是否接受该物品。 */
    public abstract boolean accepts(int slot, ItemStack stack);

    /** 插入前的额外校验；默认等同 {@link #accepts(int, ItemStack)}。 */
    protected boolean canInsert(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    public IItemHandler getItemHandler() {
        return proxy;
    }

    /** 内部存储，供本方块自身读写。 */
    public IItemHandler getInput() {
        return input;
    }

    @Override
    public ItemStack getRecipeFilter() {
        return recipeFilter;
    }

    @Override
    public void setRecipeFilter(ItemStack stack) {
        this.recipeFilter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        syncToClient();
    }

    protected void syncToClient() {
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

    public final void tick() {
        if (level == null || level.isClientSide) return;

        boolean changed = false;
        // 仅收集投进顶部凹槽内（水平 2..14/16、高度 10..16 像素）的掉落物；
        // 产物生成在台体下方（见 dropItem），位于此 AABB 之外，不会被吸回。
        AABB box = new AABB(
                getBlockPos().getX() + 0.125, getBlockPos().getY() + 0.625, getBlockPos().getZ() + 0.125,
                getBlockPos().getX() + 0.875, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.875);

        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (entity.isRemoved() || entity.getItem().isEmpty()) continue;

            ItemStack item = entity.getItem();
            ItemStack remaining = collectInto(item.copy(), entity);
            if (remaining.getCount() == item.getCount()) continue;

            changed = true;
            if (remaining.isEmpty()) {
                entity.discard();
            } else {
                entity.setItem(remaining);
            }
        }

        if (changed) syncToClient();
    }

    /** 把一份掉落物插入槽位，返回插入后的剩余；数量不变表示没有插入。 */
    protected abstract ItemStack collectInto(ItemStack stack, ItemEntity entity);

    // ==================== 槽位操作 ====================

    protected boolean isValidSlot(int slot) {
        return slot >= 0 && slot < slots.length;
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (!isValidSlot(slot)) return;
        slots[slot] = stack.copy();
        syncToClient();
    }

    // ==================== 产物输出 ====================

    /** 在台体正下方生成 count 份掉落物；超出单堆上限时按堆叠上限分堆。 */
    protected void dropItem(ItemStack stack, int count) {
        if (level == null || level.isClientSide || stack.isEmpty() || count <= 0) return;
        int max = stack.getMaxStackSize();
        while (count > 0) {
            int batch = Math.min(count, max);
            dropItem(stack.copyWithCount(batch));
            count -= batch;
        }
    }

    /** 在台体正下方生成一个掉落物（位于顶部收集 AABB 外，不会被自身 tick 吸回）。 */
    protected void dropItem(ItemStack stack) {
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

    protected void playEffects(Level level) {
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
        tag.put("RecipeFilter", recipeFilter.saveOptional(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ListTag list = tag.getList("Slots", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), slots.length); i++) {
            slots[i] = ItemStack.parseOptional(registries, list.getCompound(i));
        }
        recipeFilter = ItemStack.parseOptional(registries, tag.getCompound("RecipeFilter"));
    }
}
