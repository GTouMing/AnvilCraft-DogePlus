package dev.anvilcraft.gtouming.doge_plus.event;

import com.mojang.blaze3d.platform.InputConstants;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.client.gui.screen.MobileSilencerScreen;
import dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip.CarrierInspectionProvider;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.GiantDogeAnvilRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayCarrierRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayCraftingTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.InlayTableRenderer;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.blockentity.TranscendiumInlayCarrierRenderer;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierPhase;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;
import dev.anvilcraft.gtouming.doge_plus.network.AdjustGateValuePacket;
import dev.anvilcraft.gtouming.doge_plus.network.ToggleCarrierPhasePacket;
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

    /** 切换超限镶嵌载体相位（α/β）。 */
    public static final KeyMapping TOGGLE_CARRIER_PHASE = new KeyMapping(
            "key.anvilcraft_doge_plus.toggle_carrier_phase",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_X,
            "key.categories.anvilcraft_doge_plus"
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_SILENCER);
        event.register(TOGGLE_CARRIER_PHASE);
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

        if (TOGGLE_CARRIER_PHASE.consumeClick()) {
            ItemStack stack = player.getMainHandItem();
            if (stack.is(ModBlocks.TRANSCENDIUM_INLAY_CARRIER.get().asItem())) {
                CarrierPhase next = stack
                        .getOrDefault(ModDataComponentTypes.CARRIER_PHASE, CarrierPhase.ALPHA)
                        .other();
                PacketDistributor.sendToServer(new ToggleCarrierPhasePacket(next));
            }
        }
    }

    /** 铁砧锤指向门所在面时，Ctrl + 滚轮调整门设定值（Shift 步幅 5）。 */
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
        LogicGateType gateType = inlays.getGateType(face);
        if (!gateType.isSettable()) return;

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
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_TABLE.get(), InlayTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.INLAY_CRAFTING_TABLE.get(), InlayCraftingTableRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.GIANT_DOGE_ANVIL.get(), GiantDogeAnvilRenderer::new);
        event.registerBlockEntityRenderer(
                ModBlockEntities.TRANSCENDIUM_INLAY_CARRIER.get(), TranscendiumInlayCarrierRenderer::new);
    }
}
