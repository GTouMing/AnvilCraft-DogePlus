package dev.anvilcraft.gtouming.doge_plus.client.gui.wheel;

import dev.anvilcraft.lib.v2.wheel.api.WheelMenuBuilder;
import dev.anvilcraft.lib.v2.wheel.api.WheelMenuModel;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.function.Consumer;

/**
 * 载体面属性轮盘的菜单模型：把一组 {@link WheelOption} 组装成 lib 的 {@link WheelMenuModel}。
 *
 * <p>每个选项一个 action 扇区：图标由 {@link WheelOption#icon()} 绘制，名称走 label，选中时回调
 * {@code onSelect}。轮盘界面本身由前置的 {@code WheelScreen}（hold 手势）提供，本类只负责模型。</p>
 *
 * <p>逻辑载体的逻辑门类型与管道载体的存入 / 取出共用同一套组装逻辑（选项数决定扇区数，
 * {@code slotsPerPage} 取选项总数，因此不分页）。</p>
 */
public final class CarrierWheelModel {

    private CarrierWheelModel() {
    }

    /**
     * @param options  轮盘里的全部选项（顺序即扇区顺序）
     * @param onSelect 选中某项时的回调（发对应的编程包）
     */
    public static <T extends WheelOption> WheelMenuModel build(List<T> options, Consumer<T> onSelect) {
        WheelMenuBuilder builder = WheelMenuBuilder.create().slotsPerPage(options.size());
        for (T option : options) {
            builder.action(
                    option.labelKey(),
                    Component.translatable(option.labelKey()),
                    (graphics, pose, width, height) -> graphics.renderItem(option.icon(), -8, -8),
                    context -> onSelect.accept(option));
        }
        return builder.build();
    }
}
