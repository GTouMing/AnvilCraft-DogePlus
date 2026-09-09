package dev.anvilcraft.gtouming.doge_plus.block.chute;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.ChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ChuteDispenserBlock extends AbstractChuteDispenserBlock {
    public static final MapCodec<ChuteDispenserBlock> CODEC = simpleCodec(ChuteDispenserBlock::new);
    /** 非磁力溜槽不可朝上，与 anvilcraft 原版溜槽一致。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;

    public ChuteDispenserBlock(Properties properties) {
        super(properties);
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
        return new ChuteDispenserBlockEntity(ModBlockEntities.CHUTE_DISPENSER.get(), pos, state);
    }
}