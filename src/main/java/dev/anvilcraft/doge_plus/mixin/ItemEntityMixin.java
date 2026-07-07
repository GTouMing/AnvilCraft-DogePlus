package dev.anvilcraft.doge_plus.mixin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

import static dev.anvilcraft.doge_plus.entity.NodeSupportEntity.*;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Unique
    private boolean ac_frozen = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void beforeTick(CallbackInfo ci) {
        @SuppressWarnings("DataFlowIssue") ItemEntity self = (ItemEntity) (Object) this;
        if (self.level().isClientSide) return;

        CompoundTag data = self.getPersistentData();
        boolean captured = data.getBoolean(TAG_CAPTURED);
        if (!captured) {
            ac_frozen = false;
            return;
        }

        Vec3 nodePos = readNodePos(data);
        if (nodePos == null) {
            ac_frozen = false;
            return;
        }

        if (self.position().distanceToSqr(nodePos) > 0.0001) {
            ac_frozen = false;
            return;
        }

        self.setDeltaMovement(Vec3.ZERO);
        self.setNoGravity(true);
        self.hasImpulse = false;
        ac_frozen = true;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void afterTick(CallbackInfo ci) {
        if (!ac_frozen) return;
        ac_frozen = false;

        @SuppressWarnings("DataFlowIssue") ItemEntity self = (ItemEntity) (Object) this;
        resetAge(self);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"), cancellable = true)
    private void onPlayerTouch(Player player, CallbackInfo ci) {
        @SuppressWarnings("DataFlowIssue") ItemEntity self = (ItemEntity) (Object) this;
        CompoundTag data = self.getPersistentData();
        if (!data.getBoolean(TAG_CAPTURED)) return;

        Vec3 nodePos = readNodePos(data);
        if (nodePos != null && self.position().distanceToSqr(nodePos) <= 0.0001) {
            ci.cancel();
        }
    }

    @Unique
    private static Vec3 readNodePos(CompoundTag data) {
        if (!data.contains(TAG_NODE_X) || !data.contains(TAG_NODE_Y) || !data.contains(TAG_NODE_Z)) {
            return null;
        }
        return new Vec3(data.getDouble(TAG_NODE_X), data.getDouble(TAG_NODE_Y), data.getDouble(TAG_NODE_Z));
    }

    @Unique
    private static void resetAge(ItemEntity entity) {
        try {
            Field ageField = ItemEntity.class.getDeclaredField("age");
            ageField.setAccessible(true);
            ageField.setInt(entity, 0);
        } catch (Exception ignored) {
        }
    }
}
