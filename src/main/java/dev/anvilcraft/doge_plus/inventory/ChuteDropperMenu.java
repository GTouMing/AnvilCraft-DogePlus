package dev.anvilcraft.doge_plus.inventory;

import dev.anvilcraft.doge_plus.block.entity.ChuteDropperBlockEntity;
import dev.anvilcraft.doge_plus.init.ModBlocks;
import dev.anvilcraft.doge_plus.init.ModMenuTypes;
import dev.dubhe.anvilcraft.inventory.BaseChuteMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Objects;

public class ChuteDropperMenu extends BaseChuteMenu<ChuteDropperBlockEntity> {

    public ChuteDropperMenu(int id, Inventory inv, BlockEntity be) {
        super(ModMenuTypes.CHUTE_DROPPER.get(), id, inv, be);
    }

    public static ChuteDropperMenu fromNetwork(int id, Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        Objects.requireNonNull(be, () -> "Missing block entity at " + pos);
        return new ChuteDropperMenu(id, inv, be);
    }

    @Override
    protected Block getBlock() {
        return ModBlocks.CHUTE_DROPPER.get();
    }
}