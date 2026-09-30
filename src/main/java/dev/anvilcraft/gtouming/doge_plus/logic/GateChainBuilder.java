package dev.anvilcraft.gtouming.doge_plus.logic;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogicCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogisticsCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 门链铺放（服务端权威）。
 *
 * <p>把客户端手势推导出的一串相邻方块落地为逻辑载体并编程各个面，形成单向信号链：
 * 链内每个元素是「{@link FaceMode#INPUT}（朝向上一元素）+ {@link FaceMode#OUTPUT}（朝向下一元素）」，
 * 只有<b>链尾</b>的出口面用副手材料对应的门种——于是无副手材料时是「输入-输出门链」，副手拉杆时是
 * 「输入-锁存门链」。</p>
 *
 * <p>两端还会各自接上相邻的载体：起点面若属于尚未编程的逻辑载体，则该面编程为副手门种，
 * 成为驱动整条链的源头；链尾出口面指向的逻辑载体则接成输入门，让门链驱动它。</p>
 */
public final class GateChainBuilder {

    /** 起点到玩家的距离上限（8 格，仅用于挡住伪造包）。 */
    private static final double MAX_DISTANCE = 64.0;

    private GateChainBuilder() {
    }

    /**
     * 校验并铺放门链。任一步校验失败即截断（已通过前缀仍会落地），全不合法则无操作。
     *
     * @param gateType 副手门种；{@link FaceMode#NONE} 视为默认输出门
     */
    public static void build(
            Player player,
            BlockPos originPos,
            Direction originFace,
            List<BlockPos> requested,
            FaceMode gateType,
            @Nullable Direction finalFace) {
        Level level = player.level();
        if (level.isClientSide) return;
        if (isTooFarAway(player, originPos)) return;

        ItemStack held = player.getMainHandItem();
        boolean logistics = held.is(ModBlocks.LOGISTICS_CARRIER.asItem());
        if (!logistics && !held.is(ModBlocks.LOGIC_CARRIER.asItem())) return;

        FaceMode gate = gateType == FaceMode.NONE ? FaceMode.OUTPUT : gateType;

        List<BlockPos> chain = sanitize(level, player, originPos, originFace, requested);
        if (chain.isEmpty()) return;

        boolean creative = player.getAbilities().instabuild;
        int placeable = creative ? chain.size() : Math.min(chain.size(), held.getCount());
        if (placeable <= 0) return;
        chain = chain.subList(0, placeable);

        BlockState carrier = (logistics ? ModBlocks.LOGISTICS_CARRIER : ModBlocks.LOGIC_CARRIER)
                .get().defaultBlockState();
        for (BlockPos pos : chain) {
            level.setBlock(pos, carrier, Block.UPDATE_CLIENTS);
        }

        if (logistics) {
            InlayCarrierBlock.programFaces(level, planLogisticsFaces(level, originPos, originFace, chain, finalFace));
        } else {
            InlayCarrierBlock.programFaces(
                    level, planFaces(level, originPos, originFace, chain, gate, finalFace));
        }

        if (!creative) held.shrink(chain.size());
    }

    /**
     * 拆除一条门链：逐格校验确为逻辑载体且与前一格相邻，然后移除方块并在生存模式下
     * 返还载体物品。
     *
     * <p>方块移除走 {@code Level.removeBlock}，其 {@code onRemove} 会清理该位置的镶嵌数据、
     * 有状态门运行状态并更新逻辑网络拓扑。任一步校验失败即停止，已通过的前缀仍会被拆除。</p>
     */
    public static void remove(Player player, List<BlockPos> requested) {
        Level level = player.level();
        if (level.isClientSide || requested.isEmpty()) return;
        if (isTooFarAway(player, requested.getFirst())) return;

        int max = AnvilCraftDogePlus.CONFIG.maxGateChainLength;
        boolean creative = player.getAbilities().instabuild;
        // 链首决定这次拆的是哪一族载体（物流 / 逻辑），后续逐格按同一族校验并返还。
        boolean logistics = level.getBlockState(requested.getFirst()).getBlock() instanceof LogisticsCarrierBlock;
        BlockPos prev = null;
        int removed = 0;
        for (BlockPos pos : requested) {
            if (removed >= max) break;
            if (prev != null && directionBetween(prev, pos) == null) break;                          // 非相邻：停止
            BlockState state = level.getBlockState(pos);
            if (logistics ? !(state.getBlock() instanceof LogisticsCarrierBlock)
                     : !(state.getBlock() instanceof LogicCarrierBlock)) {
                break;                                                                              // 非同类载体：停止
            }
            level.removeBlock(pos, false);
            if (!creative) {
                player.getInventory().placeItemBackInInventory(new ItemStack(
                        (logistics ? ModBlocks.LOGISTICS_CARRIER : ModBlocks.LOGIC_CARRIER).asItem()));
            }
            prev = pos;
            removed++;
        }
    }

    /**
     * 校验客户端给的方块序列，返回可安全落地的相邻前缀。
     *
     * <p>要求：首元素恰为起始面外侧一格；逐格相邻且不重复；每格可替换且不与玩家自身碰撞。</p>
     */
    private static List<BlockPos> sanitize(
            Level level, Player player, BlockPos originPos, Direction originFace, List<BlockPos> requested) {
        BlockPos expectedFirst = originPos.relative(originFace);
        if (requested.isEmpty() || !requested.getFirst().equals(expectedFirst)) return List.of();

        int max = AnvilCraftDogePlus.CONFIG.maxGateChainLength;
        AABB playerBox = player.getBoundingBox();
        List<BlockPos> chain = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        BlockPos prev = null;
        for (BlockPos pos : requested) {
            if (chain.size() >= max) break;
            if (prev != null && directionBetween(prev, pos) == null) break; // 非相邻：截断
            if (!seen.add(pos)) break;                                      // 自交 / 重复：截断
            if (!isReplaceable(level, pos)) break;                          // 被占用：截断
            if (playerBox.intersects(new AABB(pos))) break;                 // 与玩家碰撞盒重叠：截断
            chain.add(pos);
            prev = pos;
        }
        return chain;
    }

    /** 规划各链元素与两端面的门种写入。{@code finalFace} 是最终点击面（未命中为 {@code null}）。 */
    private static Map<BlockPos, Map<Direction, FaceMode>> planFaces(
            Level level,
            BlockPos originPos,
            Direction originFace,
            List<BlockPos> chain,
            FaceMode gate,
            @Nullable Direction finalFace) {
        Map<BlockPos, Map<Direction, FaceMode>> changes = new HashMap<>();
        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            boolean isTail = i == chain.size() - 1;
            // 入口面：朝向上一元素（链首则朝向起始方块）。
            Direction inFace = i == 0
                    ? directionBetween(pos, originPos)
                    : directionBetween(pos, chain.get(i - 1));
            if (inFace == null) continue;
            // 出口面：链内各元素朝下一元素；链尾写进「最终点击面的反向」（朝向被点击的那个方块），
            // 没有最终点击面时退回朝向入口的反面。
            Direction outFace = isTail
                    ? (finalFace == null ? inFace.getOpposite() : finalFace.getOpposite())
                    : directionBetween(pos, chain.get(i + 1));
            if (outFace == null) continue;
            Map<Direction, FaceMode> faces = new HashMap<>();
            if (outFace.equals(inFace)) {
                // 出口与入口落在同一面（链只有一个元素、最终点击面就是起始面）：出口用副手门种，
                // 反面作输入。
                faces.put(inFace, gate);
                faces.put(inFace.getOpposite(), FaceMode.INPUT);
            } else {
                faces.put(inFace, FaceMode.INPUT);
                faces.put(outFace, isTail ? gate : FaceMode.OUTPUT);
            }
            changes.put(pos, faces);
        }

        // 起点面：属于尚未编程的逻辑载体时改为 G（成为驱动端），否则保持外部信号来源不变。
        // 单元素链例外：它的输出指向点击面（见上），起点这一侧应当是接收端，留给下面的终点逻辑接成输入门。
        BlockState originState = level.getBlockState(originPos);
        if (chain.size() > 1
                && originState.getBlock() instanceof LogicCarrierBlock
                && BlockInlayManager.get(level, originPos).getFace(originFace) == FaceMode.NONE) {
            changes.computeIfAbsent(originPos, key -> new HashMap<>()).putIfAbsent(originFace, gate);
        }

        // 终点面：链尾出口面指向的方块若也是逻辑载体且该面未编程，则把它接成输入门（让门链驱动它）。
        // 于是起点与终点都是逻辑载体时两端都接上，即使链只有一个元素、两面直接相邻相对。
        BlockPos tail = chain.getLast();
        Direction tailInFace = chain.size() == 1
                ? directionBetween(tail, originPos)
                : directionBetween(tail, chain.get(chain.size() - 2));
        if (tailInFace == null) return changes;
        // 与上面的出口面取法一致：有最终点击面时写进它的反向（朝向被点击方块），否则背离链条。
        Direction exitFace = finalFace == null ? tailInFace.getOpposite() : finalFace.getOpposite();
        BlockPos beyond = tail.relative(exitFace);
        if (level.getBlockState(beyond).getBlock() instanceof LogicCarrierBlock
                && BlockInlayManager.get(level, beyond).getFace(exitFace.getOpposite()) == FaceMode.NONE) {
            changes.computeIfAbsent(beyond, key -> new HashMap<>())
                    .putIfAbsent(exitFace.getOpposite(), FaceMode.INPUT);
        }
        return changes;
    }

    /**
     * 该位置是否已超出与玩家的距离上限。
     *
     * <p>{@link Player#distanceToSqr(Vec3)} 返回的是三个轴向差值的<b>平方和</b>，不是距离——原版也没有
     * {@code distanceTo(Vec3)} 可用。所以要单独用它当距离必须先开方，否则等于拿「格数的平方」和
     * 格数比，阈值含义就错了。</p>
     */
    private static boolean isTooFarAway(Player player, BlockPos pos) {
        return Math.sqrt(player.distanceToSqr(Vec3.atCenterOf(pos))) > MAX_DISTANCE;
    }

    /**
     * 规划物流链各元素与两端面的搬运角色。
     *
     * <p>每个元素「朝上一格 = 取出」（从上一格取货）、「朝下一格 = 存入」（把货交给下一格），于是有向边
     * 从链首一路连到链尾。链首朝起始方块的面本身就是货源（从源容器取货），链尾朝最终点击面的是终点
     * （送进目标容器），所以两端与链内用的是同一套角色，没有特例。</p>
     *
     * <p>角色写死是有意的：传输网里货源只沿「本方存入面 → 正邻格的取出面」往下游传，节点必须一取一存
     * 才收得到货源。若按邻格已有角色取补，邻格正巧是「取出」时就会把链首两面都写成「存入」——那种节点
     * 没有取出面，永远收不到货源，整条链是死的。</p>
     *
     * <p>邻格若是尚未编程的物流载体，仍然替它写上接缝角色（链首那一侧写「存入」、链尾那一侧写「取出」）：
     * 这是为「拆掉半条链再重新接上」准备的，残留的接缝面标出了那一侧的方向，新链顺着它接才连得上边。
     * 邻格那一面已经带角色时不动它；若它的角色与接缝所需相反（取出 ↔ 取出、存入 ↔ 存入），这一侧就
     * 接不上边，渲染器会把这两面都当成链端画出来。</p>
     */
    private static Map<BlockPos, Map<Direction, FaceMode>> planLogisticsFaces(
            Level level,
            BlockPos originPos,
            Direction originFace,
            List<BlockPos> chain,
            @Nullable Direction finalFace) {
        Map<BlockPos, Map<Direction, FaceMode>> changes = new HashMap<>();

        // 链首：邻格未编程时替它写「存入」（把货推给链首的取出面）。
        linkLogisticsFace(level, changes, originPos, originFace, FaceMode.INSERT);

        // 链尾：只有确实有出口面（手势结束时有最终点击面）才接外侧，否则那一面没有对端。
        if (finalFace != null) {
            Direction exitFace = finalFace.getOpposite();
            linkLogisticsFace(level, changes, chain.getLast().relative(exitFace), exitFace.getOpposite(),
                    FaceMode.EXTRACT);
        }

        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            boolean isTail = i == chain.size() - 1;
            // 朝上一格：链首朝起始方块（源容器所在），其余朝链内上一格。
            Direction prevFace = i == 0
                    ? directionBetween(pos, originPos)
                    : directionBetween(pos, chain.get(i - 1));
            if (prevFace == null) continue;
            // 朝下一格：链内朝下一格；链尾改朝「最终点击面的反向」（目标容器所在）。
            Direction nextFace = isTail
                    ? (finalFace == null ? null : finalFace.getOpposite())
                    : directionBetween(pos, chain.get(i + 1));

            Map<Direction, FaceMode> faces = new HashMap<>();
            // 朝上一格的面「取出」：链首从面朝容器取物，其余格从上一格取货。
            faces.put(prevFace, FaceMode.EXTRACT);
            if (nextFace != null && !nextFace.equals(prevFace)) {
                // 朝下一格的面「存入」：非尾格把货交给下一格，尾格把货送进面朝容器。
                faces.put(nextFace, FaceMode.INSERT);
            }
            changes.put(pos, faces);
        }
        return changes;
    }

    /**
     * 邻格若是物流载体、且那一面尚未编程，就替它写上接缝角色（本侧于是不必迁就它）。
     * 非物流载体、或者那一面已经有角色时不动它。
     */
    private static void linkLogisticsFace(
            Level level,
            Map<BlockPos, Map<Direction, FaceMode>> changes,
            BlockPos pos,
            Direction face,
            FaceMode fill) {
        if (!(level.getBlockState(pos).getBlock() instanceof LogisticsCarrierBlock)) return;
        if (BlockInlayManager.get(level, pos).getFace(face) != FaceMode.NONE) return;
        changes.computeIfAbsent(pos, key -> new HashMap<>()).putIfAbsent(face, fill);
    }

    /** {@code from} 到 {@code to} 相差一格时的方向；不相邻返回 {@code null}。 */
    @Nullable
    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (Direction direction : Direction.values()) {
            if (from.relative(direction).equals(to)) return direction;
        }
        return null;
    }

    /** 该位置是否可被门链载体替换。 */
    private static boolean isReplaceable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
}
