package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.block.DogeAnvil;
import dev.anvilcraft.gtouming.doge_plus.block.GiantDogeAnvil;
import dev.anvilcraft.gtouming.doge_plus.block.chute.ChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.ChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.MagneticChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.MagneticChuteDropperBlock;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModBlocks {
    static {
        REGISTRUM.defaultCreativeTab(ModCreativeTab.DOGE_PLUS_TAB.getKey());
    }

    public static final BlockEntry<DogeAnvil> DOGE_ANVIL =
            REGISTRUM.block("doge_anvil", DogeAnvil::new)
            .initialProperties(() -> Blocks.BONE_BLOCK)
            .blockstate(DataGenUtil::noExtraModelOrState)
            .item()
            .tag(ItemTags.ANVIL)
            .build()
            .tag(BlockTags.ANVIL, ModBlockTags.NON_MAGNETIC, ModBlockTags.CANT_BROKEN_ANVIL)
            .register();

    public static final BlockEntry<GiantDogeAnvil> GIANT_DOGE_ANVIL =
            REGISTRUM.block("giant_doge_anvil", GiantDogeAnvil::new)
                    .initialProperties(() -> Blocks.ANVIL)
                    .properties(properties -> properties
                            .noOcclusion()
                            .isValidSpawn(Blocks::never)
                            .strength(4.0F)
                            .sound(GiantAnvilBlock.SOUND_TYPE)
                            .explosionResistance(1200)
                            .isViewBlocking(ModBlocks::never))
                    .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
                    .properties(properties -> properties.stacksTo(16))
                    .build()
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .tag(BlockTags.ANVIL, ModBlockTags.GIANT_ANVIL, BlockTags.MINEABLE_WITH_PICKAXE,
                            ModBlockTags.NON_MAGNETIC, ModBlockTags.CANT_BROKEN_ANVIL)
                    .register();

    public static final BlockEntry<Block> DOGE_STEEL_BLOCK =
            REGISTRUM.block("doge_steel_block", Block::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

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

    /**
     * 供  等使用的恒 false 判定。
     */
    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }
}
