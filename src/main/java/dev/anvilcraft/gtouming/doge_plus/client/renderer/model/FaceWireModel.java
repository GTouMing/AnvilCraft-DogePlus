package dev.anvilcraft.gtouming.doge_plus.client.renderer.model;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.ClientBlockInlayData;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.data.MultipartModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 单个面的<strong>动态</strong>通道模型：按邻居决定是否用 port，按镶嵌材料决定逻辑 / 物流 wire。
 *
 * <p>两个条件都依赖世界数据，无法写进 blockstate。这里把每个面拆成一个独立模型，模型自己知道
 * 面方向；烘焙时同时准备普通 / port / 物流 / 物流 port 四套几何，渲染时用 {@link #PORT} 与
 * {@link #LOGISTICS} 选择。模型数据在 {@link #getModelData} 里读邻居方块状态与客户端
 * {@link BlockInlayManager} 算出，不会膨胀方块状态数。</p>
 *
 * <p>Sodium 的 multipart 路径会把顶层 {@code MultipartModelData} 直接传给子模型（而不是
 * {@code MultiPartBakedModel#getQuads} 里那样逐个 resolve），所以 {@link #getQuads} 先调用
 * {@link MultipartModelData#resolve(ModelData, BakedModel)} 兜底；非 multipart 路径下 resolve
 * 会原样返回。</p>
 */
public final class FaceWireModel implements IDynamicBakedModel {

    /** 该面是否使用 port 变体。 */
    public static final ModelProperty<Boolean> PORT = new ModelProperty<>();

    /** 该面是否使用物流 wire（镶嵌材料是搬运角色）。 */
    public static final ModelProperty<Boolean> LOGISTICS = new ModelProperty<>();

    private final Direction face;
    private final BakedModel normal;
    private final BakedModel port;
    private final BakedModel logistics;
    private final BakedModel logisticsPort;

    public FaceWireModel(
            Direction face,
            BakedModel normal,
            BakedModel port,
            BakedModel logistics,
            BakedModel logisticsPort) {
        this.face = face;
        this.normal = normal;
        this.port = port;
        this.logistics = logistics;
        this.logisticsPort = logisticsPort;
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource random,
            ModelData modelData,
            @Nullable RenderType renderType) {
        ModelData resolved = MultipartModelData.resolve(modelData, this);
        boolean usePort = Boolean.TRUE.equals(resolved.get(PORT));
        boolean useLogistics = Boolean.TRUE.equals(resolved.get(LOGISTICS));
        BakedModel model = useLogistics
                ? (usePort ? this.logisticsPort : this.logistics)
                : (usePort ? this.port : this.normal);
        return model.getQuads(state, side, random, resolved, renderType);
    }

    @Override
    public ModelData getModelData(
            BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        boolean usePort = InlayCarrierBlock.usesPort(level, pos, this.face);
        // Sodium 的模型构建线程拿到的是 LevelSlice，不是 Level；直接读客户端镜像，
        // 避免 BlockInlayManager.get 因 level 类型不对而退回空数据。
        boolean useLogistics = ClientBlockInlayData.get(pos).getFace(this.face).isTransfer();
        if (!usePort && !useLogistics) return modelData;
        ModelData.Builder builder = modelData.derive();
        if (usePort) builder.with(PORT, true);
        if (useLogistics) builder.with(LOGISTICS, true);
        return builder.build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData modelData) {
        ModelData resolved = MultipartModelData.resolve(modelData, this);
        return ChunkRenderTypeSet.union(List.of(
                this.normal.getRenderTypes(state, random, resolved),
                this.port.getRenderTypes(state, random, resolved),
                this.logistics.getRenderTypes(state, random, resolved),
                this.logisticsPort.getRenderTypes(state, random, resolved)));
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.normal.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.normal.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.normal.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.normal.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.normal.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.normal.getOverrides();
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        return this.normal.getRenderPasses(stack, fabulous);
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return this.normal.getRenderTypes(stack, fabulous);
    }
}
