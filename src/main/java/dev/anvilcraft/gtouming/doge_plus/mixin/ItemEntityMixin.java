package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.entity.ICaptured;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static net.minecraft.network.syncher.SynchedEntityData.*;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin extends Entity implements ICaptured {

    @Unique
    private static final EntityDataAccessor<Boolean> doge_plus$CAPTURED;
    @Unique
    private static final EntityDataAccessor<Integer> doge_plus$INDEX;

    public ItemEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public boolean doge_plus$isCaptured() {
        return this.getEntityData().get(doge_plus$CAPTURED);
    }

    @Override
    public void doge_plus$setCaptured(boolean captured) {
        this.getEntityData().set(doge_plus$CAPTURED, captured);
    }

    @Override
    public int doge_plus$getIndex() {
        return this.getEntityData().get(doge_plus$INDEX);
    }

    @Override
    public void doge_plus$setIndex(int index) {
        this.getEntityData().set(doge_plus$INDEX, index);
    }

    @Inject(method = "defineSynchedData", at = @At(value = "TAIL"))
    protected void defineSynchedData(Builder builder, CallbackInfo ci) {
        builder.define(doge_plus$CAPTURED, false).define(doge_plus$INDEX, 0);
    }

    /**
     * 重定向 move 调用，被捕获时执行自定义移动
     */
    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/item/ItemEntity;move(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void anvilcraft$redirectMove(ItemEntity itemEntity, MoverType moverType, Vec3 vec3) {
        if (!doge_plus$isCaptured()) itemEntity.move(moverType, vec3);
        //move方法中热方块点燃原油锅的必要检测
        if (!this.isRemoved()) this.tryCheckInsideBlocks();
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void onTickRemove(CallbackInfo ci) {
        if (!this.doge_plus$isCaptured()) return;
        if (this.level().getEntitiesOfClass(MagnetizedNodeEntity.class,
                new AABB(this.position(), this.position()).inflate(0.6)).isEmpty()) {
            this.doge_plus$setCaptured(false);
            ((ItemEntity) Util.cast(this)).setNoPickUpDelay();
        }
    }

    static {
         doge_plus$CAPTURED = defineId(ItemEntity.class, EntityDataSerializers.BOOLEAN);
         doge_plus$INDEX = defineId(ItemEntity.class, EntityDataSerializers.INT);
    }
}
