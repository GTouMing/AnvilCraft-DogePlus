package dev.anvilcraft.gtouming.doge_plus.event;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import dev.anvilcraft.gtouming.doge_plus.transfer.ItemTransferNetworkManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecartContainer;
import net.minecraft.world.entity.vehicle.ChestBoat;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * 会移动的实体容器（箱子 / 漏斗矿车、箱船）与物品传输网的联动。
 *
 * <p>网络只在拓扑重建 / 记录重算时登记端点，而实体容器是自己会走动的：停到物流面前、或者被推走，
 * 都不会产生方块更新。这里补上这个触发，让「矿车开到位」也能被登记、开走也能被摘掉。</p>
 */
@EventBusSubscriber(modid = AnvilCraftDogePlus.MOD_ID)
public class EntityEventListener {

    /** 该实体是不是「会移动的物品容器」：箱子 / 漏斗矿车、箱船。 */
    private static boolean isMovingContainer(Entity entity) {
        return entity instanceof AbstractMinecartContainer || entity instanceof ChestBoat;
    }

    /** 放下 / 加载进来时通知一次：它可能正好停在某个物流面旁边。 */
    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!isMovingContainer(entity)) return;
        ItemTransferNetworkManager.containerChanged(event.getLevel(), entity.blockPosition());
    }

    /**
     * 跨格时通知：矿车开进 / 开出物流面前要即时生效。
     *
     * <p>不做任何世界查询——每 tick 的代价只是一次 instanceof 与一次方块坐标比较；
     * {@code xo/yo/zo} 是上一 tick 的位置，只有真的换了格才继续往下走。</p>
     */
    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity entity = event.getEntity();
        if (!isMovingContainer(entity)) return;
        BlockPos current = entity.blockPosition();
        BlockPos previous = BlockPos.containing(entity.xo, entity.yo, entity.zo);
        if (current.equals(previous)) return;
        ItemTransferNetworkManager.containerChanged(entity.level(), previous);
        ItemTransferNetworkManager.containerChanged(entity.level(), current);
    }
}
