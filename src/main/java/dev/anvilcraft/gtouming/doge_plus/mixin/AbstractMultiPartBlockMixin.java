package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.block.IMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractMultiPartBlock.class)
public abstract class AbstractMultiPartBlockMixin implements IMultiPartBlock {
    @Shadow
    public abstract BlockPos getMainPartPos(BlockPos pos, BlockState state);

    @Unique
    public BlockPos doge_plus$getMainPos(BlockPos pos, BlockState state) {
        return getMainPartPos(pos, state);
    }
}
