package dev.anvilcraft.gtouming.doge_plus.attribute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 等级属性数值管理器：数据驱动地加载 {@code data/<ns>/attributes/<属性>.json}，以**文件名作键**。
 *
 * <p>等级有规律时用函数表达，无规律时用显式表；二者取其一：</p>
 * <ul>
 *   <li>函数：{@code {"function": "2 * level"}}（见 {@link LevelFunction}，变量为 {@code level}）</li>
 *   <li>表：{@code {"values": [v1, v2, ...]}}，下标 0 对应等级 1，超出表长取最后一档</li>
 * </ul>
 *
 * <p>键即属性 id（如 {@code anvilcraft_doge_plus:defense}），与 {@link LeveledAttributes} 中登记的行为配合：
 * 文件只提供每级数值，具体施加方式写死在代码里。</p>
 */
public class AttributeManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    public static final AttributeManager INSTANCE = new AttributeManager();

    /** 属性 id → 每级数值表。 */
    private static final Map<ResourceLocation, AttributeValues> TABLES = new HashMap<>();

    private AttributeManager() {
        super(GSON, "attributes");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> map,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        TABLES.clear();
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            try {
                AttributeValues values = AttributeValues.CODEC
                        .parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow();
                TABLES.put(entry.getKey(), values);
            } catch (Exception e) {
                AnvilCraftDogePlus.LOGGER.error("Failed to load attribute values {}: {}", entry.getKey(), e.getMessage());
            }
        }
    }

    /**
     * 查询该等级属性在指定等级的数值。
     *
     * @return 等级 ≤0 或未定义该属性时返回 0；等级超出表长时取最后一档
     */
    public static double valueOf(ResourceLocation id, int level) {
        AttributeValues values = TABLES.get(id);
        if (values == null || level <= 0) return 0.0;
        return values.valueAt(level);
    }

    /** 是否定义了该属性的数值表。 */
    public static boolean hasTable(ResourceLocation id) {
        return TABLES.containsKey(id);
    }

    /**
     * 每级数值：函数或显式表二选一。
     *
     * @param values   显式表（下标 0 = 等级 1）；用函数时为 null
     * @param function 等级函数；用显式表时为 null
     */
    public record AttributeValues(@Nullable List<Double> values, @Nullable LevelFunction function) {

        public static final Codec<AttributeValues> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.DOUBLE.listOf().optionalFieldOf("values")
                        .forGetter(v -> Optional.ofNullable(v.values())),
                Codec.STRING.optionalFieldOf("function")
                        .forGetter(v -> Optional.ofNullable(v.function()).map(LevelFunction::source))
        ).apply(instance, (values, function) -> new AttributeValues(
                values.map(List::copyOf).orElse(null),
                function.map(LevelFunction::parse).orElse(null))));

        public double valueAt(int level) {
            if (function != null) return function.evaluate(level);
            if (values == null || values.isEmpty()) return 0.0;
            return values.get(Math.min(level, values.size()) - 1);
        }
    }
}
