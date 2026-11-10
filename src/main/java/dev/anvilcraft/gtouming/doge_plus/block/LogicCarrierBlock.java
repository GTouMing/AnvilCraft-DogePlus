package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierKind;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.FaceRole;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 逻辑载体：没有任何镶孔，六个面的门类型由玩法即时编程。
 *
 * <p>门类型来自三处：铁砧锤长按右键的轮盘、门链放置流程，以及既有的门设定值调整。
 * 它们都写进 {@link dev.anvilcraft.gtouming.doge_plus.data.BlockInlays} 的 {@code faces}，
 * 因此与普通载体共用同一套逻辑网络与渲染。</p>
 *
 * <p>放置时六个面都未编程，也就是「无外显镶嵌」：既不接收红石输入也不输出红石信号；
 * 某个面一旦被编程即视为外显，可正常收发。</p>
 */
public class LogicCarrierBlock extends InlayCarrierBlock {

    public static final MapCodec<LogicCarrierBlock> CODEC = simpleCodec(LogicCarrierBlock::new);

    public LogicCarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public CarrierKind kind() {
        return CarrierKind.LOGIC;
    }

    @Override
    public boolean acceptsInlays() {
        return false;
    }

    @Override
    public boolean acceptsMode(FaceMode mode) {
        return mode == FaceMode.NONE || mode.isRemote() || mode.isRedstone();
    }

    @Override
    public FaceMode inputMode() {
        return FaceMode.INPUT;
    }

    @Override
    public FaceMode outputMode() {
        return FaceMode.OUTPUT;
    }

    @Override
    public List<FaceMode> wheelModes() {
        return FaceMode.redstoneValues();
    }

    @Override
    public FaceRole faceRole(FaceMode mode) {
        if (mode == FaceMode.NONE) return FaceRole.EMPTY;
        return mode == FaceMode.INPUT || mode == FaceMode.DELAY_INPUT_GATE
                ? FaceRole.INPUT
                : FaceRole.OUTPUT;
    }

    @Override
    public void clearFaceRuntime(Level level, BlockPos pos, Set<Direction> faces) {
        for (Direction face : faces) {
            LogicGateNetworkManager.clearFaceSignal(level, pos, face);
        }
    }

    @Override
    protected VoxelShape visualCoreShape(BlockState state) {
        return state.getValue(HUB) ? LOGIC_CORE : union(coreCrossBoxes(state));
    }

    @Override
    protected VoxelShape[] shapeCache() {
        return LOGIC_SHAPE_CACHE;
    }

    @Override
    protected Map<Integer, VoxelShape> portShapeCache() {
        return LOGIC_PORT_SHAPE_CACHE;
    }

    @Override
    protected MapCodec<? extends InlayCarrierBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InlayCarrierBlockEntity(ModBlockEntities.LOGIC_CARRIER.get(), pos, state);
    }
}
