package dev.anvilcraft.gtouming.doge_plus.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.gtouming.doge_plus.block.LogicCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.PipeCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MobileSilencerScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip.CarrierInspectionProvider;
import dev.anvilcraft.gtouming.doge_plus.client.gui.wheel.CarrierWheelModel;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayCarrierRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayCraftingTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.gtouming.doge_plus.network.AdjustGateValuePacket;
import dev.anvilcraft.gtouming.doge_plus.network.AdjustPipeThroughputPacket;
import dev.anvilcraft.gtouming.doge_plus.network.SetCarrierFacePacket;
import dev.anvilcraft.gtouming.doge_plus.network.SetPipeFilterPacket;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import dev.anvilcraft.lib.v2.wheel.client.input.WheelScreenController;
import dev.dubhe.anvilcraft.api.tooltip.HudTooltipManager;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
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
            // 按住 Ctrl 是「门链手势」（移除链需要铁砧锤）：此时不进入轮盘长按计时，避免两者冲突。
            if (Screen.hasControlDown()) return;
            if (!player.getMainHandItem().is(ModItemTags.ANVIL_HAMMER)) return;
            if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

            BlockPos pos = hit.getBlockPos();
            BlockState state = client.level.getBlockState(pos);
            if (!(state.getBlock() instanceof LogicCarrierBlock) && !(state.getBlock() instanceof PipeCarrierBlock)) {
                return;
            }

            // 与交互 / HUD 同一套「按模型部件判定面」，仅命中中心体时退回方块面。
            Vec3 from = player.getEyePosition(1.0F);
            Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
            Direction picked = InlayCarrierBlock.pickFace(state, pos, from, to);

            carrierWheelPressTick = client.level.getGameTime();
            carrierWheelPos = pos;
            carrierWheelFace = picked != null ? picked : hit.getDirection();
            return;
        }

        if (event.getAction() == GLFW.GLFW_RELEASE) {
            // 松开右键 = 确认：交给 lib 的轮盘控制器触发选中项并收起轮盘。
            WHEEL_CONTROLLER.onHoldKeyReleased();
            // 未开轮盘即为「点按」：管道载体的存入面用副手物品设置 / 替换过滤（副手空 = 清除）。
            // 长按已把 carrierWheelOpened 置真，不会走到这里。
            if (!carrierWheelOpened && carrierWheelPos != null && carrierWheelFace != null && client.level != null
                    && client.level.getBlockState(carrierWheelPos).getBlock() instanceof PipeCarrierBlock
                    && BlockInlayManager.get(client.level, carrierWheelPos).getFace(carrierWheelFace)
                            == FaceMode.INSERT) {
                ItemStack offhand = player.getOffhandItem();
                ItemStack filter = offhand.isEmpty() ? ItemStack.EMPTY : offhand.copyWithCount(1);
                PacketDistributor.sendToServer(new SetPipeFilterPacket(carrierWheelPos, carrierWheelFace, filter));
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
        carrierWheelOpened = true;
        // 按方块类型构造选项清单，交给 lib 的 hold 轮盘。
        openFaceWheel(pos, face, client.level.getBlockState(pos));
        stopUsingHammer(client, player);
    }

    /**
     * 打开面属性轮盘：逻辑载体列红石类选项（各门 + 清除），管道载体列搬运类选项（存入 / 取出 / 清除）。
     *
     * <p>两者都把选项组装成 lib 的 {@link WheelMenuModel}（见 {@link CarrierWheelModel}），
     * 由 {@link WheelScreenController} 以 hold 手势打开；松手时发出同一个编程包
     * {@link SetCarrierFacePacket}，由服务端按方块类型校验取值域。</p>
     */
    private static void openFaceWheel(BlockPos pos, Direction face, BlockState state) {
        List<FaceMode> options;
        if (state.getBlock() instanceof LogicCarrierBlock) {
            options = FaceMode.redstoneValues();
        } else if (state.getBlock() instanceof PipeCarrierBlock) {
            options = FaceMode.transferValues();
        } else {
            return;
        }
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

    private static void resetCarrierWheel() {
        carrierWheelPressTick = -1L;
        carrierWheelPos = null;
        carrierWheelFace = null;
        carrierWheelOpened = false;
    }

    /** 铁砧锤指向面时，Ctrl + 滚轮调整该面的参数：管道存入面调物流量，逻辑门面调设定值（Shift 大步）。 */
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

        // 管道载体的「存入」面：调节物流量。范围比门设定值大，Shift 步幅取 10。
        if (state.getBlock() instanceof PipeCarrierBlock && inlays.getFace(face) == FaceMode.INSERT) {
            int step = (int) Math.signum(event.getScrollDeltaY()) * (Screen.hasShiftDown() ? 10 : 1);
            if (step == 0) return;
            PacketDistributor.sendToServer(new AdjustPipeThroughputPacket(pos, face, step));
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
    }

    @SubscribeEvent
    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : InlayCarrierRenderer.MODELS) {
            event.register(model);
        }
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_CARRIER.get(), InlayCarrierRenderer::new);
        // 逻辑载体与管道载体复用载体的方块实体类型，因此共用这一个渲染器注册（棱角规则由渲染器分辨）。
        event.registerBlockEntityRenderer(ModBlockEntities.LOGIC_CARRIER.get(), InlayCarrierRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_TABLE.get(), InlayTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_CRAFTING_TABLE.get(), InlayCraftingTableRenderer::new);
    }
}
