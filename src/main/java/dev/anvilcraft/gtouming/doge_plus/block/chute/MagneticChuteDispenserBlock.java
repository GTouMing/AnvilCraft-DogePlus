package dev.anvilcraft.gtouming.doge_plus.block.chute;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.MagneticChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class MagneticChuteDispenserBlock extends AbstractChuteDispenserBlock {
    public static final MapCodec<MagneticChuteDispenserBlock> CODEC = simpleCodec(MagneticChuteDispenserBlock::new);
    /** 磁力变体允许朝上，使用完整六向属性。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public MagneticChuteDispenserBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected boolean canFaceUp() {
        return true;
    }

    @Override
    protected DirectionProperty facingProperty() {
        return FACING;
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