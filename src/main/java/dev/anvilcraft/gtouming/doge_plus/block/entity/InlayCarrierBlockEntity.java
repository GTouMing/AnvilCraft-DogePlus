package dev.anvilcraft.gtouming.doge_plus.block.entity;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.data.BlockInlayManager;
import dev.dubhe.anvilcraft.api.item.IDiskCloneable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 镶嵌载体方块实体。
 *
 * <p>镶嵌仍由 {@code BlockInlayManager} 按坐标管理；这里保存服务端算出的各面红石信号强度
 * 与运行时显示值（计数门的已计数 / 延时门的已计时），同步给客户端供 HUD 显示
 * （无需再回服务端查询）。同时作为连接件渲染器的挂载点。</p>
 */
public class InlayCarrierBlockEntity extends BlockEntity implements IDiskCloneable {

    /** 各面的信号强度，下标同 {@link Direction#ordinal()}。 */
    private final int[] signals = new int[Direction.values().length];

    /** 各面的运行时显示值（计数门的已计数 / 延时门的已计时），下标同上。 */
    private final int[] runtime = new int[Direction.values().length];

    public InlayCarrierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** 该面当前的信号强度；未镶嵌或非逻辑门面为 0。 */
    public int signal(Direction direction) {
        return this.signals[direction.ordinal()];
    }

    /** 更新各面信号强度；有变化时返回 true。 */
    public boolean setSignals(int[] values) {
        boolean changed = false;
        for (int i = 0; i < this.signals.length; i++) {
            if (this.signals[i] != values[i]) {
                this.signals[i] = values[i];
                changed = true;
            }
        }
        if (changed) this.setChanged();
        return changed;
    }

    /** 该面的运行时显示值；非有状态门为 0。 */
    public int runtime(Direction direction) {
        return this.runtime[direction.ordinal()];
    }

    /** 更新各面运行时显示值；有变化时返回 true。 */
    public boolean setRuntime(int[] values) {
        boolean changed = false;
        for (int i = 0; i < this.runtime.length; i++) {
            if (this.runtime[i] != values[i]) {
                this.runtime[i] = values[i];
                changed = true;
            }
        }
        if (changed) this.setChanged();
        return changed;
    }

    // ==================== 磁盘复制 ====================

    /**
     * 把该坐标的镶嵌材料与各面编程（门种 / 设定值 / 物流量 / 过滤 / 信道）写入磁盘数据。
     */
    @Override
    public void storeDiskData(CompoundTag tag) {
        if (this.level == null) return;
        BlockInlayManager.writeInlays(
                tag, BlockInlayManager.get(this.level, this.worldPosition), this.level.registryAccess());
    }

    /**
     * 把磁盘数据整体套用到该坐标，并重建网络与外观。
     *
     * <p>方块字段以本方块为准，来源只提供镶嵌内容与各面编程。兼容性由 {@link #getDiskCompatibleGroups()}
     * 保证（只有同为逻辑载体 / 物流载体 / 普通载体的磁盘才会被套用）。</p>
     */
    @Override
    public void applyDiskData(CompoundTag tag) {
        if (this.level == null) return;
        InlayCarrierBlock.replaceInlays(
                this.level, this.worldPosition,
                BlockInlayManager.readInlays(tag, this.level.registryAccess()));
    }

    /**
     * 磁盘兼容组：按载体方块区分，逻辑 / 物流 / 普通载体的编程互不套用（取值域本就不同）。
     */
    @Override
    public List<String> getDiskCompatibleGroups() {
        return List.of(BuiltInRegistries.BLOCK.getKey(this.getBlockState().getBlock()).toString());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putIntArray("Signals", this.signals);
        tag.putIntArray("Runtime", this.runtime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int[] loadedSignals = tag.getIntArray("Signals");
        for (int i = 0; i < this.signals.length && i < loadedSignals.length; i++) {
            this.signals[i] = loadedSignals[i];
        }
        int[] loadedRuntime = tag.getIntArray("Runtime");
        for (int i = 0; i < this.runtime.length && i < loadedRuntime.length; i++) {
            this.runtime[i] = loadedRuntime[i];
        }
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
}
