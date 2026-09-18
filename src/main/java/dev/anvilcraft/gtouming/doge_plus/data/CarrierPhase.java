package dev.anvilcraft.gtouming.doge_plus.data;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * 超限镶嵌载体的相位：α / β。
 *
 * <p>相位是物品上的数据组件，手持物品按快捷键切换；放置成方块时写入方块状态，
 * 破坏时回写到掉落物。放置后不再切换。</p>
 */
public enum CarrierPhase implements StringRepresentable {
    ALPHA("alpha"),
    BETA("beta");

    public static final Codec<CarrierPhase> CODEC =
            Codec.STRING.xmap(CarrierPhase::byName, CarrierPhase::getSerializedName);

    public static final StreamCodec<ByteBuf, CarrierPhase> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(CarrierPhase::byOrdinal, CarrierPhase::ordinal);

    private final String name;

    CarrierPhase(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** 另一相位。 */
    public CarrierPhase other() {
        return this == ALPHA ? BETA : ALPHA;
    }

    /** 按下标取相位（越界回退 α）。 */
    public static CarrierPhase byOrdinal(int ordinal) {
        return ordinal == BETA.ordinal() ? BETA : ALPHA;
    }

    /** 按名称取相位（未知回退 α）。 */
    public static CarrierPhase byName(String name) {
        return BETA.name.equals(name) ? BETA : ALPHA;
    }
}
