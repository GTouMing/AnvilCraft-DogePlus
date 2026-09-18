package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateStateData;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 镶嵌载体：固定 6 个镶孔，槽位下标 i ↔ 面 {@code Direction.values()[i]}。
 *
 * <p>镶嵌材料存于 {@link BlockInlayManager}（按坐标的 SavedData），因此现有方块镶嵌属性
 * （永恒 / 耐火 / 磁性 / 发生器 / 方向逻辑门等）自动生效，并在破坏/放置时随掉落物往返。</p>
 *
 * <p>方块状态只保留静态几何与通道的通电外观：每面一个 3 值 {@link CarrierWire}
 * （{@code none / unpowered / powered}）+ {@code hub}（中心体是否绘制），共 {@code 3^6 × 2 = 1458} 个状态。
 * 通道与中心体由 multipart 绘制（逐顶点 AO / 方向光照与普通方块一致）；棱角需要逻辑门的
 * 「输入门收到信号 / 消耗输入面的门」分类，无法用低基数方块状态表达，由
 * {@code InlayCarrierRenderer} 依据客户端镶嵌数据逐棱绘制。</p>
 */
public class InlayCarrierBlock extends Block implements EntityBlock {

    public static final int SOCKETS = 6;

    /** 各面的通道状态（静态几何 + 通电外观，决定通道用普通还是 powered 模型）。 */
    public static final EnumProperty<CarrierWire> DOWN = EnumProperty.create("down", CarrierWire.class);
    public static final EnumProperty<CarrierWire> UP = EnumProperty.create("up", CarrierWire.class);
    public static final EnumProperty<CarrierWire> NORTH = EnumProperty.create("north", CarrierWire.class);
    public static final EnumProperty<CarrierWire> SOUTH = EnumProperty.create("south", CarrierWire.class);
    public static final EnumProperty<CarrierWire> WEST = EnumProperty.create("west", CarrierWire.class);
    public static final EnumProperty<CarrierWire> EAST = EnumProperty.create("east", CarrierWire.class);

    /**
     * 是否绘制中心体（core）。
     *
     * <p>派生属性：镶孔恰好构成「一对对面」时为 false（此时两条通道直接贯通，不需要中心体），
     * 其余情况为 true。multipart 只能叠加、无法表达「非」，故把它算成一个状态属性。</p>
     */
    public static final BooleanProperty HUB = BooleanProperty.create("hub");

    private static final Map<Direction, EnumProperty<CarrierWire>> FACE_PROPERTIES = Map.of(
            Direction.DOWN, DOWN,
            Direction.UP, UP,
            Direction.NORTH, NORTH,
            Direction.SOUTH, SOUTH,
            Direction.WEST, WEST,
            Direction.EAST, EAST
    );

    public InlayCarrierBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction), CarrierWire.NONE);
        }
        this.registerDefaultState(state.setValue(HUB, true));
    }

    /** 方向 → 该面通道状态属性。 */
    public static EnumProperty<CarrierWire> property(Direction direction) {
        return FACE_PROPERTIES.get(direction);
    }

    /** 该槽位是否已镶嵌：非空占位即视为已镶嵌（任何材料都伸出通道）。 */
    public static boolean isActiveSlot(List<InlayEntry> inlays, int slot) {
        if (slot < 0 || slot >= inlays.size()) return false;
        return !inlays.get(slot).isEmpty();
    }

    /** 把镶嵌写入指定槽位（不足处补空占位）。 */
    public static List<InlayEntry> withSlot(List<InlayEntry> existing, int slot, InlayEntry entry) {
        List<InlayEntry> list = new ArrayList<>(existing);
        while (list.size() <= slot) {
            list.add(InlayEntry.nulls());
        }
        list.set(slot, entry);
        return list;
    }

    /**
     * 是否绘制中心体：镶孔恰好构成「一对对面」时不绘制（两条通道直接贯通）。
     * 空、单面、三面及以上、任意含夹角的组合都绘制。
     */
    public static boolean shouldDrawHub(List<InlayEntry> inlays) {
        List<Direction> active = new ArrayList<>(2);
        for (Direction direction : Direction.values()) {
            if (isActiveSlot(inlays, direction.ordinal())) active.add(direction);
        }
        if (active.size() != 2) return true;
        return active.getFirst().getOpposite() != active.get(1);
    }

    /** 从存储数据重算 6 个面的镶嵌布尔 + hub，并写入各面信号强度。 */
    public static void refreshState(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock)) return;
        BlockInlays data = BlockInlayManager.get(level, pos);
        List<InlayEntry> inlays = data.inlays();
        BlockState newState = state;
        int[] signals = new int[Direction.values().length];
        int[] runtime = new int[Direction.values().length];
        LogicGateStateData gateState = LogicGateStateData.get(level);
        for (Direction direction : Direction.values()) {
            boolean active = isActiveSlot(inlays, direction.ordinal());
            LogicGateType type = active ? data.getGateType(direction) : LogicGateType.NONE;
            int signal = active ? signalStrength(level, pos, direction, type) : 0;
            signals[direction.ordinal()] = signal;
            runtime[direction.ordinal()] = active
                    ? runtimeValue(gateState, pos, direction, type, data.getValue(direction))
                    : 0;
            newState = newState.setValue(property(direction), CarrierWire.of(active, signal > 0));
        }
        newState = newState.setValue(HUB, shouldDrawHub(inlays));
        if (newState != state) {
            // 仅几何变化，无需通知邻居：信号变化由逻辑网络自行 notifyNeighbors。
            level.setBlock(pos, newState, Block.UPDATE_CLIENTS);
        }
        if (level.getBlockEntity(pos) instanceof InlayCarrierBlockEntity carrier) {
            // 信号强度与运行时显示值由 BE 同步给客户端 HUD / 渲染器，不进入方块状态。
            boolean changed = carrier.setSignals(signals);
            changed |= carrier.setRuntime(runtime);
            if (changed) {
                level.sendBlockUpdated(pos, newState, newState, Block.UPDATE_CLIENTS);
            }
        }
    }

    /** 该面的运行时显示值：计数门为已计数、延时门为已计时（其余为 0）。 */
    private static int runtimeValue(
            @Nullable LogicGateStateData stateData, BlockPos pos, Direction direction, LogicGateType type, int value) {
        if (stateData == null) return 0;
        return switch (type) {
            case COUNTER_GATE -> stateData.getCount(pos, direction);
            case DELAY_GATE -> {
                int remaining = stateData.getRemaining(pos, direction);
                yield remaining > 0 ? Math.max(0, value - remaining) : 0;
            }
            default -> 0;
        };
    }

    /** 该面当前信号强度：输入门为收到的值，其余逻辑门为输出值，非门面为 0。 */
    private static int signalStrength(Level level, BlockPos pos, Direction direction, LogicGateType type) {
        if (type == LogicGateType.NONE) return 0;
        if (type == LogicGateType.INPUT) return LogicGateNetworkManager.peekInput(level, pos, direction);
        return LogicGateNetworkManager.peekOutput(level, pos, direction);
    }

    @Override
    protected MapCodec<? extends InlayCarrierBlock> codec() {
        return simpleCodec(InlayCarrierBlock::new);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InlayCarrierBlockEntity(ModBlockEntities.INLAY_CARRIER.get(), pos, state);
    }

    /**
     * 允许原版红石粉与铁砧工艺红石导线把载体视为可连接对象，从而真正接入载体。
     *
     * <p>vanilla 红石粉的 {@code getConnectingSide} 与铁砧工艺红石导线的 {@code canAttachTo}
     * 都经由 {@code BlockState#canRedstoneConnectTo} 调用本方法；返回 {@code true} 后二者会
     * 向载体所在方向绘制连线并开放该端口的信号交换。</p>
     *
     * <p>只有<b>已镶嵌</b>的面才允许连接：{@code direction} 是「连线 → 载体」的方向
     * （见铁砧工艺 {@code canAttachTo} 传入的切线），载体朝向连线的面为其反向，
     * 该面没有镶嵌（没有通道/端口）时返回 {@code false}，避免空面被红石粉/导线错误连上。
     * {@code direction} 为 {@code null} 时表示原版斜下/下方探测，无法对应具体面，保持允许连接。</p>
     */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (direction == null) return true;
        return state.getValue(property(direction.getOpposite())).isInlaid();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        List<InlayEntry> inlays = InlayUtil.getInlays(context.getItemInHand());
        for (Direction direction : Direction.values()) {
            state = state.setValue(property(direction),
                    CarrierWire.of(isActiveSlot(inlays, direction.ordinal()), false));
        }
        return state.setValue(HUB, shouldDrawHub(inlays));
    }

    // ==================== 形状 ====================

    /** 中心体。 */
    private static final VoxelShape CORE_SHAPE =
            Shapes.box(5 / 16.0, 5 / 16.0, 5 / 16.0, 11 / 16.0, 11 / 16.0, 11 / 16.0);

    /** 朝北的通道（canonical，其余面由旋转得到）。 */
    private static final AABB WIRE_NORTH = new AABB(6 / 16.0, 6 / 16.0, 0.0, 10 / 16.0, 10 / 16.0, 8 / 16.0);

    /** 上-北棱角件的两块（canonical）。 */
    private static final AABB CORNER_MAIN = new AABB(7 / 16.0, 10 / 16.0, 4 / 16.0, 9 / 16.0, 12 / 16.0, 6 / 16.0);
    private static final AABB CORNER_BEVEL =
            new AABB(6.875 / 16.0, 10.001 / 16.0, 3.875 / 16.0, 9.125 / 16.0, 12.125 / 16.0, 5.999 / 16.0);

    /** 各面的通道形状，下标同 {@link Direction#ordinal()}。 */
    private static final VoxelShape[] WIRE_SHAPES = new VoxelShape[6];

    /** 一条棱（相邻两面）与其角件形状。 */
    private record Edge(Direction a, Direction b, VoxelShape shape) {
    }

    private static final List<Edge> EDGE_SHAPES = new ArrayList<>();

    /** 形状缓存：下标 = hub 位（bit6）+ 六面布尔位。 */
    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[128];

    static {
        // 通道：与 blockstate 同一套旋转（上/下仅用 x，其余仅用 y）
        WIRE_SHAPES[Direction.NORTH.ordinal()] = Shapes.create(WIRE_NORTH);
        WIRE_SHAPES[Direction.EAST.ordinal()] = Shapes.create(rotate(WIRE_NORTH, 0, 90, 0));
        WIRE_SHAPES[Direction.SOUTH.ordinal()] = Shapes.create(rotate(WIRE_NORTH, 0, 180, 0));
        WIRE_SHAPES[Direction.WEST.ordinal()] = Shapes.create(rotate(WIRE_NORTH, 0, 270, 0));
        WIRE_SHAPES[Direction.UP.ordinal()] = Shapes.create(rotate(WIRE_NORTH, 270, 0, 0));
        WIRE_SHAPES[Direction.DOWN.ordinal()] = Shapes.create(rotate(WIRE_NORTH, 90, 0, 0));

        // 角件：8 条水平棱（x 先 y 后）+ 4 条竖棱（z 先 y 后），与 blockstate 对应
        corner(Direction.UP, Direction.NORTH, 0, 0, 0);
        corner(Direction.UP, Direction.EAST, 0, 90, 0);
        corner(Direction.UP, Direction.SOUTH, 0, 180, 0);
        corner(Direction.UP, Direction.WEST, 0, 270, 0);
        corner(Direction.DOWN, Direction.SOUTH, 180, 0, 0);
        corner(Direction.DOWN, Direction.WEST, 180, 90, 0);
        corner(Direction.DOWN, Direction.NORTH, 180, 180, 0);
        corner(Direction.DOWN, Direction.EAST, 180, 270, 0);
        corner(Direction.NORTH, Direction.EAST, 0, 0, 90);
        corner(Direction.SOUTH, Direction.EAST, 0, 90, 90);
        corner(Direction.SOUTH, Direction.WEST, 0, 180, 90);
        corner(Direction.NORTH, Direction.WEST, 0, 270, 90);
    }

    private static void corner(Direction a, Direction b, int xRot, int yRot, int zRot) {
        VoxelShape shape = Shapes.or(
                Shapes.create(rotate(CORNER_MAIN, xRot, yRot, zRot)),
                Shapes.create(rotate(CORNER_BEVEL, xRot, yRot, zRot)));
        EDGE_SHAPES.add(new Edge(a, b, shape));
    }

    /** 按 blockstate 同款顺序旋转一个盒：先 z、再 x、后 y（仅 90 的整数倍）。 */
    private static AABB rotate(AABB box, int xRot, int yRot, int zRot) {
        AABB box1 = box;
        if (zRot != 0) box1 = rotateZ(box1, zRot);
        if (xRot != 0) box1 = rotateX(box1, xRot);
        if (yRot != 0) box1 = rotateY(box1, yRot);
        return box1;
    }

    /** z 旋转：(x,y,z) → (y, 1-x, z)。 */
    private static AABB rotateZ(AABB b, int rot) {
        return switch (rot) {
            case 90 -> new AABB(b.minY, 1 - b.maxX, b.minZ, b.maxY, 1 - b.minX, b.maxZ);
            case 180 -> new AABB(1 - b.maxX, 1 - b.maxY, b.minZ, 1 - b.minX, 1 - b.minY, b.maxZ);
            case 270 -> new AABB(1 - b.maxY, b.minX, b.minZ, 1 - b.minY, b.maxX, b.maxZ);
            default -> b;
        };
    }

    /** x 旋转：(x,y,z) → (x, z, 1-y)。 */
    private static AABB rotateX(AABB b, int rot) {
        return switch (rot) {
            case 90 -> new AABB(b.minX, b.minZ, 1 - b.maxY, b.maxX, b.maxZ, 1 - b.minY);
            case 180 -> new AABB(b.minX, 1 - b.maxY, 1 - b.maxZ, b.maxX, 1 - b.minY, 1 - b.minZ);
            case 270 -> new AABB(b.minX, 1 - b.maxZ, b.minY, b.maxX, 1 - b.minZ, b.maxY);
            default -> b;
        };
    }

    /** y 旋转：(x,y,z) → (1-z, y, x)。 */
    private static AABB rotateY(AABB b, int rot) {
        return switch (rot) {
            case 90 -> new AABB(1 - b.maxZ, b.minY, b.minX, 1 - b.minZ, b.maxY, b.maxX);
            case 180 -> new AABB(1 - b.maxX, b.minY, 1 - b.maxZ, 1 - b.minX, b.maxY, 1 - b.minZ);
            case 270 -> new AABB(b.minZ, b.minY, 1 - b.maxX, b.maxZ, b.maxY, 1 - b.minX);
            default -> b;
        };
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = state.getValue(HUB) ? 64 : 0;
        for (Direction direction : Direction.values()) {
            if (state.getValue(property(direction)).isInlaid()) index |= 1 << direction.ordinal();
        }
        VoxelShape cached = SHAPE_CACHE[index];
        if (cached == null) {
            cached = buildShape(state);
            SHAPE_CACHE[index] = cached;
        }
        return cached;
    }

    /** 当前状态下可见部件的并集。 */
    private static VoxelShape buildShape(BlockState state) {
        VoxelShape shape = state.getValue(HUB) ? CORE_SHAPE : Shapes.empty();
        for (Direction direction : Direction.values()) {
            if (state.getValue(property(direction)).isInlaid()) {
                shape = Shapes.or(shape, WIRE_SHAPES[direction.ordinal()]);
            }
        }
        for (Edge edge : EDGE_SHAPES) {
            if (state.getValue(property(edge.a())).isInlaid() && state.getValue(property(edge.b())).isInlaid()) {
                shape = Shapes.or(shape, edge.shape());
            }
        }
        return shape;
    }

    /**
     * 按射线命中的模型部件判定目标面（用于交互与提示）。
     *
     * <p>直接按方块面判定时，从侧面看到的通道会被误判成侧面的镶孔——例如朝北伸出的通道，
     * 站在东边看过去命中的却是通道的东侧面。这里改为对各部件单独做射线检测：</p>
     * <ul>
     *   <li>命中通道 → 该通道对应的面；</li>
     *   <li>命中棱角 → 两个相邻面中更正对射线的那一面；</li>
     *   <li>命中中心区域（中心体所在范围，含对侧镶嵌时中心体隐藏的情况）或未命中任何部件
     *       → {@code null}，由调用方回退到方块面。</li>
     * </ul>
     *
     * <p>中心区域必须回退到方块面：存在对侧镶嵌时中心体隐藏、两条通道在中间贯通成一条线段，
     * 若该区域仍按部件判定就会一直选到通道面，导致缺通道的那一面永远无法新增镶嵌。</p>
     *
     * @param from 射线起点（世界坐标）
     * @param to   射线终点（世界坐标）
     */
    @Nullable
    public static Direction pickFace(BlockState state, BlockPos pos, Vec3 from, Vec3 to) {
        Direction best = null;
        double bestDistance = Double.MAX_VALUE;

        for (Direction direction : Direction.values()) {
            if (!state.getValue(property(direction)).isInlaid()) continue;
            BlockHitResult hit = WIRE_SHAPES[direction.ordinal()].clip(from, to, pos);
            if (hit == null) continue;
            double distance = hit.getLocation().distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = direction;
            }
        }

        Vec3 ray = to.subtract(from);
        for (Edge edge : EDGE_SHAPES) {
            if (!state.getValue(property(edge.a())).isInlaid() || !state.getValue(property(edge.b())).isInlaid()) {
                continue;
            }
            BlockHitResult hit = edge.shape().clip(from, to, pos);
            if (hit == null) continue;
            double distance = hit.getLocation().distanceToSqr(from);
            if (distance < bestDistance) {
                // 取法线更背向射线的那个面，即玩家更正对着的一面。
                bestDistance = distance;
                best = ray.dot(Vec3.atLowerCornerOf(edge.a().getNormal()))
                        <= ray.dot(Vec3.atLowerCornerOf(edge.b().getNormal())) ? edge.a() : edge.b();
            }
        }

        // 中心区域无论中心体是否绘制都按方块面判定（对侧镶嵌时中心体隐藏，该区域被通道贯通）。
        BlockHitResult centerHit = CORE_SHAPE.clip(from, to, pos);
        if (centerHit != null && centerHit.getLocation().distanceToSqr(from) < bestDistance) {
            best = null;
        }
        return best;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction direction : Direction.values()) {
            builder.add(property(direction));
        }
        builder.add(HUB);
    }
}
