package dev.anvilcraft.gtouming.doge_plus.api.entity;

/**
 * 为 {@link net.minecraft.world.entity.LivingEntity} 添加标记数。
 * <p>磁铁锭通过视线射线标记目标，标记越多，铁砧对目标的伤害越高。</p>
 */
public interface IAnvilTarget {
    int doge_plus$getMarks();

    void doge_plus$setMarks(int marks);
}
