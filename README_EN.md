# AnvilCraft Doge+

![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange?style=flat-square)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

**AnvilCraft Doge+** adds a Doge series of content to [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft): chute variants, a Doge Anvil growth mechanic, the Giant Doge Anvil, Doge Steel materials, the Doge  Magnet, and the Doge Node.

---

## Content

### Blocks

| Block                    | Obtaining                                                         | Function                                                                                           |
|--------------------------|-------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Chute Dispenser          | Chute + Dispenser                                                 | Pulls items from above and dispenses along the facing direction                                    |
| Chute Dropper            | Chute + Dropper                                                   | Pulls items from above and drops along the facing direction                                        |
| Magnetic Chute Dispenser | Magnetic Chute + Dispenser                                        | Supports upward placement; input from opposite of output                                           |
| Magnetic Chute Dropper   | Magnetic Chute + Dropper                                          | Supports upward placement; input from opposite of output                                           |
| Doge Anvil               | Doge Steel Ingot + Doge Steel Block                               | Feed raw meat to grow; grows into the Giant Doge Anvil when full; right-click opens the anvil menu |
| Giant Doge Anvil         | Multiblock conversion                                             | 3×3×3 giant structure inheriting the Giant Anvil's features (falling, anvil menu, landing shock)   |
| Doge Steel Block         | 9 Doge Steel Ingots craft / Iron Block + Bone Block super-heating | Stores Doge Steel Ingots                                                                           |

All chutes support item filtering, slot disabling, and comparator output.

### Items

| Item             | Obtaining                                          | Function                                                                                           |
|------------------|----------------------------------------------------|----------------------------------------------------------------------------------------------------|
| Doge Steel Ingot | Iron Ingot + Bone Meal super-heating               | Material                                                                                           |
| Hand Doge Steel  | Ender Pearl + Redstone + Doge Steel Ingot (shaped) | Magnet tool: store/place/launch anvils, attract items & XP, shift-right-click to place a Doge Node |
| Mobile Silencer  | Silencer conversion                                | Worn on head to mute selected sounds; supports Curios API (optional dependency)                    |

### Mechanics

**Doge Anvil growth**: Right-click a Doge Anvil with raw meat (beef/porkchop/chicken/mutton/rabbit) to increase its growth value (default +1 per meat, cap 128 — both configurable in the server config). When full, it grows into a 3×3×3 Giant Doge Anvil in place.

**Giant Doge Anvil multiblock conversion**: Build a 3×3×3 with 19 Doge Steel Blocks as shown (`D` = Doge Steel Block), place a crafting table on top of the top layer as the anchor, then drop a Giant Anvil onto the crafting table to trigger the conversion:

```
Bottom      Middle      Top
DDD         ···         DDD
DDD         ·D·         DDD
DDD         ···         DDD
```

**Doge Magnet**:
- Right-click an anvil block to store it; right-click block to place the stored anvil.
- Right-click air with an anvil item in the other hand to store it.
- With a stored anvil, right-click to charge and release to launch a flying anvil.
- Without a stored anvil, right-click to attract nearby items and XP.
- Shift-right-click a block to place a Doge Node.

**Doge Node**: Inherits the Magnetized Node's item attraction and self-implements item capture (up to 8 items, rendered orbiting the node). Right-click the node with an empty hand to retrieve items; shift-right-click with Doge Magnet to remove the node. When a chute outputs toward the node, items are captured directly into it.

---

## Inlay System

**Inlay Table**: Place an *inlay material* and a *base* onto the table, then drop an anvil onto it to inlay. The base provides sockets; the material provides properties. When sockets are full, inlaying again replaces the oldest material.

**Data-driven**: `data/<ns>/material/inlay|base/*.json` define material properties and base socket counts; recipes live in `data/<ns>/recipe/inlay/*.json`.

| Property | Effect |
| --- | --- |
| Fire-Proof / Magnetic / High Temp / Cold Forged / Eternal / Nirvana | Item & block-level effects: fire immunity, magnet attraction, stacking heat damage, durability repair, indestructible, totem trigger |
| Defense / Life / Attack | +2 armor / +2 max health / +2 attack when held or equipped |
| Enchant | Merges enchantments on inlay, extracts on removal |
| Resonance | Enhances other sockets' materials: doubled attributes, enchantment boost chance, 50% Nirvana shatter, slower heat consumption, faster repair |
| Direction / Output / Input / NOT / AND / OR | Form a dimension-based directional logic-gate network |
| Generator | Produces 512 kW power after place, connecting to the AnvilCraft grid |

**Block-level inlay**: An inlaid block retains its properties when placed (magnetic attraction, fire-proof, eternal explosion-proof & unbreakable, high-temp burns walkers, etc.); breaking it drops an item that keeps the inlay.

---

## License

MIT © GTouMing
