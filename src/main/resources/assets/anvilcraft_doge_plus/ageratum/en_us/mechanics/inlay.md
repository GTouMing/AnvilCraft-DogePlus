---
navigation:
  title: "Inlay and Inlay Crafting"
  icon: "anvilcraft_doge_plus:inlay_table"
categories:
  - mechanics
---

# Inlay and Inlay Crafting

The [Inlay Table](../block/inlay_table.md) adds, replaces or removes inlays; the [Inlay Crafting Table](../block/inlay_crafting_table.md) turns a base and materials into new products. See [Carriers](carriers.md) for directions and transport.

## Inlay

<recipe id="anvilcraft_doge_plus:inlay/totems_crab_claw"/>

The <ref item="anvilcraft_doge_plus:inlay_table"/> has one base slot and one material slot. Insert a socketed base and a material matching an inlay recipe, then strike the table with an anvil. Results drop below it.

- **Add:** fill the first empty socket. A strike processes the smaller of the base and material counts; unused items stay in their slots.

- **Replace:** when the base is full, replace the selected inlay and return the old material.

- **Remove:** leave the material slot empty and strike to remove the selected inlay. Selecting an empty socket does nothing, and later sockets do not shift.

- Replacement and removal select sockets by the fall distance at impact: the 1st block selects socket 1, the 2nd selects socket 2, and so on; distances beyond the range select the last socket.

Only socketed bases can be inlaid, and explicit recipes determine which materials fit. Inlay Carriers have 6 sockets and Doge Steel Blocks have 5.

### Properties

| Property | Source material | Use |
|---|---|---|
| Fire-proof | Netherite Ingot, Ember Metal Ingot | Items do not burn; placed blocks are not flammable. |
| Magnetic | <ref item="anvilcraft:magnet_ingot"/> | Attracts anvils and repels them when activated. |
| High Temp | <ref item="anvilcraft:ember_metal_ingot"/> | Accumulates damage in fire or lava and consumes it on attacks. |
| Cold Forged | <ref item="anvilcraft:frost_metal_ingot"/> | Repairs durability in water or powder snow. |
| Eternal | <ref item="anvilcraft:transcendium_ingot"/> | Items resist fire, explosions, time and the void; blocks resist explosions and cannot be mined. |
| Nirvana | Totem of Undying | Triggers a totem on death and consumes that inlay. |
| Defense | Netherite Ingot | Adds armor. |
| Life | <ref item="anvilcraft:royal_steel_ingot"/> | Adds maximum health. |
| Attack | <ref item="anvilcraft:cursed_gold_ingot"/> | Adds attack damage. |
| Enchant | Book, Enchanted Book | Merges enchantments when inlaid; extracts one enchantment when removed. |
| Effect | Regular, Splash or Lingering Potion | Grants effects while held or equipped; a placed block affects creatures stepping on it. |
| Resonance | Amethyst Shard | Enhances other properties on the same item. |
| Generator | <ref item="anvilcraft:supercapacitor"/> | Generates 512 kW by default when placed. |

| Face property | Source material |
|---|---|
| Output | Redstone |
| Input | <ref item="anvilcraft:redstone_wire"/> |
| NOT / AND Gate | Redstone Torch / Repeater |
| Counter / Latch Gate | Button / Lever |
| Delay / Delayed Input Gate | Pressure Plate / Clock |
| Remote Gate | <ref item="minecraft:ender_pearl"/> |
| Insert / Extract | <ref item="anvilcraft:chute"/> / <ref item="anvilcraft:magnetic_chute"/> |
| Craft Insert | Crafting Table |

See [Carriers](carriers.md) for signal rules and adjustments.

### Levels and resonance

Only the main hand, offhand and four armor slots count; each valid inlay contributes 1 level, adding 2 by default. Resonance only affects the item containing it: Defense, Life, Attack and Effect count double; Enchant merging/extraction and Nirvana triggering each have a 50% chance of the bonus; High Temp drains more slowly and Cold Forged repairs faster.

### Recipe filtering

Hold an Anvil Hammer in the main hand and a sample item in the offhand, then right-click a horizontal side of the table; filtering only restricts accepted items and cannot replace a recipe.

## Inlay Crafting

<recipe id="anvilcraft_doge_plus:inlay_smithing/netherite_sword_upgrade"/>

The table accepts one base and up to 6 material slots. A strike first tries silent inlays, then matches an inlay crafting recipe; silent inlays need no separate inlay recipe.

- Existing inlays count toward matching. Material order does not matter, but the number of filled sockets must match the recipe; an extra inlay can prevent crafting.

- With every material slot empty, an already inlaid base that matches a recipe can still be crafted.

- A strike processes as many copies as the available materials support, dropping results below the table; no matching recipe means no consumption.

The usual example is a vanilla upgrade: inlay a Netherite Upgrade Smithing Template with a Netherite Ingot and diamond gear to obtain the corresponding netherite gear.
