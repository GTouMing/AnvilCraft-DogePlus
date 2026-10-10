package dev.anvilcraft.gtouming.doge_plus.client.selection;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.block.Block;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.Set;

/**
 * 不参与模型轮廓的方块名单，做法与前置 {@code ModelSelectionDenylist} 一致：读资源包里的一份
 * {@value #PATH}，方块按 id 在<b>资源重载时</b>解析。
 *
 * <p>不在静态初始化里解析方块（那时方块还没注册），因此文件名、id 都可随资源包覆盖，也便于以后扩充。</p>
 */
public final class DogeSelectionDenylist {
    public static final String PATH = "model_selection_denylist.json";
    public static final ResourceLocation LOCATION = AnvilCraftDogePlus.of(PATH);
    /** 只在资源重载时才会被真正的名单替换。 */
    private static Set<Block> outline = Set.of();

    private DogeSelectionDenylist() {
    }

    public static void reload(ResourceManager resources) {
        Set<Block> loaded = Set.of();
        for (Resource resource : resources.getResourceStack(LOCATION)) {
            try (Reader reader = resource.openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                loaded = readBlocks(json, "disable_model_outline");
            } catch (IOException | RuntimeException exception) {
                AnvilCraftDogePlus.LOGGER.warn(
                    "Unable to load {} from {}; keeping lower-priority denylist", LOCATION, resource.sourcePackId(), exception);
            }
        }
        outline = loaded;
    }

    /** 该方块是否不参与模型轮廓（落回原版碰撞箱轮廓）。 */
    public static boolean disablesOutline(Block block) {
        return outline.contains(block);
    }

    private static Set<Block> readBlocks(JsonObject json, String name) {
        if (!json.has(name)) return Set.of();
        Set<Block> blocks = new HashSet<>();
        for (JsonElement entry : GsonHelper.getAsJsonArray(json, name)) {
            ResourceLocation id = ResourceLocation.parse(GsonHelper.convertToString(entry, name));
            if (!AnvilCraftDogePlus.MOD_ID.equals(id.getNamespace())) {
                throw new IllegalArgumentException("Model selection denylist only supports anvilcraft_doge_plus blocks: " + id);
            }
            blocks.add(BuiltInRegistries.BLOCK.getOptional(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown block in model selection denylist: " + id)));
        }
        return Set.copyOf(blocks);
    }
}
