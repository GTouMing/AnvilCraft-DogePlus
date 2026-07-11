package dev.anvilcraft.gtouming.doge_plus.init;

import com.mojang.datafixers.DSL;
import dev.anvilcraft.gtouming.doge_plus.block.entity.ChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.ChuteDropperBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.MagneticChuteDispenserBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.MagneticChuteDropperBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

import static dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus.MOD_ID;

public class ModBlockEntities {

    private static final String CHUTE_DISPENSER_ID = "chute_dispenser";
    private static final String CHUTE_DROPPER_ID = "chute_dropper";
    private static final String MAGNETIC_CHUTE_DROPPER_ID = "magnetic_chute_dropper";
    private static final String MAGNETIC_CHUTE_DISPENSER_ID = "magnetic_chute_dispenser";

    public static final DeferredRegister<BlockEntityType<?>> MOD_BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MOD_ID);

    public static final Supplier<BlockEntityType<ChuteDispenserBlockEntity>> CHUTE_DISPENSER =
            MOD_BLOCK_ENTITIES.register(CHUTE_DISPENSER_ID,
                    () -> BlockEntityType.Builder.of(
                            ChuteDispenserBlockEntity::new,
                            ModBlocks.CHUTE_DISPENSER.get()
                    ).build(DSL.emptyPartType()));

    public static final Supplier<BlockEntityType<ChuteDropperBlockEntity>> CHUTE_DROPPER =
            MOD_BLOCK_ENTITIES.register(CHUTE_DROPPER_ID,
                    () -> BlockEntityType.Builder.of(
                            ChuteDropperBlockEntity::new,
                            ModBlocks.CHUTE_DROPPER.get()
                    ).build(DSL.emptyPartType()));

    public static final Supplier<BlockEntityType<MagneticChuteDropperBlockEntity>> MAGNETIC_CHUTE_DROPPER =
            MOD_BLOCK_ENTITIES.register(MAGNETIC_CHUTE_DROPPER_ID,
                    () -> BlockEntityType.Builder.of(
                            MagneticChuteDropperBlockEntity::new,
                            ModBlocks.MAGNETIC_CHUTE_DROPPER.get()
                    ).build(DSL.emptyPartType()));

    public static final Supplier<BlockEntityType<MagneticChuteDispenserBlockEntity>> MAGNETIC_CHUTE_DISPENSER =
            MOD_BLOCK_ENTITIES.register(MAGNETIC_CHUTE_DISPENSER_ID,
                    () -> BlockEntityType.Builder.of(
                            MagneticChuteDispenserBlockEntity::new,
                            ModBlocks.MAGNETIC_CHUTE_DISPENSER.get()
                    ).build(DSL.emptyPartType()));

    public static void register(IEventBus modEventBus) {
        MOD_BLOCK_ENTITIES.register(modEventBus);
    }
}
