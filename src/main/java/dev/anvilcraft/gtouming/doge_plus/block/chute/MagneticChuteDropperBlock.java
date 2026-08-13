package dev.anvilcraft.gtouming.doge_plus.block.chute;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.MagneticChuteDropperBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
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
        return new MagneticChuteDropperBlockEntity(ModBlockEntities.MAGNETIC_CHUTE_DROPPER.get(), pos, state);
    }
}