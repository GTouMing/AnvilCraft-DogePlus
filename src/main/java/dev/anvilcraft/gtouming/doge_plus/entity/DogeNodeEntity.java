package dev.anvilcraft.gtouming.doge_plus.entity;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.gtouming.doge_plus.mixin.MagnetizedNodeEntityAccessor;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class DogeNodeEntity extends MagnetizedNodeEntity implements IItemHandler {

    private static final int MAX_CAPTURED = 8;
    private static final double CAPTURE_RADIUS = 0.5;
    private static final int MAX_STACK_SIZE = 64;

    private final List<ItemEntity> capturedItems = new ArrayList<>();

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

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        capturedItems.removeIf(item ->
                !((ICaptured) Util.cast(item)).doge_plus$isCaptured() || item.isRemoved() || !position().equals(item.position()));

        if (isFull()) return;

        // 捕获范围内的物品
        captureNearby(new AABB(above(), above()).inflate(CAPTURE_RADIUS));
    }

    /** 捕获 box 内的物品。 */
    private void captureNearby(AABB box) {
        for (ItemEntity entity : level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (capturedItems.contains(entity)) continue;

            var stack = entity.getItem();
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

            // 2. 检查容量
            if (isFull()) return;

            // 3. 通过 add 方法标记并加入列表
            entity.setPos(this.position());
            add(entity);

            if (isFull()) return;
        }
    }

    private boolean isFull() {
        return capturedItems.size() >= MAX_CAPTURED;
    }

    private Vec3 above() {
        return this.position().add(0, CAPTURE_RADIUS, 0);
    }

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
        this.discard();
    }

    public void add(ItemEntity entity) {
        entity.setUnlimitedLifetime();
        entity.setNeverPickUp();
        ((ICaptured) entity).doge_plus$setIndex(capturedItems.size());
        ((ICaptured) entity).doge_plus$setCaptured(true);
        capturedItems.add(entity);
    }

    private boolean isSlotInvalid(int slot) {
        return slot < 0 || slot >= MAX_CAPTURED;
    }

    private boolean isEntityInvalid(ItemEntity entity) {
        return entity.isRemoved() || entity.getItem().isEmpty();
    }

    // ==================== IItemHandler 接口实现 ====================

    @Override
    public int getSlots() {
        return MAX_CAPTURED;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (isSlotInvalid(slot)) return ItemStack.EMPTY;

        ItemEntity entity = capturedItems.get(slot);
        if (isEntityInvalid(entity)) return ItemStack.EMPTY;

        return entity.getItem().copy();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        if (isSlotInvalid(slot)) return remaining;
        if (isFull()) return remaining;

        if (slot >= capturedItems.size()) {
            if (simulate) return ItemStack.EMPTY;
            ItemEntity entity = new ItemEntity(this.level(), this.xo, this.yo, this.zo, remaining);
            entity.setPos(this.position());
            add(entity);
            level().addFreshEntity(entity);
            return ItemStack.EMPTY;
        }

        // 1. 尝试合并到已有槽位
        ItemEntity existing = capturedItems.get(slot);
        if (isEntityInvalid(existing)) return remaining;
        ItemStack existingStack = existing.getItem();
        if (!ItemEntity.areMergable(existingStack, remaining)) return remaining;
        int space = existingStack.getMaxStackSize() - existingStack.getCount();
        if (space <= 0) return remaining;
        int toInsert = Math.min(remaining.getCount(), space);
        remaining.shrink(toInsert);
        if (simulate) return remaining;
        existingStack.grow(toInsert);
        existing.setItem(existingStack);

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

        if (simulate) return result;
        current.shrink(extracted);
        entity.setItem(current);
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

        // 如果该槽位未被占用
        if (slot >= capturedItems.size()) return true;

        // 如果该槽位已被占用
        ItemEntity entity = capturedItems.get(slot);
        if (isEntityInvalid(entity)) return false;

        ItemStack existing = entity.getItem();
        return ItemEntity.areMergable(existing, stack)
                && existing.getCount() < existing.getMaxStackSize();
    }
}