package dev.anvilcraft.gtouming.doge_plus.api;

import dev.dubhe.anvilcraft.api.sound.ISoundEventListener;

public class SoundTransformer {
    public static ISoundEventListener asSoundListener(Object obj) {
        if (obj instanceof ISoundEventListener) {
            return (ISoundEventListener) obj;
        }
        return null;
    }
}