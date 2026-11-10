package dev.anvilcraft.gtouming.doge_plus.data;

/**
 * 载体某个面在「快捷编辑」和「门链」里的方向角色。
 *
 * <p>由面属性（{@link FaceMode}）按载体变体解释而来：同一个 {@code INPUT} 在逻辑载体上是输入侧，
 * 在物流载体上则不是合法值；{@code EXTRACT} 反之。</p>
 */
public enum FaceRole {
    EMPTY,
    INPUT,
    OUTPUT,
    OTHER
}
