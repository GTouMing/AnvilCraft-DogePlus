package dev.anvilcraft.gtouming.doge_plus.inventory;

import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.MagneticChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.dubhe.anvilcraft.inventory.BaseChuteMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ChuteDispenserMenu extends BaseChuteMenu<MagneticChuteDispenserBlockEntity> {

    public ChuteDispenserMenu(MenuType<?> type, int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        super(type, id, inv, buf);
    }

    public ChuteDispenserMenu(MenuType<?> type, int id, Inventory inv, BlockEntity blockEntity) {
        super(type, id, inv, blockEntity);
    }
    @Override
    protected Block getBlock() {
        return ModBlocks.CHUTE_DISPENSER.get();
    }
}
