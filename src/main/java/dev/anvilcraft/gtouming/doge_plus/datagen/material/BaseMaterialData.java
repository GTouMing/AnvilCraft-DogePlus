package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialBuilder.builder;

/**
 * 基材（base material）源数据：{@code data/anvilcraft_doge_plus/material/base/<name>.json}。
 * <p>通过 {@link MaterialBuilder} 声明式描述 ingredient 匹配条件（物品/标签），
 * 序列化后与原先手写内容等价。</p>
 *
 * @param name       文件键（不含 {@code .json}）
 * @param sockets    镶孔数
 * @param ingredient ingredient 字段的 JSON 数组
 */
public record BaseMaterialData(String name, int sockets, JsonArray ingredient) {

    public static final BaseMaterialData[] ALL = {
            builder().name("acceleration_ring").item("anvilcraft:acceleration_ring").sockets(1).buildBase(),
            builder().name("armors").tag("c:armors").sockets(3).buildBase(),
            builder().name("crab_claw").item("anvilcraft:crab_claw").sockets(3).buildBase(),
            builder().name("deflection_ring").item("anvilcraft:deflection_ring").sockets(1).buildBase(),
            builder().name("emerald_block").item("minecraft:emerald_block").sockets(6).buildBase(),
            builder().name("enchantables").tag("c:enchantables").sockets(2).buildBase(),
            builder().name("gem_blocks").tag("anvilcraft:gem_blocks").sockets(6).buildBase(),
            builder().name("melee_weapons").tag("c:tools/melee_weapon").sockets(2).buildBase(),
            builder().name("multiphase_matter_block").item("anvilcraft:multiphase_matter_block").sockets(6).buildBase(),
            builder().name("ruby_block").item("anvilcraft:ruby_block").sockets(6).buildBase(),
            builder().name("sapphire_block").item("anvilcraft:sapphire_block").sockets(6).buildBase(),
            builder().name("tools").tag("c:tools").sockets(2).buildBase(),
            builder().name("topaz_block").item("anvilcraft:topaz_block").sockets(6).buildBase()
    };
}
