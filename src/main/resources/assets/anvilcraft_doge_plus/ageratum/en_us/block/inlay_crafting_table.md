---
navigation:
  title: "Inlay Crafting Table"
  icon: "anvilcraft_doge_plus:inlay_crafting_table"
categories:
  - items
items:
  - anvilcraft_doge_plus:inlay_crafting_table
---

# Inlay Crafting Table

<block id="anvilcraft_doge_plus:inlay_crafting_table"/>

The workstation for inlay crafting recipes:

- One base slot plus material slots whose count is determined by the target-product filter/recipe (up to 6); each material slot only accepts materials matching its socket requirement.
- Right-click its top face with an item to insert it (empty/same slots are merged first; when all are full, the first accepting slot is replaced and the old contents are returned); right-click with an empty hand to take everything out.
- An anvil landing on the table first applies all **silent inlays** from the material slots to the base, then matches the inlay crafting recipe; one strike produces the output, which drops below and can be collected automatically with hoppers/chutes.

The Inlay Crafting Table and the [Inlay Table](inlay_table.md) can be crafted into each other:

<recipe id="anvilcraft_doge_plus:crafting_shapeless/inlay_crafting_table_from_inlay_table"/>

<recipe id="anvilcraft_doge_plus:crafting_shapeless/inlay_table_from_inlay_crafting_table"/>

::: info
For the complete inlay and inlay crafting mechanics, see [Mechanics](../mechanics.md).
:::
