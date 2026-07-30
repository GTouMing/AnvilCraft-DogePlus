package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.dubhe.anvilcraft.api.sound.ISoundEventListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public class ItemStackMixin implements ISoundEventListener {
    @Unique
    private AABB doge_plus$range;

    public boolean shouldMute(ResourceLocation sound, Vec3 pos) {
        return MobileSilencer.getMutedSounds(((ItemStack)(Object) this)).contains(sound) && doge_plus$range != null && doge_plus$range.contains(pos);
    }

    @Inject(method = "inventoryTick", at = @At(value = "HEAD"))
    private void onTick(Level level, Entity entity, int inventorySlot, boolean isCurrentItem, CallbackInfo ci) {
        doge_plus$range = AABB.ofSize(Vec3.atCenterOf(entity.getOnPos()), 31, 31, 31);
    }
}
