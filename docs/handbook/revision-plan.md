# 手册改版计划

依据：`adr/0001`（新手优先）、`adr/0002`（条目页入口化）、`adr/0003`（删减标准）、`glossary.md`（术语）。
范围：`src/main/resources/assets/anvilcraft_doge_plus/ageratum/{zh_cn,en_us}`，中英同步，结构不变。

## 一、全局规则

- 条目页只留：是什么 → 最短上手（≤3 步）→ 配方 → 一句指路。
- 同一规则只在 `mechanics/` 一处展开；条目页不复述。
- 删除非损失型纠错句；“不是 X 而是 Y”仅在会导致丢物品、浪费材料或卡流程时保留。
- 删除所有配置键、配置页链接与 `*.toml` 描述。
- 保留 frontmatter、`<recipe>`、`<structure>`、`<ref>` 等组件；表格保留，叙述压缩。

### 内联默认值白名单

| 位置 | 保留值 | 处理 |
|---|---|---|
| Doge 成长 | 每份生肉 +1，默认 128 点 | 注明“默认值，服务器可改” |
| 飞行铁砧 | 伤害 = 10 + 标记数 × 2 | 同上 |
| 镶嵌属性 | 每级防御 / 生命 / 攻击 +2 | 同上 |
| 磁铁耐久 | 255 | 直接写 |
| 节点容量 | 8 组 | 直接写 |
| 消音范围 | 每个坐标轴 ≤15.5 格 | 直接写 |
| 溜槽 | 9 槽、每槽 1 件、脉冲后 4 tick | 直接写 |

其余数值（计数/延时上限、物流量上限、信道上限、门链长度、速度、范围、存活时间等）不再出现在手册。

## 二、逐文件改动

### 删除

- `zh_cn/mechanics/configuration.md`
- `en_us/mechanics/configuration.md`

### 链接清理（6 处）

| 文件 | 改动 |
|---|---|
| `zh_cn/index.md` / `en_us/index.md` | 删除 Contents 表 Configuration 行 |
| `zh_cn/mechanics.md` / `en_us/mechanics.md` | 删除“配置参考”行 |
| `zh_cn/mechanics/doge.md` / `en_us/mechanics/doge.md` | 删除 2 处配置链接，改为内联值 |
| `zh_cn/block/doge_anvil.md` / `en_us/block/doge_anvil.md` | 删除配置链接 |
| `zh_cn/entity/flying_anvil.md` / `en_us/entity/flying_anvil.md` | 删除配置链接 |

### `mechanics/` 分册（目标 400～700 字）

- `inlay.md`
  - 删除“配方来源”整段与“超限合金”整节。
  - 保留 1 个代表性镶合示例（下界合金升级）及其 `<recipe>`。
  - 压缩属性表、面属性表、等级、共鸣、过滤、放置后效果。
  - 仅保留损失型提醒：选中空孔不执行、后孔不前移、非空镶嵌数不符不合成、过滤不代替配方。
- `carriers.md`
  - 保留门表、远程、物流、快捷编辑、门链（结构不动）。
  - 删除重复解释与配置键名；数字改为默认范围或省略。
- `chutes.md`
  - 保留选型表与输入/输出规则；压缩红石、槽位、过滤段落。
  - 删除“不是原版发射器随机选 1 槽”等非损失纠错。
- `doge.md`
  - 内联成长与伤害默认值；压缩磁铁、节点、飞行、消音为操作要点。
  - 保留损失型提醒：收纳不保存成长值、支撑丢失释放物品、飞行铁砧不自动成块、Shift+右键空气不吸物。
- `mechanics.md`（hub）
  - 删除配置行。
  - 术语表压成 10 个核心词，与 `docs/handbook/glossary.md` 对齐。

### 条目页（目标 150～250 字）

| 文件 | 保留 | 删除/压缩 |
|---|---|---|
| `block/inlay_table.md` | 用途、3 步、台子分工、配方 | 批量、选孔、过滤细节 → 指路 `inlay.md` |
| `block/inlay_crafting_table.md` | 用途、3 步、配方 | 槽位、匹配、余料细节 → 指路 |
| `block/inlay_carrier.md` | 6 孔、孔位顺序、最短连接、配方 | 门行为、调节、显示 → 指路 `carriers.md` |
| `block/logic_carrier.md` | 可编程、3 步最短连接、配方 | 门表、门链、护目镜细节 → 指路 |
| `block/logistics_carrier.md` | 可编程、3 步最短连接、配方 | 竞争、冷却、合成余料 → 指路 |
| `block/chute_*.md` ×4 | 输入位置、输出方向、脉冲行为、选型一句、配方 | 重复的 9 槽/过滤/断电说明压成一句 |
| `block/doge_steel_block.md` | 储块、双向合成、超温配方 | 与成长重复的段落 |
| `block/doge_anvil.md` | 喂食、128 点、空间要求、配方 | 配置链接、重复说明 |
| `block/giant_doge_anvil.md` | 喂食主路线、一句“19 钢块转化”、`<structure>` 图 | 转化长文、转化 `<recipe>`、失败处理 |
| `item/doge_steel_ingot.md` | 来源（铁+骨粉）、用途、配方 | 重复的溜槽材料说明 |
| `item/doge_magnet.md` | 收纳/放置/发射/吸物、耐久 255、配方 | 优先级长表 → 指路 `doge.md` |
| `item/mobile_silencer.md` | 佩戴、按键、15.5 格、配方 | 排除式纠错句 |
| `entity/doge_node.md` | 放置/回收、8 组 | 支撑容错长文 |
| `entity/flying_anvil.md` | 发射、10+2×、不自动成块 | 配置链接、重复操作 |
| `block/index.md`、`item/index.md`、`entity/index.md` | 一句话索引 | 规则与配置入口 |

### 英文侧

按中文同结构、同字数改写全部对应文件；删除 `en_us/mechanics/configuration.md`，同步清理链接与内联值。

## 三、执行顺序

1. 删两个 `configuration.md`，清 6 处链接，避免死链扩散。
2. 改 5 个 `mechanics/` 分册（事实来源先稳定）。
3. 改条目页与三个索引。
4. 英文侧逐文件同步。
5. 验收。

## 四、验收清单

- [x] `configuration.md` 与“配置参考”引用为 0；仅保留一句“数值为默认值”的总体说明。
- [x] 条目页 ≤260 字（不含表格/组件）；机制页 840～1770 字，超出预算的原因见 ADR 0002 实施记录。
- [x] 中英各 26 个 Markdown 文件，frontmatter 键与组件 id 一一对应。
- [x] 每个条目页都含指向对应机制分册的链接。
- [x] 保留的纠错句均属于“会丢物品 / 浪费材料 / 卡流程”。
- [x] `giant_doge_anvil.md` 仍指向存在的 `<structure>` 渲染文件。
