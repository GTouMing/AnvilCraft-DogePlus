---
navigation:
  title: "镶嵌与镶合"
  icon: "anvilcraft_doge_plus:inlay_table"
categories:
  - mechanics
---

# 镶嵌与镶合

[镶嵌台](../block/inlay_table.md)添加、替换或取下镶嵌；[镶合台](../block/inlay_crafting_table.md)按镶合配方把基材与材料变成新产物。载体与物流规则见[载体](carriers.md)。

## 镶嵌

<recipe id="anvilcraft_doge_plus:inlay/totems_crab_claw"/>

<ref item="anvilcraft_doge_plus:inlay_table"/>有一个基材槽和一个材料槽。放入有镶孔的基材和符合镶嵌配方的材料，用铁砧砸击台面，产物掉落在台面下方。

- **添加**：优先填最前面的空孔；一次砸击按基材与材料中较少的一方处理，余料留在槽中。

- **替换**：基材镶满后，替换选中的镶嵌并返还旧材料。

- **取下**：材料槽留空再砸击，取下选中的镶嵌。选中空孔不会执行，后面的镶孔也不会前移。

- 替换和取下按砸击时的下落距离选孔：第 1 格选第 1 孔，第 2 格选第 2 孔，依此类推；超出范围选末孔。

只有带镶孔的基材才能镶嵌，能否镶入某材料以显式配方为准。镶嵌载体有 6 孔，Doge 钢块有 5 孔。

### 属性

| 属性 | 来源材料 | 用途 |
|---|---|---|
| 耐火 | 下界合金锭、余烬金属锭 | 物品不被火烧毁，方块不可燃 |
| 磁性 | <ref item="anvilcraft:magnet_ingot"/> | 吸引铁砧，激活时排斥 |
| 高温 | <ref item="anvilcraft:ember_metal_ingot"/> | 在火或熔岩中积累伤害，攻击时消耗 |
| 冷锻 | <ref item="anvilcraft:frost_metal_ingot"/> | 在水或细雪中修复耐久 |
| 永恒 | <ref item="anvilcraft:transcendium_ingot"/> | 物品免疫火焰、爆炸、时间与虚空；方块防爆且不可挖掘 |
| 涅槃 | 不死图腾 | 死亡时触发图腾并消耗该镶嵌 |
| 防御 | 下界合金锭 | 增加护甲 |
| 生命 | <ref item="anvilcraft:royal_steel_ingot"/> | 增加生命上限 |
| 攻击 | <ref item="anvilcraft:cursed_gold_ingot"/> | 增加攻击伤害 |
| 附魔 | 书、附魔书 | 镶嵌时合并附魔，取下时提取其中一条 |
| 效果 | 普通、喷溅、滞留药水 | 手持或装备时提供效果，放置后作用于踩踏者 |
| 共鸣 | 紫水晶碎片 | 增强同一物品的其他属性 |
| 发电 | <ref item="anvilcraft:supercapacitor"/> | 放置后发电，默认 512 kW |

| 面属性 | 来源材料 |
|---|---|
| 输出 | 红石 |
| 输入 | <ref item="anvilcraft:redstone_wire"/> |
| 非门 / 与门 | 红石火把 / 中继器 |
| 计数门 / 锁存门 | 按钮 / 拉杆 |
| 延时门 / 延时输入门 | 压力板 / 时钟 |
| 远程门 | <ref item="minecraft:ender_pearl"/> |
| 存入 / 取出 | <ref item="anvilcraft:chute"/> / <ref item="anvilcraft:magnetic_chute"/> |
| 合成存入 | 工作台 |

信号规则与调节见[载体](carriers.md)。

### 等级与共鸣

只统计主手、副手和四个护甲槽；每件有效镶嵌贡献 1 级，默认每级 +2 点。共鸣只增强所在物品：防御、生命、攻击、效果翻倍；附魔合并或提取、涅槃触发各有 50% 概率获得加成；高温消耗更慢、冷锻修复更快。

### 配方过滤

主手持铁砧锤、副手拿示例物品，右键台子水平侧面设置过滤；过滤只限制收料，不能代替配方。

## 镶合

<recipe id="anvilcraft_doge_plus:inlay_smithing/netherite_sword_upgrade"/>

镶合台接收一个基材和最多 6 槽材料。砸击时先尝试静默镶嵌，再匹配镶合配方；静默镶嵌不要求单独的镶嵌配方。

- 已有镶嵌参与匹配，材料顺序不限，但非空镶嵌的数量必须与配方一致；多镶一个也可能无法合成。

- 材料槽全空时，已镶好且符合配方的基材仍可直接镶合。

- 一次砸击批量处理当前材料能支持的份数，产物掉落在台面下方；无匹配配方时不消耗物品。

典型用法是原版升级：下界合金升级模板镶入下界合金锭与钻石装备，得到下界合金装备。
