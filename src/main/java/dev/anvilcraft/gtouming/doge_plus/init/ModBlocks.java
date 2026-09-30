package dev.anvilcraft.gtouming.doge_plus.init;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.LogicCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.DogeAnvil;
import dev.anvilcraft.gtouming.doge_plus.block.GiantDogeAnvil;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCraftingTableBlock;
import dev.anvilcraft.gtouming.doge_plus.block.InlayTableBlock;
import dev.anvilcraft.gtouming.doge_plus.block.LogisticsCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.ChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.ChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.MagneticChuteDispenserBlock;
import dev.anvilcraft.gtouming.doge_plus.block.chute.MagneticChuteDropperBlock;
import dev.anvilcraft.gtouming.doge_plus.datagen.blockstate.InlayCarrierBlockStateGenerator;
import dev.anvilcraft.lib.v2.registrum.providers.loot.RegistrumBlockLootTables;
import dev.anvilcraft.lib.v2.registrum.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.item.SimpleMultiPartBlockItem;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.util.DataGenUtil;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.REGISTRUM;

public class ModBlocks {
    static {
        REGISTRUM.defaultCreativeTab(ModCreativeTab.DOGE_PLUS_TAB.getKey());
    }

    public static final BlockEntry<DogeAnvil> DOGE_ANVIL =
            REGISTRUM.block("doge_anvil", DogeAnvil::new)
            .initialProperties(() -> Blocks.BONE_BLOCK)
            .blockstate(DataGenUtil::noExtraModelOrState)
            .loot(ModBlocks::dropSelfLoot)
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
                            .lightLevel(state -> 9).noOcclusion().emissiveRendering(ModBlocks::always)
                            .isViewBlocking(ModBlocks::never))
                    .item(SimpleMultiPartBlockItem<Cube3x3PartHalf>::new)
                    .properties(properties -> properties.stacksTo(16))
                    .tag(ItemTags.ANVIL)
                    .build()
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::giantDogeAnvilLoot)
                    .tag(ModBlockTags.GIANT_ANVIL, BlockTags.MINEABLE_WITH_PICKAXE,
                            ModBlockTags.NON_MAGNETIC, ModBlockTags.CANT_BROKEN_ANVIL)
                    .register();

    public static final BlockEntry<Block> DOGE_STEEL_BLOCK =
            REGISTRUM.block("doge_steel_block", Block::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::dropSelfLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .register();

    public static final BlockEntry<ChuteDispenserBlock> CHUTE_DISPENSER =
            REGISTRUM.block("chute_dispenser", ChuteDispenserBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::copyComponentsLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<ChuteDropperBlock> CHUTE_DROPPER =
            REGISTRUM.block("chute_dropper", ChuteDropperBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::copyComponentsLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<MagneticChuteDropperBlock> MAGNETIC_CHUTE_DROPPER =
            REGISTRUM.block("magnetic_chute_dropper", MagneticChuteDropperBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::copyComponentsLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<MagneticChuteDispenserBlock> MAGNETIC_CHUTE_DISPENSER =
            REGISTRUM.block("magnetic_chute_dispenser", MagneticChuteDispenserBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::copyComponentsLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<InlayTableBlock> INLAY_TABLE =
            REGISTRUM.block("inlay_table", InlayTableBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::dropSelfLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<InlayCraftingTableBlock> INLAY_CRAFTING_TABLE =
            REGISTRUM.block("inlay_crafting_table", InlayCraftingTableBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.noOcclusion().isValidSpawn(Blocks::never))
                    .blockstate(DataGenUtil::noExtraModelOrState)
                    .loot(ModBlocks::dropSelfLoot)
                    .item()
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<InlayCarrierBlock> INLAY_CARRIER =
            REGISTRUM.block("inlay_carrier", InlayCarrierBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn(Blocks::never))
                    .blockstate(InlayCarrierBlockStateGenerator::generate)
                    .loot(ModBlocks::dropSelfLoot)
                    // 方块状态是 multipart，Registrum 取不到「默认变体的模型」，只能自己指定物品模型。
                    // 部件模型是家族目录，不再按 id 推导，三个载体都得显式给。
                    .item()
                    .model((context, provider) -> provider.withExistingParent(
                            context.getName(), AnvilCraftDogePlus.of("block/carrier/inlay/item"))
                            .renderType("minecraft:translucent"))
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<LogicCarrierBlock> LOGIC_CARRIER =
            REGISTRUM.block("logic_carrier", LogicCarrierBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn(Blocks::never))
                    .blockstate(InlayCarrierBlockStateGenerator::generate)
                    .loot(ModBlocks::dropSelfLoot)
                    .item()
                    .model((context, provider) -> provider.withExistingParent(
                            context.getName(), AnvilCraftDogePlus.of("block/carrier/logic/item"))
                        .renderType("minecraft:translucent"))
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static final BlockEntry<LogisticsCarrierBlock> LOGISTICS_CARRIER =
            REGISTRUM.block("logistics_carrier", LogisticsCarrierBlock::new)
                    .initialProperties(() -> Blocks.IRON_BLOCK)
                    .properties(p -> p.isValidSpawn(Blocks::never))
                    // 物流面不参与红石、永远不会通电：没有通电变体（相应模型名传 null）；中心体也不是
                    // hub 那一支（恒渲染、由未编程面逐面画），贯通模型与贯通通电模型同样传 null。
                    .blockstate((context, provider) -> InlayCarrierBlockStateGenerator.generate(
                            context, provider,
                            "carrier/logistics/core", null, null, null,
                            // 物流载体的远程门也不分通电与否（它整块都不参与红石）：通电变体传 null。
                            "carrier/logistics/wire", null, "carrier/logistics/wire_remote", null,
                            "carrier/logistics/edge", null, "carrier/logistics/edge_vertical", null,
                            "carrier/logistics/corner"))
                    .loot(ModBlocks::dropSelfLoot)
                    .item()
                    .model((context, provider) -> provider.withExistingParent(
                                    context.getName(), AnvilCraftDogePlus.of("block/carrier/logistics/item"))
                            // 方块物品的渲染层不看部件模型的 render_type（默认 solid），
                            // 必须写在物品模型自己身上才会走半透明通道
                            .renderType("minecraft:translucent"))
                    .build()
                    .tag(BlockTags.MINEABLE_WITH_PICKAXE)
                    .tag(ModBlockTags.HAMMER_REMOVABLE)
                    .register();

    public static void register() {
    }

    // ==================== 战利品表生成 ====================

    /** 掉落自身。 */
    static void dropSelfLoot(RegistrumBlockLootTables tables, Block block) {
        tables.dropSelf(block);
    }

    /** chute 系列：掉落自身并复制方块实体组件（槽位内容随方块保留）。 */
    static void copyComponentsLoot(RegistrumBlockLootTables tables, Block block) {
        tables.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(block)
                                .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)))
                        .when(ExplosionCondition.survivesExplosion())));
    }

    /** 巨型 Doge 砧：仅 mid_center 部件掉落本体。 */
    static void giantDogeAnvilLoot(RegistrumBlockLootTables tables, Block block) {
        tables.add(block, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0f))
                        .add(LootItem.lootTableItem(block))
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)))
                        .when(ExplosionCondition.survivesExplosion())));
    }

    /**
     * 供  等使用的恒 false 判定。
     */
    public static boolean never(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return false;
    }
    public static boolean always(BlockState state, BlockGetter blockGetter, BlockPos pos) {
        return true;
    }
}
