package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.data.FaceRole;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;

/**
 * 载体快捷编辑：手持同类型载体右键时，把点击面与其正对面相邻载体的相对面编程成一条通路。
 *
 * <p>相邻两个载体（A 点击面 {@code face}、B 的朝向 A 的面）按现有角色补全：</p>
 * <ul>
 *   <li>两面都空：点击面写成输入（物流为取出）、对面写成输出（物流为存入）；{@code invert} 时取反；</li>
 *   <li>点击面有角色、对面空：对面补成相反角色；</li>
 *   <li>点击面空、对面有角色：点击面补成相反角色；</li>
 *   <li>两面都已有角色：只在它们已构成「输入→输出」通路时保持不动，否则也不改。</li>
 * </ul>
 *
 * <p>各面属性的「输入侧 / 输出侧」由载体自己的 {@link InlayCarrierBlock#faceRole(FaceMode)}、
 * {@link InlayCarrierBlock#inputMode()} 与 {@link InlayCarrierBlock#outputMode()} 决定；
 * 点击面朝外是容器还是红石粉也由载体自己的 {@link InlayCarrierBlock#supportsEndpoint} 判断。</p>
 */
public final class CarrierQuickEdit {

    private CarrierQuickEdit() {
    }

    /** 该方块是否是可通过面属性编程的载体（逻辑 / 物流；普通载体的面由镶嵌材料决定）。 */
    public static boolean isProgrammableCarrier(Block block) {
        return block instanceof InlayCarrierBlock carrier && carrier.isProgrammable();
    }

    /** 两个方块是否是同一类可编程载体。 */
    public static boolean isSameType(Block a, Block b) {
        if (!(a instanceof InlayCarrierBlock carrierA) || !(b instanceof InlayCarrierBlock carrierB)) {
            return false;
        }
        return carrierA.isProgrammable() && carrierA.kind() == carrierB.kind();
    }

    /** 手持物品是否是目标方块同类型的载体方块物品。 */
    public static boolean isHeldCarrierOf(ItemStack held, Block target) {
        return held.getItem() instanceof BlockItem blockItem && isSameType(blockItem.getBlock(), target);
    }

    /**
     * 执行快捷编辑。
     *
     * @param held   玩家主手物品（用于服务端校验，必须是与目标同类型的载体方块物品）
     * @param invert 两面都空时是否取反（Ctrl）
     * @return 是否实际改写了面属性
     */
    public static boolean apply(Level level, BlockPos pos, Direction face, ItemStack held, boolean invert) {
        if (level.isClientSide) return false;

        Block targetBlock = level.getBlockState(pos).getBlock();
        if (!(targetBlock instanceof InlayCarrierBlock target) || !target.isProgrammable()) return false;
        if (!isHeldCarrierOf(held, targetBlock)) return false;

        BlockPos otherPos = pos.relative(face);
        Block other = level.getBlockState(otherPos).getBlock();
        if (!isSameType(targetBlock, other)) {
            // 点击面朝外是容器 / 红石粉：只编辑点击面。
            if (target.supportsEndpoint(level, otherPos, face.getOpposite())) {
                return applySingle(level, pos, face, target, invert);
            }
            return false;
        }

        Direction otherFace = face.getOpposite();
        BlockInlays targetInlays = BlockInlayManager.get(level, pos);
        BlockInlays otherInlays = BlockInlayManager.get(level, otherPos);
        FaceMode modeA = targetInlays.getFace(face);
        FaceMode modeB = otherInlays.getFace(otherFace);

        FaceRole roleA = target.faceRole(modeA);
        FaceRole roleB = target.faceRole(modeB);
        // 远程等不参与方向的面：不猜、不改。
        if (roleA == FaceRole.OTHER || roleB == FaceRole.OTHER) return false;
        // 已经构成 输入/取出 → 输出/存入 的通路：保持不动。
        if ((roleA == FaceRole.INPUT && roleB == FaceRole.OUTPUT)
                || (roleA == FaceRole.OUTPUT && roleB == FaceRole.INPUT)) {
            return false;
        }

        FaceMode newA;
        FaceMode newB;
        if (roleA == FaceRole.EMPTY && roleB == FaceRole.EMPTY) {
            if (invert) {
                newA = target.outputMode();
                newB = target.inputMode();
            } else {
                newA = target.inputMode();
                newB = target.outputMode();
            }
        } else if (roleB == FaceRole.EMPTY) {
            newA = modeA;
            newB = roleA == FaceRole.INPUT ? target.outputMode() : target.inputMode();
        } else if (roleA == FaceRole.EMPTY) {
            newA = roleB == FaceRole.INPUT ? target.outputMode() : target.inputMode();
            newB = modeB;
        } else {
            // 两侧都已是方向角色却没构成通路（例如都是输入）：不覆盖玩家已有的编程。
            return false;
        }

        Map<BlockPos, Map<Direction, FaceMode>> changes = new HashMap<>();
        changes.put(pos, Map.of(face, newA));
        changes.put(otherPos, Map.of(otherFace, newB));
        return InlayCarrierBlock.programFaces(level, changes);
    }

    /** 点击面朝外是容器 / 红石粉时：只编辑点击面。 */
    private static boolean applySingle(
            Level level, BlockPos pos, Direction face, InlayCarrierBlock carrier, boolean invert) {
        FaceMode mode = BlockInlayManager.get(level, pos).getFace(face);
        FaceMode newMode = switch (mode) {
            case INPUT -> FaceMode.OUTPUT;
            case OUTPUT -> FaceMode.INPUT;
            case EXTRACT -> FaceMode.INSERT;
            case INSERT -> FaceMode.EXTRACT;
            case NONE -> defaultEndMode(level, pos, face, carrier, invert);
            default -> null;
        };
        if (newMode == null) return false;
        return InlayCarrierBlock.programFaces(level, Map.of(pos, Map.of(face, newMode)));
    }

    /**
     * 点击面为空时的方向：只有输入侧时补输出侧，否则（两侧都有 / 只有输出 / 都没有）优先输入侧；
     * Ctrl 取反。
     */
    private static FaceMode defaultEndMode(
            Level level, BlockPos pos, Direction face, InlayCarrierBlock carrier, boolean invert) {
        BlockInlays data = BlockInlayManager.get(level, pos);
        boolean hasInput = false;
        boolean hasOutput = false;
        for (Direction dir : Direction.values()) {
            if (dir == face) continue;
            FaceRole role = carrier.faceRole(data.getFace(dir));
            if (role == FaceRole.INPUT) hasInput = true;
            else if (role == FaceRole.OUTPUT) hasOutput = true;
        }
        FaceMode input = carrier.inputMode();
        FaceMode output = carrier.outputMode();
        FaceMode preferred = hasInput && !hasOutput ? output : input;
        if (invert) {
            preferred = preferred == input ? output : input;
        }
        return preferred;
    }
}
