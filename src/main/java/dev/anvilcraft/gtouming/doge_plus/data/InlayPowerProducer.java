package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * 镶嵌发电组件 - 实现 IPowerProducer，直接接入铁砧工艺电网。
 * 功率取配置 {@code generatorPower}（默认 512 kW），范围 2 格。
 */
public class InlayPowerProducer implements IPowerProducer {

    private static final int RANGE = 2;

    private final Level level;
    private final BlockPos pos;
    private final AABB shape;
    /** 创建时的镶嵌数据快照；同一坐标重新镶嵌后旧 producer 据此判定失效。 */
    private final BlockInlays inlays;
    private PowerGrid grid;

    public InlayPowerProducer(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
        this.shape = new AABB(pos).inflate(RANGE);
        this.inlays = BlockInlayManager.get(level, pos);
    }

    @Override
    public AABB getShape() {
        return shape;
    }

    /** 创建时的镶嵌数据是否仍是当前位置的数据。 */
    public boolean isCurrent() {
        return BlockInlayManager.hasProperty(level, pos, InlayProperty.GENERATOR)
                && this.inlays == BlockInlayManager.get(level, pos);
    }

    @Override
    public PowerComponentType getComponentType() {
        // 方块/镶嵌数据已经不存在时让异步入队项失效，避免拆掉后仍被重新加入电网。
        return isCurrent() ? PowerComponentType.PRODUCER : PowerComponentType.INVALID;
    }

    @Override
    public BlockPos getPos() {
        return pos;
    }

    @Override
    public int getRange() {
        return RANGE;
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
        return AnvilCraftDogePlus.CONFIG.generatorPower;
    }
}