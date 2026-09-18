package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.TranscendiumInlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierPhase;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import dev.anvilcraft.gtouming.doge_plus.init.ModDataComponentTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 超限镶嵌载体：动态镶孔（已镶嵌数量 + 1，上限 216）与 α/β 双相位。
 *
 * <p>镶嵌数据自持于 {@link TranscendiumInlayCarrierBlockEntity}；相位由方块状态 {@link #PHASE}
 * 决定基座模型，放置时从物品组件读入、破坏时回写到掉落物。</p>
 */
public class TranscendiumInlayCarrierBlock extends Block implements EntityBlock {

    public static final EnumProperty<CarrierPhase> PHASE =
            EnumProperty.create("phase", CarrierPhase.class);

    public TranscendiumInlayCarrierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(PHASE, CarrierPhase.ALPHA));
    }

    @Override
    protected MapCodec<? extends TranscendiumInlayCarrierBlock> codec() {
        return simpleCodec(TranscendiumInlayCarrierBlock::new);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        CarrierPhase phase = context.getItemInHand()
                .getOrDefault(ModDataComponentTypes.CARRIER_PHASE, CarrierPhase.ALPHA);
        return this.defaultBlockState().setValue(PHASE, phase);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TranscendiumInlayCarrierBlockEntity(
                ModBlockEntities.TRANSCENDIUM_INLAY_CARRIER.get(), pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        for (ItemStack drop : drops) {
            if (drop.is(this.asItem())) {
                drop.set(ModDataComponentTypes.CARRIER_PHASE, state.getValue(PHASE));
            }
        }
        return drops;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
    }
}
