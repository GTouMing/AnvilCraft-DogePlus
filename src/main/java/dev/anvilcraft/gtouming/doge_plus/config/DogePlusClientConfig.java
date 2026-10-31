package dev.anvilcraft.gtouming.doge_plus.config;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import net.neoforged.fml.config.ModConfig;

/**
 * DogePlus 客户端配置。
 */
@Config(name = AnvilCraftDogePlus.MOD_ID, type = ModConfig.Type.CLIENT)
public class DogePlusClientConfig {

    /** 是否在物品图标左上角绘制镶嵌属性像素点阵列。 */
    @Comment("在物品图标左上角用像素点显示镶嵌属性；默认关闭。")
    public boolean showInlayDots = false;

    /**
     * 像素点阵列方向。
     *
     * <p>{@code false}：镶孔沿横向排列，属性沿纵向展开；
     * {@code true}：镶孔沿纵向排列，属性沿横向展开。</p>
     */
    @Comment("像素点阵列改为竖向：镶孔沿纵向排列、属性沿横向展开；关闭时镶孔横向、属性纵向。")
    public boolean verticalInlayDots = false;
}
