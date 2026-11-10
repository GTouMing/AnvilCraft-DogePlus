package dev.anvilcraft.gtouming.doge_plus.data;

/**
 * 载体变体标识：普通镶嵌载体、逻辑载体、物流载体。
 *
 * <p>只用于身份比较（快捷编辑同类型匹配、门链族、网络归属）。变体行为与面属性取值域
 * 由 {@code InlayCarrierBlock} 及其子类覆写，不再集中在这里。</p>
 */
public enum CarrierKind {
    INLAY,
    LOGIC,
    LOGISTICS
}
