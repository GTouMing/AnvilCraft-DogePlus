package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.client.renderer.entity.FlyingAnvilEntityRenderer;
import dev.anvilcraft.gtouming.doge_plus.entity.FlyingAnvilEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import net.minecraft.world.entity.MobCategory;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModEntities {

    public static final EntityEntry<? extends FlyingAnvilEntity> FLYING_ANVIL = REGISTRUM
            .<FlyingAnvilEntity>entity("flying_anvil", FlyingAnvilEntity::new, MobCategory.MISC)
            .properties(it -> it.sized(0.98F, 0.98F).clientTrackingRange(80).updateInterval(1))
            .renderer(() -> FlyingAnvilEntityRenderer::new)
            .register();

    public static void register() {
    }
}
