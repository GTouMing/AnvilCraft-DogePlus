package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.ICaptured;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin implements ICaptured {

    @Unique
    private static final EntityDataAccessor<Boolean> doge_plus$CAPTURED;
    @Unique
    private static final EntityDataAccessor<Integer> doge_plus$INDEX;


    @Override
    public boolean doge_plus$isCaptured() {
        return ((ItemEntity) (Object) this).getEntityData().get(doge_plus$CAPTURED);
    }

    @Override
    public void doge_plus$setCaptured(boolean captured) {
        ((ItemEntity) (Object) this).getEntityData().set(doge_plus$CAPTURED, captured);
    }

    @Override
    public int doge_plus$getIndex() {

        return ((ItemEntity) (Object) this).getEntityData().get(doge_plus$INDEX);
    }

    @Override
    public void doge_plus$setIndex(int index) {
        ((ItemEntity) (Object) this).getEntityData().set(doge_plus$INDEX, index);
    }

    @Inject(method = "defineSynchedData", at = @At(value = "TAIL"))
    protected void defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
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
    }

    static {
         doge_plus$CAPTURED = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.BOOLEAN);
         doge_plus$INDEX = SynchedEntityData.defineId(ItemEntity.class, EntityDataSerializers.INT);
    }
}
