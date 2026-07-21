# AnvilCraft Doge+

![NeoForge](https://img.shields.io/badge/NeoForge-21.1+-orange?style=flat-square)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.1-green?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

**AnvilCraft Doge+** adds four new chute variants and a Magnetized Node item capture enhancement to [AnvilCraft](https://github.com/Anvil-Dev/AnvilCraft).

---

## Content

### Blocks

| Block | Recipe | Function |
|---|---|---|
| Chute Dispenser | Chute + Dispenser | Pulls items from above and dispenses along facing direction |
| Chute Dropper | Chute + Dropper | Pulls items from above and drops along facing direction |
| Magnetic Chute Dispenser | Magnetic Chute + Dispenser | Supports upward placement; input from opposite of output |
| Magnetic Chute Dropper | Magnetic Chute + Dropper | Supports upward placement; input from opposite of output |

All blocks support item filtering, slot disabling, and comparator output.

### Enhancement: Magnetized Node Capture

Via Mixin, an `ICaptured` interface is added directly to `ItemEntity`, with `EntityDataAccessor` syncing the capture state to the client. Captured items orbit around the Magnetized Node in a circular arrangement (8 positions, 75° outward tilt). Features include auto-merging, right-click retrieval with empty hand, item deposit with held items, and automatic release when the node disappears.

---

## License

MIT © GTouMing
