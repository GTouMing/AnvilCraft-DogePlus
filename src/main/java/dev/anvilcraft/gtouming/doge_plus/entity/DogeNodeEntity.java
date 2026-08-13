package dev.anvilcraft.gtouming.doge_plus.entity;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import dev.anvilcraft.gtouming.doge_plus.init.ModEntities;
import dev.anvilcraft.gtouming.doge_plus.mixin.MagnetizedNodeEntityAccessor;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Doge 节点：继承 {@link MagnetizedNodeEntity} 的基类吸附（吸附 isAdsorbable 物品），
 * 并在本类自实现物品捕获列表与释放（不依赖任何修改原版的 mixin）。
 */
public class DogeNodeEntity extends MagnetizedNodeEntity {

    private static final int MAX_CAPTURED = 8;
    private static final double CAPTURE_RADIUS = 0.6;

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
        ((MagnetizedNodeEntityAccessor) this).anvilcraft$setBlockState(level.getBlockState(blockPos));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;

        capturedItems.removeIf(item -> !((ICaptured) Util.cast(item)).doge_plus$isCaptured());
        if (capturedItems.size() >= MAX_CAPTURED) return;

        // 节点正上方0.6，半径0.6的box
        AABB box = new AABB(above(), above()).inflate(CAPTURE_RADIUS);
        List<ItemEntity> nearby = level().getEntitiesOfClass(ItemEntity.class, box);

        if (capturedItems.size() >= 8) return;

        // 捕获：在AABB中、未捕获、不在列表中、存活
        for (ItemEntity item : nearby) {
            if (capturedItems.contains(item)) continue;
            captureOrMerge(item, position());
            if (capturedItems.size() >= 8) break;
        }
    }

    private Vec3 above() {
        return this.position().add(new Vec3(0, DogeNodeEntity.CAPTURE_RADIUS, 0));
    }

    /**
     * 把捕获的物品释放给玩家（返回物品逻辑由 doge 节点持有）。
     */
    public void releaseToPlayer(Player player) {
        if (this.level().isClientSide) return;
        for (ItemEntity item : capturedItems) {
            ((ICaptured) Util.cast(item)).doge_plus$setCaptured(false);
            item.setNoPickUpDelay();
            item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
        }
        capturedItems.clear();
    }

    /**
     * 移除节点并把捕获物品释放（避免物品悬浮卡死）。
     */
    public void removeNodeAndRelease() {
        if (this.level().isClientSide) return;
        for (ItemEntity item : capturedItems) {
            ((ICaptured) Util.cast(item)).doge_plus$setCaptured(false);
            item.setNoPickUpDelay();
        }
        capturedItems.clear();
        this.discard();
    }

    /**
     * 右键节点实体：把捕获的物品释放给玩家。
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        this.releaseToPlayer(player);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }


    public void add(ItemEntity entity, int index) {
        entity.setUnlimitedLifetime();
        entity.setNeverPickUp();
        ((ICaptured) entity).doge_plus$setIndex(index);
        ((ICaptured) entity).doge_plus$setCaptured(true);
        capturedItems.add(entity);
    }

    public void captureOrMerge(ItemEntity entity, Vec3 pos) {
        var stack = entity.getItem();
        if (stack.isEmpty()) return;

        // 1. 先尝试合并到现有物品
        for (ItemEntity existing : capturedItems) {
            if (ItemEntity.areMergable(existing.getItem(), stack)) {
                int maxStack = existing.getItem().getMaxStackSize();
                int currentCount = existing.getItem().getCount();
                int space = maxStack - currentCount;

                if (space > 0) {
                    int toMerge = Math.min(stack.getCount(), space);
                    existing.getItem().grow(toMerge);
                    stack.shrink(toMerge);
                }
            }
        }

        // 2. 检查容量
        if (capturedItems.size() >= 8) return;

        // 3. 通过 add 方法标记并加入列表
        entity.setPos(pos);
        add(entity, capturedItems.size());
    }

}
