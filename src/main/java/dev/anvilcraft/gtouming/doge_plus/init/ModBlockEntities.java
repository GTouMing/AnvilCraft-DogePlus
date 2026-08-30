package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.entity.GiantDogeAnvilBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.ChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.ChuteDropperBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.MagneticChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.chute.MagneticChuteDropperBlockEntity;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntityEntry;

import static dev.dubhe.anvilcraft.AnvilCraft.REGISTRUM;

public class ModBlockEntities {
    public static final BlockEntityEntry<ChuteDispenserBlockEntity> CHUTE_DISPENSER = REGISTRUM.blockEntity(
            "chute_dispenser",
            ChuteDispenserBlockEntity::new
    ).validBlock(ModBlocks.CHUTE_DISPENSER).register();

    public static final BlockEntityEntry<ChuteDropperBlockEntity> CHUTE_DROPPER = REGISTRUM.blockEntity(
            "chute_dropper",
            ChuteDropperBlockEntity::new).validBlock(ModBlocks.CHUTE_DROPPER).register();

    public static final BlockEntityEntry<MagneticChuteDropperBlockEntity> MAGNETIC_CHUTE_DROPPER = REGISTRUM.blockEntity(
            "magnetic_chute_dropper",
    MagneticChuteDropperBlockEntity::new).validBlock(ModBlocks.MAGNETIC_CHUTE_DROPPER).register();

    public static final BlockEntityEntry<MagneticChuteDispenserBlockEntity> MAGNETIC_CHUTE_DISPENSER =REGISTRUM.blockEntity(
            "magnetic_chute_dispenser",
            MagneticChuteDispenserBlockEntity::new).validBlock(ModBlocks.MAGNETIC_CHUTE_DISPENSER).register();

    public static final BlockEntityEntry<InlayTableBlockEntity> INLAY_TABLE = AnvilCraftDogePlus.REGISTRUM.blockEntity(
            "inlay_table",
            InlayTableBlockEntity::new).validBlock(ModBlocks.INLAY_TABLE).register();
    public static final BlockEntityEntry<GiantDogeAnvilBlockEntity> GIANT_DOGE_ANVIL = AnvilCraftDogePlus.REGISTRUM.blockEntity(
            "giant_doge_anvil",
            GiantDogeAnvilBlockEntity::new).validBlock(ModBlocks.GIANT_DOGE_ANVIL).register();

    public static void register() {
    }
}
