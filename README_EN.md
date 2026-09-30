# AnvilCraft Doge+

![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange?style=flat-square)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

**AnvilCraft: Doge+** is an add-on for [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft) built around the **Doge series** of materials, and it ships a brand-new **Inlay System**. Everything is **data-driven** so modpack authors can extend it freely.

## Features

- **Doge materials & tools**: Doge Steel Ingots and Blocks smelted from iron and bone; a Doge Anvil that feeds on raw meat and grows into a 3×3×3 Giant Doge Anvil; a hand-held Doge Magnet that stores, places, and launches anvils (the Flying Anvil deals damage on hit) and attracts items & XP; and a Doge Node that captures items (up to 8).
- **Inlay System**: hammer inlay materials into the **sockets** of a base material on the Inlay Table to grant properties — Fire-Proof, Magnetic, High Temp, Cold Forged, Eternal, Nirvana, Defense, Life, Attack, Enchant, Effect, Resonance, and more. Inlays survive being placed as blocks, and drops keep them.
- **Inlay Crafting**: press a **fully inlaid base material** on the Inlay Crafting Table to produce a result, covering vanilla smithing, all 18 armor trims, Hollow Magnet Blocks, Neutronium, and silent inlaying.
- **Logic-gate networks**: inlays grant **Input / Output / NOT / AND / OR / Counter / Latch / Delay** properties, forming a dimension-based directional logic-gate network; gate-chain gestures build and program a whole run at once.
- **Carriers & logistics**: the Inlay Carrier (6 directional sockets), Logic Carrier, and Logistics Carrier are all face-programmable; the Logistics Carrier behaves as per-face **Insert / Extract** with throughput limits and item filtering.
- **Quality of life**: chute dispenser/dropper hybrids (with filtering, slot disabling, comparator output, and up to 9× amounts) plus a wearable Mobile Silencer (Curios optional).

## Data-driven

- `data/<namespace>/material/base/*.json`: base materials (socket counts).
- `data/<namespace>/material/inlay/*.json`: inlay materials (attributes).
- `data/<namespace>/recipe/inlay/*.json`: inlay recipes.
- `data/<namespace>/recipe/inlay_crafting|inlay_smithing|inlay_copying/*.json`: inlay crafting recipes.

## In-game handbook

Integrates the [Ageratum](https://github.com/Anvil-Dev/Ageratum) handbook framework with a bilingual (EN/ZH) in-game handbook: open it with `/ageratum anvilcraft_doge_plus`, or hover one of this mod's items and hold **W** to jump to its page. JEI provides Inlay and Inlay Crafting recipe categories.

## Requirements

NeoForge 21.1+ · Minecraft 1.21.1 · Java 21, plus [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft) 1.6.0+. Optional: Curios API, JEI.

## License

MIT © GTouMing
