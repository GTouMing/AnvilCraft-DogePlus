package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.sound.DogePlusSoundHelper;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
    @Inject(method = "play", at = @At(value = "HEAD"), cancellable = true)
    private void onSoundPlay(SoundInstance sound, CallbackInfo ci) {
        if (DogePlusSoundHelper.INSTANCE.shouldMute(
                sound.getLocation(),
                new Vec3(sound.getX(), sound.getY(), sound.getZ())
        )) {
            ci.cancel();
        }
    }
}
