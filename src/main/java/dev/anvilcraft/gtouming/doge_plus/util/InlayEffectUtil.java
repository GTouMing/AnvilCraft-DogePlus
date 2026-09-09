package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 效果属性（{@link InlayProperty#EFFECT}）的实体级表现工具。
 *
 * <p>当实体踩上（{@code Block.stepOn}）携带效果镶嵌的方块时，对其施加该 inlay
 * 记录的药水效果：效果取自 {@link InlayEntry#extra()} 中 potion 条目对应的
 * {@code Potion} 定义（{@code Potion.getEffects()}，含 long/strong 时长语义）。</p>
 */
public final class InlayEffectUtil {

    /** 踩踏施加效果的冷却周期（tick）：与 AnvilCraft {@code StepEffectBlock.EFFECT_PERIOD} 一致。 */
    public static final int STEP_EFFECT_PERIOD = 80;

    private InlayEffectUtil() {
    }

    /**
     * 实体踩踏携带效果镶嵌的方块：查询该坐标的 inlay，若有效果属性则施加药水效果。
     *
     * @param level  服务端世界
     * @param pos    被踩方块坐标
     * @param entity 踩踏实体
     */
    public static void applyStepEffects(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof LivingEntity living)) return;
        if (level.getGameTime() % STEP_EFFECT_PERIOD != 0) return;

        var inlays = BlockInlayManager.get(level, pos).inlays();
        List<MobEffectInstance> effects = collectEffects(inlays);
        if (effects.isEmpty()) return;
        for (MobEffectInstance effect : effects) {
            if (effect.getEffect().value().isInstantenous()) {
                // 瞬时效果（瞬间伤害/治疗等）：不通过 addEffect 附加，直接以踩踏方块为来源施加
                effect.getEffect().value().applyInstantenousEffect(null, null, living, effect.getAmplifier(), 1.0);
            } else {
                living.addEffect(effect);
            }
        }
    }

    /** 从镶嵌条目列表中收集全部药水效果（仅带效果属性的条目；取每个条目的首个有效 potion）。 */
    private static List<MobEffectInstance> collectEffects(List<InlayEntry> inlays) {
        List<MobEffectInstance> result = new ArrayList<>();
        if (inlays.isEmpty()) return result;
        for (InlayEntry entry : inlays) {
            if (entry.isEmpty() || !entry.containsAttributes(InlayProperty.EFFECT)) continue;
            MobEffectInstance effect = firstEffectFrom(entry);
            if (effect != null) result.add(effect);
        }
        return result;
    }

    @Nullable
    private static MobEffectInstance firstEffectFrom(InlayEntry entry) {
        for (ResourceLocation extraId : entry.extra()) {
            var potionHolder = BuiltInRegistries.POTION.getHolder(extraId).orElse(null);
            if (potionHolder == null) continue;
            List<MobEffectInstance> effects = potionHolder.value().getEffects();
            if (!effects.isEmpty()) return effects.getFirst();
        }
        return null;
    }
}
