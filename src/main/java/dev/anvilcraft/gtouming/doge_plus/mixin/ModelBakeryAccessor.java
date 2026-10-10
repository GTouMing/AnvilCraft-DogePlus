package dev.anvilcraft.gtouming.doge_plus.mixin;

import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * 取出 {@code ModelBakery} 里未烘焙的模型，供 {@code DogeBlockSelection} 解码出模型几何。
 * 与前置 {@code dev.dubhe.anvilcraft.mixin.accessor.ModelBakeryAccessor} 同样的两个成员。
 */
@Mixin(ModelBakery.class)
public interface ModelBakeryAccessor {
    @Accessor
    Map<ModelResourceLocation, UnbakedModel> getTopLevelModels();

    @Accessor
    Map<ResourceLocation, UnbakedModel> getUnbakedCache();
}
