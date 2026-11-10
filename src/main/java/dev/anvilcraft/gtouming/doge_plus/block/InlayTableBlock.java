package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.AbstractProcessingTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 镶嵌台：镶嵌配方的工作方块。
 *
 * <p>顶面为输入区：手持物品放入时按栈式交互（全空→基材，否则→镶嵌材料，
 * 替换仅能替换镶嵌材料）；空手右键取出全部（基材+材料）。
 * 顶部被铁砧砸击时执行镶嵌（见 {@link InlayTableBlockEntity#processInlay}），
 * 产物与旧材料以掉落物形式生成。</p>
 */
public class InlayTableBlock extends AbstractInlayTableBlock {

    public InlayTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<InlayTableBlock> codec() {
        return simpleCodec(InlayTableBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InlayTableBlockEntity(ModBlockEntities.INLAY_TABLE.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.INLAY_TABLE.get()
                ? (l, p, s, be) -> ((InlayTableBlockEntity) be).tick()
                : null;
    }

    /** 空手取出基材与材料。 */
    @Override
    protected void retrieve(AbstractProcessingTableBlockEntity table, Level level, BlockPos pos, Player player) {
        for (int slot = 0; slot < InlayTableBlockEntity.SLOT_COUNT; slot++) {
            ItemStack slotStack = table.getItemHandler().getStackInSlot(slot);
            if (slotStack.isEmpty()) continue;
            player.getInventory().placeItemBackInInventory(slotStack.copy());
            table.setStackInSlot(slot, ItemStack.EMPTY);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
    }

    /**
     * 放入：基材槽为空则放基材，否则放镶嵌材料（材料槽已有则替换，旧材料返还玩家）。
     * 掉落物统一收集为材料（见 {@link AbstractProcessingTableBlockEntity#tick}），基材经右键放入。
     */
    @Override
    protected void place(
            AbstractProcessingTableBlockEntity table, Level level, BlockPos pos, Player player, ItemStack stack) {
        int target = table.getItemHandler().getStackInSlot(InlayTableBlockEntity.SLOT_BASE).isEmpty()
                ? InlayTableBlockEntity.SLOT_BASE
                : InlayTableBlockEntity.SLOT_MATERIAL;

        // 配方过滤：优先按栈式规则选槽；该槽不接受时，若另一槽为空且接受则改放另一槽，
        // 避免设定过滤后只能先放基材才能放材料。
        if (!table.accepts(target, stack)) {
            int other = target == InlayTableBlockEntity.SLOT_BASE
                    ? InlayTableBlockEntity.SLOT_MATERIAL
                    : InlayTableBlockEntity.SLOT_BASE;
            if (!table.getItemHandler().getStackInSlot(other).isEmpty() || !table.accepts(other, stack)) return;
            target = other;
        }

        ItemStack existing = table.getItemHandler().getStackInSlot(target);
        ItemStack stored = stack.copy();
        table.setStackInSlot(target, stored);
        if (!player.getAbilities().instabuild) stack.shrink(stored.getCount());
        if (!existing.isEmpty()) {
            player.getInventory().placeItemBackInInventory(existing.copy());
        }
        level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
    }
}
