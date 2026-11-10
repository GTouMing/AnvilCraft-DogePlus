package dev.anvilcraft.gtouming.doge_plus.block.entity;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 可设置「配方过滤」的方块实体。
 *
 * <p>镶嵌台把过滤解释为基材，镶合台解释为目标产物；具体校验由实现决定。
 * 封包与客户端交互只需要认识这个接口，不再区分两种台子。</p>
 */
public interface RecipeFilterHost {

    /** 该过滤物品是否可以设置为当前台子的配方过滤。 */
    boolean canSetFilter(ItemStack stack, Level level);

    /** 设置 / 清除配方过滤（服务端调用）。 */
    void setRecipeFilter(ItemStack stack);

    /** 当前配方过滤；空栈表示不过滤。 */
    ItemStack getRecipeFilter();
}
