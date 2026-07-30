package dev.anvilcraft.gtouming.doge_plus.curios;

import dev.anvilcraft.gtouming.doge_plus.api.SoundTransformer;
import dev.anvilcraft.gtouming.doge_plus.api.curios.ICurios;
import dev.anvilcraft.gtouming.doge_plus.api.sound.DogePlusSoundHelper;
import dev.anvilcraft.gtouming.doge_plus.client.renderer.CuriosRenderer;
import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;
import top.theillusivec4.curios.api.event.CurioChangeEvent;

public class CuriosExist implements ICurios {
    @Override
    public void register() {
        var bus = NeoForge.EVENT_BUS;

        bus.addListener(this::onPlayerLogin);
        bus.addListener(this::onCurioChange);

    }

    @Override
    public @Nullable ItemStack findMobileSilencer(Player player) {
        var result = CuriosApi.getCuriosInventory(player);
        if (result.isEmpty()) return null;
        var handler = result.get();
        var found = handler.findFirstCurio(stack -> stack.getItem() instanceof MobileSilencer);
        return found.map(SlotResult::stack).orElse(player.getItemBySlot(EquipmentSlot.HEAD));
    }

    public void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(
                () -> CuriosRendererRegistry.register(ModItems.MOBILE_SILENCER.get(), CuriosRenderer::new));
    }

    public void onCurioChange(CurioChangeEvent event) {
        if (!"head".equals(event.getIdentifier())) return;
        if (!(event.getEntity() instanceof Player)) return;
        ItemStack from = event.getFrom();
        ItemStack to = event.getTo();

        if (from.getItem() instanceof MobileSilencer) {
            DogePlusSoundHelper.INSTANCE.unregister(SoundTransformer.asSoundListener(from));
        }
        if (to.getItem() instanceof MobileSilencer) {
            DogePlusSoundHelper.INSTANCE.register(SoundTransformer.asSoundListener(to));
        }
    }


    /**
     * 玩家登录事件。
     * 处理玩家已在 Curios 槽中装备 MobileSilencer 加入世界的情况，
     * 确保其注册到 {@link DogePlusSoundHelper}。
     */
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {

        var stack = findMobileSilencer(event.getEntity());
        if (stack == null) return;
        if (!(stack.getItem() instanceof MobileSilencer)) return;

        DogePlusSoundHelper.INSTANCE.register(SoundTransformer.asSoundListener(stack));
    }
}
