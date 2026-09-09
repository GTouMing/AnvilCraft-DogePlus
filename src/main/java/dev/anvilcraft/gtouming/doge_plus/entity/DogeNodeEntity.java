package dev.anvilcraft.gtouming.doge_plus.entity;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.injection.entity.IItemEntityExtension;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * Doge 节点：吸附并捕获物品的节点实体（不继承前置磁化节点，自行实现等价行为）。
 *
 * <p>放置在方块顶面，两层能力：</p>
 * <ol>
 *   <li><b>磁化吸附</b>：把邻近方块碰撞体内可吸附的掉落物拉到自身中心（同前置磁化节点）；</li>
 *   <li><b>物品捕获</b>：把吸到自身位置的物品收纳为最多 {@value #MAX_CAPTURED} 组，绕节点展示，
 *       经 {@link #itemHandler} 供玩家/溜槽/漏斗存取。</li>
 * </ol>
 *
 * <p><b>方块自适应</b>：附着方块改变时按顶面承载高度调整自身位置——完整方块变为不完整方块
 * （顶面降低）时下沉；反之（顶面升高）上抬；完整↔完整不变化；方块变为空气或承载顶面
 * 完全消失（如活板门打开）时节点消失并返还物品。</p>
 */
public class DogeNodeEntity extends Entity {

    private static final EntityDataAccessor<BlockPos> DATA_BLOCK_POS =
            SynchedEntityData.defineId(DogeNodeEntity.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockState> DATA_BLOCK_STATE =
            SynchedEntityData.defineId(DogeNodeEntity.class, EntityDataSerializers.BLOCK_STATE);

    public static final int MAX_CAPTURED = 8;
    private static final double CAPTURE_RADIUS = 0.5;
    private static final int MAX_STACK_SIZE = 64;
    /** 自适应检测周期（tick）。 */
    private static final int ADAPT_CHECK_PERIOD = 5;

    /** 附着方块坐标（public 以兼容原 {@code MagnetizedNodeEntity#blockPos} 用法）。 */
    public BlockPos blockPos = BlockPos.ZERO;
    /** 附着方块状态（同步给客户端）。 */
    private BlockState blockState = Blocks.AIR.defaultBlockState();

    /**
     * -- GETTER --
     * 被捕获物品列表（渲染用）。
     * -- GETTER --
     * 被捕获物品列表（仅服务端维护，供清理/释放逻辑使用）。

     */
    @Getter
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
                ItemEntity entity = new ItemEntity(level(), position().x, position().y, position().z, remaining);
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
        this.noPhysics = true;
        this.setInvulnerable(true);
    }

    public DogeNodeEntity(Level level, Vec3 pos, BlockPos blockPos) {
        this(ModEntities.DOGE_NODE.get(), level);
        this.blockPos = blockPos.immutable();
        this.blockState = level.getBlockState(blockPos);
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
        this.getEntityData().set(DATA_BLOCK_POS, this.blockPos);
        this.getEntityData().set(DATA_BLOCK_STATE, this.blockState);
    }

    // ==================== 基础实体属性 ====================

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK_POS, BlockPos.ZERO).define(DATA_BLOCK_STATE, Blocks.AIR.defaultBlockState());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
        this.blockPos = NbtUtils.readBlockPos(compoundTag, "BlockPos").orElse(BlockPos.ZERO);
        this.blockState = NbtUtils.readBlockState(
                this.level().holderLookup(Registries.BLOCK), compoundTag.getCompound("BlockState"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
        compoundTag.put("BlockPos", NbtUtils.writeBlockPos(this.blockPos));
        compoundTag.put("BlockState", NbtUtils.writeBlockState(this.blockState));
    }

    @Override
    protected AABB makeBoundingBox() {
        // 节点本体为 1/16 格小方块
        return EntityDimensions.scalable(1 / 16F, 1 / 16F).makeBoundingBox(this.position());
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    // ==================== tick ====================

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (this.isRemoved()) return;

        // 方块自适应：每 ADAPT_CHECK_PERIOD tick 检测附着方块顶面变化
        if (this.tickCount % ADAPT_CHECK_PERIOD == 0) {
            this.adaptToBlock();
            if (this.isRemoved()) return;
        }

        // 吸附层：把方块碰撞体内可吸附的掉落物拉到节点中心
        AABB aabb = new AABB(
                blockPos.getX() - 0.01, blockPos.getY() - 0.01, blockPos.getZ() - 0.01,
                blockPos.getX() + 1.01, blockPos.getY() + 1.01, blockPos.getZ() + 1.01);
        level().getEntities(EntityType.ITEM, aabb, IItemEntityExtension::anvilcraft$isAdsorbable)
                .forEach(entity -> {
                    entity.teleportTo(position().x, position().y, position().z);
                    entity.setDeltaMovement(Vec3.ZERO);
                });

        // 捕获层：清理失效，捕获上方掉落物
        capturedItems.removeIf(item ->
                !((ICaptured) Util.cast(item)).doge_plus$isCaptured()
                        || item.isRemoved()
                        || !position().equals(item.position()));
        if (isFull()) return;
        captureNearby(new AABB(above(), above()).inflate(CAPTURE_RADIUS));
    }

    // ==================== 方块自适应 ====================

    /**
     * 依据附着方块顶面承载高度调整节点位置。
     *
     * <p>顶面高度取方块碰撞体在 (0.5, 0.5) 处的 Y 最大值：完整方块为 1.0，
     * 台阶/半砖等不完整方块小于 1.0；形状在该处为空时返回 -Infinity（如空气、打开的活板门）。
     * 比较上一检测周期的承载高度：</p>
     * <ul>
     *   <li>当前方块为空气，或承载顶面完全消失（如活板门打开）→ 节点消失（返还物品）；</li>
     *   <li>承载高度升高（不完整→完整）→ 节点上抬；</li>
     *   <li>承载高度降低（完整→不完整）→ 节点下沉；</li>
     *   <li>同为完整（1.0↔1.0）→ 位置不变。</li>
     * </ul>
     */
    private void adaptToBlock() {
        if (this.level().isClientSide) return;
        BlockState current = this.level().getBlockState(blockPos);
        if (current.isAir() && !blockState.isAir()) {
            // 附着方块消失：节点移除，释放捕获物品
            this.removeNodeAndRelease();
            return;
        }

        double maxY = current.getCollisionShape(this.level(), blockPos).max(Direction.Axis.Y, 0.5, 0.5);
        double prevMaxY = this.blockState.getCollisionShape(this.level(), blockPos).max(Direction.Axis.Y, 0.5, 0.5);

        // 承载顶面完全消失：碰撞形状在该处为空时 maxY 为 -Infinity（如活板门打开、方块变空气），
        // 节点失去支撑 → 移除并返还捕获物品（<=0 同时覆盖 0 与 -Infinity）
        if (maxY <= 0) {
            this.removeNodeAndRelease();
            return;
        }

        boolean full = maxY >= 1.0;
        boolean wasFull = prevMaxY >= 1.0;
        boolean sameShape = !full && !wasFull && Math.abs(maxY - prevMaxY) < 1.0E-4;
        // 完整↔完整、或承载面高度不变（同为不完整且等高的）→ 不做位置改变
        if ((full && wasFull) || sameShape) {
            // 仍刷新记录的方块状态（完整方块换成另一种完整方块等）
            if (!this.blockState.equals(current)) {
                this.blockState = current;
                this.getEntityData().set(DATA_BLOCK_POS, blockPos);
                this.getEntityData().set(DATA_BLOCK_STATE, current);
            }
            return;
        }

        double targetY = blockPos.getY() + maxY;
        if (Math.abs(this.getY() - targetY) > 1.0E-4) {
            this.setPos(this.getX(), targetY, this.getZ());
            this.xo = this.getX();
            this.yo = this.getY();
            this.zo = this.getZ();
            // 已捕获物品跟随节点移动（保持与节点重合）
            for (ItemEntity item : capturedItems) {
                if (!item.isRemoved()) item.setPos(this.getX(), this.getY(), this.getZ());
            }
        }

        this.blockState = current;
        this.getEntityData().set(DATA_BLOCK_POS, blockPos);
        this.getEntityData().set(DATA_BLOCK_STATE, current);
    }

    // ==================== 物品捕获 ====================

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

    /** 把一个掉落物登记为被捕获（绕节点展示、不可拾取、无限存活）。 */
    public void add(ItemEntity entity) {
        entity.setUnlimitedLifetime();
        entity.setNeverPickUp();
        ((ICaptured) entity).doge_plus$setIndex(capturedItems.size());
        ((ICaptured) entity).doge_plus$setCaptured(true);
        capturedItems.add(entity);
    }

    /**
     * 供渲染使用的最新被捕获物品列表（按槽位 index 排序）。
     *
     * <p>服务端直接返回内部列表；客户端不维护 {@code capturedItems}，改为扫描节点邻域中
     * 标记为「已捕获」的 {@link ItemEntity}，并按槽位 index 排序以稳定环状布局。</p>
     *
     * <p>为避免误扫：① 相邻节点捕获的物品（在本节点邻域内）② 被吸附到节点中心但尚未
     * 捕获的滞留物——候选必须同时满足「{@code ICaptured} 已捕获」与「位置与本节点中心
     * 重合」两个条件。被捕获物品由服务端 {@code setPos} 到节点中心且移动被禁用，
     * 客户端位置即为节点中心（误差极小），故用紧容差即可精确归属。</p>
     */
    public List<ItemEntity> getRenderItems() {
        if (this.level().isClientSide) {
            AABB area = new AABB(this.position(), this.position()).inflate(0.6);
            List<ItemEntity> found = this.level().getEntitiesOfClass(ItemEntity.class, area,
                    e -> !e.isRemoved()
                            && ((ICaptured) e).doge_plus$isCaptured()
                            && e.position().distanceToSqr(this.position()) < 0.01);
            found.sort(java.util.Comparator.comparingInt(e -> ((ICaptured) e).doge_plus$getIndex()));
            return found;
        }
        return capturedItems;
    }

    // ==================== 对外接口 ====================

    public void releaseToPlayer(Player player) {
        if (level().isClientSide) return;

        Inventory inventory = player.getInventory();
        for (ItemEntity item : capturedItems) {
            ((ICaptured) Util.cast(item)).doge_plus$setCaptured(false);
            item.setNoPickUpDelay();
            ItemStack stack = item.getItem();
            // 能放入背包的直接放入，移除对应的掉落物实体
            if (inventory.add(stack) || stack.isEmpty()) {
                item.discard();
            } else {
                // 背包放不下 → 剩余部分以掉落物形式出现在玩家身上（随后被拾取）
                item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            }
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