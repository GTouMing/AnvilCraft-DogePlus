package dev.anvilcraft.gtouming.doge_plus.mixin;

import dev.anvilcraft.gtouming.doge_plus.data.BlockInlays;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * 方块级镶嵌的记录迁移/清理与「永恒」挖掘保护：
 * <ul>
 *   <li>{@code onRemove}：方块被移动（moved 为 true，即 setBlock flags 含 SUPPRESS_DROPS，
 *       活塞/滑轨等推动）时暂存映射供新位置恢复；被替换（新状态非空气）时直接清理；
 *       新状态为空气且非移动（玩家挖掘、爆炸等）时保留记录，
 *       由 {@link BlockMixin} 的 {@code getDrops} 写回掉落物后清理。</li>
 *   <li>{@code getDestroyProgress}：永恒镶嵌方块不可被挖掘（返回 0，效果同基岩）。</li>
 * </ul>
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin {

    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void onGetDrops(BlockState state, LootParams.Builder params, CallbackInfoReturnable<List<ItemStack>> cir) {
        Level level = params.getLevel();
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));
        BlockInlays bi = BlockInlayManager.get(level, pos);
        if (bi == null) return;
        List<ResourceLocation> inlays = bi.inlays();
        if (inlays.isEmpty()) return;
        Item item = state.getBlock().asItem();
        List<ItemStack> drops = new ArrayList<>(cir.getReturnValue());
        if (item == Items.AIR) return;
        for (ItemStack drop : drops) {
            if (!drop.is(item)) continue;
            InlayUtil.setInlays(drop, new ArrayList<>(inlays));
            InlayUtil.reapplyAttributeModifiers(drop);
            break;
        }
        cir.setReturnValue(drops);
        // 方块已不存在，清理记录
        BlockInlayManager.remove(level, pos);
    }

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void doge_plus$eternalUnbreakable(
            BlockState state, Player player, BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (BlockInlayManager.hasProperty(level, pos, InlayProperty.ETERNAL)) {
            cir.setReturnValue(0.0F);
        }
    }
}
