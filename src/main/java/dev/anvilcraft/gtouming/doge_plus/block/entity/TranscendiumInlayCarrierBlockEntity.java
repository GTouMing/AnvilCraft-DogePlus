package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.data.InlayEntry;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 超限镶嵌载体方块实体：自持动态镶孔列表。
 *
 * <p>镶孔数为「已镶嵌数量 + 1」，上限 {@link #MAX_SOCKETS}（= 6 面 × 每面 6×6 像素）。
 * 槽位下标 i 对应面 {@code Direction.values()[i % 6]}，对应像素 {@code i / 6}。</p>
 */
@Getter
public class TranscendiumInlayCarrierBlockEntity extends BlockEntity {

    /** 最大镶孔数：6 面 × 每面 36 像素。 */
    public static final int MAX_SOCKETS = 6 * 36;
    /** 每个面的方向数（循环周期）。 */
    public static final int FACES = 6;

    /**
     * -- GETTER --
     * 当前镶孔内容（紧凑，不含空占位）。
     */
    private final List<InlayEntry> inlays = new ArrayList<>();

    public TranscendiumInlayCarrierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 已镶嵌数量。 */
    public int count() {
        return this.inlays.size();
    }

    /** 动态镶孔数 = 已镶嵌数量 + 1（封顶 {@link #MAX_SOCKETS}）。 */
    public int socketCount() {
        return Math.min(this.inlays.size() + 1, MAX_SOCKETS);
    }

    public boolean isFull() {
        return this.inlays.size() >= MAX_SOCKETS;
    }

    /**
     * 在指定面添加镶嵌：未满时追加到下一个空镶孔；已满时替换该面最后一个镶孔。
     *
     * @return 被替换下来的旧镶嵌（无则 {@link InlayEntry#nulls()}）
     */
    public InlayEntry addOnFace(int faceOrdinal, InlayEntry entry) {
        if (entry.isEmpty()) return InlayEntry.nulls();
        if (!this.isFull()) {
            this.inlays.add(entry);
            this.sync();
            return InlayEntry.nulls();
        }
        int index = this.lastIndexOnFace(faceOrdinal);
        if (index < 0) return InlayEntry.nulls();
        InlayEntry old = this.inlays.set(index, entry);
        this.sync();
        return old;
    }

    /**
     * 移除指定面最后一个镶孔（紧凑删除）。
     *
     * @return 被移除的镶嵌（无则 {@link InlayEntry#nulls()}）
     */
    public InlayEntry removeOnFace(int faceOrdinal) {
        int index = this.lastIndexOnFace(faceOrdinal);
        if (index < 0) return InlayEntry.nulls();
        InlayEntry removed = this.inlays.remove(index);
        this.sync();
        return removed;
    }

    /** 指定面的最后一个镶孔下标（无则 -1）。 */
    private int lastIndexOnFace(int faceOrdinal) {
        for (int i = this.inlays.size() - 1; i >= 0; i--) {
            if (i % FACES == faceOrdinal) return i;
        }
        return -1;
    }

    /** 槽位下标对应的面。 */
    public static Direction faceOf(int slot) {
        return Direction.values()[slot % FACES];
    }

    /** 槽位下标对应的像素序号（每面 0..35）。 */
    public static int pixelOf(int slot) {
        return slot / FACES;
    }

    // ==================== 同步 ====================

    private void sync() {
        this.setChanged();
        if (this.level == null || this.level.isClientSide) return;
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        this.saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // ==================== 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (InlayEntry entry : this.inlays) {
            InlayEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry).result().ifPresent(list::add);
        }
        tag.put("Inlays", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.inlays.clear();
        ListTag list = tag.getList("Inlays", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            InlayEntry.CODEC.parse(NbtOps.INSTANCE, list.getCompound(i)).result().ifPresent(this.inlays::add);
        }
    }
}
