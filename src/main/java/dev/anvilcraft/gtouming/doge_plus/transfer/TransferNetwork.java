package dev.anvilcraft.gtouming.doge_plus.transfer;

import dev.anvilcraft.gtouming.doge_plus.data.FaceMode;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * 一张物品传输网：由「本方存入面 ↔ 正邻格的取出面」这条有向关系弱连通起来的全部传输节点。
 *
 * <p>网内维护每个节点的「货源记录」（{@link TransferSource}）与需要按冷却搬运的「存入」面清单。
 * 记录是拓扑的派生缓存：只在内存里，拓扑重建时重算。</p>
 */
final class TransferNetwork {

    /** 网内节点：位置 → 该位置的两个面掩码。 */
    final Long2ObjectLinkedOpenHashMap<TransferNode> nodes;

    /** 网络跨越的区块，用于区块卸载时精确失效。 */
    final LongOpenHashSet chunks = new LongOpenHashSet();

    /** 是否有效（拓扑重建时旧网络会被失效）。 */
    boolean valid = true;

    /**
     * 节点位置 → 该节点（的存入面）能到达的全部「取出」货源（顶点图里的出边），先近后远。
     *
     * <p>这是一份<b>按需缓存</b>（照前置流体管网的 reachability 缓存）：真正要搬运时才用一次反向
     * BFS 算出并填进来，空结果也缓存（负缓存）；有效取出集合一变就整体清空重算。
     * 因此没被搬运过的节点不占内存，也不会随取出数量线性膨胀。</p>
     */
    final Long2ObjectOpenHashMap<ObjectArrayList<TransferSource>> sources = new Long2ObjectOpenHashMap<>();

    /**
     * 已记录的有效「取出」集合（顶点图里的取出顶点）。
     *
     * <p>拓扑 / 端点容器变化时拿它与当前有效取出求差，只增删变化的那部分，不必整网重算。</p>
     */
    final ObjectOpenHashSet<TransferSource> extracts = new ObjectOpenHashSet<>();

    /**
     * 需要按冷却搬运的「存入」面：位置与面按索引一一对应。
     *
     * <p>建好记录后一次性算出来，避免每 tick 遍历整张网。</p>
     */
    final LongArrayList servicePositions = new LongArrayList();
    final ByteArrayList serviceFaces = new ByteArrayList();

    TransferNetwork(Long2ObjectLinkedOpenHashMap<TransferNode> nodes) {
        this.nodes = nodes;
    }
}

/**
 * 传输节点：一个方块上六个面里哪些带「存入」、哪些带「取出」，以及参与哪些远程门信道。
 *
 * @param packedPos       位置（{@link BlockPos#asLong()}）
 * @param insertMask      「存入」面掩码，位下标同 {@link Direction#ordinal()}
 * @param extractMask     「取出」面掩码
 * @param remoteChannels  该节点上远程面（{@link FaceMode#REMOTE}）的信道键（去重）
 */
record TransferNode(long packedPos, int insertMask, int extractMask, long[] remoteChannels) {

    boolean hasInsert() {
        return this.insertMask != 0;
    }

    /** 是否参与远程门（有远程面）。 */
    boolean isRemote() {
        return this.remoteChannels.length != 0;
    }

    /** 该面是否是「存入」（物品从这里进入本节点面朝的容器）。 */
    boolean isInsert(Direction face) {
        return (this.insertMask & (1 << face.ordinal())) != 0;
    }

    /** 该面是否是「取出」（物品从本节点面朝的容器被取走）。 */
    boolean isExtract(Direction face) {
        return (this.extractMask & (1 << face.ordinal())) != 0;
    }
}

/**
 * 一个「存入」面的货源记录：从 {@code sourcePos} 那一格的 {@code sourceFace} 面朝着的容器取货。
 *
 * @param sourcePos  「取出」所在的方块位置（{@link BlockPos#asLong()}）
 * @param sourceFace 该「取出」面；它朝着的容器即取货对象
 */
record TransferSource(long sourcePos, Direction sourceFace) {

    /** 取货容器的位置。 */
    BlockPos containerPos() {
        return BlockPos.of(this.sourcePos).relative(this.sourceFace);
    }

    /** 查询容器能力时的朝向：容器朝向货源方块的那一面。 */
    Direction containerSide() {
        return this.sourceFace.getOpposite();
    }
}
