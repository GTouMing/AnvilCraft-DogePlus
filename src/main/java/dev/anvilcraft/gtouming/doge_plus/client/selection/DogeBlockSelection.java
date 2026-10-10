package dev.anvilcraft.gtouming.doge_plus.client.selection;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.mixin.ModelBakeryAccessor;
import dev.anvilcraft.lib.v2.cube.client.CubeSelection;
import dev.anvilcraft.lib.v2.cube.client.OutlineRenderer;
import dev.anvilcraft.lib.v2.cube.client.SelectionPart;
import dev.anvilcraft.lib.v2.cube.geometry.ConvexShape;
import dev.anvilcraft.lib.v2.cube.geometry.PackedOutline;
import dev.anvilcraft.lib.v2.cube.geometry.SelectionGeometry;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.MultiVariant;
import net.minecraft.client.renderer.block.model.Variant;
import net.minecraft.client.renderer.block.model.multipart.MultiPart;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.model.geometry.UnbakedGeometryHelper;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本模组方块的「选中描边」：直接按<b>模型几何</b>画轮廓，而不是碰撞箱（{@code getShape}）的轴对齐盒子，
 * 因此 45° 斜面也贴合模型。做法与前置 {@code ModelBlockSelection} 一致。
 *
 * <p>在 {@code ModelEvent.BakingCompleted} 通过 {@link ModelBakeryAccessor} 取出未烘焙模型，
 * 逐方块状态解码成 anvillib 的凸体（见 {@link DogeModelGeometry}），再在
 * {@code RenderHighlightEvent.Block} 里用 {@code OutlineRenderer} 画出来并 cancel 原版描边。
 * 凸体可任意旋转，所以 45°/22.5° 都是真斜面；轮廓由 anvillib 在后台生成并集线框。</p>
 *
 * <p>多方块（如巨型 Doge 砧）整套模型只挂在承载部件上，其余部件是空模型。这里按前置
 * {@code multipartOutline} 的约定，把承载格的模型<b>平移到每个部件状态</b>后存储——于是任意部件状态
 * 都能直接在自己的位置上画出整套轮廓。放置预览（{@link #outline}）也复用同一份数据。</p>
 *
 * <p>载体系列的方块状态是 multipart 拼出来的（状态数上千、几何由大量薄件拼合），按模型几何描边既庞大
 * 又杂乱，因此整族列进 {@link DogeSelectionDenylist}，落回原版碰撞箱轮廓。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID, value = Dist.CLIENT)
public final class DogeBlockSelection {
    private static Map<BlockState, List<SelectionPart>> selections = Map.of();

    private DogeBlockSelection() {
    }

    /** 该状态对应的整套轮廓（部件状态也已平移到自身位置）；没有几何时返回空表。 */
    public static List<SelectionPart> outline(BlockState state) {
        List<SelectionPart> parts = selections.get(state);
        return parts == null ? List.of() : parts;
    }

    public static void bake(ModelEvent.BakingCompleted event) {
        DogeSelectionDenylist.reload(Minecraft.getInstance().getResourceManager());
        ModelBakeryAccessor bakery = (ModelBakeryAccessor) event.getModelBakery();
        Map<ModelResourceLocation, UnbakedModel> topLevel = bakery.getTopLevelModels();
        Map<ResourceLocation, UnbakedModel> cache = bakery.getUnbakedCache();
        Map<BlockState, List<SelectionPart>> baked = new IdentityHashMap<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (!AnvilCraftDogePlus.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) continue;
            if (DogeSelectionDenylist.disablesOutline(block)) continue;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                // 整套模型只在承载部件：模型照承载状态取，再按「本部件 → 承载格」的位移记在各部件状态上。
                BlockState source = IMultiPartBlockModelHolder.modelHolderState(state);
                UnbakedModel model = topLevel.get(BlockModelShaper.stateToModelLocation(source));
                if (model == null) continue;
                Matrix4f shift = shift(block, state, source);
                List<SelectionPart> parts;
                try {
                    parts = resolve(model, cache, source, 0, shift);
                } catch (IllegalArgumentException exception) {
                    parts = List.of();
                }
                if (!parts.isEmpty()) baked.put(state, parts);
            }
        }
        selections = Map.copyOf(baked);
        AnvilCraftDogePlus.LOGGER.info("Prepared doge cube selection: {} states", baked.size());
    }

    /** 「本部件 → 承载格」的平移；非多方块、或本身即承载格、或位移为零时返回 {@code null}。 */
    private static @Nullable Matrix4f shift(Block block, BlockState state, BlockState holder) {
        if (!(block instanceof AbstractMultiPartBlock<?> multipart) || holder == state) return null;
        Vec3i offset = holderOffset(multipart, state, holder);
        if (offset.equals(Vec3i.ZERO)) return null;
        return new Matrix4f().translation(offset.getX(), offset.getY(), offset.getZ());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Vec3i holderOffset(AbstractMultiPartBlock block, BlockState state, BlockState holder) {
        return block.offsetFrom(state, (Enum) holder.getValue((Property) block.getPart()));
    }

    private static List<SelectionPart> resolve(
        UnbakedModel model, Map<ResourceLocation, UnbakedModel> cache, BlockState state, int depth,
        @Nullable Matrix4f shift
    ) {
        if (depth > 8) return List.of();
        if (model instanceof MultiVariant multi) {
            for (Variant variant : multi.getVariants()) {
                List<SelectionPart> parts = variant(variant, cache, state, depth, shift);
                if (!parts.isEmpty()) return parts;
            }
            return List.of();
        }
        if (model instanceof MultiPart multipart) {
            List<SelectionPart> parts = new ArrayList<>();
            for (Selector selector : multipart.getSelectors()) {
                if (!selector.getPredicate(state.getBlock().getStateDefinition()).test(state)) continue;
                parts.addAll(resolve(selector.getVariant(), cache, state, depth + 1, shift));
            }
            return parts;
        }
        if (model instanceof BlockModel blockModel) return blockModel(blockModel, BlockModelRotation.X0_Y0, shift);
        return List.of();
    }

    private static List<SelectionPart> variant(
        Variant variant, Map<ResourceLocation, UnbakedModel> cache, BlockState state, int depth, @Nullable Matrix4f shift
    ) {
        UnbakedModel model = cache.get(variant.getModelLocation());
        if (model == null) return List.of();
        if (!(model instanceof BlockModel blockModel)) return resolve(model, cache, state, depth + 1, shift);
        return blockModel(blockModel, variant, shift);
    }

    private static List<SelectionPart> blockModel(BlockModel model, ModelState state, @Nullable Matrix4f shift) {
        if (model.customData.hasCustomGeometry()) return List.of();
        List<ConvexShape> shapes;
        try {
            shapes = DogeModelGeometry.decode(model.getElements());
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
        if (shapes.isEmpty()) return List.of();
        ModelState transformed = model.customData.getRootTransform().isIdentity() ? state
            : UnbakedGeometryHelper.composeRootTransformIntoModelState(state, model.customData.getRootTransform());
        Matrix4f matrix = new Matrix4f().translation(0.5F, 0.5F, 0.5F)
            .mul(transformed.getRotation().getMatrix()).translate(-0.5F, -0.5F, -0.5F).scale(1 / DogeModelGeometry.SCALE);
        if (shift != null) matrix = new Matrix4f(shift).mul(matrix);
        try {
            return List.of(new SelectionPart(new SelectionGeometry(shapes), matrix));
        } catch (IllegalArgumentException exception) {
            return List.of();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void highlight(RenderHighlightEvent.Block event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.options.hideGui) return;
        BlockPos pos = event.getTarget().getBlockPos();
        BlockState state = level.getBlockState(pos);
        if (!AnvilCraftDogePlus.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace())) return;
        List<SelectionPart> parts = selections.get(state);
        if (parts == null || parts.isEmpty()) return;
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.lines());
        int segments = 0;
        AABB bounds = null;
        for (SelectionPart part : parts) {
            segments += CubeSelection.outlines().get(part.geometry()).segmentCount();
            bounds = bounds == null ? part.bounds() : bounds.minmax(part.bounds());
        }
        pose.pushPose();
        pose.translate(pos.getX() - camera.x, pos.getY() - camera.y, pos.getZ() - camera.z);
        if (segments > SelectionGeometry.MAX_OUTLINE_SEGMENTS) {
            LevelRenderer.renderLineBox(pose, consumer, bounds, 0.0F, 0.0F, 0.0F, 0.4F);
        } else {
            for (SelectionPart part : parts) {
                pose.pushPose();
                part.apply(pose);
                PackedOutline outline = CubeSelection.outlines().get(part.geometry());
                OutlineRenderer.render(pose, consumer, outline, 0.0F, 0.0F, 0.0F, 0.4F);
                pose.popPose();
            }
        }
        pose.popPose();
        event.setCanceled(true);
    }
}
