package dev.anvilcraft.gtouming.doge_plus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.GiantDogeAnvil;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.*;

/**
 * 巨型 Doge 砧中心 {@code light} 组的呼吸灯。
 *
 * <p>没有方块实体，所以既没有现成的实例列表、也没有可以存相位的地方，都在这里自己解决：</p>
 * <ul>
 *   <li><b>位置</b>：整套巨型砧模型只由 {@code cube=center} 那一格承载（见
 *       {@code GiantAnvilBlock#getModelHolderState}），于是「一个成型的砧」就等于「一个 cube=center 的方块」。
 *       放置 / 拆除 / 落地重建由方块的状态变化回调当场增删（见 {@link #updateAnchor}），
 *       区块加载那类不走方块更新的情况由客户端扫描兜底：每 tick 按「离玩家最近的先扫」
 *       扫固定几个区块，渲染时再逐个校验方块还在不在。整条链路不需要方块实体，也不需要新增网络包。</li>
 *   <li><b>相位</b>：呼吸亮度只是世界时间的函数，直接从
 *       {@link net.minecraft.world.level.Level#getGameTime()} 推出来即可，不需要任何状态。</li>
 * </ul>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class GiantDogeAnvilRenderer {

    /**
     * 模型 {@code light} 组（一串同心发光立方体）的独立模型。
     *
     * <p>该组已从方块模型 {@code giant_doge_anvil} 中移除，由本渲染器以同等大小在锚点处绘制，
     * 并按世界时间调制亮度做出呼吸灯——方块模型整体是 {@code emissiveRendering(always)}，
     * 静态渲染里没法单独调 light 组的亮度。</p>
     */
    private static final ModelResourceLocation LIGHT =
            ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/giant_doge_anvil_light"));

    /** 呼吸周期（tick）：约 6 秒。 */
    private static final float LIGHT_PERIOD = 120.0F;

    /** 扫描半径（区块）。light 只是砧中间一小块，超出可视距离本来也看不清。 */
    private static final int SCAN_RADIUS = 8;
    /** 每 tick 最多扫几个区块：整圈 (2R+1)² 块扫完约需这些 tick 数。 */
    private static final int CHUNKS_PER_TICK = 3;

    /** 已发现的锚点（{@code cube=center} 的方块位置）。 */
    private static final Set<BlockPos> ANCHORS = new HashSet<>();
    /** 相对玩家所在区块的扫描顺序，按距离由近到远；换区块时重建。 */
    private static final List<BlockPos> SCAN_ORDER = new ArrayList<>();
    private static long scannedChunk = Long.MIN_VALUE;
    private static int cursor;

    private GiantDogeAnvilRenderer() {
    }

    /** 该状态是不是「一个成型的巨型砧」的锚点。 */
    private static boolean isAnchor(BlockState state) {
        return state.is(ModBlocks.GIANT_DOGE_ANVIL.get())
                && state.getValue(GiantDogeAnvil.CUBE) == GiantAnvilCube.CENTER;
    }

    /**
     * 方块状态变化后由方块调用（客户端侧）：是锚点就当场记下，不再是就把旧的删掉。
     *
     * <p>用的是 NeoForge 的 {@code IBlockExtension#onBlockStateChange}——它**两侧都会调用**；原版的
     * {@code onPlace}/{@code onRemove} 在 {@code LevelChunk.setBlockState} 里被
     * {@code !isClientSide} 挡掉了，客户端根本收不到，所以那条路走不通。</p>
     */
    public static void updateAnchor(BlockState state, BlockPos pos) {
        if (isAnchor(state)) ANCHORS.add(pos.immutable());
        else ANCHORS.remove(pos);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null) {
            ANCHORS.clear();
            scannedChunk = Long.MIN_VALUE;
            return;
        }

        long chunk = player.chunkPosition().toLong();
        if (chunk != scannedChunk) {
            // 玩家换区块：按到玩家的距离重排扫描顺序，最近的最先扫到，附近新放的砧几乎立刻可见。
            scannedChunk = chunk;
            cursor = 0;
            SCAN_ORDER.clear();
            for (int dx = -SCAN_RADIUS; dx <= SCAN_RADIUS; dx++) {
                for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
                    SCAN_ORDER.add(new BlockPos(dx, 0, dz));
                }
            }
            SCAN_ORDER.sort(Comparator.comparingInt(a -> a.getX() * a.getX() + a.getZ() * a.getZ()));
        }

        for (int i = 0; i < CHUNKS_PER_TICK; i++) {
            BlockPos offset = SCAN_ORDER.get(cursor);
            cursor = (cursor + 1) % SCAN_ORDER.size();
            scanChunk(level, player.chunkPosition().x + offset.getX(), player.chunkPosition().z + offset.getZ());
        }
    }

    /** 扫一个区块，把其中的锚点收进集合。没加载的区块直接跳过。 */
    private static void scanChunk(ClientLevel level, int chunkX, int chunkZ) {
        ChunkAccess access = level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (!(access instanceof LevelChunk chunk)) return;
        int minY = level.getMinBuildHeight();
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (section == null || section.hasOnlyAir()) continue;
            // 调色板里没有本方块就整段跳过；全局调色板的分段 maybeHas 一律返回 true，只能老实逐格看。
            if (!section.maybeHas(GiantDogeAnvilRenderer::isAnchor)) continue;
            int sectionY = minY + index * LevelChunkSection.SECTION_HEIGHT;
            for (int y = 0; y < LevelChunkSection.SECTION_HEIGHT; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (isAnchor(section.getBlockState(x, y, z))) {
                            ANCHORS.add(new BlockPos((chunkX << 4) + x, sectionY + y, (chunkZ << 4) + z));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            ANCHORS.clear();
            SCAN_ORDER.clear();
            scannedChunk = Long.MIN_VALUE;
        }
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) return;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || ANCHORS.isEmpty()) return;

        // 呼吸灯亮度同样只是世界时间的函数，无需任何状态。
        float breath = breathBrightness(
                level.getGameTime() + event.getPartialTick().getGameTimeDeltaPartialTick(false));

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer consumer = buffers.getBuffer(RenderType.cutout());

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        Iterator<BlockPos> iterator = ANCHORS.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            // 顺手校验：拆掉或被替换掉的锚点在这里淘汰，不需要任何拆除通知。
            if (!isAnchor(level.getBlockState(pos))) {
                iterator.remove();
                continue;
            }

            // 呼吸灯：light 组已从方块模型移除，这里以同等大小在方块局部空间绘制，亮度按时间脉动。
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            renderLight(poseStack, consumer, breath);
            poseStack.popPose();
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.cutout());
    }

    /**
     * 在给定姿态下渲染一次 {@code light} 组呼吸灯，亮度同样只由世界时间推出，效果与
     * {@link #onRenderLevelStage} 一致。
     *
     * <p>供坠落实体渲染器复用：巨型砧下落实体的方块模型同样以 {@code cube=center} 的局部空间绘制，
     * 姿态定位到同一原点即可对齐。方块形态的呼吸灯仍由世界阶段事件负责。</p>
     *
     * @param poseStack   已定位到巨型砧中心格原点的姿态
     * @param buffer      目标缓冲
     * @param partialTick 当前渲染的插值 tick
     */
    public static void renderBreathingLight(PoseStack poseStack, MultiBufferSource buffer, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        float breath = breathBrightness(level.getGameTime() + partialTick);
        VertexConsumer consumer = buffer.getBuffer(RenderType.cutout());
        renderLight(poseStack, consumer, breath);
        if (buffer instanceof MultiBufferSource.BufferSource buffers) buffers.endBatch(RenderType.cutout());
    }

    /**
     * {@link #renderBreathingLight(PoseStack, MultiBufferSource, float)} 的便捷入口：用当前帧插值 tick
     * 计算相位。物品渲染路径拿不到 partialTick，走这里。
     */
    public static void renderBreathingLight(PoseStack poseStack, MultiBufferSource buffer) {
        renderBreathingLight(
                poseStack, buffer, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
    }

    /** 呼吸灯亮度：正弦在 0.7~1.0 之间脉动，周期约 6 秒。 */
    private static float breathBrightness(float time) {
        // 相位用取模后的时间算，避免世界时间很大时 float 精度损失让正弦跳变甚至冻结。
        float phase = (time % LIGHT_PERIOD) * (float) (Math.PI * 2.0 / LIGHT_PERIOD);
        float pulse = Mth.sin(phase) * 0.5F + 0.5F;
        return 0.7F + 0.3F * pulse;
    }

    /**
     * 渲染 light 组呼吸灯。
     *
     * <p>亮度用一层 {@link BrightnessVertexConsumer} 乘到顶点色上（不依赖 {@code renderModel}
     * 的 r/g/b 参数是否生效）；顶点光取全亮，做自发光。</p>
     */
    private static void renderLight(PoseStack poseStack, VertexConsumer consumer, float brightness) {
        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        new BrightnessVertexConsumer(consumer, brightness),
                        null,
                        Minecraft.getInstance().getModelManager().getModel(LIGHT),
                        1.0F, 1.0F, 1.0F,
                        LightTexture.FULL_BLOCK,
                        OverlayTexture.NO_OVERLAY
                );
    }

    /** 把顶点色整体乘上 {@code brightness}（保留模型自带的逐面明暗），透明度不变。 */
    private record BrightnessVertexConsumer(VertexConsumer delegate, float brightness) implements VertexConsumer {
        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            this.delegate.setColor(
                    Math.round(red * this.brightness),
                    Math.round(green * this.brightness),
                    Math.round(blue * this.brightness),
                    alpha);
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
            this.delegate.setUv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            this.delegate.setNormal(x, y, z);
            return this;
        }
    }
}
