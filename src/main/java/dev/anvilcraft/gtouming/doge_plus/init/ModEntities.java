package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.client.renderer.entity.FlyingAnvilEntityRenderer;
import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import dev.anvilcraft.gtouming.doge_plus.entity.FlyingAnvilEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.EntityEntry;
import dev.dubhe.anvilcraft.client.renderer.entity.MagnetizedNodeEntityRenderer;
import net.minecraft.world.entity.MobCategory;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModEntities {

    public static final EntityEntry<? extends FlyingAnvilEntity> FLYING_ANVIL = REGISTRUM
            .<FlyingAnvilEntity>entity("flying_anvil", FlyingAnvilEntity::new, MobCategory.MISC)
            .properties(it -> it.sized(0.98F, 0.98F).clientTrackingRange(80).updateInterval(1))
            .renderer(() -> FlyingAnvilEntityRenderer::new)
            .register();

    /** Doge 节点：复用前置模组的磁化节点渲染器。 */
    public static final EntityEntry<? extends DogeNodeEntity> DOGE_NODE = REGISTRUM
            .<DogeNodeEntity>entity("doge_node", DogeNodeEntity::new, MobCategory.MISC)
            .properties(it -> it.sized(0.25F, 0.25F).clientTrackingRange(80).updateInterval(1))
            .renderer(() -> MagnetizedNodeEntityRenderer::new)
            .register();

    public static void register() {
    }
}
