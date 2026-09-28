package dev.anvilcraft.gtouming.doge_plus.recipe.inlay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.init.ModRecipeTypes;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayCraftingRecipe;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay_crafting.InlayMatcher;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
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
        MaterialManager.InlayMaterial inlayMaterial = getInlayMaterial();
        MaterialManager.BaseMaterial baseMaterial = getBaseMaterial();
        if (inlayMaterial == null || baseMaterial == null) return false;
        if (inlayStack.isEmpty() || baseStack.isEmpty()) return false;

        // 使用 Ingredient 匹配
        if (!inlayMaterial.ingredient().test(inlayStack)) return false;
        if (!baseMaterial.ingredient().test(baseStack)) return false;

        // 未注册的材料没有固定性质，不可镶嵌
        if (MaterialManager.getInlayMaterial(inlayStack) == null) return false;

        return MaterialManager.hasSocket(baseStack);
    }

    /**
     * 在配方管理器中查找匹配「材料 + 基材」的镶嵌配方。
     *
     * <p>镶嵌台与镶嵌载体共用此校验：只有存在配方的组合才允许镶嵌，
     * 未定义配方的材料（如把下界合金直接镶到镶嵌载体上）一律拒绝。</p>
     *
     * @return 匹配的配方；没有可用配方时返回 {@code null}
     */
    @Nullable
    public static InlayRecipe find(Level level, ItemStack inlayStack, ItemStack baseStack) {
        List<RecipeHolder<InlayRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_TYPE.get());

        for (RecipeHolder<InlayRecipe> holder : recipes) {
            if (holder.value().matches(inlayStack, baseStack)) {
                return holder.value();
            }
        }
        return null;
    }

    /**
     * 静默镶嵌：显式镶嵌配方被移除后，仍然允许「符合镶合条件」的组合镶上去。
     *
     * <p>判定依据是镶合配方本身：只要存在一条以该基材为模具、且 {@code inlays} 中有任一
     * ingredient 接受该材料的镶合配方，这次镶嵌就放行——但它不写进 {@code recipe/inlay}，
     * 因此不会出现在 JEI 的镶嵌配方里。材料<b>不需要</b>有 {@code material/inlay} 定义
     * （两种中子锭就是如此：没有属性，只作为镶合条件里的材料）；基材的镶孔定义仍照常生效。</p>
     *
     * @return 是否按静默规则放行
     */
    public static boolean canSilentlyInlay(Level level, ItemStack inlayStack, ItemStack baseStack) {
        if (inlayStack.isEmpty() || baseStack.isEmpty()) return false;
        if (!MaterialManager.hasSocket(baseStack)) return false;

        List<RecipeHolder<InlayCraftingRecipe>> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.INLAY_CRAFTING_TYPE.get());
        for (RecipeHolder<InlayCraftingRecipe> holder : recipes) {
            InlayCraftingRecipe crafting = holder.value();
            if (!baseStack.is(crafting.getBaseItem())) continue;
            for (InlayMatcher matcher : crafting.inlays()) {
                if (matcher.test(inlayStack)) return true;
            }
        }
        return false;
    }

    @Override
    public RecipeSerializer<InlayRecipe> getSerializer() {
        return ModRecipeTypes.INLAY_SERIALIZER.get();
    }

    @Override
    public RecipeType<InlayRecipe> getType() {
        return ModRecipeTypes.INLAY_TYPE.get();
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