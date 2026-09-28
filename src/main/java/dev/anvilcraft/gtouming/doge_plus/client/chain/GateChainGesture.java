package dev.anvilcraft.gtouming.doge_plus.client.chain;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogicCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.PipeCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.network.BuildGateChainPacket;
import dev.anvilcraft.gtouming.doge_plus.network.RemoveGateChainPacket;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 逻辑载体的门链手势（客户端），放置与移除共用一套操作。
 *
 * <p>主手持有逻辑载体时按住 Ctrl 右键为「放置」：记录起始面后不再执行原版放置，改为预览
 * 一条沿准星延伸的门链。主手持有铁砧锤并对准一个逻辑载体时按住 Ctrl 右键为「移除」：
 * 预览沿准星连续的逻辑载体链。两者都是右键固定拐点、左键移除拐点、松开 Ctrl 生效；手势中
 * 换掉对应的手持物品（或打开任意界面）都会取消本次操作。</p>
 */
public final class GateChainGesture {

    /** 判定起始面所用射线长度（与轮盘 / HUD / 交互一致）。 */
    private static final double RAY_LENGTH = 6.0;

    /** 手势模式：放置新链 / 移除已有链。 */
    public enum Mode {
        PLACE,
        REMOVE
    }

    @Getter
    private static boolean active = false;
    private static Mode mode = Mode.PLACE;
    private static BlockPos originPos = null;
    private static Direction originFace = null;
    private static FaceMode gateType = FaceMode.OUTPUT;

    /** 本次手势作用于管道载体（物品传输）还是逻辑载体（红石门）；由手势开始时的手持物 / 点击方块决定。 */
    private static boolean pipe = false;
    private static final List<BlockPos> WAYPOINTS = new ArrayList<>();

    private GateChainGesture() {
    }

    /** 当前是否为移除手势（预览据此换色）。 */
    public static boolean isRemoval() {
        return active && mode == Mode.REMOVE;
    }

    /** 本次手势是否作用于管道载体（预览据此选默认方块状态与判定）。 */
    public static boolean isPipe() {
        return active && pipe;
    }

    /** 起始方块（第一个点击面所在方块）；未在手势中时为 {@code null}。预览据此补上链首那一格的连接面。 */
    @Nullable
    public static BlockPos originBlock() {
        return originPos;
    }

    /** 开始手势；按住 Ctrl 且主手为逻辑载体或对准载体的铁砧锤时返回 true（调用方据此拦截原版放置）。 */
    public static boolean start(Minecraft minecraft) {
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null || !Screen.hasControlDown()) return false;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return false;
        }

        ItemStack main = player.getMainHandItem();
        BlockPos pos = hit.getBlockPos().immutable();
        if (main.is(ModBlocks.LOGIC_CARRIER.asItem()) || main.is(ModBlocks.PIPE_CARRIER.asItem())) {
            mode = Mode.PLACE;
            pipe = main.is(ModBlocks.PIPE_CARRIER.asItem());
            // 放置：链首为起始面外侧第一格，起始面属于未编程的载体时由服务端改为驱动端 / 源端。
            originPos = pos;
            originFace = pickedFace(player, level, hit, pos);
            gateType = resolveGateType(player.getOffhandItem());
        } else if (main.is(ModItemTags.ANVIL_HAMMER)
                && (level.getBlockState(pos).getBlock() instanceof LogicCarrierBlock
                    || level.getBlockState(pos).getBlock() instanceof PipeCarrierBlock)) {
            mode = Mode.REMOVE;
            pipe = level.getBlockState(pos).getBlock() instanceof PipeCarrierBlock;
            // 移除：链首就是点击的载体本身，之后沿准星跟随相邻的同类载体。
            originPos = pos;
            originFace = pickedFace(player, level, hit, pos);
            gateType = FaceMode.OUTPUT;
        } else {
            return false;
        }

        WAYPOINTS.clear();
        active = true;
        return true;
    }

    /**
     * 起始面：与轮盘 / HUD / 交互一致，按<b>模型部件</b>判定，退回方块面。
     *
     * <p>载体的通道与棱件是独立的模型部件，直接用方块面会把「看着某个通道」的点选落到别的面上；
     * 非载体（放置模式的起点常常是源容器）没有这些部件，直接返回方块面。</p>
     */
    private static Direction pickedFace(
            LocalPlayer player, ClientLevel level, BlockHitResult hit, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock)) return hit.getDirection();
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
        Direction picked = InlayCarrierBlock.pickFace(state, pos, from, to);
        return picked != null ? picked : hit.getDirection();
    }

    /** 把当前末端固定为拐点，之后从拐点继续按新方向延伸。 */
    public static void addWaypoint(Minecraft minecraft) {
        if (!active) return;
        List<BlockPos> chain = preview(minecraft, 1.0F);
        if (chain.size() <= 1) return;
        BlockPos tip = chain.getLast();
        if (!WAYPOINTS.isEmpty() && WAYPOINTS.getLast().equals(tip)) return;
        WAYPOINTS.add(tip);
    }

    /** 移除最后一个拐点。 */
    public static void removeWaypoint() {
        if (!active || WAYPOINTS.isEmpty()) return;
        WAYPOINTS.removeLast();
    }

    /** 结束手势并把链发给服务端。 */
    public static void finish(Minecraft minecraft) {
        if (active && originPos != null) {
            List<BlockPos> chain = preview(minecraft, 1.0F);
            if (!chain.isEmpty()) {
                if (mode == Mode.REMOVE) {
                    PacketDistributor.sendToServer(new RemoveGateChainPacket(List.copyOf(chain)));
                } else if (originFace != null) {
                    // 最终点击面 = 松开瞬间准星命中的那个面；未命中（看向天空等）则为空。
                    Direction finalFace = minecraft.hitResult instanceof BlockHitResult hit
                            && hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : null;
                    PacketDistributor.sendToServer(new BuildGateChainPacket(
                            originPos, originFace, List.copyOf(chain), gateType, finalFace));
                }
            }
        }
        clear();
    }

    /** 取消手势，不产生任何效果。 */
    public static void clear() {
        active = false;
        mode = Mode.PLACE;
        originPos = null;
        originFace = null;
        gateType = FaceMode.OUTPUT;
        WAYPOINTS.clear();
    }

    /** 每 tick 检查取消条件与 Ctrl 松开。 */
    public static void tick(Minecraft minecraft) {
        if (!active) return;
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null || minecraft.screen != null || !holdsGestureItem(player)) {
            clear();
            return;
        }
        if (!Screen.hasControlDown()) finish(minecraft);
    }

    /** 当前预览的完整方块序列；未在 Gesture 中时返回空列表。 */
    public static List<BlockPos> preview(Minecraft minecraft, float partialTick) {
        if (!active || originPos == null || originFace == null) return List.of();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) return List.of();

        Vec3 eye = player.getEyePosition(partialTick);
        Vec3 look = player.getViewVector(partialTick);
        int maxSteps = AnvilCraftDogePlus.CONFIG.maxGateChainPreview;

        BlockPos start = mode == Mode.REMOVE ? originPos : originPos.relative(originFace);
        return GateChainGeometry.build(
                start,
                WAYPOINTS,
                // 被点击的方块：锚点在它外侧，朝它那条轴第一步必然撞上它，不参与寻路方向的选择。
                originPos,
                aim(minecraft, player, level, eye, look),
                maxSteps,
                AnvilCraftDogePlus.CONFIG.maxGateChainLength,
                acceptance(level));
    }

    /**
     * 该模式下某一格能否进入预览链。
     *
     * <p>移除模式不再「只要挨着是逻辑方块就收」：相邻两格之间还必须确实按信号方向接着，
     * 否则顺着准星会把旁边无关的载体一起圈进来。优先认「本载体在该面输出 / 驱动，对面反向接成输入门」
     * 这一方向，反向（对面驱动、本载体接成输入门）同样接受，这样朝上游拖也能跟到源头。</p>
     *
     * <p>走位方向仍完全由准星决定，所以链条不会自动拐弯——要拐弯依旧靠右键加拐点。</p>
     */
    private static GateChainGeometry.Candidate acceptance(ClientLevel level) {
        if (mode == Mode.REMOVE) {
            return (from, step, next) -> {
                Block block = level.getBlockState(next).getBlock();
                if (pipe ? !(block instanceof PipeCarrierBlock) : !(block instanceof LogicCarrierBlock)) return false;
                // 链首是玩家点击的那个载体，没有「上一格」可比。
                if (from == null || step == null) return true;
                return pipe ? transferLinked(level, from, step, next) : signalLinked(level, from, step, next);
            };
        }
        // 与服务端 GateChainBuilder 的 isReplaceable 保持同一判定，避免预览与实际落地不一致。
        return (from, step, next) -> {
            BlockState state = level.getBlockState(next);
            return state.isAir() || state.canBeReplaced();
        };
    }

    /**
     * 相邻两格之间是否按信号方向接着。
     *
     * <p>接收面只有标记为 {@link FaceMode#INPUT} 的才收信号（见 {@code collectNodeInputs}），
     * 所以「谁驱动谁」是可判定的：优先「from 在该面驱动、next 反向接成输入门」，反向亦然。</p>
     */
    private static boolean signalLinked(ClientLevel level, BlockPos from, Direction step, BlockPos next) {
        BlockInlays nearer = BlockInlayManager.get(level, from);
        BlockInlays further = BlockInlayManager.get(level, next);
        Direction back = step.getOpposite();
        // 输出方向（优先）：本载体在这一面驱动，对面反向接成输入门。
        if (drives(nearer, step) && further.getFace(back) == FaceMode.INPUT) return true;
        // 反向：对面在这一面驱动、本载体反向接成输入门。
        return drives(further, back) && nearer.getFace(step) == FaceMode.INPUT;
    }

    /** 该面是否在驱动信号（未编程的面与输入门都只进不出；搬运角色不接红石，同样不算驱动）。 */
    private static boolean drives(BlockInlays inlays, Direction face) {
        FaceMode mode = inlays.getFace(face);
        return mode.isRedstone() && mode != FaceMode.INPUT;
    }

    /**
     * 相邻两格之间是否按物品搬运方向接着。
     *
     * <p>与 {@link #signalLinked} 同一套写法，只是判据换成「取出 / 存入」：一方在该面取出、
     * 另一方反向存入即连通（有向边），反向亦然，于是朝上游拖也能跟到源头。</p>
     */
    private static boolean transferLinked(ClientLevel level, BlockPos from, Direction step, BlockPos next) {
        BlockInlays nearer = BlockInlayManager.get(level, from);
        BlockInlays further = BlockInlayManager.get(level, next);
        Direction back = step.getOpposite();
        if (nearer.getFace(step) == FaceMode.EXTRACT
                && further.getFace(back) == FaceMode.INSERT) {
            return true;
        }
        return further.getFace(back) == FaceMode.EXTRACT
                && nearer.getFace(step) == FaceMode.INSERT;
    }

    /** 手势进行中主手必须仍持有开始时的物品（否则取消）。 */
    private static boolean holdsGestureItem(LocalPlayer player) {
        if (mode == Mode.REMOVE) return player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER);
        return player.getMainHandItem().is(pipe
                ? ModBlocks.PIPE_CARRIER.asItem()
                : ModBlocks.LOGIC_CARRIER.asItem());
    }

    /**
     * 瞄准信息：准星的命中结果（方块 + 命中面，未命中为 {@code null}）、眼位与视线，以及「延长视线」
     * 时用的射线追踪器（与准星同一套判定：{@code ClipContext.Block.OUTLINE}，不追踪流体）。
     *
     * <p>命中面会参与分段方向的取舍，见 {@code GateChainGeometry} 的说明。</p>
     */
    private static GateChainGeometry.Aim aim(
            Minecraft minecraft, LocalPlayer player, ClientLevel level, Vec3 eye, Vec3 look) {
        BlockHitResult crosshair = minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? hit : null;
        return new GateChainGeometry.Aim(crosshair, eye, look, (from, to) -> {
            BlockHitResult result = level.clip(new ClipContext(
                    from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            return result.getType() == HitResult.Type.BLOCK ? result : null;
        });
    }

    /** 副手材料决定链上门种；没有有效红石门材料（含搬运材料）时默认输出门。 */
    private static FaceMode resolveGateType(ItemStack offhand) {
        FaceMode mode = BlockInlays.faceModeOf(InlayEntry.fromItemStack(offhand));
        return mode.isRedstone() ? mode : FaceMode.OUTPUT;
    }
}
