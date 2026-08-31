---
navigation:
  title: "Flying Anvil"
categories:
  - items
---

# Flying Anvil

The anvil launched by the [Hand Doge Magnet](../item/doge_magnet.md). Damage is determined by the server config (`baseDamage` + `perMark` per mark on the target), flight speed is configurable, and it forcibly disappears after `flyLifetime` ticks.

## Related config

| Option | Default | Meaning |
| --- | --- | --- |
| `baseDamage` | 10 | Base damage when the flying anvil hits an entity. |
| `perMark` | 2 | Extra damage per mark on the target. |
| `anvilSpeed` | 2.5 | Flying anvil speed. |
| `markRange` | 64 | Sight-ray range (blocks) for marking targets with the magnet ingot. |
| `flyLifetime` | 400 | Hard timeout (ticks) of the flying anvil. |
