package dev.anvilcraft.gtouming.doge_plus.recipe.inlay;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.init.ModIngredients;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

/**
 * 按镶嵌属性匹配的 ingredient：物品是镶嵌材料、且其材料定义携带 {@link #properties()} 中<b>任意一条</b>属性即命中。
 *
 * <p>「某个镶孔要什么材料」本来只能用具体物品/标签枚举，遇到「门种不限，只要是门就行」这类要求就写不出来。
 * 这里把判定改成看材料定义（{@code material/inlay/} 里的 {@code attributes}），于是配方可以按属性种类书写：</p>
 *
 * <pre>{@code
 * { "type": "anvilcraft_doge_plus:inlay_property",
 *   "properties": ["and_gate", "or_gate", "not_gate"] }
 * }</pre>
 *
 * <p>{@link #getItems()} 返回携带这些属性的材料物品，供 JEI 与配方书展示——
 * 这也是本类存在的意义：硬编码的「非空镶孔数 = N 且都是门」那种特例判定没法给 JEI 任何可展示的东西。</p>
 *
 * @param properties 命中所需的属性（任意一条；已去重保序）。
 */
public record InlayPropertyIngredient(List<InlayProperty> properties) implements ICustomIngredient {

    public static final MapCodec<InlayPropertyIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            InlayProperty.CODEC.listOf().fieldOf("properties").forGetter(InlayPropertyIngredient::properties)
    ).apply(instance, InlayPropertyIngredient::new));

    public InlayPropertyIngredient(List<InlayProperty> properties) {
        // 去重保序：同一属性写两遍不影响判定，展示顺序仍按书写顺序。
        this.properties = List.copyOf(new LinkedHashSet<>(properties));
    }

    /**
     * 便捷构造：直接得到可放进配方 {@code inlays} 的 {@link Ingredient}。
     */
    public static Ingredient of(List<InlayProperty> properties) {
        return new InlayPropertyIngredient(properties).toVanilla();
    }

    @Override
    public boolean test(ItemStack stack) {
        MaterialManager.InlayMaterial material = MaterialManager.getInlayMaterial(stack);
        if (material == null) return false;
        for (InlayProperty property : properties) {
            if (material.properties().contains(property)) return true;
        }
        return false;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return MaterialManager.itemsWithAnyOf(properties).stream().map(ItemStack::new);
    }

    @Override
    public boolean isSimple() {
        // 不是物品/标签的简单并集：命中与否要看材料定义，必须逐堆走 test。
        return false;
    }

    @Override
    public IngredientType<?> getType() {
        return ModIngredients.INLAY_PROPERTY.get();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof InlayPropertyIngredient(
                List<InlayProperty> properties1
        ) && this.properties.equals(properties1));
    }

    @Override
    public String toString() {
        return "InlayPropertyIngredient" + this.properties;
    }
}
