package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCraftingTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 镶合台：监听铁砧落地（{@link AnvilEvent.OnLand}），
 * 铁砧落在镶合台上时触发镶合配方合成。
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class InlayCraftingEvent {

    @SubscribeEvent
    public static void onAnvilLand(AnvilEvent.OnLand event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        // OnLand.pos 是铁砧落点（台面正上方），台子在其下方
        BlockPos tablePos = event.getPos().below();
        BlockState state = level.getBlockState(tablePos);
        if (!state.is(ModBlocks.INLAY_CRAFTING_TABLE)) return;

        BlockEntity be = level.getBlockEntity(tablePos);
        if (!(be instanceof InlayCraftingTableBlockEntity table)) return;
        table.processCrafting();
    }
}
