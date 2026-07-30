package dev.anvilcraft.gtouming.doge_plus.api.sound;

import dev.dubhe.anvilcraft.api.sound.ISoundEventListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class DogePlusSoundHelper {
    public static final DogePlusSoundHelper INSTANCE = new DogePlusSoundHelper();
    private final List<ISoundEventListener> listeners = new CopyOnWriteArrayList<>();

    public void register(ISoundEventListener listener) {
        listeners.add(listener);
    }

    public void unregister(ISoundEventListener listener) {
        listeners.remove(listener);
    }

    public boolean shouldMute(ResourceLocation sound, Vec3 pos) {
        return listeners.stream().anyMatch(it -> it.shouldMute(sound, pos));
    }
}
