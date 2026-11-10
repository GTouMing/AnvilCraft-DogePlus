package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.AbstractProcessingTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCraftingTableBlockEntity;
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
 * 镶合台：镶合配方的工作方块。
 *
 * <p>一个基材槽 + 由目标产物过滤/配方决定数量的材料槽（最多 6 个，每槽对应一条镶孔要求）。顶面为输入区：手持物品在顶面放入（优先并入空槽/同类槽，
 * 全满时替换并返还旧物），空手右键取出全部；顶部被铁砧砸击时先执行静默镶嵌、再匹配镶合配方
 * （见 {@link InlayCraftingTableBlockEntity#processCrafting}），产物以掉落物形式生成在台体下方，
 * 供漏斗/溜槽等自动化收集。</p>
 */
public class InlayCraftingTableBlock extends AbstractInlayTableBlock {

    public InlayCraftingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<InlayCraftingTableBlock> codec() {
        return simpleCodec(InlayCraftingTableBlock::new);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new InlayCraftingTableBlockEntity(ModBlockEntities.INLAY_CRAFTING_TABLE.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return type == ModBlockEntities.INLAY_CRAFTING_TABLE.get()
                ? (l, p, s, be) -> ((InlayCraftingTableBlockEntity) be).tick()
                : null;
    }

    /** 空手取出全部槽位物品。 */
    @Override
    protected void retrieve(AbstractProcessingTableBlockEntity table, Level level, BlockPos pos, Player player) {
        boolean any = false;
        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            ItemStack slotStack = table.getItemHandler().getStackInSlot(slot);
            if (slotStack.isEmpty()) continue;
            player.getInventory().placeItemBackInInventory(slotStack.copy());
            table.setStackInSlot(slot, ItemStack.EMPTY);
            any = true;
        }
        if (any) {
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
        }
    }

    /** 放入：优先并入可合并的空槽/同类槽；全满时替换第一个接受的槽，旧物返还玩家。 */
    @Override
    protected void place(
            AbstractProcessingTableBlockEntity table, Level level, BlockPos pos, Player player, ItemStack stack) {
        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            if (!table.accepts(slot, stack)) continue;
            ItemStack existing = table.getItemHandler().getStackInSlot(slot);
            if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, stack)) continue;

            ItemStack remaining = table.getItemHandler().insertItem(slot, stack, false);
            int moved = stack.getCount() - remaining.getCount();
            if (moved <= 0) continue;
            if (!player.getAbilities().instabuild) stack.shrink(moved);
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            return;
        }

        for (int slot = 0; slot < InlayCraftingTableBlockEntity.SLOT_COUNT; slot++) {
            if (!table.accepts(slot, stack)) continue;
            ItemStack existing = table.getItemHandler().getStackInSlot(slot);
            ItemStack stored = stack.copy();
            table.setStackInSlot(slot, stored);
            if (!player.getAbilities().instabuild) stack.shrink(stored.getCount());
            if (!existing.isEmpty()) {
                player.getInventory().placeItemBackInInventory(existing.copy());
            }
            level.playSound(null, pos, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8f, 1.0f);
            return;
        }
    }
}
