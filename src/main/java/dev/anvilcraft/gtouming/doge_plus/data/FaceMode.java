package dev.anvilcraft.gtouming.doge_plus.data;

import com.mojang.serialization.Codec;
import dev.anvilcraft.gtouming.doge_plus.client.gui.wheel.WheelOption;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 载体单个面的方向性镶嵌属性：这个面「做什么」。
 *
 * <p>把原先平行的 {@code LogicGateType}（红石端口）与 {@code TransferMode}（物品搬运角色）合并成一个枚举：
 * 两者本来就是同一件事的两个取值域——都挂在某个 {@link Direction} 上、都以 {@link #NONE} 表示未编程、
 * 都能由镶嵌材料（{@code InlayProperty}）或轮盘编程写入。</p>
 *
 * <p>取值域按 {@link Kind} 分为红石 / 搬运两类且互斥：逻辑载体只收红石类，物流载体只收搬运类，
 * 由 {@code InlayCarrierBlock#programFace} 按方块类型校验。</p>
 */
public enum FaceMode implements StringRepresentable, WheelOption {

    /** 无：该面未编程 / 不参与任何网络。 */
    NONE(Kind.NONE, "wheel.anvilcraft_doge_plus.gate.none"),

    /** 非门：输入 > 0 ? 0 : 15 */
    NOT_GATE(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.not_gate"),

    /** 与门：min(输入1, 输入2) */
    AND_GATE(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.and_gate"),

    /** 输出：最大输入值 */
    OUTPUT(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.output"),

    /** 输入：来自红石信号或逻辑门 */
    INPUT(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.input"),

    /** 计数门：累计输入脉冲次数，达到设定次数时输出 1 tick 的 15 并清零。 */
    COUNTER_GATE(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.counter_gate"),

    /** 锁存门：收到输入则记录并输出该信号，再收到则清除记录、停止输出。 */
    LATCH_GATE(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.latch_gate"),

    /** 延时门：收到输入起输出该信号，持续设定 tick 数后停止。 */
    DELAY_GATE(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.delay_gate"),

    /** 延时输入门：记录本面收到的输入信号，等待设定 tick 数后作为该面的输入向本方块的门送出 1 tick。 */
    DELAY_INPUT_GATE(Kind.REDSTONE, "wheel.anvilcraft_doge_plus.gate.delay_input_gate"),

    /** 远程门：同信道编号的远程面互连为一条总线；红石侧为双向导线，物品侧提供远程查找取出货源。 */
    REMOTE(Kind.REMOTE, "wheel.anvilcraft_doge_plus.gate.remote"),

    /** 存入：把物品存入面朝的容器。 */
    INSERT(Kind.TRANSFER, "wheel.anvilcraft_doge_plus.transfer.insert"),

    /** 取出：从面朝的容器取出物品。 */
    EXTRACT(Kind.TRANSFER, "wheel.anvilcraft_doge_plus.transfer.extract");

    /** 取值域分类：决定该值归红石网还是物品传输网。 */
    public enum Kind {
        NONE,
        REDSTONE,
        TRANSFER,
        /** 远程：既服务红石（总线导线）也服务物品（远程取货），两端轮盘都列出。 */
        REMOTE
    }

    public static final Codec<FaceMode> CODEC = StringRepresentable.fromEnum(FaceMode::values);

    public static final StreamCodec<ByteBuf, FaceMode> STREAM_CODEC = ByteBufCodecs.fromCodec(CODEC);

    /** 逻辑载体的轮盘选项：{@link #NONE} + 各红石门（顺序与枚举声明一致，与拆分前相同）。 */
    private static final List<FaceMode> REDSTONE_VALUES =
            Arrays.stream(values()).filter(mode -> !mode.isTransfer()).toList();

    /** 物流载体的轮盘选项：{@link #NONE} + 存入 / 取出（顺序与拆分前相同）。 */
    private static final List<FaceMode> TRANSFER_VALUES =
            Arrays.stream(values()).filter(mode -> !mode.isRedstone()).toList();

    private final Kind kind;
    private final String labelKey;

    FaceMode(Kind kind, String labelKey) {
        this.kind = kind;
        this.labelKey = labelKey;
    }

    public Kind kind() {
        return this.kind;
    }

    /** 是否是红石端口（逻辑门）。 */
    public boolean isRedstone() {
        return this.kind == Kind.REDSTONE;
    }

    /** 是否是物品搬运角色。 */
    public boolean isTransfer() {
        return this.kind == Kind.TRANSFER;
    }

    /** 是否是远程门（既服务红石总线也服务物品远程取货）。 */
    public boolean isRemote() {
        return this.kind == Kind.REMOTE;
    }

    /** 逻辑载体轮盘的选项清单。 */
    public static List<FaceMode> redstoneValues() {
        return REDSTONE_VALUES;
    }

    /** 物流载体轮盘的选项清单。 */
    public static List<FaceMode> transferValues() {
        return TRANSFER_VALUES;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase();
    }

    @Override
    public String labelKey() {
        return this.labelKey;
    }

    /** 各面在轮盘里的代表物品；与 {@code datagen/material/InlayMaterialData} 的绑定保持一致。 */
    @Override
    public ItemStack icon() {
        return new ItemStack(switch (this) {
            case NONE -> Items.BARRIER;
            case NOT_GATE -> Items.REDSTONE_TORCH;
            case AND_GATE -> Items.REPEATER;
            case OUTPUT -> Items.REDSTONE;
            case INPUT -> ModBlocks.REDSTONE_WIRE.get().asItem();
            case COUNTER_GATE -> Items.STONE_BUTTON;
            case LATCH_GATE -> Items.LEVER;
            case DELAY_GATE -> Items.OAK_PRESSURE_PLATE;
            case DELAY_INPUT_GATE -> Items.CLOCK;
            case REMOTE -> Items.ENDER_PEARL;
            case INSERT -> ModBlocks.CHUTE.asItem();
            case EXTRACT -> ModBlocks.MAGNETIC_CHUTE.asItem();
        });
    }

    /** 该门是否有内部时序状态：输出由每 tick 驱动写入，而非输入面的纯函数。 */
    public boolean isStateful() {
        return this == COUNTER_GATE || this == LATCH_GATE || this == DELAY_GATE || this == DELAY_INPUT_GATE;
    }

    /** 该门是否消耗输入面（与门 / 输出门 / 各状态门）。 */
    public boolean consumesInputFace() {
        return this == AND_GATE || this == OUTPUT || isStateful();
    }

    /** 该门的设定值是否可由玩家调整（输入门 / 输出门 / 各状态门）。 */
    public boolean isSettable() {
        return this == INPUT || this == OUTPUT || isStateful();
    }

    /**
     * 该面「设定值」在玩家没调过时的缺省值。
     *
     * <p>锁存门的设定值存的就是它当前锁存到的信号（见 {@code LevelNetworks#arriveLatch}），没锁存过自然是 0；
     * 其余门（输入 / 输出的截断值、计数阈值、延时 tick）缺省都按 15 起步。非红石类不使用设定值。</p>
     */
    public int defaultValue() {
        return this == LATCH_GATE ? 0 : 15;
    }

    /**
     * 计算输出。
     *
     * @param inputs 输入面输入信号（仅含标记为 {@link #INPUT} 的方向）
     * @param value  该面逻辑门的设定值（仅 {@link #OUTPUT} 使用：输出取 max 输入与设定值的较小者）
     */
    public int calculate(Map<Direction, Integer> inputs, int value) {
        return switch (this) {
            // 搬运角色与无、以及有状态门（输出由 LevelNetworks 每 tick 驱动写入）都不经本方法。
            case NONE, INSERT, EXTRACT, INPUT, COUNTER_GATE, LATCH_GATE, DELAY_GATE, DELAY_INPUT_GATE, REMOTE -> 0;
            case NOT_GATE -> {
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    if (face.getValue() > 0) yield 0;
                }
                yield 15;
            }
            case AND_GATE -> {
                // 与运算需至少两个输入面，否则不满足条件，永远返回 0
                if (inputs.size() < 2) yield 0;
                int min = 15;
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    min = Math.min(min, face.getValue());
                }
                yield min;
            }
            case OUTPUT -> {
                // 输出门：取最大输入，并按设定值取小截断
                if (inputs.isEmpty()) yield 0;
                int max = 0;
                for (Map.Entry<Direction, Integer> face : inputs.entrySet()) {
                    max = Math.max(max, face.getValue());
                }
                yield Math.min(max, value);
            }
        };
    }
}
