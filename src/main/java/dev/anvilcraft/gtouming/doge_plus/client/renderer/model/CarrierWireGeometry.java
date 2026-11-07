package dev.anvilcraft.gtouming.doge_plus.client.renderer.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * 面专用动态通道模型的 geometry：读取一个面方向，烘焙普通 / port 两套子模型。
 *
 * <p>可选提供 {@code logistics} / {@code logistics_port}：镶嵌载体的面会按镶嵌材料在
 * 逻辑 wire 与物流 wire 之间切换。未提供时物流两套与普通两套复用同一份烘焙结果。</p>
 *
 * <p>blockstate 仍按原有 {@code rotationX/rotationY} 旋转这个部件，因此子模型会跟着旋转；
 * 这里的 {@code face} 只用于 {@link FaceWireModel} 判断邻居与材料，几何旋转由 blockstate 负责。</p>
 */
public final class CarrierWireGeometry implements IUnbakedGeometry<CarrierWireGeometry> {

    private final Direction face;
    private final ResourceLocation normalLocation;
    private final ResourceLocation portLocation;
    @Nullable
    private final ResourceLocation logisticsLocation;
    @Nullable
    private final ResourceLocation logisticsPortLocation;

    @Nullable
    private UnbakedModel normal;
    @Nullable
    private UnbakedModel port;
    @Nullable
    private UnbakedModel logistics;
    @Nullable
    private UnbakedModel logisticsPort;

    private CarrierWireGeometry(
            Direction face,
            ResourceLocation normalLocation,
            ResourceLocation portLocation,
            @Nullable ResourceLocation logisticsLocation,
            @Nullable ResourceLocation logisticsPortLocation) {
        this.face = face;
        this.normalLocation = normalLocation;
        this.portLocation = portLocation;
        this.logisticsLocation = logisticsLocation;
        this.logisticsPortLocation = logisticsPortLocation;
    }

    @Override
    public void resolveParents(
            Function<ResourceLocation, UnbakedModel> resolver, IGeometryBakingContext context) {
        this.normal = resolver.apply(this.normalLocation);
        this.port = resolver.apply(this.portLocation);
        this.normal.resolveParents(resolver);
        this.port.resolveParents(resolver);
        if (this.logisticsLocation != null) {
            this.logistics = resolver.apply(this.logisticsLocation);
            this.logistics.resolveParents(resolver);
        }
        if (this.logisticsPortLocation != null) {
            this.logisticsPort = resolver.apply(this.logisticsPortLocation);
            this.logisticsPort.resolveParents(resolver);
        }
    }

    @Override
    public BakedModel bake(
            IGeometryBakingContext context,
            ModelBaker baker,
            Function<Material, TextureAtlasSprite> spriteGetter,
            ModelState modelState,
            ItemOverrides overrides) {
        if (this.normal == null || this.port == null) {
            throw new IllegalStateException("Carrier wire geometry was not resolved: " + this.face);
        }
        BakedModel normalBaked = baker.bakeUncached(this.normal, modelState, spriteGetter);
        BakedModel portBaked = baker.bakeUncached(this.port, modelState, spriteGetter);
        if (normalBaked == null || portBaked == null) {
            throw new IllegalStateException("Carrier wire submodel failed to bake: " + this.face);
        }
        BakedModel logisticsBaked = normalBaked;
        BakedModel logisticsPortBaked = portBaked;
        if (this.logistics != null) {
            logisticsBaked = baker.bakeUncached(this.logistics, modelState, spriteGetter);
            if (logisticsBaked == null) {
                throw new IllegalStateException("Carrier logistics wire submodel failed to bake: " + this.face);
            }
        }
        if (this.logisticsPort != null) {
            logisticsPortBaked = baker.bakeUncached(this.logisticsPort, modelState, spriteGetter);
            if (logisticsPortBaked == null) {
                throw new IllegalStateException("Carrier logistics port submodel failed to bake: " + this.face);
            }
        }
        return new FaceWireModel(this.face, normalBaked, portBaked, logisticsBaked, logisticsPortBaked);
    }

    public static final class Loader implements IGeometryLoader<CarrierWireGeometry> {

        @Override
        public CarrierWireGeometry read(JsonObject json, JsonDeserializationContext context)
                throws JsonParseException {
            Direction face = Direction.byName(GsonHelper.getAsString(json, "face"));
            if (face == null) {
                throw new JsonParseException("Unknown carrier wire face: " + GsonHelper.getAsString(json, "face"));
            }
            ResourceLocation normal = ResourceLocation.parse(GsonHelper.getAsString(json, "normal"));
            ResourceLocation port = ResourceLocation.parse(GsonHelper.getAsString(json, "port"));
            ResourceLocation logistics = json.has("logistics")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "logistics"))
                    : null;
            ResourceLocation logisticsPort = json.has("logistics_port")
                    ? ResourceLocation.parse(GsonHelper.getAsString(json, "logistics_port"))
                    : null;
            return new CarrierWireGeometry(face, normal, port, logistics, logisticsPort);
        }
    }
}
