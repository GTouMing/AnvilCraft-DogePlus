package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.api.entity.IAnvilTarget;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 为所有 {@link LivingEntity} 添加标记数存储（服务端）。
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IAnvilTarget {

    @Unique
    private int doge_plus$marks;

    @Override
    public int doge_plus$getMarks() {
        return this.doge_plus$marks;
    }

    @Override
    public void doge_plus$setMarks(int marks) {
        this.doge_plus$marks = marks;
    }
}
