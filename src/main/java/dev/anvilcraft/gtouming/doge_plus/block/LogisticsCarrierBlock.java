package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierKind;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.FaceRole;
import dev.anvilcraft.gtouming.doge_plus.transfer.ItemTransferNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 物流载体：没有任何镶孔，六个面的物品搬运角色（存入 / 取出）由玩法即时编程。
 *
 * <p>与 {@link LogicCarrierBlock} 平行：那边编程红石门类型、走逻辑门网络，这边编程物品搬运角色、
 * 走 {@link dev.anvilcraft.gtouming.doge_plus.transfer.ItemTransferNetworkManager} 的物品传输网。
 * 两者共用轮廓、通道与棱角的几何，但棱角规则相反——物流在<b>两个邻面都没有编程</b>时才画棱
 * （把没开通的边封上壳）。</p>
 *
 * <p>放置时六个面都未编程：既不进也不出，只显示外壳。</p>
 */
public class LogisticsCarrierBlock extends InlayCarrierBlock {

    public static final MapCodec<LogisticsCarrierBlock> CODEC = simpleCodec(LogisticsCarrierBlock::new);

    public LogisticsCarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public CarrierKind kind() {
        return CarrierKind.LOGISTICS;
    }

    @Override
    public boolean acceptsInlays() {
        return false;
    }

    @Override
    public boolean acceptsMode(FaceMode mode) {
        return mode == FaceMode.NONE || mode.isRemote() || mode.isTransfer();
    }

    @Override
    public FaceMode inputMode() {
        return FaceMode.EXTRACT;
    }

    @Override
    public FaceMode outputMode() {
        return FaceMode.INSERT;
    }

    @Override
    public List<FaceMode> wheelModes() {
        return FaceMode.transferValues();
    }

    @Override
    public FaceRole faceRole(FaceMode mode) {
        return switch (mode) {
            case NONE -> FaceRole.EMPTY;
            case EXTRACT -> FaceRole.INPUT;
            case INSERT, CRAFT_INSERT -> FaceRole.OUTPUT;
            default -> FaceRole.OTHER;
        };
    }

    @Override
    public boolean supportsEndpoint(Level level, BlockPos neighborPos, Direction side) {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, side) != null) return true;
        AABB box = new AABB(neighborPos).setMaxY(neighborPos.getY() + 1.1);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box, Entity::isAlive)) {
            if (entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side) != null) return true;
        }
        return false;
    }

    @Override
    public boolean supportsTransferSettings(FaceMode mode) {
        return mode == FaceMode.INSERT || mode == FaceMode.CRAFT_INSERT;
    }

    @Override
    public boolean showsRedstoneSignal() {
        return false;
    }

    @Override
    public boolean shouldDrawHub(List<Direction> active) {
        // 中心体是被外壳包住的内芯，没有「对穿时隐藏」的语义。
        return true;
    }

    @Override
    protected boolean showsEdge(BlockState state, Edge edge) {
        // 物流把没开通的边封上壳：两面都未编程才画棱。
        return !state.getValue(property(edge.a())).isInlaid()
                && !state.getValue(property(edge.b())).isInlaid();
    }

    @Override
    public List<Edge> edgeShapes() {
        return LOGISTICS_EDGE_SHAPES;
    }

    @Override
    public CarrierWire[] edgeStates() {
        return new CarrierWire[] {CarrierWire.NONE};
    }

    @Override
    public boolean showsCoreOnFace() {
        return true;
    }

    @Override
    protected VoxelShape visualCoreShape(BlockState state) {
        return LOGISTICS_CORE;
    }

    @Override
    protected VoxelShape[] wireShapes() {
        return LOGISTICS_WIRE_SHAPES;
    }

    @Override
    protected VoxelShape wireShape(BlockState state, Direction direction) {
        return state.getValue(property(direction)) == CarrierWire.REMOTE
                ? LOGISTICS_REMOTE_WIRE_SHAPES[direction.ordinal()]
                : LOGISTICS_WIRE_SHAPES[direction.ordinal()];
    }

    @Override
    protected VoxelShape portWireShape(BlockState state, Direction face) {
        return PORT_LOGISTICS_WIRE_SHAPES[face.ordinal()];
    }

    @Override
    protected VoxelShape[] shapeCache() {
        return LOGISTICS_SHAPE_CACHE;
    }

    @Override
    protected Map<Integer, VoxelShape> portShapeCache() {
        return LOGISTICS_PORT_SHAPE_CACHE;
    }

    @Override
    protected VoxelShape[] collisionShapeCache() {
        return LOGISTICS_COLLISION_SHAPE_CACHE;
    }

    @Override
    protected VoxelShape fixedFrameShape() {
        return LOGISTICS_CORNER_SHAPE;
    }

    @Override
    public void onFacesProgrammed(Level level, BlockPos pos, Set<Direction> faces) {
        // 面属性就是物品传输网的拓扑：重建网络并重算货源记录。
        ItemTransferNetworkManager.topologyChanged(level, pos);
    }

    @Override
    public void onChannelChanged(Level level, BlockPos pos) {
        ItemTransferNetworkManager.topologyChanged(level, pos);
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        // 面是物品搬运角色而不是红石端口，红石粉 / 导线不该连上它。
        return false;
    }

    /**
     * 相邻方块也是镶嵌载体、且本面与它朝向本方的面都开通了通道时，两侧通道对接：剔除本面通道端盖，
     * 免得两层端盖在方块交界处重叠闪烁。只要有一侧未开通通道，端盖就保留。
     *
     * <p>任一侧是远程面时不再对接剔除：远程面看到的应该是 port 变体的法兰正面，
     * 不能因为两边都「已镶嵌」就把法兰也剔掉。</p>
     *
     * <p>通道模型本就给端盖标了 {@code cullface}，只是载体的形状不是满方，默认遮挡判定不会把它当实心，
     * 所以相邻两个载体之间从不触发。相邻的是普通方块时交给默认判定。</p>
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getBlock() instanceof InlayCarrierBlock
                && state.getValue(property(side)).isInlaid()
                && adjacentBlockState.getValue(property(side.getOpposite())).isInlaid()
                && state.getValue(property(side)) != CarrierWire.REMOTE
                && adjacentBlockState.getValue(property(side.getOpposite())) != CarrierWire.REMOTE;
    }

    @Override
    protected MapCodec<? extends InlayCarrierBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return super.newBlockEntity(pos, state);
    }
}
