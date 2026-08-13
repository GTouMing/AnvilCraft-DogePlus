package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import dev.dubhe.anvilcraft.block.entity.BaseChuteBlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * 溜槽（{@link BaseChuteBlockEntity}）输出物品时，若输出方向朝向 doge 节点，
 * 直接将该物品捕获进节点（而不是生成一个掉落物再等节点吸附）。
 */
@Mixin(BaseChuteBlockEntity.class)
public class ChuteDogeNodeMixin {

    @Redirect(
        method = "tick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"
        )
    )
    private boolean anvilcraft$redirectOutputToNode(Level level, Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            List<DogeNodeEntity> nodes = level.getEntitiesOfClass(DogeNodeEntity.class,
                    new AABB(itemEntity.position(), itemEntity.position()).inflate(1.0));
            if (!nodes.isEmpty()) {
                DogeNodeEntity node = nodes.getFirst();
                // 先加入世界（释放时物品才能回到玩家），再直接捕获进节点
                level.addFreshEntity(entity);
                node.captureOrMerge(itemEntity, node.position());
                return true;
            }
        }
        return level.addFreshEntity(entity);
    }
}
