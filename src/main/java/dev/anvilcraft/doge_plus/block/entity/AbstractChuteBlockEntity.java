package dev.anvilcraft.doge_plus.block.entity;

import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public abstract class AbstractChuteBlockEntity extends BaseChuteBlockEntity {

    public AbstractChuteBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected final Direction getOutputDirection() {
        return getDirection();
    }

    @Override
    public abstract Component getDisplayName();

    @Nullable
    @Override
    public abstract AbstractContainerMenu createMenu(int i, Inventory inventory, Player player);

    public void tick() {
        if (level == null) return;
        if (getCooldown() > 0) setCooldown(getCooldown() - 1);

        boolean resetCD = false;
        if (getCooldown() <= 0 && isEnabled()) {
            if (!this.inventoryFull()) {
                IItemHandler source = ItemHandlerUtil.getSourceItemHandler(getBlockPos().relative(getInputDirection()), getInputDirection().getOpposite(), level);
                if (source != null) {
                    resetCD |= ItemHandlerUtil.importFromTarget(getItemHandler(), 64, stack -> true, source);
                } else {
                    List<ItemEntity> itemEntities = Objects.requireNonNull(getLevel()).getEntitiesOfClass(ItemEntity.class, new AABB(getBlockPos().relative(getInputDirection())), itemEntity -> !itemEntity.getItem().isEmpty());
                    for (ItemEntity itemEntity : itemEntities) {
                        ItemStack itemStack = itemEntity.getItem();
                        ItemStack remaining = ItemHandlerHelper.insertItem(getFilteredItemStackHandler(), itemStack, true);
                        if (remaining.getCount() == itemStack.getCount()) continue;
                        ItemHandlerHelper.insertItem(getFilteredItemStackHandler(), itemEntity.getItem(), false);
                        itemEntity.setItem(remaining);
                        resetCD = true;
                    }
                }
            }
        }
        level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        if (resetCD) setCooldown(dev.dubhe.anvilcraft.AnvilCraft.CONFIG.chuteMaxCooldown);
    }

    private boolean inventoryFull() {
        for (int i = 0; i < getFilteredItemStackHandler().getSlots(); i++) {
            ItemStack itemstack = getFilteredItemStackHandler().getStackInSlot(i);
            if (itemstack.isEmpty() || itemstack.getCount() != itemstack.getMaxStackSize()) return false;
        }
        return true;
    }
}