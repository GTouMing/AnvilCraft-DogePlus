package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.entity.AnvilStrikeProcessor;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 铁砧落地监听：把砸击分发给台面下方的加工台方块实体。
 *
 * <p>镶嵌台与镶合台都实现 {@link AnvilStrikeProcessor}，因此这里不需要按方块类型分支。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class TableAnvilEvent {

    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        // OnLand.pos 是铁砧落点（台面正上方），台子在其下方
        BlockPos tablePos = event.getPos().below();
        BlockEntity be = level.getBlockEntity(tablePos);
        if (be instanceof AnvilStrikeProcessor processor) {
            processor.onAnvilStrike(level, tablePos, event.getFallDistance());
        }
    }
}
