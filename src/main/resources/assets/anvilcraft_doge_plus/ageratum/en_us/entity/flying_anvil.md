---
navigation:
  title: "Flying Anvil"
categories:
  - items
---

# Flying Anvil

An anvil launched by the [Doge Magnet](../item/doge_magnet.md). Damage is decided by the server config (`baseDamage` + `perMark` for every mark on the target), the flight speed is configurable, and it disappears forcibly after `flyLifetime` ticks.

## Related config

| Option        | Default | Meaning                                                              |
|---------------|---------|----------------------------------------------------------------------|
| `baseDamage`  | 10      | Base damage when the flying anvil hits an entity.                    |
| `perMark`     | 2       | Extra damage for every mark on the target.                           |
| `anvilSpeed`  | 2.5     | Flight speed of the anvil.                                           |
| `markRange`   | 64      | Line-of-sight ray range (in blocks) for the magnet to mark a target. |
| `flyLifetime` | 400     | Hard timeout for the anvil's flight (in ticks).                      |
