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

<recipe id="anvilcraft_doge_plus:inlay/totems_crab_claw"/>

### 运作方式

<ref item="anvilcraft_doge_plus:inlay_table"/> 有两个槽位，**基底材料槽** 与 **镶嵌材料槽**：

- **基底材料槽**接受： 待镶嵌的物品，具有**镶孔**。
- **镶嵌材料槽**接受： 要嵌入的材料，具有**属性**。

两个槽的物品符合配方时，就可以进行**镶嵌**。

### 填充与替换

- 基底材料**未镶满**时，每次镶嵌都会追加。
- 基底材料**已镶满**时，下一次镶嵌会**替换铁砧下落高度对应的镶孔内的镶嵌材料**，旧镶嵌材料掉落至台面下方。

### 取下镶嵌

**基底材料存在且材料槽为空**时，会尝试取下一个镶嵌：

- 铁砧的**下落高度**决定取出镶嵌的槽位，(0,1]对应第1槽，(1,2]对应第2槽，以此类推。
- 被取下的镶嵌材料与基底材料一同掉落至台面下方。

### 属性

| 属性       | 来源材料                                    | 效果                                                 |
|------------|---------------------------------------------|------------------------------------------------------|
| **耐火**   | 下界合金锭                                  | 不会被烧毁。                                         |
| **磁性**   | <ref item="anvilcraft:magnet_ingot"/>       | 具有引力，激活时具有斥力。                           |
| **高温**   | <ref item="anvilcraft:ember_metal_ingot"/>  | 在熔岩或火中越久，累加伤害越高；攻击时消耗累加伤害。 |
| **冷锻**   | <ref item="anvilcraft:frost_metal_ingot"/>  | 在水中或细雪中缓慢回复耐久。                         |
| **永恒**   | <ref item="anvilcraft:transcendium_ingot"/> | 无法破坏，免疫火焰、爆炸、仙人掌、时间与虚空。       |
| **涅槃**   | 图腾                                        | 死亡时触发图腾，然后该镶嵌材料碎裂。                 |
| **防御**   | 下界合金锭                                  | 手持或装备时提升 2 点盔甲值。                        |
| **生命**   | <ref item="anvilcraft:royal_steel_ingot"/>  | 手持或装备时提升 2 点生命上限。                      |
| **攻击**   | <ref item="anvilcraft:cursed_gold_ingot"/>  | 手持或装备时提升 2 点攻击力。                        |
| **附魔**   | 附魔书 / 书                                 | 镶嵌时合并附魔，移除时提取附魔。                     |
| **效果**   | 药水                                        | 手持、装备或放置时提供药水效果。                     |
| **输出**   | 红石                                        | 该面输出红石信号。                                   |
| **输入**   | <ref item="anvilcraft:redstone_wire"/>      | 该面输入红石信号。                                   |
| **非门**   | 红石火把                                    | 输出对面信号的相反信号。                             |
| **与门**   | 中继器                                      | 按顺序对相邻输入做与运算。                           |
| **计数门** | 按钮                                        | 累计输入脉冲，达到设定次数时输出 1 tick 的信号。     |
| **锁存门** | 拉杆                                        | 收到输入则记录并输出，再收到则清除。                 |
| **延时门** | 压力板                                      | 收到输入起输出该信号，持续设定 tick 数。             |
| **延时输入门** | 时钟                                    | 记录输入信号，等待设定 tick 数后，作为该面的输入向本方块其它门送出 1 tick。 |
| **远程门** | <ref item="minecraft:ender_pearl"/>         | 信道相同的远程面互连：红石侧转发输入，物品侧远程取货。 |
| **存入**   | <ref item="anvilcraft:chute"/>              | 将物品存入面朝的容器                                 |
| **取出**   | <ref item="anvilcraft:magnetic_chute"/>     | 从面朝的容器取出物品                                 |
| **发电**   | <ref item="anvilcraft:supercapacitor"/>     | 放置后产生 512 kW 电力。                             |

### 共鸣

**共鸣**属性本身不生效，它会增强其他镶嵌属性：

| 被增强的属性 | 共鸣效果                                                        |
|--------------|-----------------------------------------------------------------|
| 防御         | 手持或装备时提升 4 点盔甲值。                                   |
| 生命         | 手持或装备时提升 4 点生命上限。                                 |
| 攻击         | 手持或装备时提升 4 点攻击力。                                   |
| 附魔         | 镶嵌时合并附魔并有 50% 概率提升 1 级，移除时 50% 概率提取附魔。 |
| 涅槃         | 死亡时触发图腾，然后该材料 50% 的概率碎裂。                     |
| 高温         | 在熔岩或火中越久，累加的伤害越高；攻击时缓慢消耗累加的伤害。    |
| 冷锻         | 在水中或细雪中较快回复耐久（仅耐久物品生效）。                  |

## 镶合

<recipe id="anvilcraft_doge_plus:inlay_crafting/magnet_ingot_from_hollow_magnet_block_4"/>

**镶合** 是镶嵌系统的拓展：在 [镶合台](block/inlay_crafting_table.md) 上，将带有指定镶嵌的物品/方块合成新的物品/方块。。

### 三类镶合

镶合配方分成三类：

| 类别         | `kind`     | 来源                 |
|--------------|------------|----------------------|
| **镶嵌合成** | `crafting` | 本 mod 添加          |
| **镶嵌锻造** | `smithing` | 原版以及本体锻造配方 |
| **镶嵌复制** | `copying`  | 本体珠宝复制配方     |


**静默镶嵌**：部分物品没有属性也可参与镶嵌，这些物品不会在镶嵌配方中写明，而是通过**镶合配方**定义。


### 镶合配方

模组内置/派生的镶合配方：

- **原版锻造**：下界合金升级模板镶入下界合金锭 + 钻石装备 → 对应下界合金装备。
- **本体锻造**：2/4/8 合一锻造模板与各种升级模板，遵循本体配方。
- **本体的部分珠宝复制**：以被复制物为模具，产物带 1 级消失诅咒。
- **原版盔甲纹饰**：18 种纹饰模板镶入「可纹饰装备 + 纹饰材料」。
- **空心磁铁块**：镶入 n 个铁锭 → n 个磁铁锭（n = 1..4）。
- **约束仓**：镶入 1 个充能中子锭 → 1 个约束中子锭。

**超限合金**：附属特别添加了超限合金的镶合配方，由 <ref item="anvilcraft:overheated_ember_metal_block"/> 镶嵌 <ref item="anvilcraft:charged_neutronium_ingot"/> 并镶合而成：

| 附魔条数 | 产物                                                  |
|----------|-------------------------------------------------------|
| 0        | 4 超限合金锭                                          |
| 1–10     | 4 超限合金锭 + 3n 超限合金粒 +（10n% 的概率）1 中子锭 |
| 11–14    | 4 超限合金锭 + 3n 超限合金粒 + 1 中子锭               |
| 15       | 1 中子锭 + 1 超限合金块                               |
| ≥16      | n 超限合金粒 + 1 中子锭 + 1 超限合金块                |

## 方块级镶嵌

镶嵌 **不会因方块放置而丢失**。镶嵌过的方块物品放置成方块后保留其属性：

- **磁性** 方块能够吸引下方铁砧，激活时排斥铁砧，表现为下落高度增加。
- **耐火** 方块不可燃。
- **永恒** 方块防爆且不可挖掘。
- **效果** 方块会向踩踏的生物提供药水效果。
- 以绝大部分方式破坏方块时掉落的物品保留全部镶嵌。



## 载体

**镶嵌载体**：靠镶孔获得面属性，用于镶合**逻辑载体**和**物流载体**。

<ref item="anvilcraft_doge_plus:logic_carrier"/> ：

- 能够编程各面逻辑门类型，手持铁砧锤长按右键打开轮盘。
- 准星指向门所在面并 **Ctrl + 滚轮** 调节设定值。
- 未编程的面既不接收红石输入，也不输出红石信号。

<ref item="anvilcraft_doge_plus:logistics_carrier"/> ：

- 能够编程各面物流模式，手持铁砧锤长按右键打开轮盘。
- 准星指向**存入**端物流并 **Ctrl + 滚轮** 调节**物流量**，副手物品主手铁砧锤可以右键物流设置**过滤**。
- **物流量**：单次运输物品数量；**过滤**：仅允许指定的物品通过，支持过滤器。
- 物流载体可以相连形成传输链，**存入**端会记录全部**取出**端，无论传输链多长，物品都是端到端7gt冷却。
- **存入 / 取出**的目标既可以是方块容器，也可以是 [Doge 节点](../entity/doge_node.md)这类实体容器。
