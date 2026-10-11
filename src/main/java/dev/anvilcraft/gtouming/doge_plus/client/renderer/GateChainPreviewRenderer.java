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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门链手势的幽灵预览：逐格画「将要落地那个状态」的方块模型，整体压成半透明幽灵色。
 *
 * <p>与前置机器（溜槽等）的放置预览同一套做法——不自己拼盒子，直接把方块状态交给
 * {@link BlockRenderDispatcher#renderBatched}：中心体 / 通道 / 棱件由 blockstate multipart
 * 拼出，环境光遮蔽与贴图朝向都交给模型渲染，预览与实际放置后的样子自然一致。</p>
 *
 * <p>放置用「预测的状态」（按将要镶嵌的面推出来）；移除必须用<b>世界里真实存在的方块状态</b>——
 * 待拆的载体可能早已被编程过，极端情况六向全外显，任何预测都会与实物不符。放置为淡蓝色、移除为暖橙色。</p>
 *
 * <p>整套几何只往 translucent 缓冲里画一次，透明度由顶点色统一压低，因此不会被 cutout 部件的
 * 贴图透明度截断；顶点光换成全亮，暗处也能看清轮廓。模型自带的逐面明暗仍保留（它烘在顶点色里）。</p>
 *
 * <p>虚影之外再逐格描一圈棱线：把该状态的方块形状盒交给 anvillib，由 {@code OutlineBuilder} 求出
 * <b>并集的真实棱线</b>（内部交界棱会被消掉），再用前置预览同一套 {@link OutlineRenderer} 画出来。
 * 半透明的载体细，单靠幽灵色在暗处糊成一团，棱线把轮廓勾出来才看得清落点。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class GateChainPreviewRenderer {

    /** 放置预览的底色（淡蓝）。 */
    private static final float[] PLACE_COLOR = {0.35F, 0.61F, 1.0F};
    /** 移除预览的底色（暖橙）。 */
    private static final float[] REMOVE_COLOR = {1.0F, 0.45F, 0.35F};
    /** 幽灵透明度。 */
    private static final float ALPHA = 0.5F;
    /** 描边透明度。 */
    private static final float OUTLINE_ALPHA = 0.9F;
    /** 幽灵整体外扩量，避免移除预览与真实方块共面闪烁。 */
    private static final float GHOST_OFFSET = 0.002F;
    /**
     * 描边的外扩量。描边几何就是模型本身的外壳，与幽灵面重合，而幽灵要写深度、会把它整圈盖住；
     * 比幽灵再多撑出去一点，棱线才浮在模型外侧（按方块中心等比外扩，部件形状不变形）。
     */
    private static final float OUTLINE_OFFSET = 0.008F;
    /** 模型渲染的随机源（只影响模型内部取面，固定种子即可）。 */
    private static final RandomSource RANDOM = RandomSource.create(42L);

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

    /** 各状态的描边几何（键必须是复用的实例，理由见 {@link #outline}）。 */
    private static final Map<BlockState, SelectionGeometry> OUTLINE_GEOMETRY = new HashMap<>();

    private GateChainPreviewRenderer() {
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

        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();

        // 先把链上每一格的状态算出来：虚影与描边要分两趟画（理由见下），这里只算一次。
        List<BlockState> states = new ArrayList<>(chain.size());
        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            states.add(removal
                    ? level.getBlockState(pos)
                    : previewState(pos, i, chain, GateChainGesture.originBlock(), finalFace));
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        // 第一趟：幽灵模型。方块模型按渲染类型分发部件，逐个渲染类型各画一趟，但都塞进同一个
        // translucent 缓冲：这样 cutout 部件也会跟着半透明混合，不会被贴图的透明度截断。
        VertexConsumer consumer = new TintedVertexConsumer(buffers.getBuffer(RenderType.translucent()), color);
        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            BlockState state = states.get(i);
            pushBlock(poseStack, pos, GHOST_OFFSET);
            for (RenderType renderType : dispatcher.getBlockModel(state).getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
                dispatcher.renderBatched(
                        state, pos, level, poseStack, consumer, true, RANDOM, ModelData.EMPTY, renderType);
            }
            poseStack.popPose();
        }
        // 必须先把这一批冲掉再取描边的缓冲：translucent 与 lines 都走 BufferSource 的共享池，
        // 池化缓冲一次只装得下一个渲染类型，getBuffer 拿到新类型时会顺手把上一批 endBatch 掉，
        // 正在写的批次被结掉就会在 addVertex 里抛「Not building!」。
        buffers.endBatch(RenderType.translucent());

        // 第二趟：逐格描边。几何取该状态的方块形状盒，交给 anvillib 求「并集的真实棱线」，再按前置
        // 同一套 OutlineRenderer 画（几何按状态缓存，OutlineCache 以后台线程建线、以几何实例为键）。
        VertexConsumer outline = buffers.getBuffer(RenderType.lines());
        for (int i = 0; i < chain.size(); i++) {
            BlockPos pos = chain.get(i);
            PackedOutline packed = outline(states.get(i), level, pos);
            if (packed == null) continue;
            pushBlock(poseStack, pos, OUTLINE_OFFSET);
            OutlineRenderer.render(poseStack, outline, packed, color[0], color[1], color[2], OUTLINE_ALPHA);
            poseStack.popPose();
        }
        buffers.endBatch(RenderType.lines());

        poseStack.popPose();
    }

    /**
     * 取该状态的描边（已求并集的真实棱线）；形状为空或超出几何上限时返回 {@code null}。
     *
     * <p>几何必须按状态缓存：{@link OutlineCache} 是以几何<b>实例</b>为键的，每次现造一份就永远命中不了缓存，
     * 每帧都会退回「所有盒子的棱」那种轻量兜底轮廓。</p>
     */
    @Nullable
    private static PackedOutline outline(BlockState state, Level level, BlockPos pos) {
        SelectionGeometry geometry = OUTLINE_GEOMETRY.get(state);
        if (geometry == null) {
            List<ConvexShape> shapes = new ArrayList<>();
            for (AABB box : state.getShape(level, pos).toAabbs()) {
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
            OUTLINE_GEOMETRY.put(state, geometry);
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
     */
    private static BlockState previewState(
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
        return state.setValue(InlayCarrierBlock.HUB, InlayCarrierBlock.shouldDrawHub(state.getBlock(), inlaid));
    }

    /** {@code from} 到 {@code to} 相差一格时的方向；不相邻返回 {@code null}。 */
    @Nullable
    private static Direction directionBetween(BlockPos from, BlockPos to) {
        for (Direction direction : Direction.values()) {
            if (from.relative(direction).equals(to)) return direction;
        }
        return null;
    }

    /**
     * 把模型顶点色乘上预览色、统一压低透明度，并把顶点光换成全亮。
     *
     * <p>乘而不是替换：模型自带的逐面明暗（{@code getShade}）烘在顶点色里，乘上去才能留住立体感。</p>
     */
    private record TintedVertexConsumer(VertexConsumer delegate, float[] color) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(
                    Math.round(red * this.color[0]),
                    Math.round(green * this.color[1]),
                    Math.round(blue * this.color[2]),
                    Math.round(alpha * ALPHA));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            this.delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            this.delegate.setUv2(LightTexture.FULL_BLOCK & 0xFFFF, LightTexture.FULL_BLOCK >> 16);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
    }
}
