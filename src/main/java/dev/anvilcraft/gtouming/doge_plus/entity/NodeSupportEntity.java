package dev.anvilcraft.gtouming.doge_plus.entity;

import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NodeSupportEntity extends Entity {
    private static final ResourceLocation MAGNETIZED_NODE_ENTITY_ID = ResourceLocation.parse("anvilcraft:magnetized_node");
    public static final String TAG_CAPTURED = "ac_captured";

    private static final EntityDataAccessor<CompoundTag> DATA_CAPTURED_UUIDS =
            SynchedEntityData.defineId(NodeSupportEntity.class, EntityDataSerializers.COMPOUND_TAG);
    @Getter
    private final List<ItemEntity> capturedItemEntities = new ArrayList<>();
    private int tickCounter = 0;

    public NodeSupportEntity(EntityType<? extends NodeSupportEntity> type, Level level) {
        super(type, level);
    }

    public NodeSupportEntity(Level level, Vec3 pos) {
        this(ModEntities.NODE_SUPPORT.get(), level);
        this.setPos(pos);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CAPTURED_UUIDS, new CompoundTag());
    }

    public CompoundTag getCapturedUUIDsTag() {
        return entityData.get(DATA_CAPTURED_UUIDS);
    }

    private void updateClientCapturedEntities() {
        CompoundTag tag = getCapturedUUIDsTag();
        capturedItemEntities.clear();
        if (!tag.contains("UUIDs")) return;

        ListTag list = tag.getList("UUIDs", 10);
        AABB aabb = this.getBoundingBox().inflate(1.0);
        List<ItemEntity> nearby = this.level().getEntitiesOfClass(ItemEntity.class, aabb);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("UUID")) {
                UUID uuid = entry.getUUID("UUID");
                for (ItemEntity item : nearby) {
                    if (item.getUUID().equals(uuid)) {
                        item.getPersistentData().putBoolean(TAG_CAPTURED, true);
                        capturedItemEntities.add(item);
                        break;
                    }
                }
            }
        }
    }

    private void syncCapturedUUIDs() {
        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (ItemEntity entity : capturedItemEntities) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("UUID", entity.getUUID());
            list.add(entry);
        }
        tag.put("UUIDs", list);
        entityData.set(DATA_CAPTURED_UUIDS, tag);
    }

    private void setEntityCaptured(ItemEntity itemEntity) {
        itemEntity.setPos(position());
        itemEntity.setNeverPickUp();
        itemEntity.setDeltaMovement(Vec3.ZERO);
        markCaptured(itemEntity);
    }

    private boolean tryToMerge(ItemEntity originEntity) {
        ItemStack originStack = originEntity.getItem();
        for (ItemEntity capturedEntity : capturedItemEntities) {
            ItemStack destinationStack = capturedEntity.getItem();
            if(!ItemEntity.areMergable(destinationStack, originStack)) continue;
            capturedEntity.setItem(ItemEntity.merge(destinationStack, originStack, 64));
            break;
        }
        return originStack.isEmpty();//true则合并成功
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel)) {
            updateClientCapturedEntities();
            return;
        }

        if (!isNodePresent()) {
            releaseAll();
            this.discard();
            return;
        }

        if (++tickCounter % 5 != 0) return;

        // 清理已消失或被移除的实体，并更新合并后的堆栈
        boolean changed = capturedItemEntities.removeIf(entity -> !entity.isAlive());
        if (changed) syncCapturedUUIDs();

        if (capturedItemEntities.size() >= 8) return;

        // 捕获新的实体
        AABB aabb = new AABB(position(), position()).inflate(0.6);
        for (ItemEntity itemEntity : this.level().getEntitiesOfClass(ItemEntity.class, aabb)) {
            if (capturedItemEntities.contains(itemEntity)) continue;
            if (itemEntity.position().distanceTo(position()) > 0.01) continue;

            setEntityCaptured(itemEntity);

            if(tryToMerge(itemEntity)) continue;

            capturedItemEntities.add(itemEntity);

            syncCapturedUUIDs();

            if (capturedItemEntities.size() >= 8) break;
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
        for (ItemEntity itemEntity : capturedItemEntities) {
            unmarkCaptured(itemEntity);
            itemEntity.setNoPickUpDelay();
            itemEntity.setNoGravity(false);
        }
        capturedItemEntities.clear();
        syncCapturedUUIDs();
    }

    public void releaseToPlayer(Player player) {
        for (ItemEntity itemEntity : capturedItemEntities) {
            unmarkCaptured(itemEntity);
            itemEntity.setPos(player.getX(), player.getY() + 0.5, player.getZ());
            itemEntity.setNoPickUpDelay();
        }
        capturedItemEntities.clear();
        syncCapturedUUIDs();
    }

    // ========== NBT 标记工具 ==========

    private void markCaptured(ItemEntity item) {
        CompoundTag data = item.getPersistentData();
        data.putBoolean(TAG_CAPTURED, true);
    }

    private static void unmarkCaptured(ItemEntity item) {
        CompoundTag data = item.getPersistentData();
        data.remove(TAG_CAPTURED);
    }

    @Override
    public boolean isPickable() {
        return true;
    }//可交互

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {}

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) return InteractionResult.SUCCESS;

        ItemStack mainStack = player.getItemInHand(InteractionHand.MAIN_HAND);
        ItemStack offStack = player.getItemInHand(InteractionHand.OFF_HAND);
        ItemStack handStack = mainStack.isEmpty() ? offStack : mainStack;


        if (handStack.isEmpty()) {
            releaseToPlayer(player);
            return InteractionResult.CONSUME;
        }

        if (capturedItemEntities.size() < 8) {
            ItemStack toPlace = player.isShiftKeyDown() ? handStack.copyAndClear() : handStack.split(1);
            var itemEntity = new ItemEntity(EntityType.ITEM, level());
            itemEntity.setItem(toPlace);
            setEntityCaptured(itemEntity);

            if (tryToMerge(itemEntity))
                return InteractionResult.CONSUME;

            level().addFreshEntity(itemEntity);
            syncCapturedUUIDs();
        }
        return InteractionResult.CONSUME;
    }

    // ========== 节点检测 ==========

    private boolean isNodePresent() {
        AABB aabb = new AABB(position(), position()).inflate(0.5);
        for (Entity entity : this.level().getEntities(this, aabb)) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            if (MAGNETIZED_NODE_ENTITY_ID.equals(id)) return true;
        }
        return false;
    }

    // ========== 实体类型 ==========

    public static EntityType<NodeSupportEntity> createType(String id) {
        return EntityType.Builder.<NodeSupportEntity>of(NodeSupportEntity::new, MobCategory.MISC)
                .sized(0.25f, 0.25f)
                .clientTrackingRange(8)
                .updateInterval(Integer.MAX_VALUE)
                .build(id);
    }
}
