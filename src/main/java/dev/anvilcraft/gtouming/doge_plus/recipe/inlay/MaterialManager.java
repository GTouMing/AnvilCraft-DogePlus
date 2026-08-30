package dev.anvilcraft.gtouming.doge_plus.recipe.inlay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * 材料数据管理器：数据驱动地加载 {@code data/<ns>/material/} 文件夹，并以**文件名作键**。
 *
 * <ul>
 *   <li>{@code material/inlays/<材料>.json}：定义材料物品拥有哪些镶嵌性质（{@link InlayMaterial}）。</li>
 *   <li>{@code material/bases/<基材>.json}：定义基材（物品/方块）的镶孔数（{@link BaseMaterial}）。</li>
 * </ul>
 *
 * <p>键为「文件名」对应的 {@link ResourceLocation}（如 {@code inlays/doge_steel_ingot} 的键为
 * {@code anvilcraft_doge_plus:doge_steel_ingot}），镶嵌配方通过该键查询材料/基材定义，
 * 值（属性 / 镶孔数）即为配方行为的依据。</p>
 */
public class MaterialManager extends SimpleJsonResourceReloadListener {

    private static final Gson GSON = new GsonBuilder().create();
    public static final MaterialManager INSTANCE = new MaterialManager();

    /** 未定义镶孔数据时的默认镶孔数。 */
    public static final int DEFAULT_SOCKETS = 1;

    /** 材料定义表：文件名键 → 材料定义（含 Ingredient 与性质）。 */
    private static final Map<ResourceLocation, InlayMaterial> INLAYS = new HashMap<>();
    /** 基材定义表：文件名键 → 基材定义（含 Ingredient 与镶孔数）。 */
    private static final Map<ResourceLocation, BaseMaterial> BASES = new HashMap<>();

    private MaterialManager() {
        super(GSON, "material");
    }

    @Override
    protected void apply(
            Map<ResourceLocation, JsonElement> map,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        INLAYS.clear();
        BASES.clear();
        var registryOps = RegistryOps.create(
                JsonOps.INSTANCE,
                RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY)
        );
        for (Map.Entry<ResourceLocation, JsonElement> entry : map.entrySet()) {
            ResourceLocation file = entry.getKey();
            String path = file.getPath();
            try {
                if (path.startsWith("inlay/")) {
                    InlayMaterial material = InlayMaterial.CODEC.codec()
                            .parse(registryOps, entry.getValue())
                            .getOrThrow();
                    INLAYS.put(file.withPath(path.substring("inlay/".length())), material);
                } else if (path.startsWith("base/")) {
                    BaseMaterial base = BaseMaterial.CODEC.codec()
                            .parse(registryOps, entry.getValue())
                            .getOrThrow();
                    BASES.put(file.withPath(path.substring("base/".length())), base);
                }
            } catch (Exception e) {
                AnvilCraftDogePlus.LOGGER.error("Failed to load inlay data {}: {}", file, e.getMessage());
            }
        }
    }

    /**
     * 查询物品堆是否属于镶嵌材料；非材料返回 null。
     * 匹配规则：优先匹配物品，然后匹配标签
     */
    @Nullable
    public static InlayMaterial getInlayMaterial(ItemStack stack) {
        for (InlayMaterial material : INLAYS.values()) {
            if (material.ingredient().test(stack)) {
                return material;
            }
        }
        return null;
    }

    /** 基材是否定义了镶孔数据（用于 tooltip 显示空镶孔）。 */
    public static boolean hasSocket(ItemStack baseStack) {
        for (BaseMaterial base : BASES.values()) {
            if (base.ingredient().test(baseStack)) {
                return true;
            }
        }
        return false;
    }

    /** 查询基材的镶孔数；无定义时返回 {@link #DEFAULT_SOCKETS}。 */
    public static int getSocketCount(ItemStack baseStack) {
        for (BaseMaterial base : BASES.values()) {
            if (base.ingredient().test(baseStack)) {
                return base.sockets();
            }
        }
        return DEFAULT_SOCKETS;
    }

    /** 按材料定义文件名查询（键：{@code anvilcraft_doge_plus:doge_steel_ingot}）；未定义返回 null。 */
    @Nullable
    public static InlayMaterial getInlayMaterial(ResourceLocation fileKey) {
        return INLAYS.get(fileKey);
    }

    /** 按基材定义文件名查询（键：{@code anvilcraft_doge_plus:anvil}）；未定义返回 null。 */
    @Nullable
    public static BaseMaterial getBaseMaterial(ResourceLocation fileKey) {
        return BASES.get(fileKey);
    }

    /**
     * 基材镶孔定义：优先解析数据组件格式，其次解析普通物品/标签格式。
     * JSON 格式：
     *   - 数据组件：{"ingredient": {"type": "neoforge:data_component", "items": [{"item": "minecraft:potion"}], "components": {"minecraft:potion_contents": {}}, "strict": false}, "sockets": 1}
     *   - 普通物品：{"ingredient": {"item": "minecraft:stone"}, "sockets": 3}
     *   - 物品数组：{"ingredient": [{"item": "minecraft:stone"}, {"item": "minecraft:granite"}], "sockets": 2}
     *   - 标签：{"ingredient": {"tag": "minecraft:planks"}, "sockets": 2}
     */
    public record BaseMaterial(Ingredient ingredient, int sockets) {
        public static final MapCodec<BaseMaterial> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(BaseMaterial::ingredient),
                ExtraCodecs.POSITIVE_INT.fieldOf("sockets").forGetter(BaseMaterial::sockets)
        ).apply(instance, BaseMaterial::new));
    }

    /**
     * 材料定义：优先解析数据组件格式，其次解析普通物品/标签格式。
     * JSON 格式：
     *   - 数据组件：{"ingredient": {"type": "neoforge:data_component", "items": [{"item": "minecraft:potion"}], "components": {"minecraft:potion_contents": {"potion": "minecraft:strength"}}, "strict": false}, "attributes": ["effect"]}
     *   - 普通物品：{"ingredient": {"item": "minecraft:diamond"}, "attributes": ["attack", "defense"]}
     *   - 物品数组：{"ingredient": [{"item": "minecraft:diamond"}, {"item": "minecraft:emerald"}], "attributes": ["defense"]}
     *   - 标签：{"ingredient": {"tag": "minecraft:logs"}, "attributes": ["life"]}
     */
    public record InlayMaterial(Ingredient ingredient, HashSet<InlayProperty> properties) {
        public static final MapCodec<InlayMaterial> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(InlayMaterial::ingredient),
                InlayProperty.CODEC.listOf().flatXmap(
                        list -> DataResult.success(new HashSet<>(list)),
                        set -> DataResult.success(new ArrayList<>(set))
                ).fieldOf("attributes").forGetter(InlayMaterial::properties)
        ).apply(instance, InlayMaterial::new));
    }
}