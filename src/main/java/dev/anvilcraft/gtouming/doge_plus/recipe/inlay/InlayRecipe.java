package dev.anvilcraft.gtouming.doge_plus.recipe.inlay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.util.InlayUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import mezz.jei.api.recipe.IFocusGroup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 镶嵌配方：声明「材料定义文件名 + 基材定义文件名」可进行的镶嵌。
 * <p>
 * {@code inlay} 是 {@code data/<ns>/material/inlays/} 下材料定义文件的**文件名**，
 * {@code base} 是 {@code data/<ns>/material/bases/} 下基材定义文件的**文件名**。
 * 配方通过文件名键从 {@link MaterialManager} 查询材料性质与基材镶孔数（「配方 → 数据」映射），
 * 再按定义中的 Ingredient 解析具体物品。
 * <p>
 * 继承 {@link AbstractProcessRecipe}，供 JEI 用 {@code AbstractProgressCategory} 通用布局渲染。
 */
public class InlayRecipe extends AbstractProcessRecipe<InlayRecipe> {

    /** 文件名键：裸名默认使用本 mod 命名空间（如 {@code doge_steel_ingot} → {@code anvilcraft_doge_plus:doge_steel_ingot}）。 */
    private static final Codec<ResourceLocation> FILE_ID_CODEC = Codec.STRING.xmap(
            s -> s.contains(":") ? ResourceLocation.parse(s) : AnvilCraftDogePlus.of(s),
            ResourceLocation::toString
    );
    private final ResourceLocation inlay;
    private final ResourceLocation base;

    public InlayRecipe(ResourceLocation inlay, ResourceLocation base) {
        super(new Property());
        this.inlay = inlay;
        this.base = base;
    }

    public ResourceLocation inlay() {
        return inlay;
    }

    public ResourceLocation base() {
        return base;
    }

    /** 按文件名键查询材料定义；未定义返回 null。 */
    @Nullable
    public MaterialManager.InlayMaterial getInlayMaterial() {
        return MaterialManager.getInlayMaterial(inlay);
    }

    /** 按文件名键查询基材定义；未定义返回 null。 */
    @Nullable
    public MaterialManager.BaseMaterial getBaseMaterial() {
        return MaterialManager.getBaseMaterial(base);
    }

    /**
     * 判断材料与基材是否满足本配方。
     * 使用 Ingredient.test() 进行匹配
     */
    public boolean matches(ItemStack inlayStack, ItemStack baseStack) {
        MaterialManager.InlayMaterial material = getInlayMaterial();
        MaterialManager.BaseMaterial baseMaterial = getBaseMaterial();
        if (material == null || baseMaterial == null) return false;
        if (inlayStack.isEmpty() || baseStack.isEmpty()) return false;

        // 使用 Ingredient 匹配
        if (!material.ingredient().test(inlayStack)) return false;
        if (!baseMaterial.ingredient().test(baseStack)) return false;

        // 未注册的材料没有固定性质，不可镶嵌
        if (MaterialManager.getInlayMaterial(inlayStack) == null) return false;

        return MaterialManager.hasSocket(baseStack);
    }

    @Override
    public RecipeSerializer<InlayRecipe> getSerializer() {
        return ModRecipeTypes.INLAY_SERIALIZER.get();
    }

    @Override
    public RecipeType<InlayRecipe> getType() {
        return ModRecipeTypes.INLAY_TYPE.get();
    }

    public List<ItemStack> getResults(IFocusGroup focuses) {
        MaterialManager.InlayMaterial inlayMaterial = getInlayMaterial();
        MaterialManager.BaseMaterial baseMaterial = getBaseMaterial();
        if (inlayMaterial == null || baseMaterial == null) return List.of();

        List<ItemStack> inlays = getInlays(focuses);
        List<ItemStack> bases = getBases(focuses);

        int index = inlays.size() * bases.size();
        List<ItemStack> results = new ArrayList<>();
        for (int i = 0; i < index; i++) {
            int a = i % inlays.size();
            int b = i % bases.size();
            results.add(InlayUtil.withAddedInlay(bases.get(b), InlayEntry.fromItemStack(inlays.get(a))));
        }

        return results;
    }

    public List<ItemStack> getInlays(IFocusGroup focuses) {
        MaterialManager.InlayMaterial inlayMaterial = getInlayMaterial();
        if (inlayMaterial == null) return List.of();

        List<ItemStack> inlays = Arrays.asList(inlayMaterial.ingredient().getItems());
        List<ItemStack> focused = focuses.getItemStackFocuses().map(f -> f.getTypedValue().getIngredient()).toList();
        List<ItemStack> filterInlays = inlays.stream().filter(i -> focused.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p,i))).toList();
        return filterInlays.isEmpty() ? inlays : filterInlays;

    }

    public List<ItemStack> getBases(IFocusGroup focuses) {
        MaterialManager.BaseMaterial baseMaterial = getBaseMaterial();
        if (baseMaterial == null) return List.of();

        List<ItemStack> bases = Arrays.asList(baseMaterial.ingredient().getItems());
        List<ItemStack> focused = focuses.getItemStackFocuses().map(f -> f.getTypedValue().getIngredient()).toList();
        List<ItemStack> filterBases = bases.stream().filter(i -> focused.stream().anyMatch(p -> ItemStack.isSameItemSameComponents(p,i))).toList();
        return filterBases.isEmpty()? bases : filterBases;
    }

    // ==================== 序列化器 ====================

    public static class Serializer implements RecipeSerializer<InlayRecipe> {

        public static final MapCodec<InlayRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                FILE_ID_CODEC.fieldOf("inlay").forGetter(InlayRecipe::inlay),
                FILE_ID_CODEC.fieldOf("base").forGetter(InlayRecipe::base)
        ).apply(instance, InlayRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, InlayRecipe> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, InlayRecipe::inlay,
                ResourceLocation.STREAM_CODEC, InlayRecipe::base,
                InlayRecipe::new
        );

        @Override
        public MapCodec<InlayRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, InlayRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}