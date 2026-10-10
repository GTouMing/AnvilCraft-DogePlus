package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.attribute.AttributeLevelManager;
import dev.anvilcraft.gtouming.doge_plus.attribute.InlayLevelService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 属性等级系统的事件接线：穿脱装备、登录、重生（换维度）时重算并重新施加，并保护效果不被清除。
 *
 * <p>全量重算而非增量计数，故登录/命中重生时即便存档里的旧计数与新装备一致，也会对实体重新挂上
 * 修饰器与效果——修掉「死亡重生后效果丢失」「/clear 掉物品后计数残留」这类增量方案的老问题。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class InlayLevelEvent {

    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity.level() instanceof ServerLevel level)) return;
        InlayLevelService.reconcile(level, entity);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        InlayLevelService.reconcile(level, event.getEntity());
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        // 死亡或换维度：UUID 不变，直接对新实体重算即可覆盖旧记录。
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        InlayLevelService.reconcile(level, event.getEntity());
    }

    /**
     * 反移除保护：等级系统仍在供给该效果时，取消移除请求（牛奶 / {@code /effect clear} 清不掉）。
     *
     * <p>等级系统自身的增删走 {@link InlayLevelService#isInternalEffectRemoval()} 放行，
     * 因此卸下镶嵌 / 降级时能正常移除。</p>
     */
    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (InlayLevelService.isInternalEffectRemoval()) return;

        LivingEntity entity = event.getEntity();
        MobEffectInstance effect = event.getEffectInstance();
        if (effect == null) return;
        ResourceLocation effectId = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
        if (effectId == null) return;

        AttributeLevelManager manager = AttributeLevelManager.get(entity.level());
        if (manager == null) return;
        AttributeLevelManager.Entry entry = manager.get(entity.getUUID()).get(effectId);
        if (entry != null && entry.count() > 0) {
            event.setCanceled(true);
        }
    }
}
