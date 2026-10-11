package dev.anvilcraft.gtouming.doge_plus.block.placement;

import dev.anvilcraft.gtouming.doge_plus.init.ModBlocks;
import dev.dubhe.anvilcraft.api.block.IBlockPlacementRule;
import dev.dubhe.anvilcraft.block.multipart.IMultiPartBlockModelHolder;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 巨狗砧的放置规则。
 *
 * <p>巨狗砧是多方块：整套模型与放置都由那一个方块物品承担。但前置默认的放置规则对<b>部件状态</b>
 * 解析不出放置物（{@link #getPlacementItems} 返回空），于是多方块转化配方在 JEI 里没有产物槽、
 * 对着巨狗砧按 R 也就查不到那条配方（前置自家的巨砧同样如此）。</p>
 *
 * <p>用前置的公开 API {@code BlockPlacementRules.registerFallback} 注册一条规则：任何部件状态
 * 都由巨狗砧物品放置。优先级取 1000——fallback 按优先级<b>降序</b>试（默认 0），因此会先于通用回退命中。</p>
 */
public final class GiantDogeAnvilPlacementRule implements IBlockPlacementRule {
    public static final GiantDogeAnvilPlacementRule INSTANCE = new GiantDogeAnvilPlacementRule();

    /** 高于默认的 0 即可排在通用回退之前。 */
    private static final int PRIORITY = 1000;

    private GiantDogeAnvilPlacementRule() {
    }

    @Override
    public boolean matches(BlockState state) {
        return state.is(ModBlocks.GIANT_DOGE_ANVIL.get());
    }

    @Override
    public boolean canCreate(BlockState state) {
        return this.matches(state);
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public List<PlacementItem> getPlacementItems(BlockState state) {
        // 整套结构只由巨狗砧物品放置一次：只在承载部件状态上报，否则 JEI 会把 27 个部件状态
        // 各算一份、按物品合并后变成「27 个巨狗砧」。
        if (IMultiPartBlockModelHolder.modelHolderState(state) != state) return List.of();
        return List.of(new PlacementItem(state.getBlock().asItem(), 1));
    }
}
