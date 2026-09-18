package dev.anvilcraft.gtouming.doge_plus.data;

import dev.anvilcraft.gtouming.doge_plus.logic.LogicGateType;

/**
 * 镶嵌载体单个面的外观分类（客户端渲染用，不再是方块状态属性）。
 *
 * <p>由「是否镶嵌」「门类型」「当前有无信号」三者推导，供方块实体渲染器在普通 / powered
 * 模型之间选择：</p>
 * <ul>
 *   <li>{@link #NONE}：未镶嵌，不绘制通道；</li>
 *   <li>{@link #IDLE}：已镶嵌但该面无信号（普通材料 / 输入门未收到信号 / 非门未输出）；</li>
 *   <li>{@link #SOURCE}：{@link LogicGateType#INPUT} 输入门当前收到信号（通道点亮，表示收到信号）；</li>
 *   <li>{@link #SIGNAL}：非门等「不消耗输入面」的门当前有输出（通道点亮）；</li>
 *   <li>{@link #GATE_OFF}：与门 / 或门 / 输出门当前无输出；</li>
 *   <li>{@link #GATE_ON}：与门 / 或门 / 输出门当前有输出。</li>
 * </ul>
 *
 * <p>区分 {@link #SOURCE}（收到信号）与 {@link #SIGNAL}（有输出）是为了棱角件：只有
 * 「输入门收到信号」与「消耗输入面的门」之间的棱才用 powered 模型表示信号送入逻辑门。</p>
 */
public enum CarrierFace {
    NONE,
    IDLE,
    SOURCE,
    SIGNAL,
    GATE_OFF,
    GATE_ON;

    /** 该面是否是「消耗输入面」的逻辑门（与门 / 或门 / 输出门）。 */
    public boolean consumesInput() {
        return this == GATE_OFF || this == GATE_ON;
    }

    /** 该面通道是否点亮（收到或输出信号）。 */
    public boolean isPowered() {
        return this == SOURCE || this == SIGNAL || this == GATE_ON;
    }

    /**
     * 由镶嵌状态、门类型与信号推导外观状态。
     *
     * @param active  该面是否已镶嵌
     * @param type    该面的门类型
     * @param powered 该面当前是否有信号（输入门为收到信号，其余门为输出信号）
     */
    public static CarrierFace of(boolean active, LogicGateType type, boolean powered) {
        if (!active) return NONE;
        if (type.consumesInputFace()) return powered ? GATE_ON : GATE_OFF;
        if (powered && type == LogicGateType.INPUT) return SOURCE;
        return powered ? SIGNAL : IDLE;
    }
}
