package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 物流载体：没有任何镶孔，六个面的物品搬运角色（存入 / 取出）由玩法即时编程。
 *
 * <p>与 {@link LogicCarrierBlock} 平行：那边编程红石门类型、走逻辑门网络，这边编程物品搬运角色、
 * 走 {@link dev.anvilcraft.gtouming.doge_plus.transfer.ItemTransferNetworkManager} 的物品传输网。
 * 两者共用轮廓、通道与棱角的几何，但棱角规则相反——物流在<b>两个邻面都没有编程</b>时才画棱
 * （把没开通的边封上壳）。</p>
 *
 * <p>放置时六个面都未编程：既不进也不出，只显示外壳。</p>
 */
public class LogisticsCarrierBlock extends InlayCarrierBlock {

    public static final MapCodec<LogisticsCarrierBlock> CODEC = simpleCodec(LogisticsCarrierBlock::new);

    public LogisticsCarrierBlock(Properties properties) {
        super(properties);
    }

    /**
     * 没有镶孔：不接受用镶嵌材料写入面属性，面属性只能通过轮盘 / 门链编程。
     *
     * <p>这项限制同时让材料镶嵌路径上的配方校验自动拒绝本方块（它没有
     * {@code material/base} 定义），无需在交互事件里额外判断。</p>
     */
    @Override
    public boolean acceptsInlays() {
        return false;
    }

    @Override
    protected MapCodec<? extends InlayCarrierBlock> codec() {
        return CODEC;
    }

    /**
     * 相邻方块也是镶嵌载体、且本面与它朝向本方的面都开通了通道时，两侧通道对接：剔除本面通道端盖，
     * 免得两层端盖在方块交界处重叠闪烁。只要有一侧未开通通道，端盖就保留。
     *
     * <p>通道模型本就给端盖标了 {@code cullface}，只是载体的形状不是满方，默认遮挡判定不会把它当实心，
     * 所以相邻两个载体之间从不触发。相邻的是普通方块时交给默认判定。</p>
     */
    @Override
    public boolean skipRendering(BlockState state, BlockState adjacentBlockState, Direction side) {
        return adjacentBlockState.getBlock() instanceof InlayCarrierBlock
                && state.getValue(property(side)).isInlaid()
                && adjacentBlockState.getValue(property(side.getOpposite())).isInlaid();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return super.newBlockEntity(pos, state);
    }
}
