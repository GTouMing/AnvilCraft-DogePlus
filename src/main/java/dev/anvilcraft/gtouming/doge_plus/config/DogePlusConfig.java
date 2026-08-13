package dev.anvilcraft.gtouming.doge_plus.config;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;
import net.neoforged.fml.config.ModConfig;

/**
 * DogePlus 模组配置。
 */

@Config(name = AnvilCraftDogePlus.MOD_ID, type = ModConfig.Type.SERVER)
public class DogePlusConfig {

    /** 铁砧命中实体时的基础伤害。 */
    @Comment("铁砧命中实体时的基础伤害。")
    @BoundedDiscrete(max = 100, min = 1)
    public int baseDamage = 10;

    /** 目标每有一个标记，铁砧伤害的加成值。 */
    @Comment("目标每有一个标记，铁砧伤害的加成值。")
    @BoundedDiscrete(max = 100, min = 1)
    public int perMark = 2;

    /** 铁砧发射速度。 */
    @Comment("铁砧发射速度。")
    @BoundedDiscrete(max = 10.0, min = 0.1)
    public double anvilSpeed = 2.5;

    /** 磁铁锭标记目标的视线射线范围（格）。 */
    @Comment("磁铁锭标记目标的视线射线范围（格）。")
    @BoundedDiscrete(max = 512, min = 1)
    public int markRange = 64;

    /** 铁砧飞行硬性超时（tick）。 */
    @Comment("铁砧飞行硬性超时（tick）。")
    @BoundedDiscrete(max = 72000, min = 20)
    public int flyLifetime = 400;

    @Comment("Doge砧长成巨型Doge砧所需成长值。")
    @BoundedDiscrete(max = 1280, min = 1)
    public int maxGrowth = 128;

    @Comment("每个生肉提供的成长值。")
    @BoundedDiscrete(max = 128, min = 1)
    public int growthPerMeat = 1;
}
