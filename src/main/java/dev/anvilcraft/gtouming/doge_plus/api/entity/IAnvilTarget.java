package dev.anvilcraft.gtouming.doge_plus.api.entity;

/**
 * 为 {@link net.minecraft.world.entity.LivingEntity} 添加标记数。
 * <p>手持磁铁锭通过视线射线标记目标；目标被标记后，飞行铁砧命中时
 * 每个标记提供 {@code perMark} 的额外伤害。</p>
 */
public interface IAnvilTarget {

    int doge_plus$getMarks();

    void doge_plus$setMarks(int marks);
}
