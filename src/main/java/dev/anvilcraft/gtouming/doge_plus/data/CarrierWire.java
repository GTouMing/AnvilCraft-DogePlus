package dev.anvilcraft.gtouming.doge_plus.data;

import net.minecraft.util.StringRepresentable;

/**
 * 镶嵌载体单面的通道状态（方块状态属性，3 值）。
 *
 * <p>只描述静态几何 + 通电外观：决定 multipart 里该面通道用普通还是 powered 模型。
 * 逻辑门的「输入门收到信号 / 消耗输入面的门」等分类属于客户端棱角渲染信息，见 {@link CarrierFace}；
 * 把棱角需要的信息排除在方块状态之外，是状态数从 6^6 降到 3^6 的关键。</p>
 */
public enum CarrierWire implements StringRepresentable {
    NONE("none"),
    UNPOWERED("unpowered"),
    POWERED("powered");

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
        return this == POWERED;
    }

    /** 由「是否已镶嵌」「当前是否有信号」推导。 */
    public static CarrierWire of(boolean inlaid, boolean powered) {
        if (!inlaid) return NONE;
        return powered ? POWERED : UNPOWERED;
    }
}
