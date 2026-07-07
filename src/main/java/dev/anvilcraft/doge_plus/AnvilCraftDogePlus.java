package dev.anvilcraft.doge_plus;

import dev.anvilcraft.doge_plus.client.gui.screen.ChuteDropperScreen;
import dev.anvilcraft.doge_plus.client.gui.screen.ChuteDispenserScreen;
import dev.anvilcraft.doge_plus.client.gui.screen.MagneticChuteDropperScreen;
import dev.anvilcraft.doge_plus.client.gui.screen.MagneticDispenserScreen;
import dev.anvilcraft.doge_plus.client.renderer.NodeSupportEntityRenderer;
import dev.anvilcraft.doge_plus.event.NodeCaptureHandler;
import dev.anvilcraft.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.doge_plus.init.ModBlocks;
import dev.anvilcraft.doge_plus.init.ModCreativeTab;
import dev.anvilcraft.doge_plus.init.ModEntities;
import dev.anvilcraft.doge_plus.init.ModItems;
import dev.anvilcraft.doge_plus.init.ModMenuTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(AnvilCraftDogePlus.MOD_ID)
public class AnvilCraftDogePlus {
    public static final String MOD_ID = "anvilcraft_doge_plus";

    public AnvilCraftDogePlus(IEventBus modEventBus, ModContainer modContainer) {
        // 通过代码注册 Mixin 配置（替代 neoforge.mods.toml 中的 [[mixins]] 块）
        org.spongepowered.asm.mixin.Mixins.addConfiguration("anvilcraft_doge_plus.mixins.json");

        ModMenuTypes.MENU_TYPES.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTab.CREATIVE_TABS.register(modEventBus);
        ModEntities.register(modEventBus);

        // 注册事件处理器（节点放置/破坏 + 定时扫描）
        NeoForge.EVENT_BUS.register(new NodeCaptureHandler());

        modEventBus.addListener(this::onRegisterScreens);
        modEventBus.addListener(this::onRegisterRenderers);
    }

    private void onRegisterScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.DOGE_CHUTE.get(), ChuteDispenserScreen::new);
        event.register(ModMenuTypes.MAGNETIC_CHUTE_DISPENSER.get(), MagneticDispenserScreen::new);
        event.register(ModMenuTypes.CHUTE_DROPPER.get(), ChuteDropperScreen::new);
        event.register(ModMenuTypes.MAGNETIC_CHUTE_DROPPER.get(), MagneticChuteDropperScreen::new);
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.NODE_SUPPORT.get(), NodeSupportEntityRenderer::new);
    }
}