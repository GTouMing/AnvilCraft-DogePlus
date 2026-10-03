package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateStateData;
import dev.anvilcraft.gtouming.doge_plus.transfer.ItemTransferNetworkManager;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 镶嵌载体：固定 6 个镶孔，槽位下标 i ↔ 面 {@code Direction.values()[i]}。
 *
 * <p>镶嵌材料存于 {@link BlockInlayManager}（按坐标的 SavedData），因此现有方块镶嵌属性
 * （永恒 / 耐火 / 磁性 / 发生器 / 方向逻辑门等）自动生效，并在破坏/放置时随掉落物往返。</p>
 *
 * <p>方块状态只保留静态几何与通道的通电外观：每面一个 4 值 {@link CarrierWire}
 * （{@code none / unpowered / powered / remote}）+ {@code hub}（中心体是否绘制），共 {@code 4^6 × 2 = 8192} 个状态。
 * 通道、中心体与那批棱角（见 {@link #edgeStates}）都由 multipart 绘制，因此带逐顶点环境光遮蔽、
 * 与普通方块一致。通道与棱件的外观靠模型自己换贴图（未通电 / 通电两套合成材质）。
 * BER 只剩「邻居是不是红石导线」这种逐面布尔属性会把状态数撑爆的部件（连接件），
 * 以及物流的搬运方向指示件。</p>
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

    /** 本方块的面属性是否由镶嵌材料写入；逻辑载体没有镶孔，覆写为 {@code false}。 */
    public boolean acceptsInlays() {
        return true;
    }

    /**
     * 该面是否外显（已启用）：普通载体看镶孔内容，逻辑载体看该面是否已被编程。
     *
     * @param block 该位置的实际方块（用来区分载体变体）
     * @param data  该位置的镶嵌数据；尚无数据时为 {@code null}
     */
    public static boolean isFaceActive(
            Block block, @Nullable BlockInlays data, List<InlayEntry> inlays, Direction direction) {
        if (block instanceof InlayCarrierBlock carrier && !carrier.acceptsInlays()) {
            // 逻辑 / 物流载体没有镶孔：编程（faces 里有非 NONE 的属性）即外显。
            return data != null && data.getFace(direction) != FaceMode.NONE;
        }
        return isActiveSlot(inlays, direction.ordinal());
    }

    /** 把镶嵌写入指定槽位（不足处补空占位）。 */
    public static List<InlayEntry> withSlot(List<InlayEntry> existing, int slot, InlayEntry entry) {
        List<InlayEntry> list = new ArrayList<>(existing);
        while (list.size() <= slot) {
            list.add(InlayEntry.empty());
        }
        list.set(slot, entry);
        return list;
    }

    /**
     * 是否绘制中心体：镶孔恰好构成「一对对面」时不绘制（两条通道直接贯通）。
     * 空、单面、三面及以上、任意含夹角的组合都绘制。
     */
    public static boolean shouldDrawHub(Block block, @Nullable BlockInlays data, List<InlayEntry> inlays) {
        List<Direction> active = new ArrayList<>(2);
        for (Direction direction : Direction.values()) {
            if (isFaceActive(block, data, inlays, direction)) active.add(direction);
        }
        return shouldDrawHub(block, active);
    }

    /** 中心体是否绘制。只有「恰好两个相对的有效面」才被通道贯通而隐藏中心体。 */
    public static boolean shouldDrawHub(Block block, List<Direction> active) {
        // 物流载体的中心体恒渲染：它是被外壳包住的内芯，没有「对穿时隐藏」的语义。
        if (block instanceof LogisticsCarrierBlock) return true;
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
            boolean active = isFaceActive(state.getBlock(), data, inlays, direction);
            FaceMode mode = active ? data.getFace(direction) : FaceMode.NONE;
            int signal = active ? signalStrength(level, pos, direction, mode) : 0;
            signals[direction.ordinal()] = signal;
            runtime[direction.ordinal()] = active
                    ? runtimeValue(gateState, pos, direction, mode, data.getValue(direction))
                    : 0;
            newState = newState.setValue(property(direction), CarrierWire.of(active, signal > 0, mode.isRemote()));
        }
        newState = newState.setValue(HUB, shouldDrawHub(state.getBlock(), data, inlays));
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

    /** 该面的运行时显示值：计数门为已计数、延时门 / 延时输入门为已计时（搬运角色与其余为 0）。 */
    private static int runtimeValue(
            @Nullable LogicGateStateData stateData, BlockPos pos, Direction direction, FaceMode mode, int value) {
        if (stateData == null) return 0;
        return switch (mode) {
            case COUNTER_GATE -> stateData.getCount(pos, direction);
            case DELAY_GATE, DELAY_INPUT_GATE -> {
                int remaining = stateData.getRemaining(pos, direction);
                yield remaining > 0 ? Math.max(0, value - remaining) : 0;
            }
            default -> 0;
        };
    }

    /**
     * 该面当前信号强度：远程面为总线信号，输入门 / 延时输入门为收到的值，
     * 其余逻辑门为输出值，非红石面为 0。
     */
    private static int signalStrength(Level level, BlockPos pos, Direction direction, FaceMode mode) {
        if (mode.isRemote()) return LogicGateNetworkManager.peekRemoteBus(level, pos, direction);
        // 搬运角色不接红石：必须在这里挡掉，否则会误去查逻辑网的输出。
        if (!mode.isRedstone()) return 0;
        // 延时输入门与输入门一样是「读邻居」的输入侧：它不向外输出，外观该显示收到多少而不是输出多少。
        if (mode == FaceMode.INPUT || mode == FaceMode.DELAY_INPUT_GATE) {
            return LogicGateNetworkManager.peekInput(level, pos, direction);
        }
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

    // ==================== 面属性编程 ====================

    /**
     * 编程单个面的方向性属性（{@link FaceMode#NONE} 表示清除该面的编程）。
     *
     * <p>轮盘与门链放置都走这里，保证两条路径行为一致。</p>
     *
     * @return 是否实际发生了改动
     */
    public static boolean programFace(Level level, BlockPos pos, Direction face, FaceMode mode) {
        return programFaces(level, Map.of(pos, Map.of(face, mode)));
    }

    /**
     * 批量编程多个位置的面属性：先把全部数据写入 {@link BlockInlayManager}，再统一刷新外观、
     * 按类别更新网络，避免逐面触发重复的网络重建。
     *
     * <p>只有没有镶孔的载体（逻辑载体 / 物流载体）能被编程，且只能写入与方块类型相符的一类值——
     * 逻辑载体收红石类（各门），物流载体收搬运类（存入 / 取出）；{@link FaceMode#NONE} 两类都收，
     * 用于清除。</p>
     *
     * @param changes 位置 → （面 → 面属性）
     * @return 是否至少写入了一个面
     */
    public static boolean programFaces(Level level, Map<BlockPos, Map<Direction, FaceMode>> changes) {
        if (level.isClientSide) return false;

        List<Map.Entry<BlockPos, BlockState>> touched = new ArrayList<>();
        for (Map.Entry<BlockPos, Map<Direction, FaceMode>> entry : changes.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof InlayCarrierBlock carrier) || carrier.acceptsInlays()) continue;
            if (!acceptsModes(state.getBlock(), entry.getValue())) continue;

            BlockInlays inlays = BlockInlayManager.get(level, pos);
            if (inlays.block() != state.getBlock()) {
                // 该位置还没有镶嵌数据（无镶嵌物的载体放置时不会写入）：以当前方块重建记录，
                // 否则会留下 BlockInlays.nulls() 里 minecraft:air 这样的旧方块字段。
                inlays = BlockInlays.fromInlays(state.getBlock(), inlays.inlays()).withValues(inlays.values());
            }

            BlockInlays updated = inlays;
            Map<Direction, Integer> values = new HashMap<>(inlays.values());
            boolean changed = false;
            for (Map.Entry<Direction, FaceMode> faceEntry : entry.getValue().entrySet()) {
                Direction face = faceEntry.getKey();
                FaceMode mode = faceEntry.getValue();
                if (updated.getFace(face) == mode) continue;
                // withFace 会清掉该面旧的信道标识（远程面回到默认信道，之后由轮盘 / 点按重新设置）。
                updated = updated.withFace(face, mode);
                // 换门后该面的设定值回到默认（旧设定对新门没有意义）；只有「存入」面有物流量与过滤，
                // 换成别的角色（或清除）时一并清掉，避免残留参数在切回存入时突然生效。
                values.remove(face);
                updated = updated.withThroughput(face, BlockInlays.DEFAULT_THROUGHPUT)
                        .withFilter(face, ItemStack.EMPTY);
                changed = true;
            }
            if (!changed) continue;

            BlockInlayManager.put(level, pos, updated.withValues(values));
            if (state.getBlock() instanceof LogicCarrierBlock) {
                // 旧门的输出与运行时状态必须先抹掉：有状态门会一直持有旧输出，否则该面读数不更新。
                for (Direction face : entry.getValue().keySet()) {
                    LogicGateNetworkManager.clearFaceSignal(level, pos, face);
                }
            }
            touched.add(Map.entry(pos, state));
        }

        for (Map.Entry<BlockPos, BlockState> entry : touched) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();
            InlayCarrierBlock.refreshState(level, pos);
            if (state.getBlock() instanceof LogicCarrierBlock) {
                LogicGateNetworkManager.topologyChanged(level, pos);
                level.updateNeighborsAt(pos, state.getBlock());
                // 该面的镶嵌状态变化会改变 canConnectRedstone，邻居红石导线需重建拓扑以接入或断开该面。
                for (Direction face : changes.get(pos).keySet()) {
                    BlockPos wirePos = pos.relative(face);
                    if (level.getBlockState(wirePos).getBlock() instanceof RedstoneWireBlock) {
                        RedstoneWireNetworkManager.topologyChanged(level, wirePos);
                    }
                }
            } else {
                // 面属性就是物品传输网的拓扑：重建网络并重算货源记录。
                ItemTransferNetworkManager.topologyChanged(level, pos);
            }
        }
        return !touched.isEmpty();
    }

    /**
     * 远程门信道改变后重建对应网络并刷新外观。
     *
     * <p>信道就是远程总线 / 远端货源的拓扑：物流载体的远程面归物品传输网，其余（逻辑 / 普通载体）
     * 归逻辑网。</p>
     */
    public static void channelChanged(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        refreshState(level, pos);
        if (state.getBlock() instanceof LogisticsCarrierBlock) {
            ItemTransferNetworkManager.topologyChanged(level, pos);
        } else {
            LogicGateNetworkManager.topologyChanged(level, pos);
            level.updateNeighborsAt(pos, state.getBlock());
        }
    }

    /** 该方块是否接受这一批面属性：红石类只给逻辑载体，搬运类只给物流载体，远程类两者皆可。 */
    private static boolean acceptsModes(Block block, Map<Direction, FaceMode> faces) {
        for (FaceMode mode : faces.values()) {
            if (mode.isRemote()) continue;
            if (mode.isRedstone() && !(block instanceof LogicCarrierBlock)) return false;
            if (mode.isTransfer() && !(block instanceof LogisticsCarrierBlock)) return false;
        }
        return true;
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
     *
     * <p>物流载体整块都不连：它的面是物品搬运角色而不是红石端口，红石粉 / 导线不该连上它，
     * 也不该为它画连线。</p>
     */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        if (state.getBlock() instanceof LogisticsCarrierBlock) return false;
        if (direction == null) return true;
        return state.getValue(property(direction.getOpposite())).isInlaid();
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = this.defaultBlockState();
        List<InlayEntry> inlays = InlayUtil.getInlays(context.getItemInHand());
        for (Direction direction : Direction.values()) {
            // 普通载体的面属性由材料推导：远程材料的面直接落 REMOTE，免得先闪一下普通通道。
            int slot = direction.ordinal();
            boolean active = isActiveSlot(inlays, slot);
            boolean remote = active && BlockInlays.faceModeOf(inlays.get(slot)).isRemote();
            state = state.setValue(property(direction), CarrierWire.of(active, false, remote));
        }
        return state.setValue(HUB, shouldDrawHub(this, null, inlays));
    }

    // ==================== 形状 ====================

    /** 逻辑载体的中心体（与 {@code carrier/logic/core} 模型的外壳一致）。 */
    private static final VoxelShape LOGIC_CORE =
            Shapes.box(5.475 / 16.0, 5.475 / 16.0, 5.475 / 16.0, 10.525 / 16.0, 10.525 / 16.0, 10.525 / 16.0);

    /**
     * 逻辑载体贯通（{@code hub=false}）时的中心体，对应 {@code carrier/logic/core_cross} 模型：
     * 一个中心方块 + 四根短棱管。canonical 的棱管沿 z，另两条贯通轴由 {@link #rotateAll} 转出来
     * （与 blockstate multipart 里那三份用的旋转一致）。
     *
     * <p>按<b>原始盒子</b>列出，不先取并集：中心方块与短棱管有重叠，布尔并会把它拆成互不重叠的碎片
     * （幽灵预览逐个盒子描边时会显得杂乱）。</p>
     */
    private static final List<AABB> CORE_CROSS_Z = List.of(
            new AABB(6 / 16.0, 6 / 16.0, 6 / 16.0, 10 / 16.0, 10 / 16.0, 10 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, 5.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0),
            new AABB(9.5 / 16.0, 5.5 / 16.0, 5.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, 5.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0),
            new AABB(5.5 / 16.0, 9.5 / 16.0, 5.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0));

    /** 东-西贯通、上-下贯通的十字件。 */
    private static final List<AABB> CORE_CROSS_X = rotateAll(CORE_CROSS_Z, 0, 90);
    private static final List<AABB> CORE_CROSS_Y = rotateAll(CORE_CROSS_Z, 270, 0);

    /** 物流载体的中心体：模型是逐面铺在未编程面上的 4×4 薄片，形状取其外接 4×4×4 立方。 */
    private static final VoxelShape LOGISTICS_CORE =
            Shapes.box(6 / 16.0, 6 / 16.0, 6 / 16.0, 10 / 16.0, 10 / 16.0, 10 / 16.0);

    /**
     * 朝北的通道 / 管线（canonical，其余面由旋转得到）。
     *
     * <p>两个载体的几何现在一致（一根中心管 + 四根棱管），只是各自换了贴图。按<b>原始盒子</b>列出，
     * 不先在这里取并集：布尔并会把重叠处拆成互不重叠的碎片（5 个盒子会变成 9 个），幽灵预览逐个盒子
     * 描边时就会显得杂乱。</p>
     */
    private static final List<AABB> LOGIC_WIRE_CANONICAL = List.of(
            new AABB(6 / 16.0, 6 / 16.0, 0.0, 10 / 16.0, 10 / 16.0, 6.0 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, -0.025 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(9.5 / 16.0, 5.5 / 16.0, -0.025 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, -0.025 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0),
            new AABB(5.5 / 16.0, 9.5 / 16.0, -0.025 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0));

    private static final List<AABB> LOGISTICS_WIRE_CANONICAL = List.of(
            new AABB(6 / 16.0, 6 / 16.0, 0.0, 10 / 16.0, 10 / 16.0, 6.0 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, -0.025 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(9.5 / 16.0, 5.5 / 16.0, -0.025 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, -0.025 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0),
            new AABB(5.5 / 16.0, 9.5 / 16.0, -0.025 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0));

    /**
     * 远程门面的通道形状（canonical，朝北；对应 {@code carrier/logic/wire_remote}）。
     *
     * <p>远程面不画整根通道，而是「一小段线头 + 贴在面上的珍珠辉光 + 绕中心体的装饰件」，
     * 因此形状要单独描述（前四个是线头的四根短棱管，第五个是线头的中心短管，第六个是珍珠辉光的外层盒
     * ——模型里的五层辉光都嵌在它里面，最后三个是绕中心体的实心十字：一根竖条 + 左右两条短臂，
     * 三者只相接，取并后既不留空心、也不互相重叠）。</p>
     */
    private static final List<AABB> LOGIC_REMOTE_WIRE_CANONICAL = List.of(
            new AABB(9.5 / 16.0, 5.5 / 16.0, 4.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, 4.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(5.5 / 16.0, 9.5 / 16.0, 4.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, 4.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0),
            new AABB(6 / 16.0, 6 / 16.0, 4.525 / 16.0, 10 / 16.0, 10 / 16.0, 6 / 16.0),
            new AABB(6 / 16.0, 6 / 16.0, 0.001 / 16.0, 10 / 16.0, 10 / 16.0, 4.001 / 16.0),
            new AABB(6.5 / 16.0, 5.5 / 16.0, 4.025 / 16.0, 9.5 / 16.0, 10.5 / 16.0, 5 / 16.0),
            new AABB(5.5 / 16.0, 6.5 / 16.0, 4.025 / 16.0, 6.5 / 16.0, 9.5 / 16.0, 5 / 16.0),
            new AABB(9.5 / 16.0, 6.5 / 16.0, 4.025 / 16.0, 10.5 / 16.0, 9.5 / 16.0, 5 / 16.0));

    /**
     * 物流载体的远程通道形状（几何与逻辑载体一致，对应 {@code carrier/logistics/wire_remote}）。
     *
     * <p>末尾同样是绕中心体的实心十字（「一根竖条 + 左右两条短臂」），只是换成物流自己的贴图。</p>
     */
    private static final List<AABB> LOGISTICS_REMOTE_WIRE_CANONICAL = List.of(
            new AABB(9.5 / 16.0, 5.5 / 16.0, 4.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, 4.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 5.5 / 16.0),
            new AABB(5.5 / 16.0, 9.5 / 16.0, 4.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, 4.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 5.5 / 16.0),
            new AABB(6 / 16.0, 6 / 16.0, 4.525 / 16.0, 10 / 16.0, 10 / 16.0, 6 / 16.0),
            new AABB(6 / 16.0, 6 / 16.0, 0.001 / 16.0, 10 / 16.0, 10 / 16.0, 4.001 / 16.0),
            new AABB(6.5 / 16.0, 5.5 / 16.0, 4.025 / 16.0, 9.5 / 16.0, 10.5 / 16.0, 5 / 16.0),
            new AABB(5.5 / 16.0, 6.5 / 16.0, 4.025 / 16.0, 6.5 / 16.0, 9.5 / 16.0, 5 / 16.0),
            new AABB(9.5 / 16.0, 6.5 / 16.0, 4.025 / 16.0, 10.5 / 16.0, 9.5 / 16.0, 5 / 16.0));

    /** 逻辑载体上-北棱角件的块（canonical，与 {@code carrier/logic/edge} 模型一致）。 */
    private static final AABB LOGIC_EDGE =
            new AABB(6.5 / 16.0, 10 / 16.0, 5 / 16.0, 9.5 / 16.0, 11 / 16.0, 6 / 16.0);

    /** 物流棱件的块（canonical，上-北棱；与 {@code carrier/logistics/edge} 模型一致）。 */
    private static final AABB LOGISTICS_EDGE =
            new AABB(6.5 / 16.0, 9.5 / 16.0, 5.5 / 16.0, 9.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0);

    /** 物流竖棱件的块（canonical，北-东棱；对应 {@code carrier/logistics/edge_vertical} 模型）。 */
    private static final AABB LOGISTICS_EDGE_VERTICAL =
            new AABB(9.5 / 16.0, 5.5 / 16.0, 5.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0);

    /**
     * 物流固定框架的 8 个角块（对应 {@code carrier/logistics/corner}）：它们是外壳八个角的接头，
     * 没有任何条件、恒绘制，与 {@link #LOGISTICS_EDGE_SHAPES} 的棱件一起把框架连起来。
     */
    private static final List<AABB> LOGISTICS_CORNER = List.of(
            new AABB(5.5 / 16.0, 9.5 / 16.0, 5.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, 5.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0),
            new AABB(9.5 / 16.0, 5.5 / 16.0, 5.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, 5.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0),
            new AABB(5.5 / 16.0, 9.5 / 16.0, 9.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0),
            new AABB(5.5 / 16.0, 5.5 / 16.0, 9.5 / 16.0, 6.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0),
            new AABB(9.5 / 16.0, 5.5 / 16.0, 9.5 / 16.0, 10.5 / 16.0, 6.5 / 16.0, 10.5 / 16.0),
            new AABB(9.5 / 16.0, 9.5 / 16.0, 9.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0, 10.5 / 16.0));

    /** 物流固定框架角件的形状（恒渲染，任何状态都计入）。 */
    private static final VoxelShape LOGISTICS_CORNER_SHAPE = union(LOGISTICS_CORNER);

    /** 各面的通道形状（原始盒子取并集，碰撞用），下标同 {@link Direction#ordinal()}。 */
    private static final VoxelShape[] WIRE_SHAPES = new VoxelShape[6];

    /** 各面的管线形状（物流载体用 {@link #LOGISTICS_WIRE_CANONICAL}），下标同 {@link Direction#ordinal()}。 */
    private static final VoxelShape[] LOGISTICS_WIRE_SHAPES = new VoxelShape[6];

    /** 各面远程门的形状（逻辑 / 普通载体用 {@link #LOGIC_REMOTE_WIRE_CANONICAL}）。 */
    private static final VoxelShape[] REMOTE_WIRE_SHAPES = new VoxelShape[6];

    /** 各面远程门的形状（物流载体用 {@link #LOGISTICS_REMOTE_WIRE_CANONICAL}）。 */
    private static final VoxelShape[] LOGISTICS_REMOTE_WIRE_SHAPES = new VoxelShape[6];

    /**
     * 一条棱：相邻两面、它在 blockstate multipart 里的朝向旋转、以及对应的角件形状。
     *
     * <p>旋转一并带上是给 blockstate 生成器用的——有它才能把棱件交给原版模型渲染
     * （那边会连 uv 一起烘焙，并且带环境光遮蔽），而不是只在 BER 里摆姿态。</p>
     */
    public record Edge(Direction a, Direction b, int xRot, int yRot, boolean vertical, VoxelShape shape) {
    }

    private static final List<Edge> EDGE_SHAPES = new ArrayList<>();

    /** 物流载体的棱件形状：横棱用 {@link #LOGISTICS_EDGE}、竖棱用 {@link #LOGISTICS_EDGE_VERTICAL}。 */
    private static final List<Edge> LOGISTICS_EDGE_SHAPES = new ArrayList<>();

    /** 形状缓存下标里「远程面」那一段的起始位（bit6-11），bit0-5 是「已镶嵌」，bit12 是 hub。 */
    private static final int SHAPE_INDEX_REMOTE_SHIFT = 6;

    /** 形状缓存下标里 hub 那一位。 */
    private static final int SHAPE_INDEX_HUB = 1 << 12;

    /** 形状缓存：下标 = hub 位 + 六面「已镶嵌」位 + 六面「远程门」位（共 13 位）。 */
    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[1 << 13];

    /** 物流载体的形状缓存（棱件规则与盒子都不同，不能与上者共用）。 */
    private static final VoxelShape[] LOGISTICS_SHAPE_CACHE = new VoxelShape[1 << 13];

    static {
        // 通道 / 管线：与 blockstate 同一套旋转（上/下仅用 x，其余仅用 y）
        fillWires(WIRE_SHAPES, LOGIC_WIRE_CANONICAL);
        fillWires(LOGISTICS_WIRE_SHAPES, LOGISTICS_WIRE_CANONICAL);
        fillWires(REMOTE_WIRE_SHAPES, LOGIC_REMOTE_WIRE_CANONICAL);
        fillWires(LOGISTICS_REMOTE_WIRE_SHAPES, LOGISTICS_REMOTE_WIRE_CANONICAL);

        // 角件：8 条水平棱（x 先 y 后）+ 4 条竖棱（z 先 y 后），与 blockstate 对应。
        // 物流用同一张表，只是换成物流自己的两个 canonical 盒子（竖棱不再需要 z 旋转）。
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

        logisticsCorner(Direction.UP, Direction.NORTH, 0, 0);
        logisticsCorner(Direction.UP, Direction.EAST, 0, 90);
        logisticsCorner(Direction.UP, Direction.SOUTH, 0, 180);
        logisticsCorner(Direction.UP, Direction.WEST, 0, 270);
        logisticsCorner(Direction.DOWN, Direction.SOUTH, 180, 0);
        logisticsCorner(Direction.DOWN, Direction.WEST, 180, 90);
        logisticsCorner(Direction.DOWN, Direction.NORTH, 180, 180);
        logisticsCorner(Direction.DOWN, Direction.EAST, 180, 270);
        logisticsCornerVertical(Direction.NORTH, Direction.EAST, 0);
        logisticsCornerVertical(Direction.SOUTH, Direction.EAST, 90);
        logisticsCornerVertical(Direction.SOUTH, Direction.WEST, 180);
        logisticsCornerVertical(Direction.NORTH, Direction.WEST, 270);
    }

    /** 按 canonical 朝北的原始盒子填出六个面的形状（与 blockstate 同款旋转）。 */
    private static void fillWires(VoxelShape[] shapes, List<AABB> canonical) {
        fillWire(shapes, canonical, Direction.NORTH, 0, 0);
        fillWire(shapes, canonical, Direction.EAST, 0, 90);
        fillWire(shapes, canonical, Direction.SOUTH, 0, 180);
        fillWire(shapes, canonical, Direction.WEST, 0, 270);
        fillWire(shapes, canonical, Direction.UP, 270, 0);
        fillWire(shapes, canonical, Direction.DOWN, 90, 0);
    }

    private static void fillWire(VoxelShape[] shapes, List<AABB> canonical, Direction face, int xRot, int yRot) {
        VoxelShape shape = Shapes.empty();
        for (AABB box : canonical) {
            shape = Shapes.or(shape, Shapes.create(rotate(box, xRot, yRot, 0)));
        }
        shapes[face.ordinal()] = shape;
    }

    private static void corner(Direction a, Direction b, int xRot, int yRot, int zRot) {
        VoxelShape shape = Shapes.or(
                Shapes.create(rotate(LOGIC_EDGE, xRot, yRot, zRot)));
        EDGE_SHAPES.add(new Edge(a, b, xRot, yRot, zRot == 90, shape));
    }

    /** 物流横棱：与载体同款旋转，只是盒子换成 {@link #LOGISTICS_EDGE}。 */
    private static void logisticsCorner(Direction a, Direction b, int xRot, int yRot) {
        LOGISTICS_EDGE_SHAPES.add(new Edge(a, b, xRot, yRot, false, Shapes.create(rotate(LOGISTICS_EDGE, xRot, yRot, 0))));
    }

    /** 物流竖棱：canonical 已是竖棱，只绕 y 旋转。 */
    private static void logisticsCornerVertical(Direction a, Direction b, int yRot) {
        LOGISTICS_EDGE_SHAPES.add(new Edge(a, b, 0, yRot, true, Shapes.create(rotate(LOGISTICS_EDGE_VERTICAL, 0, yRot, 0))));
    }

    /**
     * 该棱是否要画棱件：普通载体 / 逻辑载体是「两面都外显」，物流载体正好相反——两面都未编程才画。
     *
     * <p>物流是把没开通的边封上壳，所以规则取反。</p>
     */
    private static boolean showsEdge(BlockState state, Edge edge) {
        boolean a = state.getValue(property(edge.a())).isInlaid();
        boolean b = state.getValue(property(edge.b())).isInlaid();
        return state.getBlock() instanceof LogisticsCarrierBlock ? !a && !b : a && b;
    }

    /**
     * 棱件在 blockstate multipart 里要求相邻两面都落在的值——棱件不再有通电 / 未通电两套模型，
     * 它们的全套规则都由 blockstate 画（原版模型渲染会带上环境光遮蔽与 uv 旋转，BER 摆姿态只能拿到
     * 方向漫反射）。
     *
     * <p>逻辑 / 普通载体取 {@link CarrierWire#UNPOWERED}、{@link CarrierWire#POWERED}、{@link CarrierWire#REMOTE}，
     * 合起来就是「两面都已镶嵌」；物流载体取 {@link CarrierWire#NONE}，即两面都未编程——它的棱件规则取反。</p>
     */
    public static CarrierWire[] edgeStates(Block block) {
        return block instanceof LogisticsCarrierBlock
                ? new CarrierWire[] {CarrierWire.NONE}
                : new CarrierWire[] {CarrierWire.UNPOWERED, CarrierWire.POWERED, CarrierWire.REMOTE};
    }

    /** 该方块用哪张棱件形状表（带朝向旋转，渲染器与 blockstate 生成器共用）。 */
    public static List<Edge> edgeShapes(Block block) {
        return block instanceof LogisticsCarrierBlock ? LOGISTICS_EDGE_SHAPES : EDGE_SHAPES;
    }

    /**
     * 该方块是否要把中心体逐个画在未编程的面上：只有物流载体需要——未编程的面没有通道，用中心体
     * 按面朝向各画一份把开口堵上，条件与通道正好相反。逻辑 / 普通载体的中心体只由 {@code hub} 控制。
     */
    public static boolean showsCoreOnFace(Block block) {
        return block instanceof LogisticsCarrierBlock;
    }

    /** 该方块在该状态下用哪个中心体形状：逻辑载体贯通时换成十字件（物流载体的中心体恒为实心块）。 */
    private static VoxelShape coreShape(Block block, BlockState state) {
        if (block instanceof LogisticsCarrierBlock) return LOGISTICS_CORE;
        return state.getValue(HUB) ? LOGIC_CORE : union(coreCrossBoxes(state));
    }

    /**
     * 该状态下贯通十字件的原始盒：棱管要沿贯通轴摆，按那一对相对的面选朝向。
     *
     * <p>{@code hub} 为 false 就保证恰好一对相对面已镶嵌（见 {@link #shouldDrawHub}），三者必中其一。</p>
     */
    private static List<AABB> coreCrossBoxes(BlockState state) {
        if (inlaidPair(state, Direction.EAST)) return CORE_CROSS_X;
        if (inlaidPair(state, Direction.UP)) return CORE_CROSS_Y;
        return CORE_CROSS_Z;
    }

    /** 这一对相对的面是否都已镶嵌。 */
    private static boolean inlaidPair(BlockState state, Direction face) {
        return state.getValue(property(face)).isInlaid()
                && state.getValue(property(face.getOpposite())).isInlaid();
    }

    /** 把一组盒子按 blockstate 同款朝向旋转（先 z、再 x、后 y）。 */
    private static List<AABB> rotateAll(List<AABB> boxes, int xRot, int yRot) {
        List<AABB> rotated = new ArrayList<>(boxes.size());
        for (AABB box : boxes) {
            rotated.add(rotate(box, xRot, yRot, 0));
        }
        return rotated;
    }

    /** 该方块用哪张通道 / 管线形状表。 */
    private static VoxelShape[] wireShapes(Block block) {
        return block instanceof LogisticsCarrierBlock ? LOGISTICS_WIRE_SHAPES : WIRE_SHAPES;
    }

    /** 该面用哪张通道形状：远程门面用带珍珠辉光的远程形状（{@code wire_remote}），其余面用普通通道 / 管线形状。 */
    private static VoxelShape wireShape(BlockState state, Direction direction) {
        if (state.getValue(property(direction)) == CarrierWire.REMOTE) {
            VoxelShape[] remote = state.getBlock() instanceof LogisticsCarrierBlock
                    ? LOGISTICS_REMOTE_WIRE_SHAPES : REMOTE_WIRE_SHAPES;
            return remote[direction.ordinal()];
        }
        return wireShapes(state.getBlock())[direction.ordinal()];
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
        int index = state.getValue(HUB) ? SHAPE_INDEX_HUB : 0;
        for (Direction direction : Direction.values()) {
            CarrierWire wire = state.getValue(property(direction));
            if (!wire.isInlaid()) continue;
            index |= 1 << direction.ordinal();
            // 远程门面的形状与普通通道不同，必须单独占一位，否则两种形状会共用同一份缓存。
            if (wire == CarrierWire.REMOTE) index |= 1 << (SHAPE_INDEX_REMOTE_SHIFT + direction.ordinal());
        }
        // 物流与普通载体的棱件规则相反、盒子也不同，因此两者各用一份缓存（同一下标含义不同）。
        VoxelShape[] cache = state.getBlock() instanceof LogisticsCarrierBlock ? LOGISTICS_SHAPE_CACHE : SHAPE_CACHE;
        VoxelShape cached = cache[index];
        if (cached == null) {
            cached = buildShape(state);
            cache[index] = cached;
        }
        return cached;
    }

    /**
     * 相邻方块也是镶嵌载体、且它朝向本方的面同样开通了通道时，两侧通道对接：剔除本面通道端盖，
     * 免得两层端盖在方块交界处重叠闪烁。
     *
     * <p>通道模型本就给端盖标了 {@code cullface}，只是载体的形状不是满方，默认遮挡判定不会把它当实心，
     * 所以相邻两个载体之间从不触发。相邻的是普通方块时交给默认判定，相邻面未开通通道时也保留端盖。</p>
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getBlock() instanceof InlayCarrierBlock
                && adjacentBlockState.getValue(property(side.getOpposite())).isInlaid();
    }

    /** 一组原始盒子取并集（碰撞用）。 */
    private static VoxelShape union(List<AABB> boxes) {
        VoxelShape shape = Shapes.empty();
        for (AABB box : boxes) {
            shape = Shapes.or(shape, Shapes.create(box));
        }
        return shape;
    }

    /** 中心体与各面通道的并集。 */
    private static VoxelShape partShape(BlockState state) {
        VoxelShape shape = coreShape(state.getBlock(), state);
        for (Direction direction : Direction.values()) {
            if (state.getValue(property(direction)).isInlaid()) {
                shape = Shapes.or(shape, wireShape(state, direction));
            }
        }
        return shape;
    }

    /** 当前状态下可见部件的并集（含棱角件与物流固定框架）。 */
    private static VoxelShape buildShape(BlockState state) {
        VoxelShape shape = partShape(state);
        for (Edge edge : edgeShapes(state.getBlock())) {
            if (showsEdge(state, edge)) {
                shape = Shapes.or(shape, edge.shape());
            }
        }
        if (state.getBlock() instanceof LogisticsCarrierBlock) {
            // 固定框架的角块没有条件、恒绘制（见 blockstate 生成器的 cornerModel）。
            shape = Shapes.or(shape, LOGISTICS_CORNER_SHAPE);
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
            BlockHitResult hit = wireShape(state, direction).clip(from, to, pos);
            if (hit == null) continue;
            double distance = hit.getLocation().distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = direction;
            }
        }

        Vec3 ray = to.subtract(from);
        for (Edge edge : edgeShapes(state.getBlock())) {
            if (!showsEdge(state, edge)) continue;
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
        BlockHitResult centerHit = coreShape(state.getBlock(), state).clip(from, to, pos);
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
