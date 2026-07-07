package dev.anvilcraft.doge_plus.entity;

import dev.anvilcraft.doge_plus.mixin.ItemEntityMixin;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class NodeSupportEntity extends Entity {
    private static final ResourceLocation MAGNETIZED_NODE_ENTITY_ID = ResourceLocation.parse("anvilcraft:magnetized_node");
    public static final String TAG_CAPTURED = "ac_captured";
    public static final String TAG_NODE_X = "ac_node_x";
    public static final String TAG_NODE_Y = "ac_node_y";
    public static final String TAG_NODE_Z = "ac_node_z";

    private static final EntityDataAccessor<ItemStack> DATA_DISPLAY_ITEM =
            SynchedEntityData.defineId(NodeSupportEntity.class, EntityDataSerializers.ITEM_STACK);

    private Vec3 supportPos;
    private final Set<UUID> capturedItems = new HashSet<>();
    private int tickCounter = 0;

    public NodeSupportEntity(EntityType<? extends NodeSupportEntity> type, Level level) {
        super(type, level);
    }

    public NodeSupportEntity(Level level, Vec3 pos) {
        this(getEntityType(), level);
        this.supportPos = pos;
        this.setPos(pos.x, pos.y, pos.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DISPLAY_ITEM, ItemStack.EMPTY);
    }

    public ItemStack getDisplayItem() {
        return entityData.get(DATA_DISPLAY_ITEM);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (supportPos == null) return;

        if (++tickCounter % 5 != 0) return;

        if (!isNodePresent()) {
            releaseAll();
            this.discard();
            return;
        }

        this.setPos(supportPos.x, supportPos.y, supportPos.z);

        AABB aabb = new AABB(supportPos, supportPos).inflate(0.6);
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, aabb)) {
            UUID id = item.getUUID();
            if (capturedItems.contains(id)) continue;
            markCaptured(item);
            capturedItems.add(id);
            entityData.set(DATA_DISPLAY_ITEM, item.getItem().copy());
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            releaseAll();
        }
        super.remove(reason);
    }

    public void releaseAll() {
        if (capturedItems.isEmpty()) return;
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        for (UUID id : capturedItems) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof ItemEntity item) {
                unmarkCaptured(item);
            }
        }
        capturedItems.clear();
        entityData.set(DATA_DISPLAY_ITEM, ItemStack.EMPTY);
    }

    public void releaseToPlayer(Player player) {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;

        for (UUID id : capturedItems) {
            Entity entity = serverLevel.getEntity(id);
            if (entity instanceof ItemEntity item) {
                unmarkCaptured(item);
                item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
                item.setPickUpDelay(0);
            }
        }
        capturedItems.clear();
        entityData.set(DATA_DISPLAY_ITEM, ItemStack.EMPTY);
    }

    // ========== NBT 标记工具 ==========

    private void markCaptured(ItemEntity item) {
        CompoundTag data = item.getPersistentData();
        data.putBoolean(TAG_CAPTURED, true);
        data.putDouble(TAG_NODE_X, supportPos.x);
        data.putDouble(TAG_NODE_Y, supportPos.y);
        data.putDouble(TAG_NODE_Z, supportPos.z);
    }

    private static void unmarkCaptured(ItemEntity item) {
        CompoundTag data = item.getPersistentData();
        data.remove(TAG_CAPTURED);
        data.remove(TAG_NODE_X);
        data.remove(TAG_NODE_Y);
        data.remove(TAG_NODE_Z);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;
        if (supportPos == null) return InteractionResult.PASS;

        // 如果当前手为空但另一只手有物品，改用那只手
        ItemStack handStack = player.getItemInHand(hand);
        if (handStack.isEmpty()) {
            InteractionHand other = hand == InteractionHand.MAIN_HAND
                    ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack otherStack = player.getItemInHand(other);
            if (!otherStack.isEmpty()) {
                hand = other;
                handStack = otherStack;
            }
        }

        if (!handStack.isEmpty()) {
            // 持有物品右键：放入节点
            ItemStack toPlace = handStack.split(1);
            var itemEntity = new ItemEntity(level(), supportPos.x, supportPos.y, supportPos.z, toPlace);
            itemEntity.setPickUpDelay(Integer.MAX_VALUE);
            itemEntity.setDeltaMovement(0,0,0);
            level().addFreshEntity(itemEntity);
        } else {
            // 空手右键：取回所有物品到身边
            releaseToPlayer(player);
        }
        return InteractionResult.CONSUME;
    }

    // ========== 节点检测 ==========

    private boolean isNodePresent() {
        AABB aabb = new AABB(supportPos, supportPos).inflate(0.5);
        for (Entity entity : this.level().getEntities(this, aabb)) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (MAGNETIZED_NODE_ENTITY_ID.equals(id)) return true;
        }
        return false;
    }

    // ========== 持久化 ==========

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (supportPos != null) {
            tag.putDouble(TAG_NODE_X, supportPos.x);
            tag.putDouble(TAG_NODE_Y, supportPos.y);
            tag.putDouble(TAG_NODE_Z, supportPos.z);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_NODE_X) && tag.contains(TAG_NODE_Y) && tag.contains(TAG_NODE_Z)) {
            supportPos = new Vec3(tag.getDouble(TAG_NODE_X), tag.getDouble(TAG_NODE_Y), tag.getDouble(TAG_NODE_Z));
        }
    }

    // ========== 实体类型 ==========

    private static EntityType<NodeSupportEntity> getEntityType() {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(
                ResourceLocation.parse("anvilcraft_doge_plus:node_support"));
        return (EntityType<NodeSupportEntity>) type;
    }

    public static EntityType<NodeSupportEntity> createType(String id) {
        return EntityType.Builder.<NodeSupportEntity>of(NodeSupportEntity::new, MobCategory.MISC)
                .sized(0.5f, 0.5f)
                .clientTrackingRange(8)
                .updateInterval(Integer.MAX_VALUE)
                .build(id);
    }
}
