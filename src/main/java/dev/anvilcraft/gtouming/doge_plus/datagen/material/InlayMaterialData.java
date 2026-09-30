package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import dev.anvilcraft.gtouming.doge_plus.datagen.recipe.AnvilcraftSmithingRecipes;
import dev.anvilcraft.gtouming.doge_plus.init.ModItemTags;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.anvilcraft.gtouming.doge_plus.datagen.material.MaterialBuilder.builder;

/**
 * 镶嵌材料（inlay material）源数据：{@code data/anvilcraft_doge_plus/material/inlay/<name>.json}。
 * <p>通过 {@link MaterialBuilder} 声明式描述 ingredient 匹配条件（物品/标签/带数据组件的药水），
 * 序列化后与原手写内容等价。effect 系列（普通/喷溅/滞留药水）由 {@link PotionKinds} 展开
 * 42 项数据组件条件。</p>
 * <p>材料文件键（{@code name}）集中定义为下方 {@code NAME_*} 常量；匹配物品/标签直接引用
 * vanilla / AnvilCraft / 本 mod 的注册条目（{@link Items} / {@link ModItems} entry / {@link ModItemTags} tag），
 * id 由注册表解析，避免手写字面量出错。添加新材料只需在下方加一行。</p>
 *
 * @param name       文件键（不含 {@code .json}）
 * @param ingredient ingredient 字段的 JSON 数组
 * @param attributes 属性名列表
 */
public record InlayMaterialData(String name, JsonArray ingredient, String... attributes) {

    // 材料文件键常量 —— 唯一字面量定义处，ALL 与 InlayRecipeData 均引用它们
    public static final String AND_GATE = "and_gate";
    public static final String ATTACK = "attack";
    public static final String CHUTE = "chute";
    public static final String COLD_FORGED = "cold_forged";
    public static final String COUNTER_GATE = "counter_gate";
    public static final String DEFENSE = "defense";
    public static final String DELAY_GATE = "delay_gate";
    public static final String DELAY_INPUT_GATE = "delay_input_gate";
    public static final String EFFECT = "effect";
    public static final String EFFECT1 = "effect1";
    public static final String EFFECT2 = "effect2";
    public static final String EMBER_METAL_INGOT = "ember_metal_ingot";
    public static final String ENCHANT = "enchant";
    public static final String ETERNAL = "eternal";
    public static final String FIRE_PROOF = "fire_proof";
    public static final String GENERATOR = "generator";
    public static final String INPUT = "input";
    public static final String LATCH_GATE = "latch_gate";
    public static final String LIFE = "life";
    public static final String MAGNETIC = "magnetic";
    public static final String MAGNETIC_CHUTE = "magnetic_chute";
    public static final String NOT_GATE = "not_gate";
    public static final String OUTPUT = "output";
    public static final String REMOTE = "remote";
    public static final String RESONANCE = "resonance";
    public static final String TOTEMS = "totems";

    // ===== 原版锻造兼容（无属性材料）=====
    // 说明：下界合金锭、钻石装备与纹饰材料已不再定义材料——它们没有属性、也没有手写镶嵌配方，
    // 完全由「静默镶嵌」按镶合条件放行（原先的 NETHERITE_UPGRADE_INLAYS / TRIM_INLAYS 常量随之移除）。

    // ===== 空心磁铁块镶合（无属性材料，手写镶嵌配方引用其材料键）=====
    public static final String IRON_INGOT = "iron_ingot";

    /**
     * 六种「门」属性：非门 / 与门 / 计数门 / 锁存门 / 延时门 / 延时输入门。
     *
     * <p>输入与输出不算在内——它们只是端口（文本上也不叫「门」），因此在逻辑载体的配方里
     * 不能顶替任何一种门。这份清单的条数正好等于镶嵌载体的镶孔数：配方按「和」匹配，
     * 要求六个镶孔里这六种门各一个。（或门已移除，其配方位由延时输入门顶替。）</p>
     */
    public static final List<InlayProperty> GATE_PROPERTIES = List.of(
            InlayProperty.NOT_GATE,
            InlayProperty.AND_GATE,
            InlayProperty.COUNTER_GATE,
            InlayProperty.LATCH_GATE,
            InlayProperty.DELAY_GATE,
            InlayProperty.DELAY_INPUT_GATE
    );

    /** 下界合金升级 / 盔甲纹饰的材料不再定义：它们没有属性、也没有手写镶嵌配方，
     * 由「静默镶嵌」按镶合条件放行，故原 NETHERITE_UPGRADE_INLAYS / TRIM_* 常量一并移除。 */

    /** 本 mod 内置镶嵌材料（含属性）。 */
    private static final List<InlayMaterialData> MANUAL = new ArrayList<>();
    static {
        MANUAL.addAll(List.of(
            builder().name(AND_GATE)
                    .item(Items.REPEATER)
                    .attributes(InlayProperty.AND_GATE).buildInlay(),
            builder().name(ATTACK)
                    .item(ModItems.CURSED_GOLD_INGOT)
                    .attributes(InlayProperty.ATTACK).buildInlay(),
            builder().name(CHUTE)
                    .item(ModBlocks.CHUTE)
                    .attributes(InlayProperty.INSERT).buildInlay(),
            builder().name(COLD_FORGED)
                    .item(ModItems.FROST_METAL_INGOT)
                    .attributes(InlayProperty.COLD_FORGED).buildInlay(),
            builder().name(COUNTER_GATE)
                    .tag(ItemTags.BUTTONS)
                    .attributes(InlayProperty.COUNTER_GATE).buildInlay(),
            builder().name(DELAY_GATE)
                    // 走方块标签 minecraft:pressure_plates（无对应物品标签；前置也往这里加压力板）
                    .blockTag(BlockTags.PRESSURE_PLATES)
                    .attributes(InlayProperty.DELAY_GATE).buildInlay(),
            builder().name(DELAY_INPUT_GATE)
                    .item(Items.CLOCK)
                    .attributes(InlayProperty.DELAY_INPUT_GATE).buildInlay(),
            builder().name(DEFENSE)
                    .item(Items.NETHERITE_INGOT)
                    .attributes(InlayProperty.DEFENSE).buildInlay(),
            builder().name(EMBER_METAL_INGOT)
                    .item(ModItems.EMBER_METAL_INGOT)
                    .attributes(InlayProperty.FIRE_PROOF, InlayProperty.HIGH_TEMP).buildInlay(),
            builder().name(ENCHANT)
                    .item(Items.ENCHANTED_BOOK)
                    .item(Items.BOOK)
                    .attributes(InlayProperty.ENCHANT).buildInlay(),
            builder().name(ETERNAL)
                    .item(ModItems.TRANSCENDIUM_INGOT)
                    .attributes(InlayProperty.ETERNAL).buildInlay(),
            builder().name(FIRE_PROOF)
                    .item(Items.NETHERITE_INGOT)
                    .attributes(InlayProperty.FIRE_PROOF).buildInlay(),
            builder().name(GENERATOR)
                    .item(ModItems.SUPER_CAPACITOR)
                    .attributes(InlayProperty.GENERATOR).buildInlay(),
            builder().name(INPUT)
                    .item(ModBlocks.REDSTONE_WIRE)
                    .attributes(InlayProperty.INPUT).buildInlay(),
            builder().name(LATCH_GATE)
                    .item(Items.LEVER)
                    .attributes(InlayProperty.LATCH_GATE).buildInlay(),
            builder().name(LIFE)
                    .item(ModItems.ROYAL_STEEL_INGOT)
                    .attributes(InlayProperty.LIFE).buildInlay(),
            builder().name(MAGNETIC)
                    .item(ModItems.MAGNET_INGOT)
                    .attributes(InlayProperty.MAGNETIC).buildInlay(),
            builder().name(MAGNETIC_CHUTE)
                    .item(ModBlocks.MAGNETIC_CHUTE)
                    .attributes(InlayProperty.EXTRACT).buildInlay(),
            builder().name(NOT_GATE)
                    .item(Items.REDSTONE_TORCH)
                    .attributes(InlayProperty.NOT_GATE).buildInlay(),
            builder().name(OUTPUT)
                    .item(Items.REDSTONE)
                    .attributes(InlayProperty.OUTPUT).buildInlay(),
            builder().name(REMOTE)
                    .item(Items.ENDER_PEARL)
                    .attributes(InlayProperty.REMOTE).buildInlay(),
            builder().name(RESONANCE)
                    .item(Items.AMETHYST_SHARD)
                    .attributes(InlayProperty.RESONANCE).buildInlay(),
            builder().name(TOTEMS)
                    // 前置新版本移除了 anvilcraft:totem，改用本模组定义的 c:totems
                    .tag(ModItemTags.TOTEMS)
                    .attributes(InlayProperty.NIRVANA).buildInlay(),
            PotionKinds.potionInlay(EFFECT, Items.POTION),
            PotionKinds.potionInlay(EFFECT1, Items.SPLASH_POTION),
            PotionKinds.potionInlay(EFFECT2, Items.LINGERING_POTION),

            // ===== 空心磁铁块镶合：铁锭（无属性材料，但它是手写镶嵌配方的材料键，故保留定义）=====
            builder().name(IRON_INGOT).item(Items.IRON_INGOT).buildInlay()
        ));
    }

    /**
     * 全部镶嵌材料定义：仅内置清单。
     *
     * <p>前置（AnvilCraft）锻造 / 珠宝复制派生的材料<b>不再生成</b>——它们没有属性，只作为
     * 镶合条件里的材料，由「静默镶嵌」按 {@code inlay_crafting} 的 {@code inlays} 放行。</p>
     */
    public static List<InlayMaterialData> all() {
        return new ArrayList<>(MANUAL);
    }
}
