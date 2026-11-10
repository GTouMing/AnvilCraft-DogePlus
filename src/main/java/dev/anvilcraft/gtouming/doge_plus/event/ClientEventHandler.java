package dev.anvilcraft.gtouming.doge_plus.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.AbstractInlayTableBlock;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MobileSilencerScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip.CarrierInspectionProvider;
import dev.anvilcraft.gtouming.doge_plus.client.gui.wheel.CarrierWheelModel;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayCarrierRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayCraftingTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.model.BlockStateItemModel;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.gtouming.doge_plus.network.AdjustGateValuePacket;
import dev.anvilcraft.gtouming.doge_plus.network.AdjustLogisticsThroughputPacket;
import dev.anvilcraft.gtouming.doge_plus.network.AdjustRemoteChannelPacket;
import dev.anvilcraft.gtouming.doge_plus.network.SetCarrierFacePacket;
import dev.anvilcraft.gtouming.doge_plus.network.SetLogisticsFilterPacket;
import dev.anvilcraft.gtouming.doge_plus.network.SetRecipeFilterPacket;
import dev.anvilcraft.gtouming.doge_plus.network.SetRemoteChannelItemPacket;
import dev.anvilcraft.gtouming.doge_plus.network.QuickEditCarrierPacket;
import dev.anvilcraft.gtouming.doge_plus.util.CarrierQuickEdit;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;

@EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    /** 门设定值判定部件所用射线长度（与交互侧一致）。 */
    private static final double RAY_LENGTH = 6.0;

    public static final KeyMapping OPEN_SILENCER = new KeyMapping(
            "key.anvilcraft_doge_plus.open_silencer",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_V,
            "key.categories.anvilcraft_doge_plus"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SILENCER);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        var minecraft = Minecraft.getInstance();
        var player = minecraft.player;
        if (player == null) return;

        if (OPEN_SILENCER.consumeClick()) {
            var stack = MobileSilencer.findMobileSilencer(player);
            if (stack.is(ModItems.MOBILE_SILENCER)) {
                minecraft.setScreen(new MobileSilencerScreen(stack));
            }
        }
    }

    // ==================== 逻辑载体：长按右键呼出轮盘 ====================

    /** 长按右键多少 tick 后呼出轮盘（与前置的铁砧锤轮盘手势一致）。 */
    private static final long OPEN_CARRIER_WHEEL_DELAY = 4;

    /** 呼出轮盘的计时：{@code -1} 表示当前没有按住。 */
    private static long carrierWheelPressTick = -1L;
    @Nullable
    private static BlockPos carrierWheelPos = null;
    @Nullable
    private static Direction carrierWheelFace = null;
    private static boolean carrierWheelOpened = false;

    /** 该次长按是否可能呼出轮盘（只有逻辑 / 物流载体有轮盘；普通载体只支持点按设信道）。 */
    private static boolean carrierWheelCapable = false;

    /** 镶嵌台 / 镶合台配方过滤的点按目标；{@code null} 表示没有进行中。 */
    @Nullable
    private static BlockPos recipeFilterPos = null;

    /** 载体快捷编辑的点按目标；{@code null} 表示没有进行中。 */
    @Nullable
    private static BlockPos quickEditPos = null;
    @Nullable
    private static Direction quickEditFace = null;

    /** lib 的轮盘控制器：负责打开 hold 轮盘、松手时触发选中项。 */
    private static final WheelScreenController WHEEL_CONTROLLER = new WheelScreenController();

    @SubscribeEvent
    public static void onCarrierWheelMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;
        if (!client.options.keyUse.matchesMouse(event.getButton())) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            if (client.screen != null || carrierWheelOpened) return;
            if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = hit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);

            // 载体快捷编辑：手持同类型载体右键，且点击方向上的相邻方块也是同类型载体。
            // 放在 Ctrl 判断之前，Ctrl 用于取反。
            if (isCarrierQuickEdit(player, pos, state, hit.getDirection())) {
                quickEditPos = pos;
                quickEditFace = hit.getDirection();
                return;
            }

            // 按住 Ctrl 是「门链手势」（移除链需要铁砧锤）：此时不进入轮盘长按计时，避免两者冲突。
            if (Screen.hasControlDown()) return;
            if (!player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)) return;

            // 镶嵌台 / 镶合台：点按水平侧面 → 用副手物品设定配方过滤（不呼出轮盘）。
            recipeFilterPos = null;
            if (state.getBlock() instanceof AbstractInlayTableBlock
                    && hit.getDirection().getAxis().isHorizontal()) {
                recipeFilterPos = pos;
                return;
            }

            if (!(state.getBlock() instanceof InlayCarrierBlock)) return;

            // 与交互 / HUD 同一套「按模型部件判定面」，仅命中中心体时退回方块面。
            Vec3 from = player.getEyePosition(1.0F);
            Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
            Direction picked = InlayCarrierBlock.pickFace(state, pos, from, to);

            carrierWheelPressTick = client.level.getGameTime();
            carrierWheelPos = pos;
            carrierWheelFace = picked != null ? picked : hit.getDirection();
            // 只有逻辑 / 物流载体能呼出轮盘；普通载体只支持点按设信道。
            carrierWheelCapable = state.getBlock() instanceof InlayCarrierBlock carrierBlock
                    && carrierBlock.isProgrammable();
            return;
        }

        if (event.getAction() == GLFW.GLFW_RELEASE) {
            // 载体快捷编辑：松手时按 Ctrl 是否按下来决定默认方向是否取反。
            if (quickEditPos != null && quickEditFace != null) {
                PacketDistributor.sendToServer(new QuickEditCarrierPacket(
                        quickEditPos, quickEditFace, Screen.hasControlDown()));
                quickEditPos = null;
                quickEditFace = null;
                resetCarrierWheel();
                return;
            }
            // 镶嵌台 / 镶合台：点按松手 → 用副手物品设定配方过滤（副手空 = 清除）。
            if (recipeFilterPos != null) {
                ItemStack offhand = player.getOffhandItem();
                PacketDistributor.sendToServer(new SetRecipeFilterPacket(
                        recipeFilterPos, offhand.isEmpty() ? ItemStack.EMPTY : offhand.copyWithCount(1)));
                recipeFilterPos = null;
                resetCarrierWheel();
                return;
            }
            // 松开右键 = 确认：交给 lib 的轮盘控制器触发选中项并收起轮盘。
            WHEEL_CONTROLLER.onHoldKeyReleased();
            // 未开轮盘即为「点按」：用副手物品设置该面的标识物（副手空 = 清除）。
            // 远程面 → 信道标识物品；物流载体的存入面 → 过滤物品。长按已把 carrierWheelOpened 置真，不会走到这里。
            if (!carrierWheelOpened && carrierWheelPos != null && carrierWheelFace != null && client.level != null) {
                FaceMode mode = BlockInlayManager.get(client.level, carrierWheelPos).getFace(carrierWheelFace);
                ItemStack offhand = player.getOffhandItem();
                ItemStack held = offhand.isEmpty() ? ItemStack.EMPTY : offhand.copyWithCount(1);
                if (mode == FaceMode.REMOTE) {
                    PacketDistributor.sendToServer(
                            new SetRemoteChannelItemPacket(carrierWheelPos, carrierWheelFace, held));
                } else if (client.level.getBlockState(carrierWheelPos).getBlock()
                        instanceof InlayCarrierBlock carrierBlock
                        && carrierBlock.supportsTransferSettings(mode)) {
                    PacketDistributor.sendToServer(
                            new SetLogisticsFilterPacket(carrierWheelPos, carrierWheelFace, held));
                }
            }
            resetCarrierWheel();
        }
    }

    @SubscribeEvent
    public static void onCarrierWheelTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null) return;

        // 轮盘打开期间持续终止铁砧锤的「使用」计时，避免 40 tick 后呼出便携铁砧。
        if (carrierWheelOpened) {
            stopUsingHammer(client, player);
            return;
        }
        if (carrierWheelPressTick < 0) return;
        if (client.level.getGameTime() - carrierWheelPressTick <= OPEN_CARRIER_WHEEL_DELAY) return;

        BlockPos pos = carrierWheelPos;
        Direction face = carrierWheelFace;
        if (pos == null || face == null) {
            resetCarrierWheel();
            return;
        }
        // 建立手势期间换成非铁砧锤 → 取消本次操作。
        if (!player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)) {
            resetCarrierWheel();
            return;
        }
        // 普通载体没有轮盘：只等松手做「点按设信道」。
        if (!carrierWheelCapable) return;
        carrierWheelOpened = true;
        // 按方块类型构造选项清单，交给 lib 的 hold 轮盘。
        openFaceWheel(pos, face, client.level.getBlockState(pos));
        stopUsingHammer(client, player);
    }

    /**
     * 打开面属性轮盘：逻辑载体列红石类选项（各门 + 清除），物流载体列搬运类选项（存入 / 取出 / 清除）。
     *
     * <p>两者都把选项组装成 lib 的 {@link WheelMenuModel}（见 {@link CarrierWheelModel}），
     * 由 {@link WheelScreenController} 以 hold 手势打开；松手时发出同一个编程包
     * {@link SetCarrierFacePacket}，由服务端按方块类型校验取值域。</p>
     */
    private static void openFaceWheel(BlockPos pos, Direction face, BlockState state) {
        if (!(state.getBlock() instanceof InlayCarrierBlock carrier) || !carrier.isProgrammable()) return;
        List<FaceMode> options = carrier.wheelModes();
        WheelMenuModel model = CarrierWheelModel.build(
                options,
                mode -> PacketDistributor.sendToServer(new SetCarrierFacePacket(pos, face, mode)));
        WHEEL_CONTROLLER.onHoldKeyPressed(model);
    }

    /** 终止铁砧锤的「使用」计时；仅本地停止不会通知服务端，必须同时发 RELEASE_USE_ITEM。 */
    private static void stopUsingHammer(Minecraft client, LocalPlayer player) {
        if (!player.isUsingItem()) return;
        player.stopUsingItem();
        Objects.requireNonNull(client.gameMode).releaseUsingItem(player);
    }

    /** 是否是「手持同类型载体右键，且点击面朝外是同类型载体 / 容器 / 红石粉」的快捷编辑手势。 */
    private static boolean isCarrierQuickEdit(LocalPlayer player, BlockPos pos, BlockState state, Direction face) {
        Block target = state.getBlock();
        if (!CarrierQuickEdit.isProgrammableCarrier(target)) return false;
        if (!CarrierQuickEdit.isHeldCarrierOf(player.getMainHandItem(), target)) return false;
        BlockPos otherPos = pos.relative(face);
        Block other = player.level().getBlockState(otherPos).getBlock();
        if (CarrierQuickEdit.isSameType(target, other)) return true;
        return target instanceof InlayCarrierBlock carrier
                && carrier.supportsEndpoint(player.level(), otherPos, face.getOpposite());
    }

    private static void resetCarrierWheel() {
        carrierWheelPressTick = -1L;
        carrierWheelPos = null;
        carrierWheelFace = null;
        carrierWheelOpened = false;
        carrierWheelCapable = false;
    }

    /** 铁砧锤指向面时，Ctrl + 滚轮调整该面的参数：物流存入面调物流量，逻辑门面调设定值（Shift 大步）。 */
    @SubscribeEvent
    public static void onMouseScrolling(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) return;
        if (!player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)) return;
        if (!Screen.hasControlDown()) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = hit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (!(state.getBlock() instanceof InlayCarrierBlock)) return;

        // 与交互 / HUD 同一套「按模型部件判定面」，仅命中中心体时退回方块面。
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
        Direction face = InlayCarrierBlock.pickFace(state, pos, from, to);
        if (face == null) face = hit.getDirection();

        BlockInlays inlays = BlockInlayManager.get(player.level(), pos);

        // 物流载体的「存入 / 合成存入」面：调节物流量。范围比门设定值大，Shift 步幅取 10。
        FaceMode scrollMode = inlays.getFace(face);
        if (state.getBlock() instanceof InlayCarrierBlock scrollCarrier
                && scrollCarrier.supportsTransferSettings(scrollMode)) {
            int step = (int) Math.signum(event.getScrollDeltaY()) * (Screen.hasShiftDown() ? 10 : 1);
            if (step == 0) return;
            PacketDistributor.sendToServer(new AdjustLogisticsThroughputPacket(pos, face, step));
            event.setCanceled(true);
            return;
        }

        // 远程面：调节信道数字。
        if (inlays.getFace(face) == FaceMode.REMOTE) {
            int channelStep = (int) Math.signum(event.getScrollDeltaY()) * (Screen.hasShiftDown() ? 5 : 1);
            if (channelStep == 0) return;
            PacketDistributor.sendToServer(new AdjustRemoteChannelPacket(pos, face, channelStep));
            event.setCanceled(true);
            return;
        }

        if (!inlays.getFace(face).isSettable()) return;

        int delta = (int) Math.signum(event.getScrollDeltaY()) * (Screen.hasShiftDown() ? 5 : 1);
        if (delta == 0) return;
        PacketDistributor.sendToServer(new AdjustGateValuePacket(pos, face, delta));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // 头戴铁砧锤时的载体 HUD：注册以视线命中面为焦点的方块实体 tooltip provider。
        HudTooltipManager.INSTANCE.registerBlockEntityTooltip(new CarrierInspectionProvider());

        event.enqueueWork(() -> {
            // 载体的玻璃区依赖逐部件的 render_type（cutout / translucent 分层）。Sodium 的 multipart
            // 会跨层重复发射子模型，由 SimpleBakedModelRenderTypeMixin 按 renderType 过滤修复。
            // 这里把三个载体整体登记到 translucent 作为兜底：没有逐部件 render_type 时
            // （例如其它模组替换了部件模型）仍走半透明层。
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.INLAY_CARRIER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOGIC_CARRIER.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModBlocks.LOGISTICS_CARRIER.get(), RenderType.translucent());
        });
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : InlayCarrierRenderer.MODELS) {
            event.register(model);
        }
    }

    /**
     * 逻辑 / 物流载体的物品图标直接复用方块模型：选一个「四个水平面已镶嵌、上下未镶嵌」的代表状态，
     * 把该状态的 baked model 包成物品模型。这样 item 不再维护一套手写几何，形状与方块完全一致。
     */
    @SubscribeEvent
    public static void replaceCarrierItemModels(ModelEvent.ModifyBakingResult event) {
        replaceCarrierItemModel(event, ModBlocks.LOGIC_CARRIER.get());
        replaceCarrierItemModel(event, ModBlocks.LOGISTICS_CARRIER.get());
    }

    private static void replaceCarrierItemModel(ModelEvent.ModifyBakingResult event, Block block) {
        BlockState state = representativeItemState(block);
        ModelResourceLocation blockModel = BlockModelShaper.stateToModelLocation(state);
        BakedModel bakedBlockModel = event.getModels().get(blockModel);
        if (bakedBlockModel == null) {
            AnvilCraftDogePlus.LOGGER.warn("Carrier item: missing block model {}", blockModel);
            return;
        }
        Item item = block.asItem();
        if (item == Items.AIR) return;
        event.getModels().put(
                ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)),
                new BlockStateItemModel(bakedBlockModel, state));
    }

    /** 物品图标代表状态：东西南北已镶嵌（未通电），上下空、中心体绘制。 */
    private static BlockState representativeItemState(Block block) {
        BlockState state = block.defaultBlockState();
        for (Direction face : Direction.values()) {
            state = state.setValue(
                    InlayCarrierBlock.property(face),
                    face.getAxis().isHorizontal() ? CarrierWire.UNPOWERED : CarrierWire.NONE);
        }
        return state.setValue(InlayCarrierBlock.HUB, true);
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_CARRIER.get(), InlayCarrierRenderer::new);
        // 逻辑载体与物流载体复用载体的方块实体类型，因此共用这一个渲染器注册（棱角规则由渲染器分辨）。
        event.registerBlockEntityRenderer(ModBlockEntities.LOGIC_CARRIER.get(), InlayCarrierRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_TABLE.get(), InlayTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_CRAFTING_TABLE.get(), InlayCraftingTableRenderer::new);
    }
}
