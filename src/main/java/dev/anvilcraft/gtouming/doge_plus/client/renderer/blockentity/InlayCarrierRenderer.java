package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierFace;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 镶嵌载体棱角与连接件渲染器。
 *
 * <p>通道（{@code wire}）与中心体由 multipart 绘制，拥有逐顶点 AO 与方向光照；只有两类部件走方块实体
 * 渲染器：</p>
 * <ul>
 *   <li><b>棱角</b>：是否 powered 取决于相邻两面是否恰为「输入门收到信号」与「消耗输入面的门」，
 *       需要客户端镶嵌数据里的门类型，无法用低基数方块状态表达；</li>
 *   <li><b>连接件</b>：取决于邻居是否为红石粉 / 红石导线，逐面布尔属性会让状态数膨胀 64 倍。</li>
 * </ul>
 *
 * <p><b>光照</b>：不能用无着色的 {@code renderModel} 重载（六面等亮 → 又平又白），
 * 而是逐面按经姿态变换后的<b>世界方向</b>取 {@link net.minecraft.world.level.BlockAndTintGetter#getShade}
 * 做方向漫反射（上亮下暗），与前置的 {@code PipeCheckValveBERenderer} 一致（无 AO）。</p>
 *
 * <p>模型的朝向旋转与 blockstate multipart 完全一致（{@code BlockModelRotation} 等效：
 * {@code rotateYXZ(-y, -x, 0)}）。</p>
 */
public class InlayCarrierRenderer implements BlockEntityRenderer<InlayCarrierBlockEntity> {

    private static final ModelResourceLocation CORNER = standalone("inlay_carrier_corner");
    private static final ModelResourceLocation CORNER_POWERED = standalone("inlay_carrier_corner_powered");
    private static final ModelResourceLocation CORNER_VERTICAL = standalone("inlay_carrier_corner_vertical");
    private static final ModelResourceLocation CORNER_VERTICAL_POWERED =
            standalone("inlay_carrier_corner_vertical_powered");
    private static final ModelResourceLocation CONNECTED = standalone("inlay_carrier_connected");
    private static final ModelResourceLocation CONNECTED_POWERED = standalone("inlay_carrier_connected_powered");

    /** 需要在 {@code ModelEvent.RegisterAdditional} 中注册的模型。 */
    public static final List<ModelResourceLocation> MODELS = List.of(
            CORNER, CORNER_POWERED,
            CORNER_VERTICAL, CORNER_VERTICAL_POWERED,
            CONNECTED, CONNECTED_POWERED);

    private static final RandomSource RANDOM = RandomSource.create();

    /** 判定准星命中部件所用射线长度（与交互 / HUD 一致）。 */
    private static final double RAY_LENGTH = 6.0;

    /** 一条棱：所属两面、水平/竖直模型，以及 multipart 对应的 x/y 旋转。 */
    private record Edge(
            Direction a,
            Direction b,
            ModelResourceLocation plain,
            ModelResourceLocation powered,
            int xRot,
            int yRot) {
    }

    /** 与 blockstate multipart 一致的 12 条棱表。 */
    private static final List<Edge> EDGES = List.of(
            new Edge(Direction.UP, Direction.NORTH, CORNER, CORNER_POWERED, 0, 0),
            new Edge(Direction.UP, Direction.EAST, CORNER, CORNER_POWERED, 0, 90),
            new Edge(Direction.UP, Direction.SOUTH, CORNER, CORNER_POWERED, 0, 180),
            new Edge(Direction.UP, Direction.WEST, CORNER, CORNER_POWERED, 0, 270),
            new Edge(Direction.DOWN, Direction.SOUTH, CORNER, CORNER_POWERED, 180, 0),
            new Edge(Direction.DOWN, Direction.WEST, CORNER, CORNER_POWERED, 180, 90),
            new Edge(Direction.DOWN, Direction.NORTH, CORNER, CORNER_POWERED, 180, 180),
            new Edge(Direction.DOWN, Direction.EAST, CORNER, CORNER_POWERED, 180, 270),
            new Edge(Direction.NORTH, Direction.EAST, CORNER_VERTICAL, CORNER_VERTICAL_POWERED, 0, 0),
            new Edge(Direction.SOUTH, Direction.EAST, CORNER_VERTICAL, CORNER_VERTICAL_POWERED, 0, 90),
            new Edge(Direction.SOUTH, Direction.WEST, CORNER_VERTICAL, CORNER_VERTICAL_POWERED, 0, 180),
            new Edge(Direction.NORTH, Direction.WEST, CORNER_VERTICAL, CORNER_VERTICAL_POWERED, 0, 270));

    public InlayCarrierRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(
            InlayCarrierBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        Level level = blockEntity.getLevel();
        if (level == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        BlockState state = blockEntity.getBlockState();
        BlockInlays inlays = BlockInlayManager.get(level, pos);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());

        for (Edge edge : EDGES) {
            if (!isInlaid(state, edge.a()) || !isInlaid(state, edge.b())) continue;
            draw(poseStack, consumer, level, edge.xRot(), edge.yRot(),
                    isPoweredCorner(state, inlays, edge) ? edge.powered() : edge.plain(),
                    packedLight, packedOverlay);
        }

        for (Direction face : Direction.values()) {
            // 连接件是通道的延伸：只在已镶嵌、且邻居是红石粉 / 红石导线的面上绘制。
            if (!isInlaid(state, face)) continue;
            if (!isRedstoneWiring(level.getBlockState(pos.relative(face)))) continue;
            boolean powered = state.getValue(InlayCarrierBlock.property(face)).isPowered();
            draw(poseStack, consumer, level, faceRotX(face), faceRotY(face),
                    powered ? CONNECTED_POWERED : CONNECTED, packedLight, packedOverlay);
        }

        // 设定值只在准星指向该部件时渲染。
        Direction focused = focusedFace(pos, state);
        if (focused != null && isInlaid(state, focused)) {
            LogicGateType gateType = inlays.getGateType(focused);
            if (gateType.isSettable()) {
                renderGateValue(poseStack, buffer, focused, inlays.getValue(focused), packedLight);
            }
        }
    }

    /** 玩家准星指向本方块时命中的部件面；未指向本方块（或未命中）时返回 {@code null}。 */
    private static Direction focusedFace(BlockPos pos, BlockState state) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) return null;
        if (!(minecraft.hitResult instanceof BlockHitResult hit)) return null;
        if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(pos)) return null;
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
        Direction picked = InlayCarrierBlock.pickFace(state, pos, from, to);
        return picked != null ? picked : hit.getDirection();
    }

    /** 把设定值绘制在通道部件垂直于通道方向的四个侧面上（每个面都朝向该侧外侧）。 */
    private static void renderGateValue(
            PoseStack poseStack,
            MultiBufferSource buffer,
            Direction face,
            int value,
            int packedLight) {
        Font font = Minecraft.getInstance().font;
        String text = String.valueOf(value);
        // 字体以“像素”为单位绘制（行高 9），需缩到通道部件的横截面尺寸：约 1.75/16 格高。
        float scale = (1.75F / 16.0F) / font.lineHeight;
        int color = 0xFFFFFFFF;
        Vector3f along = axis(face);

        for (Direction side : Direction.values()) {
            if (side.getAxis() == face.getAxis()) continue;

            Vector3f normal = axis(side);
            // 文本“上”沿通道朝外，于是数字底部指向核心。
            Vector3f up = along;
            Vector3f right = new Vector3f(up).cross(normal);

            // Matrix3f 的构造参数按「列」填充，故三个列向量依次是 right / up / normal。
            Quaternionf rotation = new Matrix3f(
                    right.x(), right.y(), right.z(),
                    up.x(), up.y(), up.z(),
                    normal.x(), normal.y(), normal.z()
            ).getNormalizedRotation(new Quaternionf());

            poseStack.pushPose();
            // 沿通道向方块外侧偏移，避开靠近核心的棱角件对文字的遮挡。
            poseStack.translate(
                    0.5 + along.x() * 0.4F + normal.x() * 0.128F,
                    0.5 + along.y() * 0.4F + normal.y() * 0.128F,
                    0.5 + along.z() * 0.4F + normal.z() * 0.128F);
            poseStack.mulPose(rotation);
            // 原版字体以 +Y 为“下”，这里翻转 Y 使文字在世界上正立。
            poseStack.scale(scale, -scale, scale);
            font.drawInBatch(text, -font.width(text) / 2.0F, -font.lineHeight / 2.0F, color, false,
                    poseStack.last().pose(), buffer, Font.DisplayMode.NORMAL, 0, packedLight);
            poseStack.popPose();
        }
    }

    private static Vector3f axis(Direction direction) {
        return new Vector3f(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    @Override
    public AABB getRenderBoundingBox(InlayCarrierBlockEntity blockEntity) {
        // 连接件伸出方块 3 像素，包围盒需要相应外扩，避免被视锥剔除。
        return new AABB(blockEntity.getBlockPos()).inflate(0.25);
    }

    private static boolean isInlaid(BlockState state, Direction face) {
        return state.getValue(InlayCarrierBlock.property(face)).isInlaid();
    }

    /** 已镶嵌面的外观分类：门类型 + 当前是否通电。 */
    private static CarrierFace faceState(BlockState state, BlockInlays inlays, Direction face) {
        CarrierWire wire = state.getValue(InlayCarrierBlock.property(face));
        return CarrierFace.of(wire.isInlaid(), inlays.getGateType(face), wire.isPowered());
    }

    /** 棱角是否 powered：相邻两面恰为「输入门收到信号」与「消耗输入面的门」。 */
    private static boolean isPoweredCorner(BlockState state, BlockInlays inlays, Edge edge) {
        CarrierFace a = faceState(state, inlays, edge.a());
        CarrierFace b = faceState(state, inlays, edge.b());
        return (a == CarrierFace.SOURCE && b.consumesInput())
                || (b == CarrierFace.SOURCE && a.consumesInput());
    }

    /** 邻居是否为原版红石粉或铁砧工艺红石导线。 */
    private static boolean isRedstoneWiring(BlockState neighbor) {
        return neighbor.is(Blocks.REDSTONE_WIRE) || neighbor.getBlock() instanceof RedstoneWireBlock;
    }

    private static void draw(
            PoseStack poseStack,
            VertexConsumer consumer,
            Level level,
            int xRot,
            int yRot,
            ModelResourceLocation location,
            int packedLight,
            int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyRotation(poseStack, xRot, yRot);
        poseStack.translate(-0.5, -0.5, -0.5);
        renderShaded(poseStack.last(), consumer, model, packedLight, packedOverlay, level);
        poseStack.popPose();
    }

    /** 逐面按世界方向做漫反射着色渲染模型（等价于区块渲染器对静态方块的着色，无 AO）。 */
    private static void renderShaded(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            BakedModel model,
            int packedLight,
            int packedOverlay,
            Level level) {
        for (Direction cull : Direction.values()) {
            RANDOM.setSeed(42L);
            renderQuads(pose, consumer, model.getQuads(null, cull, RANDOM), packedLight, packedOverlay, level);
        }
        RANDOM.setSeed(42L);
        renderQuads(pose, consumer, model.getQuads(null, null, RANDOM), packedLight, packedOverlay, level);
    }

    private static void renderQuads(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            List<BakedQuad> quads,
            int packedLight,
            int packedOverlay,
            Level level) {
        for (BakedQuad quad : quads) {
            // 面法线经姿态法线矩阵变换到世界方向，取最近的六向，据此取漫反射系数。
            Direction local = quad.getDirection();
            Vector3f n = new Vector3f(local.getStepX(), local.getStepY(), local.getStepZ());
            n.mul(pose.normal());
            Direction worldDir = Direction.getNearest(n.x(), n.y(), n.z());
            float shade = level.getShade(worldDir, true);
            consumer.putBulkData(pose, quad, shade, shade, shade, 1.0f, packedLight, packedOverlay);
        }
    }

    /** 与 blockstate multipart 相同的朝向旋转（{@code BlockModelRotation} 等效：rotateYXZ(-y, -x, 0)）。 */
    private static void applyRotation(PoseStack poseStack, int xRot, int yRot) {
        if (yRot != 0) poseStack.mulPose(Axis.YP.rotationDegrees(-yRot));
        if (xRot != 0) poseStack.mulPose(Axis.XP.rotationDegrees(-xRot));
    }

    private static int faceRotX(Direction face) {
        return switch (face) {
            case DOWN -> 90;
            case UP -> 270;
            default -> 0;
        };
    }

    private static int faceRotY(Direction face) {
        return switch (face) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }

    private static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(
                ResourceLocation.fromNamespaceAndPath(AnvilCraftDogePlus.MOD_ID, "block/" + path));
    }
}
