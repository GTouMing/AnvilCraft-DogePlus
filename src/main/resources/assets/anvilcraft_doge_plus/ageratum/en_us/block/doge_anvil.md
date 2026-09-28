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

A special anvil: feed it raw meat to raise its growth, and once the limit is reached it turns into the [Giant Doge Anvil](giant_doge_anvil.md).

- Hold **raw meat** (beef, porkchop, chicken, mutton or rabbit) and right-click the anvil.
- Each piece of raw meat adds growth (default `+1`, adjustable in the server config).
- When growth reaches the limit (default `128`, configurable), the anvil tries to turn into a Giant Doge Anvil in place.
- If the conversion fails, the growth is kept.

<recipe id="anvilcraft_doge_plus:crafting_shaped/doge_anvil"/>

::: tip
Growth can be adjusted in the server config (`maxGrowth` and `growthPerMeat`).
:::
