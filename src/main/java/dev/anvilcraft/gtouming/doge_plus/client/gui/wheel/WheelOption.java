package dev.anvilcraft.gtouming.doge_plus.client.gui.wheel;

import net.minecraft.world.item.ItemStack;

/**
 * 轮盘里的一项：逻辑门类型与物品搬运角色都实现它，轮盘因此不必关心选项的具体类型。
 */
public interface WheelOption {

    /** 标签翻译 key。 */
    String labelKey();

    /** 图标物品。 */
    ItemStack icon();
}
