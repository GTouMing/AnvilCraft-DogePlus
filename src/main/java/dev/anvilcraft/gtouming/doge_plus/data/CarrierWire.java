package dev.anvilcraft.gtouming.doge_plus.data;

import net.minecraft.util.StringRepresentable;

/**
 * 镶嵌载体单面的通道状态（方块状态属性，4 值）。
 *
 * <p>只描述静态几何 + 通电外观：决定 multipart 里该面通道用普通 / powered / 远程 模型。
 * 逻辑门的「输入门收到信号 / 消耗输入面的门」等分类属于客户端棱角渲染信息，见 {@link CarrierFace}；
 * 把棱角需要的信息排除在方块状态之外，是状态数从 6^6 降到 5^6 的关键。</p>
 *
 * <p>{@link #REMOTE} / {@link #REMOTE_POWERED} 是远程门专属：该面画自带的「珍珠辉光」通道模型
 * （{@code wire_remote} / {@code wire_remote_powered}）。远程面是否点亮只取决于同信道总线信号，
 * 与其它面的通电状态无关。</p>
 */
public enum CarrierWire implements StringRepresentable {
    NONE("none"),
    UNPOWERED("unpowered"),
    POWERED("powered"),
    /** 远程门面，当前同信道没有信号：用 {@code wire_remote} 模型。 */
    REMOTE("remote"),
    /** 远程门面，当前同信道有信号：用 {@code wire_remote_powered} 模型。 */
    REMOTE_POWERED("remote_powered");

    private final String name;

    CarrierWire(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** 该面是否已镶嵌（决定是否绘制通道与棱角）。 */
    public boolean isInlaid() {
        return this != NONE;
    }

    /** 该面通道是否点亮。 */
    public boolean isPowered() {
        return this == POWERED || this == REMOTE_POWERED;
    }

    /** 该面是否是远程门面（含通电变体）。 */
    public boolean isRemote() {
        return this == REMOTE || this == REMOTE_POWERED;
    }

    /** 由「是否已镶嵌」「当前是否有信号」推导（非远程面）。 */
    public static CarrierWire of(boolean inlaid, boolean powered) {
        return of(inlaid, powered, false);
    }

    /** 由「是否已镶嵌」「当前是否有信号」「是否远程面」推导。 */
    public static CarrierWire of(boolean inlaid, boolean powered, boolean remote) {
        if (!inlaid) return NONE;
        if (remote) return powered ? REMOTE_POWERED : REMOTE;
        return powered ? POWERED : UNPOWERED;
    }
}
