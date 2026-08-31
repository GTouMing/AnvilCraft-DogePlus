---
navigation:
  title: "机制"
  icon: "anvilcraft_doge_plus:inlay_table"
categories:
  - mechanics
items:
  - anvilcraft_doge_plus:inlay_table
  - anvilcraft_doge_plus:doge_anvil
  - anvilcraft_doge_plus:giant_doge_anvil
  - anvilcraft:crab_claw
  - anvilcraft:deflection_ring
  - anvilcraft:acceleration_ring
  - anvilcraft:sapphire_block
  - anvilcraft:ruby_block
  - anvilcraft:topaz_block
  - anvilcraft:multiphase_matter_block
  - anvilcraft:magnet_ingot
  - anvilcraft:ember_metal_ingot
  - anvilcraft:frost_metal_ingot
  - anvilcraft:transcendium_ingot
  - anvilcraft:royal_steel_ingot
  - anvilcraft:cursed_gold_ingot
  - anvilcraft:multiphase_matter
  - anvilcraft:supercapacitor
---

# 机制

AnvilCraft: Doge+ 的核心是 **镶嵌系统**，一种把材料嵌入物品以赋予特殊属性的全新合成方式。本页将完整讲解该系统。

## 镶嵌系统

### 运作方式

<ref item="anvilcraft_doge_plus:inlay_table"/> 有两个输入与两个输出：

- **基底材料槽** —— 待镶嵌的物品。其拥有多少个 **镶孔** 决定能镶嵌多少个 **镶嵌材料** 。
- **镶嵌材料槽** —— 要嵌入的材料。其定义将会赋予 **基底材料** 的 **属性**。
- **产物槽** —— 镶嵌完成的物品。
- **旧镶嵌材料槽** —— 基底材料的镶孔已满时再次镶嵌，被替换下来的旧镶嵌材料。

执行一次镶嵌：

1. 右键台面依次放入 **基底材料** 和 **镶嵌材料**（或将物品丢到台上，掉落物会被吸入镶嵌台；或使用溜槽自动化）。
2. 让 **铁砧砸击台面**（或使用铁砧锤敲击台面）。
3. 铁砧把镶嵌材料锤进基底材料，消耗 **1 材料 + 1 基底材料**，产出 **1 件镶嵌物品**。
4. 空手右键镶嵌台侧面取出产物或旧镶嵌材料，右键上面取走全部物品。

### 填充与替换

- 基底材料 **还有空镶孔** 时，每次砸击追加一次镶嵌。
- 基底材料 **已满** 时，下一次镶嵌会 **替换铁砧下落高度对应的镶孔内的镶嵌材料**，旧镶嵌材料被弹出到旧镶嵌材料槽。
- 若被替换的材料带有 **附魔** 属性，其附魔会提取并返还到旧镶嵌材料上。

### 取下镶嵌

基底材料在位且 **材料槽为空** 时，砸击铁砧会 **取下** 一个镶嵌：

- 铁砧的 **下落距离** 决定取出镶嵌的槽位，(0,1]对应 1 槽，(1,2]对应 2 槽，以此类推。
- 被取下的材料进入旧镶嵌材料槽，基底材料失去一个镶嵌材料。

### 数据驱动设计

整个系统完全数据驱动，整合包作者可自由扩展：

- `data/<namespace>/material/base/*.json` —— 基底材料定义（含镶孔数）。
- `data/<namespace>/material/inlay/*.json` —— 材料定义（含赋予的属性）。
- `data/<namespace>/recipe/inlay/*.json` —— 将材料与基底材料绑定的配方。


### 基底材料与镶孔

**基底材料** 是定义在 `data/<namespace>/material/base/*.json` 中的物品，其 `sockets` 值决定可镶嵌的材料个数。

## 镶嵌材料与属性

**镶嵌材料** 是定义在 `data/<namespace>/material/inlay/*.json` 中的物品，其 `attributes` 值决定具有的属性。

### 物品属性

影响携带该属性的物品（以及方块，见下文）：

| 属性     | 来源材料                                        | 效果                         |
|--------|---------------------------------------------|----------------------------|
| **耐火** | 下界合金锭                                       | 不会被烧毁。                     |
| **磁性** | <ref item="anvilcraft:magnet_ingot"/>       | 会被磁铁吸附。                    |
| **高温** | <ref item="anvilcraft:ember_metal_ingot"/>  | 在熔岩或火中越久，累加伤害越高；攻击时消耗累加伤害。 |
| **冷锻** | <ref item="anvilcraft:frost_metal_ingot"/>  | 在水中或细雪中缓慢回复耐久。             |
| **永恒** | <ref item="anvilcraft:transcendium_ingot"/> | 无法破坏，免疫火焰、爆炸、仙人掌、时间与虚空。    |
| **涅槃** | 图腾                                          | 死亡时触发图腾，然后该镶嵌材料碎裂。         |
| **防御** | 下界合金锭                                       | 手持或装备时提升 2 点盔甲值。           |
| **生命** | <ref item="anvilcraft:royal_steel_ingot"/>  | 手持或装备时提升 2 点生命上限。          |
| **攻击** | <ref item="anvilcraft:cursed_gold_ingot"/>  | 手持或装备时提升 2 点攻击力。           |
| **附魔** | 附魔书 / 书                                     | 镶嵌时合并附魔，移除时提取附魔。           |
| **效果** | 药水                                          | 手持或装备时提供药水效果。              |

### 逻辑门属性

这些属性把镶嵌后的方块变成逻辑门：

| 属性     | 来源材料                                       | 效果                   |
|--------|--------------------------------------------|----------------------|
| **方向** | <ref item="anvilcraft:multiphase_matter"/> | 使镶孔具有方向性，镶孔数为 6 时生效。 |
| **输出** | 红石                                         | 该面输出红石信号。            |
| **输入** | 侦测器                                        | 该面输入红石信号。            |
| **非门** | 红石火把                                       | 输出对面信号的相反信号。         |
| **与门** | 中继器                                        | 按顺序对相邻输入做与运算。        |
| **或门** | 比较器                                        | 按顺序对相邻输入做或运算。        |
| **发电** | <ref item="anvilcraft:supercapacitor"/>    | 放置后产生 512 kW 电力。     |

### 共鸣

**共鸣** 属性本身不生效，它 **增强同一基底材料上的其他镶嵌**：

| 被增强的属性 | 共鸣效果                                   |
|--------|----------------------------------------|
| 防御     | 手持或装备时提升 4 点盔甲值。                       |
| 生命     | 手持或装备时提升 4 点生命上限。                      |
| 攻击     | 手持或装备时提升 4 点攻击力。                       |
| 附魔     | 镶嵌时合并附魔并有 50% 概率提升 1 级，移除时 50% 概率提取附魔。 |
| 涅槃     | 死亡时触发图腾，然后该材料 50% 的概率碎裂。               |
| 高温     | 在熔岩或火中越久，累加的伤害越高；攻击时缓慢消耗累加的伤害。         |
| 冷锻     | 在水中或细雪中较快回复耐久（仅耐久物品生效）。                |

## 方块级镶嵌

镶嵌 **不会因方块放置而丢失**。镶嵌过的方块物品放置成方块后保留其属性：

- **磁性** 方块吸引附近物品。
- **耐火** 方块不可燃。
- **永恒** 方块防爆且不可挖掘。
- **高温** 方块灼烧踩踏的生物。
- 破坏方块时掉落的物品保留全部镶嵌。

## 逻辑门方块

具有 **方向性**、**输入**、**输出**和各种**门** 属性的方块。

### 方向性镶孔

当基底材料具有 **6个镶孔** 且具有 **方向性** 属性时，方块的每个面都对应一个镶孔。输入与输出按面解析：

- **输入** 该面输入红石信号。
- **输出** 该面输出所有输入面的最大信号。
- **非门** 读取输入面信号并输出相反信号。
- **与门** 需要至少两个输入，输出其中最小值。
- **或门** 输出输入中的最大值。

::: tip
可以制作出很小的计算机也说不定？。
:::

## Doge 砧成长

<ref item="anvilcraft_doge_plus:doge_anvil"/> 拥有自己的成长机制：

- 右键喂食 **生肉**（每块默认 +1 成长值）。
- 达到上限（默认 128）后原地长成 <ref item="anvilcraft_doge_plus:giant_doge_anvil"/>，一个 3×3×3 多方块结构，继承了巨型铁砧的所有行为。

两个数值均可在服务端配置中调整。
