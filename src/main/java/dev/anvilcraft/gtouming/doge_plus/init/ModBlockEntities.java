package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCraftingTableBlockEntity;
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
    public static final BlockEntityEntry<InlayCraftingTableBlockEntity> INLAY_CRAFTING_TABLE =
            AnvilCraftDogePlus.REGISTRUM.blockEntity(
                    "inlay_crafting_table",
                    InlayCraftingTableBlockEntity::new).validBlock(ModBlocks.INLAY_CRAFTING_TABLE).register();

    public static final BlockEntityEntry<InlayCarrierBlockEntity> INLAY_CARRIER =
            AnvilCraftDogePlus.REGISTRUM.blockEntity(
                    "inlay_carrier_block",
                    InlayCarrierBlockEntity::new)
                    .validBlock(ModBlocks.INLAY_CARRIER)
                    // 管道载体同样只需要「各面信号强度与运行时显示值」这类数据，复用同一种方块实体。
                    .validBlock(ModBlocks.PIPE_CARRIER).register();

    /** 逻辑载体：复用载体的方块实体（只存各面信号强度与运行时显示值）。 */
    public static final BlockEntityEntry<InlayCarrierBlockEntity> LOGIC_CARRIER =
            AnvilCraftDogePlus.REGISTRUM.blockEntity(
                    "logic_carrier",
                    InlayCarrierBlockEntity::new)
                    .validBlock(ModBlocks.LOGIC_CARRIER).register();

    public static void register() {
    }
}
