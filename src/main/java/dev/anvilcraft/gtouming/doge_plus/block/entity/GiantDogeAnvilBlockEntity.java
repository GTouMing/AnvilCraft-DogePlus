package dev.anvilcraft.gtouming.doge_plus.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

@Getter
public class GiantDogeAnvilBlockEntity extends BlockEntity {

    private float rotationAngle = 0f;

    public GiantDogeAnvilBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static <T extends BlockEntity> void clientTick(Level ignoredLevel, BlockPos ignoredPos, BlockState ignoredState, T be) {
        if (!(be instanceof GiantDogeAnvilBlockEntity da)) return;
        da.rotationAngle += 1.0f;
        if (da.rotationAngle > 360) da.rotationAngle -= 360;
    }

}