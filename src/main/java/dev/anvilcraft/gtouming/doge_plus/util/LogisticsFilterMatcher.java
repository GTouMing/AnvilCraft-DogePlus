package dev.anvilcraft.gtouming.doge_plus.util;

import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.FilterItem;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 物流过滤匹配：在 {@link FilterItem#filter} 的基础上，让结构磁盘按“结构里包含的方块”匹配。
 *
 * <p>普通物品 / 过滤器仍走前置逻辑；{@code anvilcraft:structure_disk} 会读取结构 NBT 的 palette，
 * 把其中的方块物品集合缓存起来，候选物品只要是结构里出现过的方块就放行。结构文件按文件名缓存，
 * 避免每次过滤判定都读盘。</p>
 */
public final class LogisticsFilterMatcher {

    /** 服务端结构缓存上限；结构磁盘的 file 名含 UUID，基本不会冲突。 */
    private static final int MAX_CACHE = 64;
    private static final Map<String, Set<Item>> STRUCTURE_BLOCKS = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Set<Item>> eldest) {
            return size() > MAX_CACHE;
        }
    };

    private LogisticsFilterMatcher() {
    }

    /**
     * 过滤是否放行该物品。
     *
     * @param filter 过滤物品（可为空栈）
     * @param stack  待判定物品
     */
    public static boolean matches(Level level, ItemStack filter, ItemStack stack) {
        if (filter.isEmpty()) return true;
        if (!filter.is(ModItems.STRUCTURE_DISK.get())) return FilterItem.filter(filter, stack);
        if (stack.isEmpty()) return false;

        // 结构磁盘之间仍按物品本身匹配；其余候选按结构内的方块集合匹配。
        if (FilterItem.filter(filter, stack)) return true;
        return structureBlocks(level, filter).contains(stack.getItem());
    }

    private static Set<Item> structureBlocks(Level level, ItemStack disk) {
        StructureDiskData data = disk.get(ModComponents.STRUCTURE_DISK_DATA);
        if (data == null || data.file().isEmpty()) return Set.of();

        synchronized (STRUCTURE_BLOCKS) {
            Set<Item> cached = STRUCTURE_BLOCKS.get(data.file());
            if (cached != null) return cached;
        }

        Set<Item> blocks = loadStructureBlocks(level, disk);
        synchronized (STRUCTURE_BLOCKS) {
            STRUCTURE_BLOCKS.put(data.file(), blocks);
        }
        return blocks;
    }

    private static Set<Item> loadStructureBlocks(Level level, ItemStack disk) {
        StructureLoadUtil.StructureData structure = StructureLoadUtil.loadStructureFromDisk(level, disk);
        if (structure == null) return Set.of();

        Set<Item> blocks = new HashSet<>();
        for (StructureLoadUtil.BlockPosition position : structure.blocks) {
            Item item = position.state().getBlock().asItem();
            if (item != Items.AIR) blocks.add(item);
        }
        return Set.copyOf(blocks);
    }
}
