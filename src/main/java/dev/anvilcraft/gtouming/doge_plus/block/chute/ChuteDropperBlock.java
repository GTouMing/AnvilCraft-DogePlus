package dev.anvilcraft.gtouming.doge_plus.block.chute;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.ChuteDropperBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ChuteDropperBlock extends AbstractChuteDropperBlock {
    public static final MapCodec<ChuteDropperBlock> CODEC = simpleCodec(ChuteDropperBlock::new);

    public ChuteDropperBlock(Properties properties) {
        super(properties);
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