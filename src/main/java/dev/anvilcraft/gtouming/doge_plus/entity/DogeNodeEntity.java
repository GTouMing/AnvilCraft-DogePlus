package dev.anvilcraft.gtouming.doge_plus.entity;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.gtouming.doge_plus.mixin.MagnetizedNodeEntityAccessor;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class DogeNodeEntity extends MagnetizedNodeEntity {

    private static final int MAX_CAPTURED = 8;
    private static final double CAPTURE_RADIUS = 0.5;
    private static final int MAX_STACK_SIZE = 64;

    private final List<ItemEntity> capturedItems = new ArrayList<>();

    // ==================== IItemHandler 实现 ====================

    @Getter
    private final IItemHandler itemHandler = new ItemStackHandler(MAX_CAPTURED) {
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (isSlotInvalid(slot)) return ItemStack.EMPTY;
            if (slot >= capturedItems.size()) return ItemStack.EMPTY;

            ItemEntity entity = capturedItems.get(slot);
            if (isEntityInvalid(entity)) return ItemStack.EMPTY;

            return entity.getItem().copy();
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            ItemStack remaining = stack.copy();
            if (isSlotInvalid(slot)) return remaining;
            if (isFull()) return remaining;

            // 槽位未被占用 → 新建 ItemEntity
            if (slot >= capturedItems.size()) {
                if (simulate) return ItemStack.EMPTY;
                ItemEntity entity = new ItemEntity(level(), xo, yo, zo, remaining);
                entity.setPos(position());
                add(entity);
                level().addFreshEntity(entity);
                return ItemStack.EMPTY;
            }

            // 槽位已被占用 → 尝试合并
            ItemEntity existing = capturedItems.get(slot);
            if (isEntityInvalid(existing)) return remaining;

            ItemStack existingStack = existing.getItem();
            if (!ItemEntity.areMergable(existingStack, remaining)) return remaining;

            int space = existingStack.getMaxStackSize() - existingStack.getCount();
            if (space <= 0) return remaining;

            int toInsert = Math.min(remaining.getCount(), space);
            remaining.shrink(toInsert);

            if (!simulate) {
                existingStack.grow(toInsert);
                existing.setItem(existingStack);
            }

            return remaining;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (isSlotInvalid(slot)) return ItemStack.EMPTY;
            if (amount <= 0) return ItemStack.EMPTY;
            if (slot >= capturedItems.size()) return ItemStack.EMPTY;

            ItemEntity entity = capturedItems.get(slot);
            if (isEntityInvalid(entity)) return ItemStack.EMPTY;

            ItemStack current = entity.getItem();
            int extracted = Math.min(amount, current.getCount());
            ItemStack result = current.copy();
            result.setCount(extracted);

            if (!simulate) {
                current.shrink(extracted);
                entity.setItem(current);
            }

            return result;
        }

        @Override
        public int getSlotLimit(int slot) {
            return MAX_STACK_SIZE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (isSlotInvalid(slot)) return false;
            if (stack.isEmpty()) return false;
            if (isFull()) return false;

            // 槽位未被占用 → 有效
            if (slot >= capturedItems.size()) return true;

            // 槽位已被占用 → 检查是否可合并
            ItemEntity entity = capturedItems.get(slot);
            if (isEntityInvalid(entity)) return false;

            ItemStack existing = entity.getItem();
            return ItemEntity.areMergable(existing, stack)
                    && existing.getCount() < existing.getMaxStackSize();
        }

        @Override
        public int getSlots() {
            return MAX_CAPTURED;
        }
    };

    // ==================== 构造 ====================

    public DogeNodeEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    public DogeNodeEntity(Level level, Vec3 pos, BlockPos blockPos) {
        super(ModEntities.DOGE_NODE.get(), level);
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
        this.noPhysics = true;
        this.setInvulnerable(true);
        this.blockPos = blockPos;
        ((MagnetizedNodeEntityAccessor) this).doge_plus$setBlockState(level.getBlockState(blockPos));
    }

    // ==================== 核心逻辑 ====================

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        capturedItems.removeIf(item ->
                !((ICaptured) Util.cast(item)).doge_plus$isCaptured()
                        || item.isRemoved()
                        || !position().equals(item.position()));

        if (isFull()) return;

        captureNearby(new AABB(above(), above()).inflate(CAPTURE_RADIUS));
    }

    private void captureNearby(AABB box) {
        for (ItemEntity entity : level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (capturedItems.contains(entity)) continue;

            ItemStack stack = entity.getItem();
            if (stack.isEmpty()) continue;

            // 1. 先尝试合并到现有物品
            for (ItemEntity existing : capturedItems) {
                if (stack.isEmpty()) break;

                if (!ItemEntity.areMergable(existing.getItem(), stack)) continue;

                int maxStack = existing.getItem().getMaxStackSize();
                int space = maxStack - existing.getItem().getCount();
                if (space <= 0) continue;

                int toMerge = Math.min(stack.getCount(), space);
                existing.getItem().grow(toMerge);
                stack.shrink(toMerge);
            }

            if (isFull()) return;

            // 2. 捕获新物品
            entity.setPos(position());
            add(entity);

            if (isFull()) return;
        }
    }

    // ==================== 辅助方法 ====================

    private boolean isFull() {
        return capturedItems.size() >= MAX_CAPTURED;
    }

    private Vec3 above() {
        return position().add(0, CAPTURE_RADIUS, 0);
    }

    private boolean isSlotInvalid(int slot) {
        return slot < 0 || slot >= MAX_CAPTURED;
    }

    private boolean isEntityInvalid(ItemEntity entity) {
        return entity.isRemoved() || entity.getItem().isEmpty();
    }

    public void add(ItemEntity entity) {
        entity.setUnlimitedLifetime();
        entity.setNeverPickUp();
        ((ICaptured) entity).doge_plus$setIndex(capturedItems.size());
        ((ICaptured) entity).doge_plus$setCaptured(true);
        capturedItems.add(entity);
    }

    // ==================== 对外接口 ====================

    public void releaseToPlayer(Player player) {
        if (level().isClientSide) return;

        for (ItemEntity item : capturedItems) {
            ((ICaptured) Util.cast(item)).doge_plus$setCaptured(false);
            item.setNoPickUpDelay();
            item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
        }
        capturedItems.clear();
    }

    public void removeNodeAndRelease() {
        if (level().isClientSide) return;

        for (ItemEntity item : capturedItems) {
            ((ICaptured) Util.cast(item)).doge_plus$setCaptured(false);
            item.setNoPickUpDelay();
        }
        capturedItems.clear();
        discard();
    }
}