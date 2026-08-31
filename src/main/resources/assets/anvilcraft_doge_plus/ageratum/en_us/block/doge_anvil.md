---
navigation:
  title: "Doge Anvil"
  icon: "anvilcraft_doge_plus:doge_anvil"
categories:
  - items
items:
  - anvilcraft_doge_plus:doge_anvil
---

# Doge Anvil

<block id="anvilcraft_doge_plus:doge_anvil"/>

A growing anvil: feed it raw meat to increase its growth; at the cap it grows into the [Giant Doge Anvil](giant_doge_anvil.md).

- Hold a piece of **raw meat** (beef, porkchop, chicken, mutton, or rabbit) and right-click the anvil.
- Each piece adds growth (default `+1`, configurable in the server config).
- When growth reaches the cap (default `128`, configurable), the anvil **grows in place** into the Giant Doge Anvil.
- Right-clicking it opens the regular anvil menu for repairing and renaming.

<recipe id="anvilcraft_doge_plus:crafting_shaped/doge_anvil"/>

::: tip
The growth values are server-configurable in `anvilcraft_doge_plus-server.toml` (`maxGrowth` and `growthPerMeat`).
:::
