package dev.anvilcraft.gtouming.doge_plus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.client.chain.GateChainGesture;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.OutlineCache;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 门链手势的放置 / 移除预览：只逐格描出「将要落地 / 待拆」状态的轮廓，不再绘制半透明方块虚影。
 *
 * <p>描边把该状态的方块形状盒交给 anvillib，由 {@code OutlineBuilder} 求出<b>并集的真实棱线</b>
 * （内部交界棱会被消掉），再用前置预览同一套 {@link OutlineRenderer} 画出来。</p>
 *
 * <p>放置预览的链首 / 链尾朝外的一面按 port 变体收口，中间格保持普通通道；移除预览使用世界里
 * 真实存在的方块状态，不做 port 预测。放置为淡蓝色、移除为暖橙色。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class GateChainPreviewRenderer {

    /** 放置预览的底色（淡蓝）。 */
    private static final float[] PLACE_COLOR = {0.35F, 0.61F, 1.0F};
    /** 移除预览的底色（暖橙）。 */
    private static final float[] REMOVE_COLOR = {1.0F, 0.45F, 0.35F};
    /** 描边透明度。 */
    private static final float OUTLINE_ALPHA = 0.9F;
    /**
     * 描边的外扩量，避免移除预览与真实方块面共面闪烁。
     * 按方块中心等比外扩，部件形状不变形。
     */
    private static final float OUTLINE_OFFSET = 0.008F;

    /**
     * 描边几何能接受的最小「两条短边之积」。
     *
     * <p>{@code ConvexShape} 的盒子面法线是面内两条边的叉乘，长度正好等于这两条边之积；
     * 而 {@link Vec3#normalize()} 在长度小于 {@code 1.0E-4} 时直接返回零向量，法线一归零，
     * 「面过方块中心」的判据就成立，直接抛 {@code Zero-volume shape}。</p>
     *
     * <p>碰撞形状是并集拆出来的碎片：逻辑载体的中心体在 5.475/16（比 1/16 网格偏 0.025），
     * 与通道的 1/16 面一对齐就会拆出 0.0015625 × 0.0625 × … 这种细丝（两条短边之积 9.77E-05）。
     * 这类细丝对轮廓没有意义——棱线要么落在结构内部、要么与相邻盒面共面——直接丢掉。</p>
     */
    private static final double MIN_OUTLINE_EDGE_PRODUCT = 1.0E-4;

    /** 各状态 + port 面组合的描边几何（键必须是复用的实例，理由见 {@link #outline}）。 */
    private static final Map<OutlineKey, SelectionGeometry> OUTLINE_GEOMETRY = new HashMap<>();

    private GateChainPreviewRenderer() {
    }

    /** 一格预览：方块状态，以及需要用 port 变体的面（仅放置预览）。 */
    private record PreviewCell(BlockState state, Set<Direction> portFaces) {
    }

    /** 描边缓存键：状态 + port 面掩码。同一状态在链首 / 链中可能使用不同 port 面。 */
    private record OutlineKey(BlockState state, int portMask) {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (!GateChainGesture.isActive()) return;

        Minecraft minecraft = Minecraft.getInstance();
        Level level = minecraft.level;
        if (level == null) return;
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<BlockPos> chain = GateChainGesture.preview(minecraft, partialTick);
        if (chain.isEmpty()) return;

        boolean removal = GateChainGesture.isRemoval();
        // 最终点击面：与服务端 planFaces 用同一个值（松开时也是它）。
        Direction finalFace = minecraft.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK ? hit.getDirection() : null;
        float[] color = removal ? REMOVE_COLOR : PLACE_COLOR;

        // 先算好每格的状态与 port 面；描边几何按「状态 + port 面」缓存。
        List<PreviewCell> cells = new ArrayList<>(chain.size());
        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            cells.add(removal
                    ? new PreviewCell(level.getBlockState(pos), Set.of())
                    : previewCell(pos, i, chain, GateChainGesture.originBlock(), finalFace));
        }

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        // 逐格描边。几何取该状态的方块形状盒，交给 anvillib 求「并集的真实棱线」，再按前置预览
        // 同一套 OutlineRenderer 画（几何按状态 + port 面缓存，OutlineCache 以后台线程建线）。
        VertexConsumer outline = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            PackedOutline packed = outline(cells.get(i), level, pos);
            if (packed == null) continue;
            pushBlock(poseStack, pos, OUTLINE_OFFSET);
            OutlineRenderer.render(poseStack, outline, packed, color[0], color[1], color[2], OUTLINE_ALPHA);
            poseStack.popPose();
        }
        buffers.endBatch(RenderType.lines());

        poseStack.popPose();
    }

    /**
     * 取该格状态的描边（已求并集的真实棱线）；形状为空或超出几何上限时返回 {@code null}。
     *
     * <p>几何必须按「状态 + port 面」缓存：{@link OutlineCache} 是以几何<b>实例</b>为键的，
     * 每次现造一份就永远命中不了缓存，每帧都会退回「所有盒子的棱」那种轻量兜底轮廓。</p>
     */
    @Nullable
    private static PackedOutline outline(PreviewCell cell, Level level, BlockPos pos) {
        int portMask = portMask(cell.portFaces());
        OutlineKey key = new OutlineKey(cell.state(), portMask);
        SelectionGeometry geometry = OUTLINE_GEOMETRY.get(key);
        if (geometry == null) {
            List<ConvexShape> shapes = new ArrayList<>();
            VoxelShape shape = cell.state().getBlock() instanceof InlayCarrierBlock
                    ? InlayCarrierBlock.previewShape(cell.state(), cell.portFaces())
                    : cell.state().getShape(level, pos);
            for (AABB box : shape.toAabbs()) {
                if (box.getXsize() * box.getYsize() < MIN_OUTLINE_EDGE_PRODUCT
                        || box.getYsize() * box.getZsize() < MIN_OUTLINE_EDGE_PRODUCT
                        || box.getZsize() * box.getXsize() < MIN_OUTLINE_EDGE_PRODUCT) {
                    continue;
                }
                shapes.add(ConvexShape.box(box));
            }
            if (shapes.isEmpty() || shapes.size() > SelectionGeometry.MAX_SHAPES) return null;
            // 容量上限跟 OutlineCache 对齐：它淘汰掉的几何我们也没必要继续留着。
            if (OUTLINE_GEOMETRY.size() >= OutlineCache.MAX_ENTRIES) OUTLINE_GEOMETRY.clear();
            geometry = new SelectionGeometry(shapes);
            OUTLINE_GEOMETRY.put(key, geometry);
        }
        return CubeSelection.outlines().get(geometry);
    }

    /** 逐格推到位姿：把方块局部坐标搬到世界坐标，并按 {@code offset} 相对方块中心整体外扩。 */
    private static void pushBlock(PoseStack poseStack, BlockPos pos, float offset) {
        poseStack.pushPose();
        poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
        poseStack.translate(-offset, -offset, -offset);
        poseStack.scale(1.0F + 2.0F * offset, 1.0F + 2.0F * offset, 1.0F + 2.0F * offset);
    }

    /**
     * 预览用的载体状态：按与服务端 {@code planFaces} 完全相同的规则标出将要镶嵌的面。
     *
     * <p>入口面 = 朝上一元素（链首朝起始方块）；出口面 = 朝下一元素，链尾则写进「最终点击面的反向」，
     * 没有最终点击面时退回背离链条；入口与出口同面时，反面也作输入。</p>
     *
     * <p>链首的入口面与链尾的出口面朝外接端点，使用 port 变体收口；中间格保持普通通道。</p>
     */
    private static PreviewCell previewCell(
            BlockPos pos, int index, List<BlockPos> chain, @Nullable BlockPos origin, @Nullable Direction finalFace) {
        boolean isTail = index == chain.size() - 1;
        Direction inFace = index == 0
                ? (origin == null ? null : directionBetween(pos, origin))
                : directionBetween(pos, chain.get(index - 1));
        Direction outFace = isTail
                ? (finalFace == null ? (inFace == null ? null : inFace.getOpposite()) : finalFace.getOpposite())
                : directionBetween(pos, chain.get(index + 1));

        List<Direction> inlaid = new ArrayList<>(2);
        if (inFace != null && inFace.equals(outFace)) {
            inlaid.add(inFace);
            inlaid.add(inFace.getOpposite());
        } else {
            if (inFace != null) inlaid.add(inFace);
            if (outFace != null && !inlaid.contains(outFace)) inlaid.add(outFace);
        }

        // 预览的默认方块状态按手势作用的载体族取（逻辑载体 / 物流载体）。
        BlockState state = (GateChainGesture.isLogistics() ? ModBlocks.LOGISTICS_CARRIER : ModBlocks.LOGIC_CARRIER)
                .get().defaultBlockState();
        for (Direction direction : inlaid) {
            state = state.setValue(InlayCarrierBlock.property(direction), CarrierWire.of(true, false));
        }
        if (state.getBlock() instanceof InlayCarrierBlock carrier) {
            state = state.setValue(InlayCarrierBlock.HUB, carrier.shouldDrawHub(inlaid));
        }

        Set<Direction> portFaces = EnumSet.noneOf(Direction.class);
        if (index == 0 && inFace != null) portFaces.add(inFace);
        if (isTail && outFace != null) portFaces.add(outFace);
        return new PreviewCell(state, Set.copyOf(portFaces));
    }

    private static int portMask(Set<Direction> portFaces) {
        int mask = 0;
        for (Direction direction : portFaces) {
            mask |= 1 << direction.ordinal();
        }
        return mask;
    }

    /** {@code from} 到 {@code to} 相差一格时的方向；不相邻返回 {@code null}。 */
    @Nullable
    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (Direction direction : Direction.values()) {
            if (from.relative(direction).equals(to)) return direction;
        }
        return null;
    }
}
