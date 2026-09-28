package dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.PipeCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.dubhe.anvilcraft.block.RedstoneWireBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.RedstoneSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ClientHooks;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

/**
 * 镶嵌载体连接件与搬运方向指示件渲染器。
 *
 * <p>通道、中心体与棱件全部由 blockstate 的 multipart 绘制（原版模型渲染，带逐顶点环境光遮蔽与 uv
 * 旋转）；通道与棱件的通电外观靠各自换贴图（无信号 / 有信号两套合成材质）。这里只画两类部件：</p>
 * <ul>
 *   <li><b>红石连接件</b>（只有逻辑 / 普通载体）：取决于邻居是否为红石粉 / 红石导线，逐面布尔属性
 *       会让状态数膨胀 64 倍；并按「该面是不是输入门」与是否通电换用输入 / 非输入两个变体。
 *       管道面不参与红石，不画；</li>
 *   <li><b>管道搬运方向指示件</b>（存入 / 取出两个手写模型）：传输链的首尾（正邻格没有互补搬运角色
 *       ——链首从容器取货、链尾把货送进容器），以及「相对两面角色相同」这种接线异常处。
 *       链内正常相接的面不画。</li>
 * </ul>
 *
 * <p><b>光照</b>：BER 这条路径只有逐面方向漫反射（按经姿态变换后的<b>世界方向</b>取
 * {@link net.minecraft.world.level.BlockAndTintGetter#getShade}，上亮下暗），没有环境光遮蔽——
 * 与前置 {@code PipeCheckValveBERenderer} 的取舍一致。正因为如此，凡是能交给 blockstate 的部件
 * 都交给 blockstate：那边是原版模型渲染，旁边有方块时会正确压暗。</p>
 *
 * <p>模型的朝向旋转与 blockstate multipart 完全一致（{@code BlockModelRotation} 等效：
 * {@code rotateYXZ(-y, -x, 0)}）。</p>
 */
public class InlayCarrierRenderer implements BlockEntityRenderer<InlayCarrierBlockEntity> {

    private static final ModelResourceLocation CONNECTED = standalone("carrier/logic/connected");
    private static final ModelResourceLocation CONNECTED_POWERED =
            standalone("carrier/logic/connected_powered");
    /** 该面是输入门时换一套连接件：外接红石是喂给载体的，接法与非输入面不同。 */
    private static final ModelResourceLocation CONNECTED_INPUT = standalone("carrier/logic/connected_input");
    private static final ModelResourceLocation CONNECTED_INPUT_POWERED =
            standalone("carrier/logic/connected_input_powered");
    /** 管道的搬运方向指示件：存入（把货送进面朝容器）/ 取出（从面朝容器取货）。 */
    private static final ModelResourceLocation PIPE_INSERT = standalone("carrier/pipe/connected_insert");
    private static final ModelResourceLocation PIPE_EXTRACT = standalone("carrier/pipe/connected_extract");

    /** 需要在 {@code ModelEvent.RegisterAdditional} 中注册的模型。 */
    public static final List<ModelResourceLocation> MODELS = List.of(
            CONNECTED, CONNECTED_POWERED,
            CONNECTED_INPUT, CONNECTED_INPUT_POWERED,
            PIPE_INSERT, PIPE_EXTRACT);

    private static final RandomSource RANDOM = RandomSource.create();

    /**
     * BER 部件的渲染类型：红石连接件与管道方向指示件都是 BER 专用几何（不参与区块渲染，
     * 模型自己声明的 {@code render_type} 只作用于区块那趟），这里统一走镂空。
     */
    private static final RenderType PART_RENDER_TYPE = RenderType.cutout();

    /** 判定准星命中部件所用射线长度（与交互 / HUD 一致）。 */
    private static final double RAY_LENGTH = 6.0;

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
        boolean pipe = state.getBlock() instanceof PipeCarrierBlock;

        // 连接件与方向指示件都是同一种渲染类型，取一次缓冲连着画完即可：BufferSource 一被要求换成
        // 别的类型就会把上一批结束掉，之后再往旧 consumer 里写会抛「Not building!」。
        VertexConsumer consumer = buffer.getBuffer(PART_RENDER_TYPE);
        for (Direction face : Direction.values()) {
            if (pipe) {
                // 管道方向指示件：画在传输链的首尾，以及「相对两面角色相同」这种接线异常处（方便排查链路）。
                // 管道面不参与红石，所以没有红石连接件。
                FaceMode mode = inlays.getFace(face);
                if (mode == FaceMode.NONE) continue;
                if (!showsTransferArrow(level, pos, inlays, face, mode)) continue;
                draw(poseStack, consumer, level, faceRotX(face), faceRotY(face),
                        mode == FaceMode.INSERT ? PIPE_INSERT : PIPE_EXTRACT, packedLight, packedOverlay);
                continue;
            }
            // 连接件是通道的延伸：只在已镶嵌、且邻居是红石粉 / 红石导线的面上绘制。
            if (!isInlaid(state, face)) continue;
            BlockState wire = level.getBlockState(pos.relative(face));
            if (!isRedstoneWiring(wire)) continue;
            boolean powered = state.getValue(InlayCarrierBlock.property(face)).isPowered();
            // 该面是输入门时，外接红石是把信号喂给载体，接法不同，用输入变体。
            ModelResourceLocation model = inlays.getFace(face) == FaceMode.INPUT
                    ? (powered ? CONNECTED_INPUT_POWERED : CONNECTED_INPUT)
                    : (powered ? CONNECTED_POWERED : CONNECTED);
            // 连接件朝向：铁砧工艺红石导线可附着在任意面，按它当前的附着面摆姿态；
            // 原版红石粉只会平铺在方块顶面（没有可跟随的附着面），仅在导线沿本面爬升时立起来。
            int roll = wire.getBlock() instanceof RedstoneWireBlock
                    ? attachmentRoll(face, wire.getValue(RedstoneWireBlock.ATTACHMENT))
                    : (isWiringClimbing(wire, face.getOpposite()) ? 90 : 0);
            draw(poseStack, consumer, level, faceRotX(face), faceRotY(face), roll, model, packedLight, packedOverlay);
        }

        // 设定值 / 物流量只在准星指向该部件时渲染。
        Direction focused = focusedFace(pos, state);
        if (focused != null && isInlaid(state, focused)) {
            if (pipe) {
                // 管道「存入」面：过滤物品先画（在数字层之下），物流量数字压在其上层。
                // 两者都要躲开通道棱件与搬运箭头，故用更大的横向偏移（见 PIPE_VALUE_SIDE_OFFSET）。
                if (inlays.getFace(focused) == FaceMode.INSERT) {
                    renderFilterItem(poseStack, buffer, level, focused, inlays.getFilter(focused),
                            packedLight, packedOverlay);
                    renderGateValue(poseStack, buffer, focused, inlays.getThroughput(focused), packedLight,
                            PIPE_VALUE_SIDE_OFFSET);
                }
            } else {
                FaceMode gateType = inlays.getFace(focused);
                if (gateType.isSettable()) {
                    renderGateValue(poseStack, buffer, focused, inlays.getValue(focused), packedLight);
                }
            }
        }
    }

    /** 玩家准星指向本方块时命中的部件面；未指向本方块（或未命中）时返回 {@code null}。 */
    private static @Nullable Direction focusedFace(BlockPos pos, BlockState state) {
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

    /** 逻辑载体设定值数字相对通道侧面的横向偏移（通道横截面约为半径 2/16 格）。 */
    private static final float GATE_VALUE_SIDE_OFFSET = 0.128F;

    /**
     * 管道「存入」面上物流量数字的横向偏移。
     *
     * <p>横截面最外是棱件与搬运箭头，横向到 {@code 2.5/16} 格；数字与过滤物品都是垂直于该侧法线的
     * 平面，必须整个挪到那些几何之外，否则会被深度测试直接剔除（不会半透明混出来）。取 {@code 0.22}
     * 格，留出约 1 像素余量，且不越出该面。</p>
     */
    private static final float PIPE_VALUE_SIDE_OFFSET = 0.16F;

    /** 过滤物品比物流量数字再靠里一点：两者同向平行，靠这点深度差保证数字稳定压在物品之上。 */
    private static final float PIPE_FILTER_SIDE_OFFSET = PIPE_VALUE_SIDE_OFFSET;

    /** 把设定值绘制在通道部件垂直于通道方向的四个侧面上（每个面都朝向该侧外侧）。 */
    private static void renderGateValue(
            PoseStack poseStack,
            MultiBufferSource buffer,
            Direction face,
            int value,
            int packedLight) {
        renderGateValue(poseStack, buffer, face, value, packedLight, GATE_VALUE_SIDE_OFFSET);
    }

    /** 同上，但可指定横向偏移（管道面要躲开棱件 / 箭头，见 {@link #PIPE_VALUE_SIDE_OFFSET}）。 */
    private static void renderGateValue(
            PoseStack poseStack,
            MultiBufferSource buffer,
            Direction face,
            int value,
            int packedLight,
            float sideOffset) {
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
                    0.5 + along.x() * 0.4F + normal.x() * sideOffset,
                    0.5 + along.y() * 0.4F + normal.y() * sideOffset,
                    0.5 + along.z() * 0.4F + normal.z() * sideOffset);
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

    /** 过滤物品图标的缩放（相对一格，已按显示变换归一化）：与数字（约 1.75/16 格高）相近。 */
    private static final float FILTER_ITEM_SCALE = 0.3F;

    /** 大小归一化之后再缩 1/3：图标略小于物流量数字，不抢数字的视线。 */
    private static final float FILTER_ITEM_SHRINK = 1.0F / 3.0F;

    /** 沿图标法线压扁的比例：把 GUI 表现的三维图标压成一张平面图，贴在通道侧面上。 */
    private static final float FILTER_ITEM_FLATTEN = 0.05F;

    /**
     * 把过滤物品画在该面的通道「四周」——与 {@link #renderGateValue} 同一套 4 个侧向位姿，
     * 只是位置更靠核心（数字沿面轴偏移 {@code 0.4}，这里取 {@code 0.3}），且先于数字绘制，
     * 于是物品这一层在物流量数字之下。
     *
     * <p><b>表现</b>：用 {@code GUI} 显示变换（与物品栏一致），再沿图标法线压扁成平面，于是方块也是
     * 一张平贴的方图而不是立体的方块。{@code gui} 自带缩放（方块 0.625、平坦物品 1），这里按它归一化，
     * 各种过滤物品的图标一样大。</p>
     *
     * <p><b>光照</b>：不走物品渲染管线的着色——那套靠全局光照方向 uniform，而所有批次要到方块实体阶段
     * 结束才一起刷新，四个面共用同一份（剩下最后设置的那份）光照。这里和画棱件 / 箭头一样，
     * 直接按每个面的世界朝向取 {@link Level#getShade} 烘进顶点色，于是四个面各随环境明暗，
     * 也与批次刷新顺序无关。</p>
     */
    private static void renderFilterItem(
            PoseStack poseStack,
            MultiBufferSource buffer,
            Level level,
            Direction face,
            ItemStack filter,
            int packedLight,
            int packedOverlay) {
        if (filter.isEmpty()) return;
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        BakedModel model = itemRenderer.getModel(filter, level, null, 0);
        Vector3f guiScale = model.getTransforms().getTransform(ItemDisplayContext.GUI).scale;
        float scale = FILTER_ITEM_SCALE * FILTER_ITEM_SHRINK / Math.max(guiScale.x(), 0.01F);

        // 通道部件与物品都在方块图集上：直接按顶点色烘明暗即可。
        VertexConsumer consumer = buffer.getBuffer(PART_RENDER_TYPE);
        Vector3f along = axis(face);

        for (Direction side : Direction.values()) {
            if (side.getAxis() == face.getAxis()) continue;

            Vector3f normal = axis(side);
            Vector3f up = along;
            Vector3f right = new Vector3f(up).cross(normal);
            Quaternionf rotation = new Matrix3f(
                    right.x(), right.y(), right.z(),
                    up.x(), up.y(), up.z(),
                    normal.x(), normal.y(), normal.z()
            ).getNormalizedRotation(new Quaternionf());

            poseStack.pushPose();
            // 比数字（沿通道更靠外、横向也略外）更靠内，于是数字压在上层；
            // 横向偏移必须越过棱件 / 箭头，否则平面图标会被它们挡掉。
            poseStack.translate(
                    0.5 + along.x() * 0.3F + normal.x() * PIPE_FILTER_SIDE_OFFSET,
                    0.5 + along.y() * 0.3F + normal.y() * PIPE_FILTER_SIDE_OFFSET,
                    0.5 + along.z() * 0.3F + normal.z() * PIPE_FILTER_SIDE_OFFSET);
            // 压扁是绕图标中心做的，这里先把中心记下来。
            Vector3f center = poseStack.last().pose().getTranslation(new Vector3f());
            poseStack.mulPose(rotation);
            poseStack.scale(scale, scale, scale);
            // 与 renderStatic 一样：先套显示变换，再把模型中心挪到当前位姿原点。
            BakedModel icon = ClientHooks.handleCameraTransforms(
                    poseStack, model, ItemDisplayContext.GUI, false);
            poseStack.translate(-0.5F, -0.5F, -0.5F);
            renderShaded(flattened(poseStack, center, side), consumer, icon, packedLight, packedOverlay, level);
            poseStack.popPose();
        }
    }

    /**
     * 把当前姿态沿 {@code normal} 压扁成一张平面（贴在通道该侧面上），返回用于渲染的姿态。
     *
     * <p>世界轴缩放只能左乘，而 {@link PoseStack} 只有右乘，因此直接构造
     * {@code (绕图标中心缩放) × (当前矩阵)} 交给一个新栈。法线矩阵沿用压扁前的：明暗仍按图标原本的
     * 朝向算（压扁会让法线退化，用压扁后的法线矩阵反而不对）。</p>
     */
    private static PoseStack.Pose flattened(PoseStack poseStack, Vector3f center, Direction normal) {
        Matrix4f squash = new Matrix4f()
                .translate(center.x, center.y, center.z)
                .scale(
                        normal.getStepX() != 0 ? FILTER_ITEM_FLATTEN : 1.0F,
                        normal.getStepY() != 0 ? FILTER_ITEM_FLATTEN : 1.0F,
                        normal.getStepZ() != 0 ? FILTER_ITEM_FLATTEN : 1.0F)
                .translate(-center.x, -center.y, -center.z);
        PoseStack flat = new PoseStack();
        flat.last().pose().set(squash).mul(poseStack.last().pose());
        flat.last().normal().set(poseStack.last().normal());
        return flat.last();
    }

    @Override
    public AABB getRenderBoundingBox(InlayCarrierBlockEntity blockEntity) {
        // 连接件伸出方块 3 像素，包围盒需要相应外扩，避免被视锥剔除。
        return new AABB(blockEntity.getBlockPos()).inflate(0.25);
    }

    private static boolean isInlaid(BlockState state, Direction face) {
        return state.getValue(InlayCarrierBlock.property(face)).isInlaid();
    }

    /**
     * 该面是否要画搬运方向指示件。
     *
     * <p>两种情况：</p>
     * <ul>
     *   <li><b>传输链的一端</b>：传输网的边是「本方存入面 ↔ 正邻格的取出面」，正邻格没有互补角色，
     *       本面就没有下一条管道——取出面是从容器取货的链首，存入面是把货送进容器的链尾；</li>
     *   <li><b>相对两面角色相同</b>：正常链里一个管道相对的两面必是一取一存（直行）或落在相邻两面
     *       （拐角），相对同角色说明这里接错了，一并画出来方便一眼找到链路的问题。</li>
     * </ul>
     */
    private static boolean showsTransferArrow(
            Level level, BlockPos pos, BlockInlays inlays, Direction face, FaceMode mode) {
        FaceMode counterpart = mode == FaceMode.INSERT ? FaceMode.EXTRACT : FaceMode.INSERT;
        BlockInlays neighbor = BlockInlayManager.get(level, pos.relative(face));
        if (neighbor.getFace(face.getOpposite()) != counterpart) return true;
        return inlays.getFace(face.getOpposite()) == mode;
    }

    /** 邻居是否为原版红石粉或铁砧工艺红石导线。 */
    private static boolean isRedstoneWiring(BlockState neighbor) {
        return neighbor.is(Blocks.REDSTONE_WIRE) || neighbor.getBlock() instanceof RedstoneWireBlock;
    }

    /**
     * 邻居红石粉是否沿 {@code towardsUs} 面「爬升」（附着在该侧面上）。
     *
     * <p>红石粉每条边是 {@code NONE / SIDE / UP} 三态：{@code UP} 表示粉线顺着这一侧的方块
     * 表面爬上去。这种接法下连接件要立起来，平铺（{@code SIDE}）时保持躺平。</p>
     *
     * <p>属性按<b>名字</b>找（就是方向名），不依赖具体类的静态表；找不到该属性
     * （例如第三方改过的粉线）时按不爬升处理。铁砧工艺红石导线没有这个属性，取值为
     * {@code ConnectionType} 而非 {@link RedstoneSide}，也会走到这里返回 {@code false}——
     * 它的朝向改由 {@link #attachmentRoll} 按附着面决定。</p>
     */
    private static boolean isWiringClimbing(BlockState wireState, Direction towardsUs) {
        if (!isRedstoneWiring(wireState)) return false;
        for (var property : wireState.getProperties()) {
            if (!property.getName().equals(towardsUs.getSerializedName())) continue;
            if (property.getValueClass() != RedstoneSide.class) continue;
            @SuppressWarnings({"unchecked", "rawtypes"})
            var side = (RedstoneSide) wireState.getValue((Property) property);
            return side == RedstoneSide.UP;
        }
        return false;
    }

    /**
     * 连接件绕本面法线的滚转角：把模型 +Y（连接件长轴）转到导线附着面的反方向。
     *
     * <p>模型与 {@link #faceRotX}/{@link #faceRotY} 的基准姿态对应「导线附着在方块顶面」：
     * 此时 +Y 朝上，输入型连接件就落在该面下方（模型自带的下方姿态）。铁砧工艺红石导线
     * 可附着在任意面，附着方向一变，连接件就得绕面法线滚到导线所在的那一层，才贴着导线
     * 所在的平面；滚到 +Y 指向附着面的反方向时正是这个位置。</p>
     *
     * <p>附着方向落在本面法线上（导线贴在背面或远端支撑）时导线与本面不共面，没有可跟随的
     * 朝向，返回 0 保持基准姿态。</p>
     */
    private static int attachmentRoll(Direction face, Direction attachment) {
        if (attachment.getAxis() == face.getAxis()) return 0;
        Direction up = baseUp(face);
        Direction target = attachment.getOpposite();
        int cos = up.getStepX() * target.getStepX()
                + up.getStepY() * target.getStepY()
                + up.getStepZ() * target.getStepZ();
        if (cos == 1) return 0;
        if (cos == -1) return 180;
        // 绕局部 +Z（世界 -face）的有符号正弦，剩余两种情形正好落在 ±90。
        int crossX = up.getStepY() * target.getStepZ() - up.getStepZ() * target.getStepY();
        int crossY = up.getStepZ() * target.getStepX() - up.getStepX() * target.getStepZ();
        int crossZ = up.getStepX() * target.getStepY() - up.getStepY() * target.getStepX();
        int sin = -(crossX * face.getStepX() + crossY * face.getStepY() + crossZ * face.getStepZ());
        return sin > 0 ? 90 : -90;
    }

    /** 基准姿态（面旋转后）下模型 +Y 指向的世界方向。 */
    private static Direction baseUp(Direction face) {
        return switch (face) {
            case UP -> Direction.SOUTH;
            case DOWN -> Direction.NORTH;
            default -> Direction.UP;
        };
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
        draw(poseStack, consumer, level, xRot, yRot, 0, location, packedLight, packedOverlay);
    }

    /**
     * @param roll 按面转完后再绕该面法线（模型局部 +Z）滚动的角度：导线附着面在本面之外时，
     *             连接件跟着滚到导线所在的那一层
     */
    private static void draw(
            PoseStack poseStack,
            VertexConsumer consumer,
            Level level,
            int xRot,
            int yRot,
            int roll,
            ModelResourceLocation location,
            int packedLight,
            int packedOverlay) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(location);
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        applyRotation(poseStack, xRot, yRot);
        if (roll != 0) {
            // 模型本地的面朝向是 +Z，绕它滚即可把连接件摆到侧面上
            poseStack.mulPose(Axis.ZP.rotationDegrees(roll));
        }
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
