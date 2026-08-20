package dev.anvilcraft.gtouming.doge_plus.data;

import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * 镶嵌发电组件 - 实现 IPowerProducer，直接接入铁砧工艺电网。
 * 功率 512 kW，范围 2 格。
 */
public class InlayPowerProducer implements IPowerProducer {

    private static final int POWER = 512;
    private static final int RANGE = 2;

    private final Level level;
    private final BlockPos pos;
    private final AABB shape;
    private PowerGrid grid;

    public InlayPowerProducer(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
        this.shape = new AABB(pos).inflate(RANGE);
    }

    @Override
    public AABB getShape() {
        return shape;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    @Nullable
    public PowerGrid getGrid() {
        return grid;
    }

    @Override
    public Level getCurrentLevel() {
        return level;
    }

    @Override
    public int getOutputPower() {
        return POWER;
    }
}