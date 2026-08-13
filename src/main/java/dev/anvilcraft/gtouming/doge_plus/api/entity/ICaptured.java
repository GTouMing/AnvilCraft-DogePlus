package dev.anvilcraft.gtouming.doge_plus.api.entity;

/**
 * 被 Doge 节点捕获的物品标记（供客户端渲染绕圈显示）。
 */
public interface ICaptured {
    boolean doge_plus$isCaptured();
    void doge_plus$setCaptured(boolean captured);
    int doge_plus$getIndex();
    void doge_plus$setIndex(int index);
}
