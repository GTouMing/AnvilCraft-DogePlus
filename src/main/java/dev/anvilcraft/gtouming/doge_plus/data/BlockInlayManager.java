package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.MaterialManager;
import dev.anvilcraft.gtouming.doge_plus.network.BlockInlaySyncPacket;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayProperty;
import dev.anvilcraft.gtouming.doge_plus.recipe.inlay.InlayUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        for (ResourceLocation id : bi.inlays()) {
            MaterialManager.InlayMaterial material = InlayUtil.getMaterial(id);
            if (material != null && material.has(property)) return true;
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
        if (manager == null) {
            return BlockInlays.nulls();
        }

        BlockInlays bi = manager.INLAID_BLOCKS.get(pos);
        if (bi == null) {
            return BlockInlays.nulls();
        }

        // 检查方块类型是否匹配，不匹配则清理并返回空
        Block currentBlock = level.getBlockState(pos).getBlock();
        if (bi.block() != currentBlock) {
            remove(level, pos);
            return BlockInlays.nulls();
        }

        return bi;
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
        if (manager.INLAID_BLOCKS.remove(pos) != null) {
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

// 在 BlockInlayManager 中添加

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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, BlockInlays> entry : INLAID_BLOCKS.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            BlockPos pos = entry.getKey();
            BlockInlays inlays = entry.getValue();

            entryTag.putLong("P", pos.asLong());
            entryTag.putString("B", BuiltInRegistries.BLOCK.getKey(inlays.block()).toString());

            ListTag ids = new ListTag();
            for (ResourceLocation id : inlays.inlays()) {
                ids.add(StringTag.valueOf(id.toString()));
            }
            entryTag.put("I", ids);
            list.add(entryTag);
        }
        tag.put("InlaidBlocks", list);
        return tag;
    }

    public static BlockInlayManager load(CompoundTag tag, HolderLookup.Provider registries) {
        BlockInlayManager data = new BlockInlayManager();
        ListTag list = tag.getList("InlaidBlocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entryTag = list.getCompound(i);
            BlockPos pos = BlockPos.of(entryTag.getLong("P"));

            // ⭐ 读取方块 ID
            String blockId = entryTag.getString("B");
            Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockId));

            ListTag ids = entryTag.getList("I", Tag.TAG_STRING);
            List<ResourceLocation> inlayIds = new ArrayList<>(ids.size());
            for (int j = 0; j < ids.size(); j++) {
                inlayIds.add(ResourceLocation.parse(ids.getString(j)));
            }

            data.INLAID_BLOCKS.put(pos, BlockInlays.fromInlays(block, inlayIds));
        }
        return data;
    }
}
