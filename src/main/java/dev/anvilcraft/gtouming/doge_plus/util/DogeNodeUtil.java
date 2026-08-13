package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.entity.DogeNodeEntity;
import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 放置 Doge 节点：复制 AnvilCraft {@code MagnetUtil.placeMagnetizedNode} 的逻辑，
 * 但生成 {@link DogeNodeEntity}，并用 {@code getEntitiesOfClass(MagnetizedNodeEntity.class, ...)} 兼容子类节点。
 */
public class DogeNodeUtil {

    public static InteractionResult placeDogeNode(Item item, UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockPos pos = context.getClickedPos();

        BlockState blockState = level.getBlockState(pos);
        if (blockState.isAir()) return InteractionResult.PASS;
        double maxY = blockState.getCollisionShape(level, pos).max(Direction.Axis.Y, 0.5, 0.5);
        // 点击处无碰撞面（如空气/无碰撞方块）则不放置
        if (maxY == 0) return InteractionResult.PASS;

        // 移除该位置已有的（Doge/磁化）节点
        for (MagnetizedNodeEntity entity : level.getEntitiesOfClass(
            MagnetizedNodeEntity.class,
            new AABB(pos).setMaxY(pos.getY() + 1.1),
            EntitySelector.NO_SPECTATORS
        )) {
            if (entity.blockPos.equals(pos)) {
                entity.discard();
                player.getCooldowns().addCooldown(item, 5);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        Vec3 nodePos = pos.getBottomCenter().add(0, maxY, 0);
        level.addFreshEntity(new DogeNodeEntity(level, nodePos, pos));
        player.getCooldowns().addCooldown(item, 5);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
