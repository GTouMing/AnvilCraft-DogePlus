package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.dubhe.anvilcraft.entity.MagnetizedNodeEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 访问 {@link MagnetizedNodeEntity} 私有字段 {@code blockState}，供 {@code DogeNodeEntity} 以正确实体类型构造时设置。
 */
@Mixin(MagnetizedNodeEntity.class)
public interface MagnetizedNodeEntityAccessor {

    @Accessor("blockState")
    void anvilcraft$setBlockState(BlockState state);
}
