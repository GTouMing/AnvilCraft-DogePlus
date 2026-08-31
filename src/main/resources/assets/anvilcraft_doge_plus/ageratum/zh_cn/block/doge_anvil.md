---
navigation:
  title: "Doge 砧"
  icon: "anvilcraft_doge_plus:doge_anvil"
categories:
  - items
items:
  - anvilcraft_doge_plus:doge_anvil
---

# Doge 砧

<block id="anvilcraft_doge_plus:doge_anvil"/>

会成长的铁砧：喂食生肉提升成长值，达到上限后长成 [巨型 Doge 砧](giant_doge_anvil.md)。

- 手持 **生肉**（牛肉、猪排、鸡肉、羊肉或兔肉）右键砧。
- 每块生肉增加成长值（默认 `+1`，可在服务端配置中调整）。
- 当成长值达到上限（默认 `128`，可配置）时，砧 **原地长成** 巨型 Doge 砧。
- 右键砧可打开铁砧菜单进行修复与改名。

<recipe id="anvilcraft_doge_plus:crafting_shaped/doge_anvil"/>

::: tip
成长值可在服务端配置 `anvilcraft_doge_plus-server.toml` 中调整（`maxGrowth` 与 `growthPerMeat`）。
:::
