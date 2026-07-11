package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.block.ChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.ChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.block.MagneticChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.MagneticChuteDropperBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.MOD_ID;

public class ModBlocks {
    public static final DeferredRegister.Blocks MOD_BLOCK =
            DeferredRegister.createBlocks(MOD_ID);

    public static final DeferredBlock<ChuteDispenserBlock> CHUTE_DISPENSER =
            MOD_BLOCK.registerBlock("chute_dispenser", ChuteDispenserBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops().strength(3.5F));

    public static final DeferredBlock<ChuteDropperBlock> CHUTE_DROPPER =
            MOD_BLOCK.registerBlock("chute_dropper", ChuteDropperBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops().strength(3.5F));

    public static final DeferredBlock<MagneticChuteDropperBlock> MAGNETIC_CHUTE_DROPPER =
            MOD_BLOCK.registerBlock("magnetic_chute_dropper", MagneticChuteDropperBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops().strength(3.5F));

    public static final DeferredBlock<MagneticChuteDispenserBlock> MAGNETIC_CHUTE_DISPENSER =
            MOD_BLOCK.registerBlock("magnetic_chute_dispenser", MagneticChuteDispenserBlock::new,
                    BlockBehaviour.Properties.of().mapColor(MapColor.STONE)
                            .instrument(NoteBlockInstrument.BASEDRUM)
                            .requiresCorrectToolForDrops().strength(3.5F));

    public static void register(IEventBus bus) {
        MOD_BLOCK.register(bus);
    }
}
