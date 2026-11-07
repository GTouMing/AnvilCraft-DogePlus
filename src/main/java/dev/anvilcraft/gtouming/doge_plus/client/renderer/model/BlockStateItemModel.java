package dev.anvilcraft.gtouming.doge_plus.client.renderer.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 用某个固定方块状态的方块模型作为物品模型。
 *
 * <p>物品图标不再手写一套几何，而是直接复用 blockstate 选出的方块模型：中心体、通道、棱件 / 角件
 * 的形状与渲染类型都和该状态在世界上完全一致。模型在 {@code ModelEvent.ModifyBakingResult} 里
 * 替换到物品的 {@code ModelResourceLocation.inventory(...)} 键上。</p>
 *
 * <p>物品必须保留方块的两层：cutout 的不透明件（边框 / 核心外壳）和 translucent 的玻璃件分别
 * 作为两个 render pass，且 cutout 先画。否则整个模型走单一 translucent 批次时，不透明件的混合
 * 顺序会和抽屉等承载物的渲染打架。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class BlockStateItemModel implements BakedModel {

    private final BakedModel blockModel;
    private final BlockState state;

    /**
     * 物品图标固定使用 port 变体：图标不处在世界里，没有邻居可用于 port 判定；
     * 直接给动态 wire wrapper 注入 {@link FaceWireModel#PORT}。
     */
    private static final ModelData PORT_ITEM_DATA =
            ModelData.EMPTY.derive().with(FaceWireModel.PORT, true).build();

    public BlockStateItemModel(BakedModel blockModel, BlockState state) {
        this.blockModel = blockModel;
        this.state = state;
    }

    private static ModelData portItemData(ModelData modelData) {
        return modelData == ModelData.EMPTY
                ? PORT_ITEM_DATA
                : modelData.derive().with(FaceWireModel.PORT, true).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState ignored, @Nullable Direction side, RandomSource random) {
        return this.blockModel.getQuads(this.state, side, random, PORT_ITEM_DATA, null);
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState ignored,
            @Nullable Direction side,
            RandomSource random,
            ModelData modelData,
            @Nullable RenderType renderType) {
        return this.blockModel.getQuads(this.state, side, random, portItemData(modelData), renderType);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState ignored, RandomSource random, ModelData modelData) {
        return this.blockModel.getRenderTypes(this.state, random, portItemData(modelData));
    }

    /**
     * 物品渲染会按 render pass 逐个绘制。这里先 cutout（不透明件，写深度），再 translucent（玻璃件）。
     * 顶层模型自身的 render type 只作为不走 pass 时的保守兜底。
     */
    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        // 两层都使用“固定缓冲”的 block sheet：RenderBuffers 的 endBatch 会先 flush 共享缓冲里的
        // translucentItemSheet，再 flush 固定缓冲里的 cutout，导致半透明先画、遮挡后面的不透明件。
        // translucentCullBlockSheet 与 cutoutBlockSheet 都在固定缓冲表里，按表顺序 cutout 先、
        // translucent 后；方块物品（cull=true）的 canonical 物品层也正是它。
        return List.of(
                new SinglePass(this.blockModel, this.state, RenderType.cutout(), Sheets.cutoutBlockSheet()),
                new SinglePass(this.blockModel, this.state, RenderType.translucent(), Sheets.translucentCullBlockSheet()));
    }

    @Override
    public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
        return List.of(Sheets.cutoutBlockSheet());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.blockModel.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.blockModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.blockModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.blockModel.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.blockModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    /** 只取方块模型中属于 {@code blockRenderType} 那一层的 quad，并用对应的物品渲染层绘制。 */
    @OnlyIn(Dist.CLIENT)
    private record SinglePass(
            BakedModel blockModel,
            BlockState state,
            RenderType blockRenderType,
            RenderType itemRenderType) implements BakedModel {

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState ignored, @Nullable Direction side, RandomSource random) {
            return this.blockModel.getQuads(this.state, side, random, PORT_ITEM_DATA, this.blockRenderType);
        }

        @Override
        public List<BakedQuad> getQuads(
                @Nullable BlockState ignored,
                @Nullable Direction side,
                RandomSource random,
                ModelData modelData,
                @Nullable RenderType renderType) {
            return this.blockModel.getQuads(this.state, side, random, portItemData(modelData), this.blockRenderType);
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState ignored, RandomSource random, ModelData modelData) {
            return this.blockModel.getRenderTypes(this.state, random, portItemData(modelData));
        }

        @Override
        public List<RenderType> getRenderTypes(ItemStack stack, boolean fabulous) {
            return List.of(this.itemRenderType);
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        @Override
        public boolean useAmbientOcclusion() {
            return this.blockModel.useAmbientOcclusion();
        }

        @Override
        public boolean isGui3d() {
            return this.blockModel.isGui3d();
        }

        @Override
        public boolean usesBlockLight() {
            return this.blockModel.usesBlockLight();
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon() {
            return this.blockModel.getParticleIcon();
        }

        @Override
        public ItemTransforms getTransforms() {
            return this.blockModel.getTransforms();
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }
}
