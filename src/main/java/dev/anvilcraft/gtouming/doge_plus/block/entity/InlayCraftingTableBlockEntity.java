package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
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
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.armortrim.ArmorTrim;
import net.minecraft.world.item.armortrim.TrimMaterial;
import net.minecraft.world.item.armortrim.TrimMaterials;
import net.minecraft.world.item.armortrim.TrimPattern;
import net.minecraft.world.item.armortrim.TrimPatterns;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 镶合台方块实体：单个物品槽（镶满的基材）。
 *
 * <p>铁砧砸击时按 {@link InlayCraftingRecipe} 匹配整件基材（物品 + 全部镶孔按序一致），
 * 命中则一次耗尽整叠：按份生成产物与空镶嵌基材，均落在台体下方，供漏斗/溜槽等自动化收集；
 * 产物区域位于收集区之外，不会被自身 tick 吸回。</p>
 */
public class InlayCraftingTableBlockEntity extends BlockEntity {

    public static final int SLOT_BASE = 0;
    public static final int SLOT_COUNT = 1;

    /** 原版「可纹饰装备」物品标签。 */
    private static final TagKey<Item> TRIM_ARMOR =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trimmable_armor"));
    /** 原版「纹饰材料」物品标签。 */
    private static final TagKey<Item> TRIM_MATERIALS =
            TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("trim_materials"));

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

    // ==================== 构造 ====================

    public InlayCraftingTableBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        slots[SLOT_BASE] = ItemStack.EMPTY;
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
            ItemStack result = itemHandler.insertItem(SLOT_BASE, item, false);
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
     * 按份数在台体下方生成产物与空镶嵌基材（同镶嵌台整叠加工语义）。
     *
     * @return 是否完成了一次批量合成
     */
    public boolean processCrafting() {
        if (level == null || level.isClientSide) return false;

        ItemStack base = slots[SLOT_BASE];
        if (base.isEmpty()) return false;

        InlayCraftingRecipe recipe = findRecipe(base);
        if (recipe == null) return false;

        ItemStack result = recipe.derivesResult() ? deriveResult(base) : new ItemStack(recipe.getResultItem());
        if (result.isEmpty()) return false;

        int count = base.getCount();
        ItemStack consumed = base.copy();
        slots[SLOT_BASE] = ItemStack.EMPTY;

        dropItem(result, count);
        dropItem(recipe.emptyBaseOf(consumed), count);

        syncToClient();
        playEffects(level);
        return true;
    }

    /**
     * 纹饰类镶合（配方无固定 result）：给「可纹饰装备」镶孔内的装备施加由模具与
     * 材料镶孔推导出的纹饰组件，产物 = 装备副本（+ 纹饰）。
     */
    private ItemStack deriveResult(ItemStack template) {
        if (level == null) return ItemStack.EMPTY;

        ItemStack armor = ItemStack.EMPTY;
        ItemStack material = ItemStack.EMPTY;
        for (InlayEntry entry : InlayUtil.getInlays(template)) {
            if (entry.isEmpty()) continue;
            ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(entry.id()));
            if (stack.isEmpty()) return ItemStack.EMPTY;
            if (stack.is(TRIM_ARMOR) && armor.isEmpty()) armor = stack;
            else if (stack.is(TRIM_MATERIALS)) material = stack;
        }
        if (armor.isEmpty() || material.isEmpty()) return ItemStack.EMPTY;

        var access = level.registryAccess();
        Optional<Holder.Reference<TrimPattern>> pattern = TrimPatterns.getFromTemplate(access, template);
        Optional<Holder.Reference<TrimMaterial>> trimMaterial = TrimMaterials.getFromIngredient(access, material);
        if (pattern.isEmpty() || trimMaterial.isEmpty()) return ItemStack.EMPTY;

        ItemStack result = armor.copyWithCount(1);
        result.set(DataComponents.TRIM, new ArmorTrim(trimMaterial.get(), pattern.get()));
        return result;
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
