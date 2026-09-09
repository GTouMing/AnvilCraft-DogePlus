package dev.anvilcraft.gtouming.doge_plus.block.chute;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.ChuteDropperBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import org.jetbrains.annotations.Nullable;

public class ChuteDropperBlock extends AbstractChuteDropperBlock {
    public static final MapCodec<ChuteDropperBlock> CODEC = simpleCodec(ChuteDropperBlock::new);
    /** 非磁力溜槽不可朝上，与 anvilcraft 原版溜槽一致。 */
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;

    public ChuteDropperBlock(Properties properties) {
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
        return new ChuteDropperBlockEntity(ModBlockEntities.CHUTE_DROPPER.get(), pos, state);
    }
}