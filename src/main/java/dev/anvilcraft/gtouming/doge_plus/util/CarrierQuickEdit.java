package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogicCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogisticsCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;

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
 * <p>逻辑载体的输入侧是 {@link FaceMode#INPUT} / {@link FaceMode#DELAY_INPUT_GATE}，其余（含远程）
 * 视为输出侧；物流载体的输入侧是 {@link FaceMode#EXTRACT}，输出侧是 {@link FaceMode#INSERT}，
 * 远程等其它角色不参与。</p>
 */
public final class CarrierQuickEdit {

    private CarrierQuickEdit() {
    }

    /** 该方块是否是可通过面属性编程的载体（逻辑 / 物流；普通载体的面由镶嵌材料决定）。 */
    public static boolean isProgrammableCarrier(Block block) {
        return block instanceof LogicCarrierBlock || block instanceof LogisticsCarrierBlock;
    }

    /** 两个方块是否是同一类可编程载体。 */
    public static boolean isSameType(Block a, Block b) {
        return (a instanceof LogicCarrierBlock && b instanceof LogicCarrierBlock)
                || (a instanceof LogisticsCarrierBlock && b instanceof LogisticsCarrierBlock);
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

        Block target = level.getBlockState(pos).getBlock();
        if (!isProgrammableCarrier(target)) return false;
        if (!isHeldCarrierOf(held, target)) return false;

        BlockPos otherPos = pos.relative(face);
        Block other = level.getBlockState(otherPos).getBlock();
        if (!isSameType(target, other)) {
            // 点击面朝外是容器 / 红石粉：只编辑点击面。
            if (isEndpoint(level, otherPos, face.getOpposite(), target)) {
                return applySingle(level, pos, face, target, invert);
            }
            return false;
        }

        Direction otherFace = face.getOpposite();
        BlockInlays targetInlays = BlockInlayManager.get(level, pos);
        BlockInlays otherInlays = BlockInlayManager.get(level, otherPos);
        FaceMode modeA = targetInlays.getFace(face);
        FaceMode modeB = otherInlays.getFace(otherFace);

        Role roleA = classify(target, modeA);
        Role roleB = classify(other, modeB);
        // 远程等不参与方向的面：不猜、不改。
        if (roleA == Role.OTHER || roleB == Role.OTHER) return false;
        // 已经构成 输入/取出 → 输出/存入 的通路：保持不动。
        if ((roleA == Role.INPUT && roleB == Role.OUTPUT)
                || (roleA == Role.OUTPUT && roleB == Role.INPUT)) {
            return false;
        }

        FaceMode newA;
        FaceMode newB;
        if (roleA == Role.EMPTY && roleB == Role.EMPTY) {
            if (invert) {
                newA = outputMode(target);
                newB = inputMode(other);
            } else {
                newA = inputMode(target);
                newB = outputMode(other);
            }
        } else if (roleB == Role.EMPTY) {
            newA = modeA;
            newB = roleA == Role.INPUT ? outputMode(other) : inputMode(other);
        } else if (roleA == Role.EMPTY) {
            newA = roleB == Role.INPUT ? outputMode(target) : inputMode(target);
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

    /**
     * 点击面朝外的相邻方块是否是这条面的有效端点：
     * 物流载体要面对容器，逻辑载体要面对红石粉（原版 / 铁砧工艺导线）。
     */
    public static boolean isEndpoint(Level level, BlockPos neighborPos, Direction side, Block target) {
        if (target instanceof LogisticsCarrierBlock) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, side) != null) return true;
            AABB box = new AABB(neighborPos).setMaxY(neighborPos.getY() + 1.1);
            for (Entity entity : level.getEntitiesOfClass(Entity.class, box, Entity::isAlive)) {
                if (entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side) != null) return true;
            }
            return false;
        }
        BlockState state = level.getBlockState(neighborPos);
        return state.is(Blocks.REDSTONE_WIRE) || state.getBlock() instanceof RedstoneWireBlock;
    }

    /** 点击面朝外是容器 / 红石粉时：只编辑点击面。 */
    private static boolean applySingle(Level level, BlockPos pos, Direction face, Block target, boolean invert) {
        FaceMode mode = BlockInlayManager.get(level, pos).getFace(face);
        FaceMode newMode = switch (mode) {
            case INPUT -> FaceMode.OUTPUT;
            case OUTPUT -> FaceMode.INPUT;
            case EXTRACT -> FaceMode.INSERT;
            case INSERT -> FaceMode.EXTRACT;
            case NONE -> defaultEndMode(level, pos, face, target, invert);
            default -> null;
        };
        if (newMode == null) return false;
        return InlayCarrierBlock.programFaces(level, Map.of(pos, Map.of(face, newMode)));
    }

    /**
     * 点击面为空时的方向：只有输入侧时补输出侧，否则（两侧都有 / 只有输出 / 都没有）优先输入侧；
     * Ctrl 取反。
     */
    private static FaceMode defaultEndMode(Level level, BlockPos pos, Direction face, Block target, boolean invert) {
        BlockInlays data = BlockInlayManager.get(level, pos);
        boolean hasInput = false;
        boolean hasOutput = false;
        for (Direction dir : Direction.values()) {
            if (dir == face) continue;
            Role role = classify(target, data.getFace(dir));
            if (role == Role.INPUT) hasInput = true;
            else if (role == Role.OUTPUT) hasOutput = true;
        }
        FaceMode preferred = hasInput && !hasOutput ? outputMode(target) : inputMode(target);
        if (invert) {
            preferred = preferred == inputMode(target) ? outputMode(target) : inputMode(target);
        }
        return preferred;
    }

    private enum Role {
        EMPTY,
        INPUT,
        OUTPUT,
        OTHER
    }

    private static Role classify(Block block, FaceMode mode) {
        if (mode == FaceMode.NONE) return Role.EMPTY;
        if (block instanceof LogicCarrierBlock) {
            return mode == FaceMode.INPUT || mode == FaceMode.DELAY_INPUT_GATE
                    ? Role.INPUT
                    : Role.OUTPUT;
        }
        return switch (mode) {
            case EXTRACT -> Role.INPUT;
            case INSERT, CRAFT_INSERT -> Role.OUTPUT;
            default -> Role.OTHER;
        };
    }

    private static FaceMode inputMode(Block block) {
        return block instanceof LogicCarrierBlock ? FaceMode.INPUT : FaceMode.EXTRACT;
    }

    private static FaceMode outputMode(Block block) {
        return block instanceof LogicCarrierBlock ? FaceMode.OUTPUT : FaceMode.INSERT;
    }
}
