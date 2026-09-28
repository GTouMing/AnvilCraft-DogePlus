package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 管道载体：没有任何镶孔，六个面的物品搬运角色（存入 / 取出）由玩法即时编程。
 *
 * <p>与 {@link LogicCarrierBlock} 平行：那边编程红石门类型、走逻辑门网络，这边编程物品搬运角色、
 * 走 {@link dev.anvilcraft.gtouming.doge_plus.transfer.ItemTransferNetworkManager} 的物品传输网。
 * 两者共用轮廓、通道与棱角的几何，但棱角规则相反——管道在<b>两个邻面都没有编程</b>时才画棱
 * （把没开通的边封上壳）。</p>
 *
 * <p>放置时六个面都未编程：既不进也不出，只显示外壳。</p>
 */
public class PipeCarrierBlock extends InlayCarrierBlock {

    public static final MapCodec<PipeCarrierBlock> CODEC = simpleCodec(PipeCarrierBlock::new);

    public PipeCarrierBlock(Properties properties) {
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

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return super.newBlockEntity(pos, state);
    }
}
