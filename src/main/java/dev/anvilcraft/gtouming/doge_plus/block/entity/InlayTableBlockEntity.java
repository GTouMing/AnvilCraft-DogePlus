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
 * 镶嵌台方块实体：2 个槽位（镶嵌物/被镶嵌物）。
 *
 * <p>铁砧砸击时执行镶嵌：查找匹配的 {@link InlayRecipe}，消耗 1 个材料 + 1 个基材，
 * 产出镶嵌后的物品。基材镶孔数（数据驱动）决定可镶嵌次数：
 * 未满时追加镶嵌，满镶时替换指定槽位。产物与被替换的旧材料均以掉落物形式生成。</p>
 */
public class InlayTableBlockEntity extends BlockEntity {

    // ==================== 槽位常量 ====================

    public static final int SLOT_BASE = 0;
    public static final int SLOT_MATERIAL = 1;
    public static final int SLOT_COUNT = 2;

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
            if (!isValidSlot(slot) || stack.isEmpty()) return stack.copy();
            if (slot == SLOT_BASE && !hasSocket(stack)) return stack.copy();

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

    // ==================== Tick：收集掉落物 ====================

    public void tick() {
        if (level == null || level.isClientSide) return;

        boolean changed = false;
        // 仅收集投进顶部凹槽内（水平 2..14/16、高度 10..16 像素）的掉落物；
        // 产物/旧材料生成在台下（见 dropItem），位于此 AABB 之外，不会被吸回。
        AABB box = new AABB(
                getBlockPos().getX() + 0.125, getBlockPos().getY() + 0.625, getBlockPos().getZ() + 0.125,
                getBlockPos().getX() + 0.875, getBlockPos().getY() + 1.0, getBlockPos().getZ() + 0.875);

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
        InlayRecipe recipe = findRecipe(inlay, base);
        if (recipe == null) return 0;

        // 一次砸击耗尽整叠：消耗 min(材料,基材) 份，产物为整叠基材统一更新一次组件。
        int count = Math.min(inlay.getCount(), base.getCount());

        InlayEntry entry = InlayEntry.fromItemStack(inlay);
        int sockets = MaterialManager.getSocketCount(base);

        // 槽位列表：取出过的槽位为空占位，列表长度即物理槽位数
        List<InlayEntry> existing = InlayUtil.getInlays(base);
        boolean full = !hasEmptySlot(existing) && existing.size() >= sockets;

        // 满镶时准备旧材料（整叠基材同一槽位换下同一种旧材料）
        ItemStack oldStack = ItemStack.EMPTY;
        ItemStack result;
        if (full) {
            // 仅满镶时按下落高度定位替换槽位；未满（含全新基材，列表为空）不进入此处，
            // 避免 existing.size() - 1 为负而误中止追加镶嵌
            int slotToReplace = Math.min((int) Math.floor(fallDistance), existing.size() - 1);
            if (slotToReplace < 0) return 0;

            InlayEntry oldEntry = InlayUtil.getInlayAt(base, slotToReplace);
            // 命中已取走的空占位：直接中止本次操作，不消耗、不顺延到其它槽
            if (oldEntry.isEmpty()) return 0;

            oldStack = oldEntry.toItemStack();
            if (oldEntry.containsAttributes(InlayProperty.ENCHANT)) {
                oldStack = InlayUtil.extractFirstEnchantment(base, oldStack);
            }
            result = InlayUtil.withReplacedAt(base, slotToReplace, entry);
        } else {
            // 未满：追加到空占位或列表末尾
            result = InlayUtil.withAddedInlay(base, entry);
        }
        if (result.isEmpty()) return 0;

        if (entry.containsAttributes(InlayProperty.ENCHANT)) {
            InlayUtil.transferEnchantments(result, inlay);
        }

        inlay.shrink(count);
        base.setCount(0);

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

    /** 在镶嵌台正下方生成若干份掉落物；超出单堆上限时按堆叠上限分堆。 */
    private void dropItem(ItemStack stack, int count) {
        if (level == null || level.isClientSide || stack.isEmpty() || count <= 0) return;
        int max = stack.getMaxStackSize();
        while (count > 0) {
            int batch = Math.min(count, max);
            dropItem(stack.copyWithCount(batch));
            count -= batch;
        }
    }

    /** 在镶嵌台下方生成一个掉落物实体（位于收集 AABB 外，不会被自身 tick 吸回）。 */
    private void dropItem(ItemStack stack) {
        if (level == null || level.isClientSide || stack.isEmpty()) return;
        BlockPos pos = getBlockPos();
        // 显式 0 初速：5 参构造会用随机数生成初始冲量导致产物乱飞；
        // 生成点取台体正下方无碰撞处，避免被方块碰撞推出。
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