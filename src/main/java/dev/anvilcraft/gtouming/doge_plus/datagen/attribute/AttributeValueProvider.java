package dev.anvilcraft.gtouming.doge_plus.datagen.attribute;

import com.google.gson.JsonObject;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 生成 {@code data/<ns>/attributes/*.json}：与文件同名的等级属性的每级数值。
 *
 * <p>等级有规律时用函数表达（{@code {"function": "2 * level"}}，变量为 {@code level}）；无规律时才写显式表。
 * defense/life/attack 每级 +2，effect 等级 n → 强度 n-1。</p>
 */
public class AttributeValueProvider implements DataProvider {

    private final PackOutput output;

    public AttributeValueProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return CompletableFuture.allOf(
                write(cache, "defense", "2 * level"),
                write(cache, "life", "2 * level"),
                write(cache, "attack", "2 * level"),
                write(cache, "effect", "level - 1")
        );
    }

    private CompletableFuture<?> write(CachedOutput cache, String name, String function) {
        JsonObject root = new JsonObject();
        root.addProperty("function", function);

        Path path = output.getOutputFolder()
                .resolve("data/" + AnvilCraftDogePlus.MOD_ID + "/attributes/" + name + ".json");
        return DataProvider.saveStable(cache, root, path);
    }

    @Override
    public String getName() {
        return "Doge+ Attribute Values";
    }
}
