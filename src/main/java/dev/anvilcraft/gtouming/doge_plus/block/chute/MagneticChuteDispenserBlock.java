package dev.anvilcraft.gtouming.doge_plus.block.chute;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.MagneticChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class MagneticChuteDispenserBlock extends AbstractChuteDispenserBlock {
    public static final MapCodec<MagneticChuteDispenserBlock> CODEC = simpleCodec(MagneticChuteDispenserBlock::new);

    public MagneticChuteDispenserBlock(Properties properties) {
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
        return new MagneticChuteDispenserBlockEntity(ModBlockEntities.MAGNETIC_CHUTE_DISPENSER.get(), pos, state);
    }
}