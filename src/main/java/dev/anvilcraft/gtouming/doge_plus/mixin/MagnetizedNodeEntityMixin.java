package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.ICaptured;
import dev.anvilcraft.gtouming.doge_plus.entity.CapturedItemsProvider;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(MagnetizedNodeEntity.class)
public abstract class MagnetizedNodeEntityMixin implements CapturedItemsProvider {
    @Unique
    private final List<ItemEntity> doge_plus$capturedItems = new ArrayList<>();

    // ==================== CapturedItemsProvider ====================

    @Override
    public void doge_plus$add(ItemEntity entity, int index) {
        entity.noPhysics = true;
        entity.setNoGravity(true);
        entity.setUnlimitedLifetime();
        entity.setNeverPickUp();
        entity.setDeltaMovement(Vec3.ZERO);
        ((ICaptured) entity).doge_plus$setIndex(index);
        ((ICaptured) entity).doge_plus$setCaptured(true);
        doge_plus$capturedItems.add(entity);
    }

    @Override
    public void doge_plus$remove(Predicate<ItemEntity> predicate) {
        doge_plus$capturedItems.removeIf(entity -> {
            if (predicate.test(entity)) {
                entity.noPhysics = false;
                ((ICaptured) entity).doge_plus$setCaptured(false);
                entity.setNoPickUpDelay();
                entity.setNoGravity(false);
                return true;
            }
            return false;
        });
    }

    @Override
    public void doge_plus$captureOrMerge(ItemEntity entity, Level level, Vec3 pos) {
        var stack = entity.getItem();
        if (stack.isEmpty()) return;

        // 1. 先尝试合并到现有物品
        for (ItemEntity existing : doge_plus$capturedItems) {
            if (ItemEntity.areMergable(existing.getItem(), stack)) {
                int maxStack = existing.getItem().getMaxStackSize();
                int currentCount = existing.getItem().getCount();
                int space = maxStack - currentCount;

                if (space > 0) {
                    int toMerge = Math.min(stack.getCount(), space);
                    existing.getItem().grow(toMerge);
                    stack.shrink(toMerge);
                    if (stack.isEmpty()) {
                        entity.kill();
                        return;
                    }
                }
            }
        }

        // 2. 检查容量
        if (doge_plus$capturedItems.size() >= 8) return;

        // 3. 通过 add 方法标记并加入列表
        entity.setPos(pos);
        doge_plus$add(entity, doge_plus$capturedItems.size());
    }

    @Override
    public void doge_plus$releaseToPlayer(Player player) {
        for (ItemEntity item : doge_plus$capturedItems) {
            item.setPos(player.getX(), player.getY() + 0.5, player.getZ());
        }
        doge_plus$remove(entity -> true);
    }

    @Override
    public void doge_plus$release() {
        doge_plus$remove(entity -> true);
    }

    // ==================== Tick ====================

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        MagnetizedNodeEntity self = (MagnetizedNodeEntity) (Object) this;
        Level level = self.level();
        if (level.isClientSide) return;

        if (!self.isAlive() || self.isRemoved()) {
            doge_plus$release();
            return;
        }

        AABB box = new AABB(self.position(), self.position()).inflate(0.6);
        List<ItemEntity> nearby = level.getEntitiesOfClass(ItemEntity.class, box);

        // 移除：不存活或不在AABB中
        doge_plus$remove(entity -> !entity.isAlive() || !nearby.contains(entity));

        if (doge_plus$capturedItems.size() >= 8) return;

        // 捕获：在AABB中、未捕获、不在列表中、存活
        for (ItemEntity item : nearby) {
            if (!item.isAlive()) continue;
            if (((ICaptured) item).doge_plus$isCaptured()) continue;
            if (doge_plus$capturedItems.contains(item)) continue;

            doge_plus$captureOrMerge(item, level, self.position());
            if (doge_plus$capturedItems.size() >= 8) break;
        }
    }
}
