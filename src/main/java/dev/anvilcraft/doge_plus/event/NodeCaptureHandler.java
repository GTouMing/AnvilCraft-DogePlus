package dev.anvilcraft.doge_plus.event;

import dev.anvilcraft.doge_plus.entity.NodeSupportEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public class NodeCaptureHandler {
    private static final ResourceLocation MAGNETIZED_NODE_ENTITY_ID = ResourceLocation.parse("anvilcraft:magnetized_node");

    @SubscribeEvent
    public void onEntityJoin(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        Level level = entity.level();
        if (level.isClientSide()) return;

        ResourceLocation entityId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        if (!MAGNETIZED_NODE_ENTITY_ID.equals(entityId)) return;

        Vec3 pos = entity.position();
        spawnSupportIfMissing(level, pos);
    }

    private static void spawnSupportIfMissing(Level level, Vec3 pos) {
        if (findSupportEntity(level, pos) != null) return;
        var support = new NodeSupportEntity(level, pos);
        level.addFreshEntity(support);
    }

    private static NodeSupportEntity findSupportEntity(Level level, Vec3 pos) {
        var aabb = new AABB(pos, pos).inflate(0.1);
        var existing = level.getEntitiesOfClass(NodeSupportEntity.class, aabb);
        return existing.isEmpty() ? null : existing.iterator().next();
    }
}
