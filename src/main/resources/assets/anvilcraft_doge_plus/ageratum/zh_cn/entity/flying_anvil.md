---
navigation:
  title: "飞行铁砧"
categories:
  - items
---

# 飞行铁砧

由 [Doge 磁铁](../item/doge_magnet.md) 发射的铁砧。伤害由服务端配置决定（`baseDamage` + 目标每个标记 `perMark`），飞行速度可配置，`flyLifetime` 个 tick 后强制消失。

## 相关配置

| 选项            | 默认值 | 含义                 |
|---------------|-----|--------------------|
| `baseDamage`  | 10  | 铁砧飞行命中实体时的基础伤害。    |
| `perMark`     | 2   | 目标每有一个标记的额外伤害。     |
| `anvilSpeed`  | 2.5 | 铁砧飞行速度。            |
| `markRange`   | 64  | 磁铁锭标记目标的视线射线范围（格）。 |
| `flyLifetime` | 400 | 铁砧飞行的硬性超时（tick）。   |
