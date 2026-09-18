package dev.anvilcraft.gtouming.doge_plus.datagen.blockstate;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.CarrierWire;
import dev.anvilcraft.lib.v2.registrum.providers.DataGenContext;
import dev.anvilcraft.lib.v2.registrum.providers.RegistrumBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.MultiPartBlockStateBuilder;

/**
 * 镶嵌载体方块状态生成器（仿前置 {@code RedstoneWireBlockStateGenerator}）。
 *
 * <p>以 multipart 声明「哪个状态用哪个模型（含朝向旋转）」：中心体（{@code hub}）+ 6 面通道
 * （每面按 {@link CarrierWire} 的 {@code unpowered / powered} 选普通或 powered 模型，并按面朝向旋转）。
 * 棱角因需要逻辑门分类而由 {@code InlayCarrierRenderer} 绘制，不在此处生成。</p>
 */
public final class InlayCarrierBlockStateGenerator {

    private InlayCarrierBlockStateGenerator() {
    }

    public static <T extends Block> void generate(
            DataGenContext<Block, T> context, RegistrumBlockstateProvider provider) {
        MultiPartBlockStateBuilder multipart = provider.getMultipartBuilder(context.get());

        multipart.part()
                .modelFile(existing(provider, "inlay_carrier_core"))
                .addModel()
                .condition(InlayCarrierBlock.HUB, true)
                .end();

        for (Direction face : Direction.values()) {
            multipart.part()
                    .modelFile(existing(provider, "inlay_carrier_wire"))
                    .rotationX(rotX(face))
                    .rotationY(rotY(face))
                    .addModel()
                    .condition(InlayCarrierBlock.property(face), CarrierWire.UNPOWERED)
                    .end();
            multipart.part()
                    .modelFile(existing(provider, "inlay_carrier_wire_powered"))
                    .rotationX(rotX(face))
                    .rotationY(rotY(face))
                    .addModel()
                    .condition(InlayCarrierBlock.property(face), CarrierWire.POWERED)
                    .end();
        }
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
