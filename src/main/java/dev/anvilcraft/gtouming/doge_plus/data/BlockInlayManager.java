package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.network.BlockInlaySyncPacket;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.*;

/**
 * 方块级镶嵌数据管理器：记录「该坐标的方块镶嵌了什么材料」。
 *
 * <p>方块没有逐实例数据（BlockState 是共享单例），因此带镶嵌的方块物品放置成方块后，
 * 其镶嵌属性必须用外部映射记录。本类按维度存一份 {@link SavedData}：
 * 持久化到世界存档，跨区块重载与重启存活。</p>
 *
 * <p>键为 {@link BlockPos}，值为镶嵌材料 ID 列表（与物品 {@code INLAY} 组件一致，
 * 支持多个性质叠加，如「磁性 + 永恒」）。所有方法在客户端（无维度数据）静默无效。</p>
 */
public class BlockInlayManager extends SavedData {

    private static final String DATA_NAME = "doge_plus_inlaid_blocks";

    private final Map<BlockPos, BlockInlays> INLAID_BLOCKS = new HashMap<>();

    /** 获取指定维度的管理数据；客户端或不可持久化维度返回 null。 */
    @Nullable
    public static BlockInlayManager get(BlockGetter level) {
        if (level instanceof ServerLevel server) {
            return server.getDataStorage().computeIfAbsent(
                    new SavedData.Factory<>(BlockInlayManager::new, BlockInlayManager::load),
                    DATA_NAME);
        }
        return null;
    }

    /** 该坐标是否携带指定镶嵌性质（任一镶嵌材料带该性质即视为携带）。 */
    public static boolean hasProperty(BlockGetter level, BlockPos pos, InlayProperty property) {
        BlockInlays bi = get(level, pos);
        for (InlayEntry entry : bi.inlays()) {
            for (ResourceLocation propertyId : entry.attributes()) {
                if (property.id() == propertyId) return true;

            }
        }
        return false;
    }

    public static BlockInlays get(BlockGetter level, BlockPos pos) {

        // 客户端：从客户端缓存获取
        if (level instanceof Level lv && lv.isClientSide()) {
            return ClientBlockInlayData.get(pos);
        }

        // 服务端：从管理器获取
        BlockInlayManager manager = get(level);
        return manager == null ? BlockInlays.nulls() : manager.INLAID_BLOCKS.getOrDefault(pos, BlockInlays.nulls());
    }

    /** 记录坐标的镶嵌材料列表（覆盖旧值），并广播到客户端。 */
    public static void put(BlockGetter level, BlockPos pos, BlockInlays inlays) {
        BlockInlayManager manager = get(level);
        if (manager == null) return;
        manager.INLAID_BLOCKS.put(pos.immutable(), inlays);
        manager.setDirty();
        syncToClients((Level) level, pos, manager.INLAID_BLOCKS.get(pos));
    }

    /** 清除坐标的镶嵌记录（幂等：无记录时无操作），并广播到客户端。 */
    public static void remove(BlockGetter level, BlockPos pos) {
        BlockInlayManager manager = get(level);
        if (manager == null) return;
        if (manager.INLAID_BLOCKS.remove(pos) != null ) {
            manager.setDirty();
            syncToClients((Level) level, pos, BlockInlays.nulls());
        }
    }

    /** 向跟踪该方块所在区块的玩家广播同步包（空列表表示移除）。 */
    private static void syncToClients(Level level, BlockPos pos, BlockInlays inlays) {
        if (!(level instanceof ServerLevel server)) return;
        PacketDistributor.sendToPlayersTrackingChunk(server, server.getChunkAt(pos).getPos(),
                new BlockInlaySyncPacket(pos, inlays));
    }

    /** 把区块内所有镶嵌记录同步给指定玩家（用于玩家开始跟踪区块时补发历史数据）。 */
    public static void syncChunkToPlayer(ServerLevel level, ChunkPos chunkPos, ServerPlayer player) {
        BlockInlayManager manager = get(level);
        if (manager == null) return;
        for (Map.Entry<BlockPos, BlockInlays> entry : manager.INLAID_BLOCKS.entrySet()) {
            if (chunkPos.equals(new ChunkPos(entry.getKey()))) {
                PacketDistributor.sendToPlayer(player, new BlockInlaySyncPacket(entry.getKey(), entry.getValue()));
            }
        }
    }

    // ==================== 方块移动（活塞/滑轨）迁移 ====================

    /** 同时移动的方块暂存队列（FIFO，防御性限长防止移动失败时无限累积）。 */
    private static final ArrayDeque<BlockInlays> PENDING_MOVES = new ArrayDeque<>();

    /**
     * 移动开始时：从旧位置取出镶嵌数据暂存
     */
    public static void stashInlayForMove(Level level, BlockPos pos) {
        try {
            if (level.isClientSide()) return;
            BlockInlayManager manager = Objects.requireNonNull(get(level));

            BlockInlays inlays = Objects.requireNonNull(manager.INLAID_BLOCKS.remove(pos));

            if (PENDING_MOVES.size() >= 64) PENDING_MOVES.poll();
            PENDING_MOVES.add(inlays);

            syncToClients(level, pos, inlays);

        } catch (Exception ignored) {}
    }

    /**
     * 移动完成时：从暂存队列恢复镶嵌数据到新位置
     */
    public static void restoreInlayForMove(Level level, BlockPos pos, Block block) {
        if (level.isClientSide()) return;
        BlockInlayManager manager = get(level);
        if (manager == null) return;

        for (Iterator<BlockInlays> it = PENDING_MOVES.iterator(); it.hasNext(); ) {
            BlockInlays pending = it.next();
            if (pending.block() == block) {
                it.remove();
                manager.INLAID_BLOCKS.put(pos.immutable(), pending);
                manager.setDirty();

                syncToClients(level, pos, pending);
                return;
            }
        }
    }

// ==================== 持久化 ====================

    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();

        for (Map.Entry<BlockPos, BlockInlays> entry : INLAID_BLOCKS.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            BlockInlays inlays = entry.getValue();

            // 位置
            entryTag.putLong("P", pos.asLong());

            // 方块 ID
            entryTag.putString("B", BuiltInRegistries.BLOCK.getKey(inlays.block()).toString());

            // ===== 保存 InlayEntry 列表 =====
            ListTag inlayList = new ListTag();
            for (InlayEntry inlayEntry : inlays.inlays()) {
                CompoundTag inlayTag = new CompoundTag();

                // 保存 id
                inlayTag.putString("id", inlayEntry.id().toString());

                // extra 为带类型的数据（药水效果 / 附魔条数……），按 InlayExtra 编解码写成复合标签
                ListTag extraList = new ListTag();
                for (InlayExtra extra : inlayEntry.extra()) {
                    InlayExtra.CODEC.encodeStart(NbtOps.INSTANCE, extra)
                            .result().ifPresent(extraList::add);
                }
                inlayTag.put("extra", extraList);

                ListTag attributesList = new ListTag();
                for (ResourceLocation attribute : inlayEntry.attributes()) {
                    attributesList.add(StringTag.valueOf(attribute.toString()));
                }
                inlayTag.put("attributes", attributesList);

                inlayList.add(inlayTag);
            }
            entryTag.put("I", inlayList);

            // 保存各面方向性属性（门种 / 搬运角色统一在一张表里）
            ListTag faceList = new ListTag();
            for (Map.Entry<Direction, FaceMode> faceEntry : inlays.faces().entrySet()) {
                CompoundTag faceTag = new CompoundTag();
                faceTag.putString("dir", faceEntry.getKey().getName());
                faceTag.putString("mode", faceEntry.getValue().name());
                faceList.add(faceTag);
            }
            entryTag.put("M", faceList);

            // 保存各面逻辑门设定值
            ListTag valueList = new ListTag();
            for (Map.Entry<Direction, Integer> valueEntry : inlays.values().entrySet()) {
                CompoundTag valueTag = new CompoundTag();
                valueTag.putString("dir", valueEntry.getKey().getName());
                valueTag.putInt("value", valueEntry.getValue());
                valueList.add(valueTag);
            }
            entryTag.put("V", valueList);

            // 保存各面物流量（管道载体的「存入」面）
            ListTag throughputList = new ListTag();
            for (Map.Entry<Direction, Integer> throughputEntry : inlays.throughputs().entrySet()) {
                CompoundTag throughputTag = new CompoundTag();
                throughputTag.putString("dir", throughputEntry.getKey().getName());
                throughputTag.putInt("value", throughputEntry.getValue());
                throughputList.add(throughputTag);
            }
            entryTag.put("Q", throughputList);

            // 保存各面过滤物品（管道载体的「存入」面）
            ListTag filterList = new ListTag();
            for (Map.Entry<Direction, ItemStack> filterEntry : inlays.filters().entrySet()) {
                CompoundTag filterTag = new CompoundTag();
                filterTag.putString("dir", filterEntry.getKey().getName());
                filterTag.put("item", filterEntry.getValue().save(registries));
                filterList.add(filterTag);
            }
            entryTag.put("F", filterList);

            list.add(entryTag);
        }

        tag.put("InlaidBlocks", list);
        return tag;
    }

    // ==================== 加载 ====================

    public static BlockInlayManager load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockInlayManager data = new BlockInlayManager();
        ListTag list = tag.getList("InlaidBlocks", Tag.TAG_COMPOUND);

        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);

            // 读取位置
            BlockPos pos = BlockPos.of(entryTag.getLong("P"));

            // 读取方块 ID
            String blockId = entryTag.getString("B");
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));

            // ===== 读取 InlayEntry 列表 =====
            List<InlayEntry> inlayEntries = new ArrayList<>();
            ListTag inlayList = entryTag.getList("I", Tag.TAG_COMPOUND);

            for (int j = 0; j < inlayList.size(); j++) {
                CompoundTag inlayTag = inlayList.getCompound(j);

                // 读取 id
                String idStr = inlayTag.getString("id");
                ResourceLocation id = ResourceLocation.parse(idStr);

                // 读取 extra：新格式为复合标签，旧格式（纯药水 id 字符串）照旧兼容
                List<InlayExtra> extra = new ArrayList<>();
                ListTag extraList = inlayTag.getList("extra", Tag.TAG_COMPOUND);
                for (int k = 0; k < extraList.size(); k++) {
                    InlayExtra.CODEC.parse(NbtOps.INSTANCE, extraList.get(k))
                            .result().ifPresent(extra::add);
                }
                if (extraList.isEmpty()) {
                    ListTag legacyExtra = inlayTag.getList("extra", Tag.TAG_STRING);
                    for (int k = 0; k < legacyExtra.size(); k++) {
                        extra.add(new InlayExtra.Potion(ResourceLocation.parse(legacyExtra.getString(k))));
                    }
                }

                List<ResourceLocation> attributes = new ArrayList<>();
                ListTag attributesList = inlayTag.getList("attributes", Tag.TAG_STRING);
                for (int k = 0; k < attributesList.size(); k++) {
                    attributes.add(ResourceLocation.parse(attributesList.getString(k)));
                }

                inlayEntries.add(new InlayEntry(id, extra, attributes));
            }

            // 读取各面方向性属性。
            // 新格式是统一表 "M"；旧数据分别存在 "D"（门种）与 "T"（搬运角色）里，读出来合并即可
            // （两类按方块类型互斥，不会冲突）；更旧的数据连 "D" 都没有，退回从材料推导。
            Map<Direction, FaceMode> faces = new HashMap<>();
            ListTag faceList = entryTag.getList("M", Tag.TAG_COMPOUND);
            if (!faceList.isEmpty()) {
                for (int j = 0; j < faceList.size(); j++) {
                    CompoundTag faceTag = faceList.getCompound(j);
                    Direction dir = Direction.byName(faceTag.getString("dir"));
                    if (dir == null) continue;
                    faces.put(dir, parseFaceMode(faceTag.getString("mode")));
                }
            } else {
                ListTag dirList = entryTag.getList("D", Tag.TAG_COMPOUND);
                if (!dirList.isEmpty()) {
                    for (int j = 0; j < dirList.size(); j++) {
                        CompoundTag dirTag = dirList.getCompound(j);
                        Direction dir = Direction.byName(dirTag.getString("dir"));
                        if (dir == null) continue;
                        faces.put(dir, parseFaceMode(dirTag.getString("type")));
                    }
                } else {
                    faces = buildFacesFromInlays(inlayEntries);
                }
                ListTag transferList = entryTag.getList("T", Tag.TAG_COMPOUND);
                for (int j = 0; j < transferList.size(); j++) {
                    CompoundTag transferTag = transferList.getCompound(j);
                    Direction dir = Direction.byName(transferTag.getString("dir"));
                    if (dir == null) continue;
                    faces.put(dir, parseFaceMode(transferTag.getString("mode")));
                }
            }

            // 读取各面逻辑门设定值（旧数据无此 tag → 全部走默认值）
            Map<Direction, Integer> values = new HashMap<>();
            ListTag valueList = entryTag.getList("V", Tag.TAG_COMPOUND);
            for (int j = 0; j < valueList.size(); j++) {
                CompoundTag valueTag = valueList.getCompound(j);
                Direction dir = Direction.byName(valueTag.getString("dir"));
                if (dir != null) {
                    values.put(dir, valueTag.getInt("value"));
                }
            }

            // 读取各面物流量（旧数据无此 tag → 全部走默认值）
            Map<Direction, Integer> throughputs = new HashMap<>();
            ListTag throughputList = entryTag.getList("Q", Tag.TAG_COMPOUND);
            for (int j = 0; j < throughputList.size(); j++) {
                CompoundTag throughputTag = throughputList.getCompound(j);
                Direction dir = Direction.byName(throughputTag.getString("dir"));
                if (dir != null) {
                    throughputs.put(dir, throughputTag.getInt("value"));
                }
            }

            // 读取各面过滤物品（旧数据无此 tag → 全部无过滤）
            Map<Direction, ItemStack> filters = new HashMap<>();
            ListTag filterList = entryTag.getList("F", Tag.TAG_COMPOUND);
            for (int j = 0; j < filterList.size(); j++) {
                CompoundTag filterTag = filterList.getCompound(j);
                Direction dir = Direction.byName(filterTag.getString("dir"));
                if (dir == null) continue;
                ItemStack filter = ItemStack.parseOptional(registries, filterTag.getCompound("item"));
                if (!filter.isEmpty()) filters.put(dir, filter.copyWithCount(1));
            }

            // 构建 BlockInlays 并存入
            BlockInlays inlays =
                    new BlockInlays(block, inlayEntries, faces, values, throughputs, filters);
            data.INLAID_BLOCKS.put(pos, inlays);
        }

        return data;
    }

    /**
     * 从 InlayEntry 列表构建面属性表：更旧的数据连 "D" 都没有时的退回路径。
     */
    private static Map<Direction, FaceMode> buildFacesFromInlays(List<InlayEntry> inlays) {
        List<Direction> directionOrder = List.of(Direction.values());
        Map<Direction, FaceMode> faces = new HashMap<>();

        for (Direction dir : directionOrder) {
            faces.put(dir, FaceMode.NONE);
        }

        for (int i = 0; i < Math.min(inlays.size(), directionOrder.size()); i++) {
            InlayEntry entry = inlays.get(i);
            // 空镶孔（取出过的槽位）不携带任何方向性属性
            faces.put(directionOrder.get(i), entry.isEmpty() ? FaceMode.NONE : BlockInlays.faceModeOf(entry));
        }

        return faces;
    }

    /** 按名字解析面属性；未知名字（版本回退等）按未编程处理。 */
    private static FaceMode parseFaceMode(String name) {
        if (name == null || name.isEmpty()) return FaceMode.NONE;
        try {
            return FaceMode.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return FaceMode.NONE;
        }
    }
}
