package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.AnvilCraftDogePlus;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 远程门的信道标识：由「物品类型 + 数字」两部分决定（与物流载体的过滤物品同一套设置方式）。
 *
 * <p>两个远程面的信道标识相同（物品类型相同且数字相同）时互连为同一条总线；空物品 + 数字 0
 * 表示「默认信道」——数据包给普通方块镶上远程门时无法设置信道，于是都落在这条默认信道上。</p>
 *
 * @param item   信道标识物品（空栈 = 未指定物品）
 * @param number 信道数字（0-{@link #MAX_NUMBER}）
 */
public record RemoteChannel(ItemStack item, int number) {

    /** 默认信道：空物品 + 0。 */
    public static final RemoteChannel DEFAULT = new RemoteChannel(ItemStack.EMPTY, 0);

    /** 信道数字上限（服务端配置，默认 15）。 */
    public static int maxNumber() {
        return AnvilCraftDogePlus.CONFIG.remoteChannelMax;
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RemoteChannel> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, RemoteChannel::item,
            ByteBufCodecs.VAR_INT, RemoteChannel::number,
            RemoteChannel::new);

    /** 规范化：物品只留 1 个（信道只认物品类型），数字裁剪到合法范围。 */
    public RemoteChannel normalized() {
        ItemStack stack = this.item.isEmpty() ? ItemStack.EMPTY : this.item.copyWithCount(1);
        return new RemoteChannel(stack, Math.clamp(this.number, 0, maxNumber()));
    }

    /** 是否是默认信道（空物品 + 0）：无需写入存储。 */
    public boolean isDefault() {
        return this.item.isEmpty() && this.number == 0;
    }

    /**
     * 信道索引键：物品类型（注册序号，空物品取 -1）高位 + 数字低位。
     *
     * <p>只用物品类型、不比较组件，故玩家用同一种物品即可归入同一信道；区分信道靠数字。</p>
     */
    public long key() {
        long itemId = this.item.isEmpty() ? -1L : BuiltInRegistries.ITEM.getId(this.item.getItem());
        return (itemId << 32) | (this.number & 0xFFFFFFFFL);
    }

    /** 与本信道是否互通（物品类型 + 数字相同）。 */
    public boolean connectsTo(RemoteChannel other) {
        return this.key() == other.key();
    }

    /** 显示用：物品名 + 数字（未指定物品时只显示数字）。 */
    public String describe() {
        return this.item.isEmpty() ? String.valueOf(this.number) : this.item.getHoverName().getString() + " " + this.number;
    }
}
