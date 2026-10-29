package dev.anvilcraft.gtouming.doge_plus.network;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCraftingTableBlock;
import dev.anvilcraft.gtouming.doge_plus.block.InlayTableBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCraftingTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayTableBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.util.RecipeFilterUtil;
import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 设置 / 清除镶嵌台、镶合台的「配方过滤」（客户端 → 服务端）。
 *
 * <p>主手铁砧锤点按右键方块水平侧面时发出：过滤物品取自副手（空栈表示清除）。服务端按方块类型
 * 校验并解释该物品：镶嵌台视为基材、镶合台视为目标产物；{@code anvilcraft:filter} 则直接作为
 * 过滤器内容使用（见 {@link RecipeFilterUtil}）。</p>
 */
public record SetRecipeFilterPacket(BlockPos pos, ItemStack filter) implements IInsensitiveBiPacket {

    /** 交互距离上限的平方（8 格，只用于挡住伪造包）。 */
    private static final double MAX_DISTANCE_SQR = 64.0;

    public static final Type<SetRecipeFilterPacket> TYPE =
            IPacket.type(AnvilCraftDogePlus.of("set_recipe_filter"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetRecipeFilterPacket> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, SetRecipeFilterPacket::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC, SetRecipeFilterPacket::filter,
                    SetRecipeFilterPacket::new
            );

    @Override
    public Type<SetRecipeFilterPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnBothSide(Player player) {
        if (player.level().isClientSide) return;
        Level level = player.level();
        if (player.distanceToSqr(Vec3.atCenterOf(pos)) > MAX_DISTANCE_SQR) return;

        BlockState state = level.getBlockState(pos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        ItemStack normalized = filter.isEmpty() ? ItemStack.EMPTY : filter.copyWithCount(1);

        if (state.getBlock() instanceof InlayTableBlock && blockEntity instanceof InlayTableBlockEntity table) {
            if (!RecipeFilterUtil.canSetInlayTableFilter(normalized, level)) return;
            table.setRecipeFilter(normalized);
        } else if (state.getBlock() instanceof InlayCraftingTableBlock
                && blockEntity instanceof InlayCraftingTableBlockEntity crafting) {
            if (!RecipeFilterUtil.canSetCraftingTableFilter(normalized, level)) return;
            crafting.setRecipeFilter(normalized);
        } else {
            return;
        }

        level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.6f, 1.0f);
    }
}
