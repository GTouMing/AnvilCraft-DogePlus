package dev.anvilcraft.gtouming.doge_plus.datagen.material;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 镶嵌材料/基材定义输出：{@code data/anvilcraft_doge_plus/material/base|inlay/<name>.json}。
 *
 * <p>这些文件由运行时 {@code MaterialManager} 按 {@code data/<ns>/material/} 加载（文件名即键），
 * 不在 Registrum 的 ProviderType 体系内，故自写 {@link DataProvider}。</p>
 */
public class MaterialJsonProvider implements DataProvider {

    private final PackOutput packOutput;
    private final List<BaseMaterialData> baseMaterials;
    private final List<InlayMaterialData> inlayMaterials;

    public MaterialJsonProvider(
            PackOutput packOutput,
            List<BaseMaterialData> baseMaterials,
            List<InlayMaterialData> inlayMaterials) {
        this.packOutput = packOutput;
        this.baseMaterials = baseMaterials;
        this.inlayMaterials = inlayMaterials;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        Path dataPath = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK);
        String dir = AnvilCraftDogePlus.MOD_ID + "/material/";
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (BaseMaterialData base : this.baseMaterials) {
            JsonObject root = new JsonObject();
            root.add("ingredient", base.ingredient());
            root.addProperty("sockets", base.sockets());
            futures.add(DataProvider.saveStable(cache, root, dataPath.resolve(dir + "base/" + base.name() + ".json")));
        }

        for (InlayMaterialData inlay : this.inlayMaterials) {
            JsonObject root = new JsonObject();
            root.add("ingredient", inlay.ingredient());
            JsonArray attributes = new JsonArray();
            for (String attribute : inlay.attributes()) {
                attributes.add(attribute);
            }
            root.add("attributes", attributes);
            futures.add(DataProvider.saveStable(cache, root, dataPath.resolve(dir + "inlay/" + inlay.name() + ".json")));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "DogePlus material definitions (" + AnvilCraftDogePlus.MOD_ID + ")";
    }
}
