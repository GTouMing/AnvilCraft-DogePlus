package dev.anvilcraft.gtouming.doge_plus.util;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.TriggerUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 腐化信标转化：信标镶嵌诅咒金锭后，放置时按诅咒金块金字塔层数概率转化为腐化信标。
 *
 * <p>概率表与金字塔判定照抄前置 {@code dev.dubhe.anvilcraft.anvil.BeaconConversionBehavior}
 * （其概率表与 {@code getBeaconLevel} 均为 {@code private}，无法直接调用，故按数值与逻辑复刻）。</p>
 */
public final class BeaconCorruptionUtil {

    /** 照抄前置的概率表：完整诅咒金块金字塔层数（0–4）→ 转化概率。 */
    private static final double[] CHANCE = {0.0, 0.02, 0.05, 0.2, 1.0};

    private BeaconCorruptionUtil() {
    }

    /** 镶嵌条目中是否含「诅咒金锭」这个物品（按条目 item id 判定）。 */
    public static boolean hasCursedGold(List<InlayEntry> inlays) {
        ResourceLocation cursedGold = BuiltInRegistries.ITEM.getKey(ModItems.CURSED_GOLD_INGOT.asItem());
        for (InlayEntry entry : inlays) {
            if (entry.id().equals(cursedGold)) return true;
        }
        return false;
    }

    /** 照抄前置 {@code getBeaconLevel}：从信标位置向下逐层数整层诅咒金块，返回完整层数 0–4。 */
    public static int cursedPyramidLevel(Level level, BlockPos beaconPos) {
        int result = 0;
        for (int i = 1; i <= 4; i++) {
            for (BlockPos pos : BlockPos.betweenClosed(beaconPos.offset(-i, -i, -i), beaconPos.offset(i, -i, i))) {
                if (!level.getBlockState(pos).is(ModBlocks.CURSED_GOLD_BLOCK.get())) return result;
            }
            result = i;
        }
        return result;
    }

    /** 按层数概率尝试转化；命中则替换为腐化信标并触发事件，返回是否转化成功。 */
    public static boolean tryConvert(Level level, BlockPos beaconPos) {
        int pyramidLevel = cursedPyramidLevel(level, beaconPos);
        if (pyramidLevel <= 0) return false;
        if (level.random.nextDouble() >= CHANCE[pyramidLevel]) return false;
        level.setBlockAndUpdate(beaconPos, ModBlocks.CORRUPTED_BEACON.getDefaultState());
        TriggerUtil.convertBeacon(level, beaconPos);
        return true;
    }
}
