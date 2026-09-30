# AnvilCraft Doge+

![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange?style=flat-square)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

**AnvilCraft: Doge+** 是 [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft) 的附属模组，围绕 **Doge 系列**材料展开，并带来一套全新的**镶嵌系统**。整套系统**数据驱动**，整合包作者可自由扩展。

## 主要特性

- **Doge 材料与工具**：铁 + 骨高温熔炼出的 Doge 钢锭与钢块；喂食生肉成长、最终长成 3×3×3 巨型 Doge 砧的 Doge 砧；可收纳 / 放置 / 发射铁砧（飞行铁砧命中造成伤害）并吸引物品与经验的 Doge 磁铁；吸附并捕获物品（上限 8 个）的 Doge 节点。
- **镶嵌系统**：在镶嵌台上用铁砧把镶嵌材料锤入基底材料的**镶孔**，赋予物品属性——耐火、磁性、高温、冷锻、永恒、涅槃、防御、生命、攻击、附魔、效果、共鸣等。镶嵌不会因方块放置而丢失，破坏仍保留。
- **镶合**：把**镶满的基底材料**放入镶合台压合产出产物，涵盖原版锻造与 18 种盔甲纹饰、空心磁铁块、中子锭等，并支持静默镶嵌。
- **逻辑门网络**：镶嵌赋予 **输入 / 输出 / 非门 / 与门 / 计数门 / 锁存门 / 延时门 / 延时输入门 / 远程门** 等属性，构成基于维度的方向性逻辑门网络；门链手势可一次批量铺设并配置。
- **载体与物流传输**：镶嵌载体（6 面方向性镶孔）、逻辑载体、物流载体均可按面编程；物流载体按面配置**输入 / 输出**，支持吞吐量限制与物品过滤。
- **体验优化**：溜槽发射器 / 投掷器及其磁性变体（支持过滤、槽位禁用、比较器输出、最高九倍数量），以及穿戴在头部屏蔽指定声音的移动式消音器（支持 Curios）。

## 数据驱动

- `data/<namespace>/material/base/*.json`：基底材料（镶孔数）。
- `data/<namespace>/material/inlay/*.json`：镶嵌材料（属性）。
- `data/<namespace>/recipe/inlay/*.json`：镶嵌配方。
- `data/<namespace>/recipe/inlay_crafting|inlay_smithing|inlay_copying/*.json`：镶合配方。

## 游戏内手册

集成 [Ageratum](https://github.com/Anvil-Dev/Ageratum) 手册框架，提供中英双语游戏内手册：执行 `/ageratum anvilcraft_doge_plus` 打开，悬停本模组物品按住 **W** 可跳转。JEI 提供镶嵌与镶合配方分类。

## 环境

NeoForge 21.1+ · Minecraft 1.21.1 · Java 21，依赖 [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft) 1.6.0+。可选依赖：Curios API、JEI。

## 许可证

MIT © GTouMing
