package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.block.ChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.ChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.block.MagneticChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.MagneticChuteDropperBlock;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModBlocks {
    static {
        REGISTRUM.defaultCreativeTab(ModCreativeTab.DOGE_PLUS_TAB.getKey());
    }

    public static final BlockEntry<ChuteDispenserBlock> CHUTE_DISPENSER =
            REGISTRUM.block("chute_dispenser", ChuteDispenserBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

    public static final BlockEntry<ChuteDropperBlock> CHUTE_DROPPER =
            REGISTRUM.block("chute_dropper", ChuteDropperBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

    public static final BlockEntry<MagneticChuteDropperBlock> MAGNETIC_CHUTE_DROPPER =
            REGISTRUM.block("magnetic_chute_dropper", MagneticChuteDropperBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

    public static final BlockEntry<MagneticChuteDispenserBlock> MAGNETIC_CHUTE_DISPENSER =
            REGISTRUM.block("magnetic_chute_dispenser", MagneticChuteDispenserBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

    public static void register() {
    }
}
