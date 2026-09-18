package dev.anvilcraft.gtouming.doge_plus.client.gui.tooltip;

import dev.anvilcraft.gtouming.doge_plus.block.InlayCarrierBlock;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.block.entity.TranscendiumInlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.util.InlayInspection;
import dev.dubhe.anvilcraft.api.tooltip.providers.ITooltipProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 头戴铁砧锤时的镶嵌载体 HUD：聚焦显示视线所指那个面的镶嵌属性。
 *
 * <p>前置的方块实体通道只传入方块实体，拿不到瞄准面；这里自行读取客户端准星命中面，
 * 再交给 {@link InlayInspection} 生成文本，以优先级 0 覆盖前置的兜底 provider。</p>
 *
 * <p>普通载体按命中的<b>模型部件</b>判定面（{@link InlayCarrierBlock#pickFace}），
 * 避免从侧面看到通道时误判成侧面的镶孔；超限载体仍按方块面。</p>
 */
public class CarrierInspectionProvider extends ITooltipProvider.BlockEntityTooltipProvider {

    /** 部件射线检测长度（与交互侧一致）。 */
    private static final double RAY_LENGTH = 6.0;

    @Override
    public boolean accepts(BlockEntity value) {
        return value instanceof InlayCarrierBlockEntity || value instanceof TranscendiumInlayCarrierBlockEntity;
    }

    @Override
    public List<Component> tooltip(BlockEntity value) {
        BlockPos pos = value.getBlockPos();
        BlockHitResult hit = blockHit(pos);
        if (value.getLevel() == null) return List.of();
        if (value instanceof InlayCarrierBlockEntity carrier) {
            Direction focus = hit == null ? null : normalFocus(value, hit);
            return InlayInspection.carrierTooltip(value.getLevel(), pos, value.getBlockState(), focus, carrier);
        }
        Direction focus = hit == null ? null : hit.getDirection();
        TranscendiumInlayCarrierBlockEntity carrier = (TranscendiumInlayCarrierBlockEntity) value;
        return InlayInspection.transcendiumTooltip(carrier.getBlockState(), carrier.getInlays(), focus);
    }

    @Override
    public int priority() {
        return 0;
    }

    /** 准星命中的方块面；仅当命中的正是该方块实体所在方块时返回，否则退回展示全部面。 */
    @Nullable
    private static BlockHitResult blockHit(BlockPos pos) {
        HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult blockHit && blockHit.getBlockPos().equals(pos)) {
            return blockHit;
        }
        return null;
    }

    /** 普通载体：沿玩家视线按模型部件判定面；仅命中中心体时回退到方块面。 */
    private static Direction normalFocus(BlockEntity value, BlockHitResult hit) {
        LocalPlayer player = Minecraft.getInstance().player;
        Level level = value.getLevel();
        if (player == null || level == null) return hit.getDirection();
        Vec3 from = player.getEyePosition(1.0F);
        Vec3 to = from.add(player.getViewVector(1.0F).scale(RAY_LENGTH));
        Direction picked = InlayCarrierBlock.pickFace(value.getBlockState(), value.getBlockPos(), from, to);
        return picked != null ? picked : hit.getDirection();
    }
}
