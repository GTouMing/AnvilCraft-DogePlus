# AnvilCraft Doge+

![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange?style=flat-square)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

**AnvilCraft Doge+** 为 [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft) 添加了四种溜槽变体与磁化节点物品捕获增强。

---

## 内容

### 方块

| 方块 | 合成 | 功能 |
|---|---|---|
| 溜槽发射器 | 溜槽 + 发射器 | 从上方吸入物品并向 facing 方向发射 |
| 溜槽投掷器 | 溜槽 + 投掷器 | 从上方吸入物品并向 facing 方向投出 |
| 磁性溜槽发射器 | 磁性溜槽 + 发射器 | 支持朝上放置，输入来自输出反向 |
| 磁性溜槽投掷器 | 磁性溜槽 + 投掷器 | 支持朝上放置，输入来自输出反向 |

所有方块均支持物品过滤、槽位禁用、比较器输出。

### 物品

| 物品 | 描述 |
|---|---|
| 移动式消音器 | 穿戴在头部时可屏蔽指定声音。支持 Curios API（可选依赖），可在 Curios 头部饰品栏中使用。未安装 Curios 时不影响原版功能 |

### Curios 集成

移动式消音器支持 Curios API 头部饰品栏：
- 可在 Curios 头部饰品栏中穿戴，与原版头盔槽功能一致
- 按 V 键打开声音屏蔽配置界面（自动检测原版头盔槽和 Curios 槽）
- 无 Curios 时不加载相关代码，完全不影响原有功能

### 增强：磁化节点物品捕获

通过 Mixin 直接对掉落物实体（`ItemEntity`）添加 `ICaptured` 接口，使用 `EntityDataAccessor` 同步捕获状态。捕获的物品会围绕磁化节点以 8 等分圆周排列并向外倾斜 75° 展示。支持自动合并同类物品、空手右键取回、手持物品右键存入。节点消失时自动释放所有物品。

---

## 许可证

MIT © GTouMing
