---
navigation:
  title: "镶合台"
  icon: "anvilcraft_doge_plus:inlay_crafting_table"
categories:
  - items
items:
  - anvilcraft_doge_plus:inlay_crafting_table
---

# 镶合台

<block id="anvilcraft_doge_plus:inlay_crafting_table"/>

镶合配方的工作方块：

- 一个基材槽 + 由目标产物过滤/配方决定数量的材料槽（最多 6 个）；每个材料槽只接受对应镶孔要求的材料。
- 顶面手持放入物品（优先并入空槽/同类槽，槽位全满时替换并返还旧内容物），空手右键取出全部。
- 铁砧砸落在台面时，先对基材执行材料槽里的全部**静默镶嵌**，再匹配镶合配方；一次砸击产出，产物掉落在下方，使用漏斗/溜槽自动化收集。

镶合台与[镶嵌台](inlay_table.md)可以相互合成：

<recipe id="anvilcraft_doge_plus:crafting_shapeless/inlay_crafting_table_from_inlay_table"/>

<recipe id="anvilcraft_doge_plus:crafting_shapeless/inlay_table_from_inlay_crafting_table"/>

::: info
完整的镶嵌与镶合机制见 [机制](../mechanics.md)。
:::
