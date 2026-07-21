package dev.anvilcraft.gtouming.doge_plus.entity;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

public interface CapturedItemsProvider {
    void doge_plus$add(ItemEntity entity, int index);
    void doge_plus$remove(Predicate<ItemEntity> predicate);
    void doge_plus$captureOrMerge(ItemEntity entity, Level level, Vec3 pos);
    void doge_plus$releaseToPlayer(Player player);
    void doge_plus$release();
}
