package dev.anvilcraft.gtouming.doge_plus.datagen.blockstate;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.gtouming.doge_plus.mixin.SimpleBakedModelRenderTypeMixin;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * 镶嵌载体方块状态生成器（仿前置 {@code RedstoneWireBlockStateGenerator}）。
 *
 * <p>以 multipart 声明「哪个状态用哪个模型（含朝向旋转）」：中心体 + 6 面通道（每面已镶嵌即画，
 * 并按面朝向旋转）+ 物流载体的 12 条棱件（两面都落在 {@link InlayCarrierBlock#edgeStates} 时画；
 * 逻辑 / 普通载体的棱件已移除）。</p>
 *
 * <p>通道与棱件都不再分通电 / 未通电：通电与否由 overlay 那层的颜色表现（见 {@code CarrierColorHandler}），
 * 所以两者各只有一套模型，全部规则都能落在 blockstate 上。</p>
 *
 * <p>中心体有两种取法：普通 / 逻辑载体由 {@code hub} 控制（对穿时隐藏）；物流载体则按「未编程的面」
 * （{@link CarrierWire#NONE}）各画一份——它没有通道的面要把开口堵上，条件与通道正好相反
 * （见 {@link InlayCarrierBlock#showsCoreOnFace}）。</p>
 */
public final class InlayCarrierBlockStateGenerator {

    private InlayCarrierBlockStateGenerator() {
    }

    /**
     * 默认部件模型：镶嵌载体与逻辑载体共用 {@code carrier/logic} 下的这一套，两边只有物品模型不同
     * （分别由各自的方块注册指定）。
     */
    public static <T extends Block> void generate(
            DataGenContext<Block, T> context, RegistrumBlockstateProvider provider) {
        generate(context, provider,
                "carrier/logic/core", "carrier/logic/core_powered",
                "carrier/logic/core_cross", "carrier/logic/core_cross_powered",
                "carrier/logic/wire", "carrier/logic/wire_powered", null,
                "carrier/logic/wire_remote", "carrier/logic/wire_remote_powered",
                "carrier/logic/edge", "carrier/logic/edge_powered",
                "carrier/logic/edge_vertical", "carrier/logic/edge_vertical_powered",
                null);
    }

    /**
     * 按指定的模型名生成方块状态（各载体变体共用同一套几何，只是换部件）。
     *
     * <p>物流载体没有 hub 那一支（中心体恒渲染、见下），也没有通电变体（它的面不参与红石），
     * 相应的模型名传 {@code null}。</p>
     *
     * @param coreCrossModel         hub 为 false 时用的「贯通」中心体；没有这个变体时传 {@code null}
     * @param coreCrossPoweredModel  贯通中心体的通电变体；传 {@code null} 时贯通中心体不分通电与否
     * @param corePoweredModel       任意面通电时用的中心体；传 {@code null} 时中心体不分通电与否
     * @param wirePoweredModel       通电通道模型；传 {@code null} 时通道不分通电与否，都用 {@code wireModel}
     * @param wireGlassModel         通道的半透明面板模型（与 {@code wireModel} 同条件、同朝向一起画）；
     *                               只有面板贴图真的带半透明像素时才拆出来，实心通道传 {@code null}
     * @param wireRemoteModel        远程门面的通道模型（{@code wire_remote}，自带珍珠辉光）
     * @param wireRemotePoweredModel 远程门面的通电变体；与中心体同一条件（任意面通电即换用它）；
     *                               传 {@code null} 时远程面不分通电与否
     * @param edgePoweredModel       通电棱件模型；传 {@code null} 时棱件不分通电与否
     * @param edgeVerticalPoweredModel 通电竖棱件模型，同上
     * @param cornerModel            恒渲染的角件（物流载体的固定框架）；没有这个部件时传 {@code null}
     */
    public static <T extends Block> void generate(
            DataGenContext<Block, T> context,
            RegistrumBlockstateProvider provider,
            String coreModel,
            @Nullable String corePoweredModel,
            @Nullable String coreCrossModel,
            @Nullable String coreCrossPoweredModel,
            String wireModel,
            @Nullable String wirePoweredModel,
            @Nullable String wireGlassModel,
            String wireRemoteModel,
            @Nullable String wireRemotePoweredModel,
            String edgeModel,
            @Nullable String edgePoweredModel,
            String edgeVerticalModel,
            @Nullable String edgeVerticalPoweredModel,
            @Nullable String cornerModel) {
        MultiPartBlockStateBuilder multipart = provider.getMultipartBuilder(context.get());

        // 中心体：普通 / 逻辑载体由 hub 控制（对穿时隐藏）；物流载体的中心体改由「未编程的面」逐面
        // 绘制——它没有通道的面要把开口堵上，条件与通道正好相反，因此不再有 hub 这一支。
        if (InlayCarrierBlock.showsCoreOnFace(context.get())) {
            for (Direction face : Direction.values()) {
                multipart.part()
                        .modelFile(existing(provider, coreModel))
                        .rotationX(rotX(face))
                        .rotationY(rotY(face))
                        .addModel()
                        .condition(InlayCarrierBlock.property(face), CarrierWire.NONE)
                        .end();
            }
        } else {
            multipart.part()
                    .modelFile(existing(provider, coreModel))
                    .addModel()
                    .condition(InlayCarrierBlock.HUB, true)
                    .end();
            // 任意面通电时换上通电中心体。条件只能写成「某面 = POWERED」，所以逐面各来一份：
            // 六份几何与贴图完全相同，同时命中也不会看出差别；通电件比普通件外扩 0.025，
            // 稳定盖住下面那份未通电的中心体，不会闪烁。
            if (corePoweredModel != null) {
                for (Direction face : Direction.values()) {
                    multipart.part()
                            .modelFile(existing(provider, corePoweredModel))
                            .addModel()
                            .condition(InlayCarrierBlock.HUB, true)
                            .condition(InlayCarrierBlock.property(face), CarrierWire.POWERED)
                            .end();
                }
            }
            // 与中心体互斥：hub 为 false 表示恰好一对对面已镶嵌、两条通道直接贯通，中心不能再是空的，
            // 换上十字件把中间那段接起来。十字件的四根棱管要沿贯通轴摆，所以三条轴各来一份——
            // hub 已经保证只有这一对，条件写成「这对面都已镶嵌」即可，三条轴不会同时命中。
            if (coreCrossModel != null) {
                for (Direction face : List.of(Direction.NORTH, Direction.EAST, Direction.UP)) {
                    Direction opposite = face.getOpposite();
                    multipart.part()
                            .modelFile(existing(provider, coreCrossModel))
                            .rotationX(rotX(face))
                            .rotationY(rotY(face))
                            .addModel()
                            .condition(InlayCarrierBlock.HUB, false)
                            .condition(InlayCarrierBlock.property(face),
                                    CarrierWire.UNPOWERED, CarrierWire.POWERED, CarrierWire.REMOTE)
                            .condition(InlayCarrierBlock.property(opposite),
                                    CarrierWire.UNPOWERED, CarrierWire.POWERED, CarrierWire.REMOTE)
                            .end();
                    // 贯通时任意一面通电就换通电十字件。hub 为 false 保证只有这一对已镶嵌，
                    // 未镶嵌的面永远是 NONE，所以「任意面通电」等价于「这一对里有一面通电」，
                    // 两档条件即可覆盖：face 通电，或相反面通电（同时通电两份都命中，模型相同看不出差别）。
                    // 注意「已镶嵌」含 REMOTE：另一面是远程门时也要能命中通电件，否则会只剩普通十字件。
                    if (coreCrossPoweredModel != null) {
                        multipart.part()
                                .modelFile(existing(provider, coreCrossPoweredModel))
                                .rotationX(rotX(face))
                                .rotationY(rotY(face))
                                .addModel()
                                .condition(InlayCarrierBlock.HUB, false)
                                .condition(InlayCarrierBlock.property(face), CarrierWire.POWERED)
                                .condition(InlayCarrierBlock.property(opposite),
                                        CarrierWire.UNPOWERED, CarrierWire.POWERED, CarrierWire.REMOTE)
                                .end();
                        multipart.part()
                                .modelFile(existing(provider, coreCrossPoweredModel))
                                .rotationX(rotX(face))
                                .rotationY(rotY(face))
                                .addModel()
                                .condition(InlayCarrierBlock.HUB, false)
                                .condition(InlayCarrierBlock.property(face),
                                        CarrierWire.UNPOWERED, CarrierWire.REMOTE)
                                .condition(InlayCarrierBlock.property(opposite), CarrierWire.POWERED)
                                .end();
                    }
                }
            }
        }

        // 通道：远程面单独一份（wire_remote）；其余未通电 / 通电各一份模型（颜色已烘进各自的贴图）。
        // 没有通电变体时只用 base 模型，条件并列两个值表示「已镶嵌」——multipart 只能写相等，
        // 这是它表达「非 NONE」的办法。
        for (Direction face : Direction.values()) {
            // 远程面：通道换成自带珍珠辉光的 wire_remote。任意面通电时改用通电变体——与中心体同一条件
            // （远程面自身永远是 REMOTE、不会是 POWERED，所以「任意面通电」等价于「某个其它面 POWERED」）。
            // 两份模型的几何完全一致（powered 只换了贴图），所以不学中心体那套「外扩盖住」，改用互斥条件：
            // 普通件要求其余五面都不是 POWERED（枚举除 POWERED 外的全部取值），通电件逐面要求某个面
            // POWERED——两者不会同时命中，也就不会 z-fighting，模型也不必手工外扩。
            MultiPartBlockStateBuilder.PartBuilder plainRemote = multipart.part()
                    .modelFile(existing(provider, wireRemoteModel))
                    .rotationX(rotX(face))
                    .rotationY(rotY(face))
                    .addModel()
                    .condition(InlayCarrierBlock.property(face), CarrierWire.REMOTE);
            for (Direction other : Direction.values()) {
                if (other == face) continue;
                plainRemote.condition(InlayCarrierBlock.property(other),
                        CarrierWire.NONE, CarrierWire.UNPOWERED, CarrierWire.REMOTE);
            }
            plainRemote.end();
            if (wireRemotePoweredModel != null) {
                for (Direction powered : Direction.values()) {
                    if (powered == face) continue;
                    multipart.part()
                            .modelFile(existing(provider, wireRemotePoweredModel))
                            .rotationX(rotX(face))
                            .rotationY(rotY(face))
                            .addModel()
                            .condition(InlayCarrierBlock.property(face), CarrierWire.REMOTE)
                            .condition(InlayCarrierBlock.property(powered), CarrierWire.POWERED)
                            .end();
                }
            }
            // 通道本身（实心边框）与半透明面板是两个模型、两个渲染层：边框先写深度，面板再混合。
            // Sodium 的 multipart 会对渲染类型并集逐层发射每个子模型的全部 quad；由
            // SimpleBakedModelRenderTypeMixin 按 renderType 过滤后，边框与面板各只出现在自己的层。
            if (wirePoweredModel == null) {
                addWireParts(multipart, provider, face, wireModel, wireGlassModel,
                        CarrierWire.UNPOWERED, CarrierWire.POWERED);
            } else {
                addWireParts(multipart, provider, face, wireModel, wireGlassModel, CarrierWire.UNPOWERED);
                addWireParts(multipart, provider, face, wirePoweredModel, null, CarrierWire.POWERED);
            }
        }

        CarrierWire[] edgeStates = InlayCarrierBlock.edgeStates(context.get());
        // 已镶嵌里「没通电」的那部分（即除掉 POWERED），用来表达通电件的「另一面没通电」。
        CarrierWire[] edgeIdle = Arrays.stream(edgeStates)
                .filter(state -> state != CarrierWire.POWERED)
                .toArray(CarrierWire[]::new);
        for (InlayCarrierBlock.Edge edge : InlayCarrierBlock.edgeShapes(context.get())) {
            String plain = edge.vertical() ? edgeVerticalModel : edgeModel;
            if (edgePoweredModel == null || edgeVerticalPoweredModel == null) {
                addEdge(multipart, provider, edge, plain, edgeStates, edgeStates);
                continue;
            }
            String powered = edge.vertical() ? edgeVerticalPoweredModel : edgePoweredModel;
            // 三份条件互不重叠：都没通电 → 普通；a 通电 → 通电件；a 没通电而 b 通电 → 通电件。
            // 「已镶嵌」按 edgeStates 展开（含 REMOTE），所以远程面参与的棱件也照样画得出来。
            addEdge(multipart, provider, edge, plain, edgeIdle, edgeIdle);
            addEdge(multipart, provider, edge, powered, new CarrierWire[] {CarrierWire.POWERED}, edgeStates);
            addEdge(multipart, provider, edge, powered, edgeIdle, new CarrierWire[] {CarrierWire.POWERED});
        }

        // 角件：物流框架的固定件，任何状态下都画。multipart 不挂 condition 即恒成立。
        if (cornerModel != null) {
            multipart.part()
                    .modelFile(existing(provider, cornerModel))
                    .addModel()
                    .end();
        }
    }

    /**
     * 加通道：统一条件下画实心边框与半透明面板两个模型。
     *
     * <p>两者贴图不同、渲染层也不同（{@code cutout} / {@code translucent}），但几何位置完全重合，
     * 因此朝向与条件必须完全一致；面板传 {@code null} 表示该通道没有半透明部分。</p>
     *
     * <p>Sodium 的 multipart 会对渲染类型并集逐层重复发射子模型。{@link SimpleBakedModelRenderTypeMixin}
     * 让 {@code SimpleBakedModel} 按 {@code renderType} 过滤，避免面板在 cutout 趟被画成不透明。</p>
     */
    private static void addWireParts(
            MultiPartBlockStateBuilder multipart,
            RegistrumBlockstateProvider provider,
            Direction face,
            String frameModel,
            @Nullable String glassModel,
            CarrierWire... states) {
        addWirePart(multipart, provider, face, frameModel, states);
        if (glassModel != null) {
            addWirePart(multipart, provider, face, glassModel, states);
        }
    }

    /**
     * 加一个通道部件模型：按面朝向旋转，条件为「该面落在 {@code states}」。
     *
     * <p>通道使用面专用的动态模型（{@code <model>_<face>}）：模型自己读邻居决定画普通 wire
     * 还是 port 变体，几何仍由这里的旋转负责。</p>
     */
    private static void addWirePart(
            MultiPartBlockStateBuilder multipart,
            RegistrumBlockstateProvider provider,
            Direction face,
            String model,
            CarrierWire... states) {
        multipart.part()
                .modelFile(existing(provider, model + "_" + face.getSerializedName()))
                .rotationX(rotX(face))
                .rotationY(rotY(face))
                .addModel()
                .condition(InlayCarrierBlock.property(face), states)
                .end();
    }

    /** 加一条棱件：条件为「a 面落在 {@code aStates} 且 b 面落在 {@code bStates}」。 */
    private static void addEdge(
            MultiPartBlockStateBuilder multipart,
            RegistrumBlockstateProvider provider,
            InlayCarrierBlock.Edge edge,
            String model,
            CarrierWire[] aStates,
            CarrierWire[] bStates) {
        multipart.part()
                .modelFile(existing(provider, model))
                .rotationX(edge.xRot())
                .rotationY(edge.yRot())
                .addModel()
                .condition(InlayCarrierBlock.property(edge.a()), aStates)
                .condition(InlayCarrierBlock.property(edge.b()), bStates)
                .end();
    }

    /** 引用 hand-written（Blockbench 导出）的既有模型。 */
    private static ModelFile existing(RegistrumBlockstateProvider provider, String path) {
        return new ModelFile.ExistingModelFile(
                ResourceLocation.fromNamespaceAndPath(AnvilCraftDogePlus.MOD_ID, "block/" + path),
                provider.models().existingFileHelper);
    }

    /** 与 multipart 朝向旋转一致：上/下仅用 x，其余仅用 y。 */
    private static int rotX(Direction face) {
        return switch (face) {
            case DOWN -> 90;
            case UP -> 270;
            default -> 0;
        };
    }

    private static int rotY(Direction face) {
        return switch (face) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
    }
}
