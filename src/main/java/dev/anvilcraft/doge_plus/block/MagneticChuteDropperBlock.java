package dev.anvilcraft.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.doge_plus.block.entity.MagneticChuteDropperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MagneticChuteDropperBlock extends AbstractChuteDropperBlock {
    public static final MapCodec<MagneticChuteDropperBlock> CODEC = simpleCodec(MagneticChuteDropperBlock::new);

    public MagneticChuteDropperBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canFaceUp() {
        return true;
    }

    @Override
    public MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagneticChuteDropperBlockEntity(pos, state);
    }
}