package dev.anvilcraft.gtouming.doge_plus.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * 巨型 Doge 砧中心那三个各自绕轴旋转的核心模型。
 *
 * <p>没有方块实体，所以既没有现成的实例列表、也没有可以存转速的地方，两件事都在这里自己解决：</p>
 * <ul>
 *   <li><b>位置</b>：整套巨型砧模型只由 {@code cube=center} 那一格承载（见
 *       {@code GiantAnvilBlock#getModelHolderState}），于是「一个成型的砧」就等于「一个 cube=center 的方块」。
 *       放置 / 拆除 / 落地重建由方块的状态变化回调当场增删（见 {@link #updateAnchor}），
 *       区块加载那类不走方块更新的情况由客户端扫描兜底：每 tick 按「离玩家最近的先扫」
 *       扫固定几个区块，渲染时再逐个校验方块还在不在。整条链路不需要方块实体，也不需要新增网络包。</li>
 *   <li><b>转速</b>：沿用原来方块实体的 +1°/tick，那本来就只是世界时间的函数，直接从
 *       {@link net.minecraft.world.level.Level#getGameTime()} 推出来即可，不需要任何状态。</li>
 * </ul>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class GiantDogeAnvilRenderer {

    /** 三个核心模型（分别绕 X / Y / Z 轴旋转）与它们各自的轴。 */
    private static final ModelResourceLocation[] CORES = {
            ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/anvil_core")),
            ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/anvil_core1")),
            ModelResourceLocation.standalone(AnvilCraftDogePlus.of("block/anvil_core2"))};
    private static final Axis[] AXES = {Axis.XP, Axis.YP, Axis.ZP};

    /** 扫描半径（区块）。核心只是砧中间一小块，超出可视距离本来也看不清。 */
    private static final int SCAN_RADIUS = 8;
    /** 每 tick 最多扫几个区块：整圈 (2R+1)² 块扫完约需这些 tick 数。 */
    private static final int CHUNKS_PER_TICK = 3;
    /** 核心相对方块中心的缩放。 */
    private static final float CORE_SCALE = 0.8F;
    /**
     * 竖直修正：锚点是 {@code cube=center} 那一格（3×3×3 的中层中心），而原方块实体挂在 TOP_CENTER 上、
     * 把模型下移了 1.3——减掉两层之差就是现在该用的偏移，观感与原实现一致。
     */
    private static final float ANCHOR_OFFSET_Y = -0.3F;

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
            SCAN_ORDER.sort((a, b) -> Integer.compare(a.getX() * a.getX() + a.getZ() * a.getZ(),
                    b.getX() * b.getX() + b.getZ() * b.getZ()));
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

        // 转速：原方块实体每 tick +1°，纯世界时间的函数。
        float rotation = (level.getGameTime() % 360) * 1.0F
                + event.getPartialTick().getGameTimeDeltaPartialTick(false);

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
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY() + ANCHOR_OFFSET_Y, pos.getZ());
            for (int i = 0; i < CORES.length; i++) {
                renderCore(poseStack, consumer, CORES[i], AXES[i], rotation);
            }
            poseStack.popPose();
        }
        poseStack.popPose();
        buffers.endBatch(RenderType.cutout());
    }

    /** 绕指定轴旋转并渲染一个核心模型。 */
    private static void renderCore(PoseStack poseStack, VertexConsumer consumer,
                                   ModelResourceLocation model, Axis axis, float rotation) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(CORE_SCALE, CORE_SCALE, CORE_SCALE);
        poseStack.mulPose(axis.rotationDegrees(rotation));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        Minecraft.getInstance()
                .getBlockRenderer()
                .getModelRenderer()
                .renderModel(
                        poseStack.last(),
                        consumer,
                        null,
                        Minecraft.getInstance().getModelManager().getModel(model),
                        1.0F, 1.0F, 1.0F,
                        LightTexture.FULL_BLOCK,
                        OverlayTexture.NO_OVERLAY
                );
        poseStack.popPose();
    }
}
