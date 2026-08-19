package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
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
public class InlayTableBlockEntity extends BlockEntity implements IItemHandler {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_PRODUCT = 2;
    public static final int SLOT_OLD_MATERIAL = 3;
    public static final int SLOT_COUNT = 4;

    private final ItemStack[] slots = new ItemStack[SLOT_COUNT];

    public InlayTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        Arrays.fill(slots, ItemStack.EMPTY);
    }

    public boolean incorrectSlot(int slot) {
        return slot < 0 || slot >= SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return ItemStack.EMPTY;
        return slots[slot];
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    public ItemStack insertStack(int slot, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        ItemStack existing = slots[slot];
        if (existing.isEmpty()) {
            if (simulate) return ItemStack.EMPTY;
            slots[slot] = remaining;
            syncToClient();
            return ItemStack.EMPTY;
        }
        else if (!ItemStack.isSameItemSameComponents(existing, stack)) return remaining;

        int space = existing.getMaxStackSize() - existing.getCount();
        int toMove = Math.min(space, stack.getCount());
        remaining.shrink(toMove);
        if (simulate) return remaining;
        existing.grow(toMove);
        syncToClient();
        return remaining;
    }

    /** 插入仅允许 1（材料）和 0（基材）槽。 */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        if (incorrectSlot(slot)) return remaining;
        if (slot != SLOT_MATERIAL && slot != SLOT_BASE) return remaining;
        if (slot == 0 && !hasSocket(stack)) return remaining;

        return insertStack(slot, stack, simulate);
    }

    /** 取出仅允许 2（产品）和 3（旧镶嵌物）槽。 */
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != SLOT_PRODUCT && slot != SLOT_OLD_MATERIAL) return ItemStack.EMPTY;
        if (incorrectSlot(slot)) return ItemStack.EMPTY;

        ItemStack existing = slots[slot];
        int extracted = Math.min(amount, existing.getCount());
        ItemStack result = existing.copy();
        result.setCount(extracted);
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

    public void setStackInSlot(int slot, ItemStack stack) {
        slots[slot] = stack.copy();
        syncToClient();
    }

    /** 标记脏数据并同步到客户端（供渲染器显示槽位物品）。 */
    private void syncToClient() {
        setChanged();
        if (level == null || level.isClientSide) return;
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
    }

    /** 每 tick 收集上方掉落物作为材料。 */
    public void tick() {
        if (level == null || level.isClientSide) return;
        boolean changed = false;
        AABB box = new AABB(getBlockPos()).setMaxY(getBlockPos().getY() + 1.1);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            for (int i = 0; i < SLOT_COUNT; i++) {
                if (entity.isRemoved() || entity.getItem().isEmpty()) continue;
                ItemStack result = insertItem(i, entity.getItem(), false);
                if (result.getCount() != entity.getItem().getCount()) changed = true;
                else continue;
                if (result.isEmpty()) entity.discard();
                else entity.setItem(result);
            }
        }
        if (changed) syncToClient();
    }

    /**
     * 铁砧砸击处理：批量执行镶嵌。
     *
     * <p>基材的镶孔数（数据驱动，见 {@link MaterialManager}）决定可镶嵌次数：
     * 未满时追加镶嵌，满镶时替换最旧镶嵌并将旧材料弹出到旧镶嵌物槽。
     * 每次消耗 1 个材料 + 1 个基材，产出 1 个镶嵌后的物品。</p>
     *
     * @return 是否至少完成了一次镶嵌
     */
    public boolean processInlay(Level level) {
        if (level.isClientSide) return false;

        ItemStack material = slots[SLOT_MATERIAL];
        ItemStack base = slots[SLOT_BASE];
        int processed = 0;

        while (processed < 64 && !material.isEmpty() && !base.isEmpty()) {
            InlayRecipe recipe = findRecipe(level, material, base);
            if (recipe == null) break;

            ResourceLocation materialId = BuiltInRegistries.ITEM.getKey(material.getItem());
            int sockets = MaterialManager.getSocketCount(base);
            boolean full = InlayUtil.getInlayCount(base) >= sockets;

            ItemStack result;
            ItemStack oldStack = ItemStack.EMPTY;
            if (full) {
                // 满镶：替换最旧镶嵌，旧材料弹出到旧镶嵌物槽
                ResourceLocation oldId = InlayUtil.getFirstInlay(base);
                if (oldId != null) {
                    oldStack = new ItemStack(BuiltInRegistries.ITEM.get(oldId));
                    // 旧嵌材带「附魔」性质：移除时提取基材第一个附魔
                    MaterialManager.InlayMaterial oldMaterial = MaterialManager.getInlay(oldStack);
                    if (oldMaterial != null && oldMaterial.has(InlayProperty.ENCHANT)) {
                        oldStack = InlayUtil.extractFirstEnchantment(base, oldStack);
                    }
                    if (deny(SLOT_OLD_MATERIAL, oldStack)) break;
                }
                result = InlayUtil.withReplacedOldestInlay(base, materialId);
            } else {
                // 有剩余镶孔：追加镶嵌
                result = InlayUtil.withAddedInlay(base, materialId);
            }

            // 镶嵌材料带「附魔」性质：合并附魔到基材
            MaterialManager.InlayMaterial inlayMaterial = MaterialManager.getInlay(material);
            if (inlayMaterial != null && inlayMaterial.has(InlayProperty.ENCHANT)) {
                InlayUtil.transferEnchantments(result, material);
            }

            if (deny(SLOT_PRODUCT, result)) break;

            // 提交
            material.shrink(1);
            base.shrink(1);
            if (!oldStack.isEmpty()) insertStack(SLOT_OLD_MATERIAL, oldStack, false);
            insertStack(SLOT_PRODUCT, result, false);
            processed++;
        }

        if (processed > 0) {
            syncToClient();
            level.playSound(null, getBlockPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 1.0f, 1.0f);
            if (level instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.CRIT,
                        getBlockPos().getX() + 0.5, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.5,
                        12, 0.3, 0.2, 0.3, 0.05);
            }
        }
        return processed > 0;
    }

    @Nullable
    private InlayRecipe findRecipe(Level level, ItemStack material, ItemStack base) {
        List<RecipeHolder<InlayRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get());
        for (RecipeHolder<InlayRecipe> holder : recipes) {
            if (holder.value().matches(material, base)) return holder.value();
        }
        return null;
    }

    private boolean deny(int slot, ItemStack stack) {
        ItemStack existing = slots[slot];
        if (existing.isEmpty()) return false;
        return !ItemStack.isSameItemSameComponents(existing, stack)
                || existing.getCount() + stack.getCount() > existing.getMaxStackSize();
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

    // ==================== 客户端同步 ====================

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
}
