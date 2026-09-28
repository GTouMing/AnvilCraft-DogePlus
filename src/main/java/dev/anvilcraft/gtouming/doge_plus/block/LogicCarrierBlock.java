package dev.anvilcraft.gtouming.doge_plus.block;

import com.mojang.serialization.MapCodec;
import dev.anvilcraft.gtouming.doge_plus.block.entity.InlayCarrierBlockEntity;
import dev.anvilcraft.gtouming.doge_plus.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 逻辑载体：没有任何镶孔，六个面的门类型由玩法即时编程。
 *
 * <p>门类型来自三处：铁砧锤长按右键的轮盘、门链放置流程，以及既有的门设定值调整。
 * 它们都写进 {@link dev.anvilcraft.gtouming.doge_plus.data.BlockInlays} 的 {@code faces}，
 * 因此与普通载体共用同一套逻辑网络与渲染。</p>
 *
 * <p>放置时六个面都未编程，也就是「无外显镶嵌」：既不接收红石输入也不输出红石信号；
 * 某个面一旦被编程即视为外显，可正常收发。</p>
 */
public class LogicCarrierBlock extends InlayCarrierBlock {

    public static final MapCodec<LogicCarrierBlock> CODEC = simpleCodec(LogicCarrierBlock::new);

    public LogicCarrierBlock(Properties properties) {
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
        return new InlayCarrierBlockEntity(ModBlockEntities.LOGIC_CARRIER.get(), pos, state);
    }
}
