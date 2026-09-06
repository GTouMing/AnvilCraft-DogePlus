package dev.anvilcraft.gtouming.doge_plus.datagen.recipe;

import java.util.List;

/**
 * inlay（镶嵌）配方的源数据。
 *
 * <p>inlay 配方 JSON 只含两个字段：{@code inlay}（材料定义文件名）与 {@code base}（基材定义文件名），
 * 实际物品解析由运行时 {@code MaterialManager} 读取 {@code data/<ns>/material/} 完成。</p>
 *
 * @param fileName 配方文件路径（{@code recipe/inlay/<fileName>}），不含 {@code .json}
 * @param inlay    材料文件名键
 * @param base     基材文件名键
 */
public record InlayRecipeData(String fileName, String inlay, String base) {

    /**
     * 与 {@code data/anvilcraft_doge_plus/recipe/inlay/} 下 24 个手写 JSON 逐字一致。
     */
    public static final List<InlayRecipeData> ALL = List.of(
            new InlayRecipeData("and_gate_sapphire_block", "and_gate", "sapphire_block"),
            new InlayRecipeData("attack_melee_weapons", "attack", "melee_weapons"),
            new InlayRecipeData("cold_forged_tools", "cold_forged", "tools"),
            new InlayRecipeData("defense_armors", "defense", "armors"),
            new InlayRecipeData("direction_gem_blocks", "direction", "gem_blocks"),
            new InlayRecipeData("effect_armors", "effect", "armors"),
            new InlayRecipeData("effect1_melee_weapons", "effect1", "melee_weapons"),
            new InlayRecipeData("ember_metal_ingot_tools", "ember_metal_ingot", "tools"),
            new InlayRecipeData("enchant_enchantables", "enchant", "enchantables"),
            new InlayRecipeData("eternal_enchantables", "eternal", "enchantables"),
            new InlayRecipeData("generator_acceleration_ring", "generator", "acceleration_ring"),
            new InlayRecipeData("generator_deflection_ring", "generator", "deflection_ring"),
            new InlayRecipeData("input_gem_blocks", "input", "gem_blocks"),
            new InlayRecipeData("life_armors", "life", "armors"),
            new InlayRecipeData("magnetic_gem_blocks", "magnetic", "gem_blocks"),
            new InlayRecipeData("not_gate_ruby_block", "not_gate", "ruby_block"),
            new InlayRecipeData("or_gate_topaz_block", "or_gate", "topaz_block"),
            new InlayRecipeData("output_emerald_block", "output", "emerald_block"),
            new InlayRecipeData("resonance_enchantables", "resonance", "enchantables"),
            new InlayRecipeData("totems_crab_claw", "totems", "crab_claw")
    );
}
