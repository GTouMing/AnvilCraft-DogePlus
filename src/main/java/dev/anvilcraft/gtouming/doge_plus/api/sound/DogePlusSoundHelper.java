package dev.anvilcraft.gtouming.doge_plus.api.sound;

import dev.anvilcraft.gtouming.doge_plus.init.ModItems;
import dev.anvilcraft.gtouming.doge_plus.item.MobileSilencer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 移动式消音器查询助手（无注册表、无状态）。
 *
 * <p>旧实现把穿戴中的 {@link ItemStack} 本身注册成声音监听器：装备槽变动 / 物品
 * 拷贝会让旧引用滞留在监听列表里，甚至出现空 {@link ItemStack}。这里改为每次
 * 收到声音事件时直接查询 {@link Level} 中所有存活玩家的穿戴状态：谁的头部槽或
 * curios 槽戴着消音器、其静音列表包含该声音、且声源在其消音范围内，即判定静音。
 * 查询式设计不保存任何对象引用，天然不存在空栈/过期条目。</p>
 */
public class DogePlusSoundHelper {

    public static final DogePlusSoundHelper INSTANCE = new DogePlusSoundHelper();

    /** 消音范围（立方体半边长，等价原实现 AABB 边长 31 的 ±15.5）。 */
    private static final double RANGE_HALF = 15.5;

    private DogePlusSoundHelper() {
    }

    /**
     * 判断该声源是否应被某个玩家携带的移动式消音器静音。
     *
     * @param level 声源所在世界（客户端为客户端世界，服务端为服务端世界）
     * @param sound 声音 id
     * @param pos   声源位置
     */
    public boolean shouldMute(Level level, ResourceLocation sound, Vec3 pos) {
        if (level == null || sound == null || pos == null) return false;
        for (Player player : level.players()) {
            if (player.isRemoved() || !player.isAlive()) continue;
            if (!withinRange(player, pos)) continue;
            ItemStack silencer = findSilencer(player);
            if (silencer.is(ModItems.MOBILE_SILENCER.get())
                    && MobileSilencer.getMutedSounds(silencer).contains(sound)) {
                return true;
            }
        }
        return false;
    }

    private static boolean withinRange(Player player, Vec3 pos) {
        return Math.abs(pos.x - player.getX()) <= RANGE_HALF
                && Math.abs(pos.y - player.getY()) <= RANGE_HALF
                && Math.abs(pos.z - player.getZ()) <= RANGE_HALF;
    }

    /** 查找玩家身上可用的消音器：头部装备槽优先，其次 curios 槽。 */
    private static ItemStack findSilencer(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        if (head.is(ModItems.MOBILE_SILENCER.get())) return head;
        ItemStack curios = MobileSilencer.findMobileSilencer(player);
        return curios.is(ModItems.MOBILE_SILENCER.get()) ? curios : ItemStack.EMPTY;
    }
}
