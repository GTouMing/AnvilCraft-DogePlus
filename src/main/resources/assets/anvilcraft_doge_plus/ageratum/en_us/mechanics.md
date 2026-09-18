---
navigation:
  title: "Mechanics"
  icon: "anvilcraft_doge_plus:inlay_table"
categories:
  - mechanics
items:
  - anvilcraft_doge_plus:inlay_table
  - anvilcraft_doge_plus:doge_anvil
  - anvilcraft_doge_plus:giant_doge_anvil
  - anvilcraft:crab_claw
  - anvilcraft:deflection_ring
  - anvilcraft:acceleration_ring
  - anvilcraft:sapphire_block
  - anvilcraft:ruby_block
  - anvilcraft:topaz_block
  - anvilcraft:multiphase_matter_block
  - anvilcraft:magnet_ingot
  - anvilcraft:ember_metal_ingot
  - anvilcraft:frost_metal_ingot
  - anvilcraft:transcendium_ingot
  - anvilcraft:royal_steel_ingot
  - anvilcraft:cursed_gold_ingot
  - anvilcraft:multiphase_matter
  - anvilcraft:supercapacitor
---

# Mechanics

The core of AnvilCraft: Doge+ is the **Inlay System**, a brand-new crafting method that embeds materials into items to grant them special attributes. This page explains the system in full.

## The Inlay System

<recipe id="anvilcraft_doge_plus:inlay/totems_crab_claw"/>

### How it works

<ref item="anvilcraft_doge_plus:inlay_table"/> has two slots, a **base material slot** and an **inlay material slot**:

- **Base material slot**: the item to be inlaid. How many **sockets** it has decides how many **inlay materials** can be embedded.
- **Inlay material slot**: the material to embed. It defines the **attribute** granted to the **base material**.

To perform an inlay:

1. Right-click the top of the table to place the **base material** first, then the **inlay material** (or throw items onto the table, dropped items are sucked into the table; or use chutes to automate it).
2. Make an **anvil strike the table** (or use an Anvil Hammer to hit the table).
3. The anvil hammers the inlay material into the base material, consuming **1 material + 1 base material** and producing **1 inlaid item**, which drops below the table (together with any replaced old material).
4. Right-click with an empty hand to take the base material and the inlay material back, or right-click to put items in.

### Filling and replacing

- As long as the base material has **free sockets**, each impact adds one more inlay.
- When the base material is **full**, the next inlay **replaces the inlay material in the socket corresponding to the anvil's fall height**, and the old inlay material drops below the table.
- If the replaced material carried the **Enchant** attribute, its enchantments are extracted back onto the old inlay material.

### Removing inlays

With the base material in place and the **material slot empty**, an anvil impact **removes** one inlay instead:

- The anvil's **fall distance** decides which socket the inlay is taken from: (0,1] corresponds to slot 1, (1,2] to slot 2, and so on.
- The removed inlay material and the base material (minus that inlay) both drop below the table.

### Data-driven design

The whole system is fully data-driven, so modpack authors can extend it freely:

- `data/<namespace>/material/base/*.json`: base material definitions (including socket counts).
- `data/<namespace>/material/inlay/*.json`: material definitions (including granted attributes).
- `data/<namespace>/recipe/inlay/*.json`: recipes binding a material to a base material.
- `data/<namespace>/recipe/inlay_crafting/*.json`: Inlay Crafting recipes (see [Inlay Crafting](#inlay-crafting) below).

### Base materials and sockets

A **base material** is any item defined in `data/<namespace>/material/base/*.json`. Its `sockets` value decides how many materials can be inlaid.

## Inlay materials and attributes

An **inlay material** is any item defined in `data/<namespace>/material/inlay/*.json`. Its `attributes` value decides which attributes it has.

### Item attributes

These affect the item (and block, see below) that carries them:

| Attribute       | Source material                             | Effect                                                                                                       |
|-----------------|---------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Fire-proof**  | Netherite Ingot                             | Cannot be burned.                                                                                            |
| **Magnetic**    | <ref item="anvilcraft:magnet_ingot"/>       | Has attraction; repels when activated.                                                                       |
| **High Temp**   | <ref item="anvilcraft:ember_metal_ingot"/>  | The longer it stays in lava or fire, the more damage accumulates; attacking consumes the accumulated damage. |
| **Cold Forged** | <ref item="anvilcraft:frost_metal_ingot"/>  | Slowly repairs durability while in water or powder snow.                                                     |
| **Eternal**     | <ref item="anvilcraft:transcendium_ingot"/> | Indestructible: immune to fire, explosion, cactus, time, and the void.                                       |
| **Nirvana**     | Totem                                       | On death, triggers a totem, then the inlay material shatters.                                                |
| **Defense**     | Netherite Ingot                             | Grants +2 armor when held or equipped.                                                                       |
| **Life**        | <ref item="anvilcraft:royal_steel_ingot"/>  | Grants +2 max health when held or equipped.                                                                  |
| **Attack**      | <ref item="anvilcraft:cursed_gold_ingot"/>  | Grants +2 attack damage when held or equipped.                                                               |
| **Enchant**     | Enchanted Book / Book                       | Merges enchantments on inlay, extracts them on removal.                                                      |
| **Effect**      | Potion                                      | Grants potion effects when held, equipped or placed.                                                         |

### Logic-gate attributes

These turn an inlaid block into a logic gate:

| Attribute        | Source material                         | Effect                                                             |
|------------------|-----------------------------------------|--------------------------------------------------------------------|
| **Output**       | Redstone                                | Outputs redstone signals from this face.                           |
| **Input**        | <ref item="anvilcraft:redstone_wire"/>  | Inputs redstone signals from this face.                            |
| **NOT Gate**     | Redstone Torch                          | Outputs the inverted signal of the opposite face.                  |
| **AND Gate**     | Repeater                                | Outputs the AND of adjacent inputs, in order.                      |
| **OR Gate**      | Comparator                              | Outputs the OR of adjacent inputs, in order.                       |
| **Counter Gate** | Button                                  | Counts input pulses; outputs a 1-tick signal at the set count.     |
| **Latch Gate**   | Lever                                   | Records and outputs the received signal; a second input clears it. |
| **Delay Gate**   | Pressure Plate                          | Outputs the received signal for the set number of ticks.           |
| **Generator**    | <ref item="anvilcraft:supercapacitor"/> | Produces 512 kW of power once placed.                              |

### Resonance

The **Resonance** attribute does not act on its own, it **enhances other inlays on the same base material**:

| Enhanced attribute | Resonance effect                                                                                                    |
|--------------------|---------------------------------------------------------------------------------------------------------------------|
| Defense            | Grants +4 armor when held or equipped.                                                                              |
| Life               | Grants +4 max health when held or equipped.                                                                         |
| Attack             | Grants +4 attack damage when held or equipped.                                                                      |
| Enchant            | Merges enchantments with a 50% chance to boost the level by 1, and a 50% chance to extract them when removed.       |
| Nirvana            | Triggers a totem on death, then the material has a 50% chance to shatter.                                           |
| High Temp          | The longer it stays in lava or fire, the more damage accumulates; attacking slowly consumes the accumulated damage. |
| Cold Forged        | Repairs durability faster in water or powder snow (durable items only).                                             |

## Inlay Crafting

**Inlay Crafting** is the second step of the Inlay System: on the [Inlay Crafting Table](block/inlay_crafting_table.md), a **base material already filled with all its inlays** is pressed into a new product.

1. First fill the base material on the [Inlay Table](block/inlay_table.md) until it is full (the socket count matches the required materials, e.g. the Hollow Magnet Block takes 4 Iron Ingots, the Netherite Upgrade Template takes a Netherite Ingot + a piece of diamond gear).
2. Put the filled base into the Inlay Crafting Table and strike it with an anvil.
3. On a match the base is consumed, producing the **product** and an **empty-inlaid base** (the mold minus all inlays); both drop below the table.

### Inlay Crafting recipes

Inlay Crafting recipes live in `data/<namespace>/recipe/inlay_crafting/*.json`:

```json
{
  "type": "anvilcraft_doge_plus:inlay_crafting",
  "base": "minecraft:netherite_upgrade_smithing_template",
  "inlays": [
    {"item": "minecraft:netherite_ingot"},
    {"item": "minecraft:diamond_axe"}
  ],
  "result": "minecraft:netherite_axe"
}
```

- `base`: the id of the base item that acts as the **mold**.
- `inlays`: the set of materials that must appear in the base's sockets; each entry may be an item or a tag (e.g. `#minecraft:trimmable_armor`). Matching **ignores socket order**: every non-empty socket must pair with one `inlays` entry one-to-one — empty placeholders, extras, or anything unmatchable count as no match.
- `result`: the product; the vanilla multi-output form `{"id": "...", "count": n}` is supported. **Omitting** it means the product is derived at craft time from the base and its inlays (as for armor trims: the trimmable gear receives the trim component derived from template and material).
- `curse_of_vanishing` (optional, default `false`): when true the product carries Vanishing Curse I (the jewel-copying semantics of the prerequisite mod; the mold is unaffected).

Built-in / derived Inlay Crafting recipes:

- **Vanilla smithing**: Netherite Upgrade Template inlaid with a Netherite Ingot + diamond gear → the matching netherite gear.
- **Vanilla armor trims**: the 18 trim templates inlaid with "trimmable armor + trim material" (both matched by tag), product derived at craft time.
- **Hollow Magnet Block**: n Iron Ingots → n Magnet Ingots (n = 1..4).
- **Prerequisite (AnvilCraft) smithing**: the 2/4/8-to-one forge templates and the various upgrade templates, mirroring the prerequisite recipes.
- **Prerequisite jewel copying**: the copied item acts as the mold and the product carries Vanishing Curse I.

## Block-level inlays

Inlays are **not lost when a block is placed**. An inlaid block item keeps its attributes in the world:

- **Magnetic** blocks have attraction; they repel when activated.
- **Fire-proof** blocks cannot burn.
- **Eternal** blocks resist explosions and cannot be mined.
- **Effect** blocks grant potion effects to entities that step on them.
- Breaking the block returns an item that keeps all its inlays.

## Logic gate blocks

Blocks with **Input**, **Output**, and various **gate** attributes.

### Directional sockets

When a base material has **6 sockets**, each face of the block corresponds to a socket. Inputs and outputs are then resolved per-face:

- **Input** inputs redstone signals from this face; a 0–15 cutoff can be set so only signals up to it pass.
- **Output** outputs the maximum signal of all input faces from this face; a 0–15 cutoff can be set so the output never exceeds it.
- **NOT Gate** reads the input face signal and outputs the inverted signal.
- **AND Gate** requires at least two inputs and outputs their minimum.
- **OR Gate** outputs the maximum of its inputs.
- **Counter Gate** counts input pulses (any input face going from 0 to a signal counts once) and, at the set count, outputs 15 for 1 tick and resets.
- **Latch Gate** records and outputs the received signal; a second input clears the record and stops output.
- **Delay Gate** outputs the received signal for the set number of ticks; a new input during the countdown only updates the output value, it does not reset the timer.

::: tip Gate value
Hold the anvil hammer aiming at the gate's face and use **Ctrl + scroll** to change the value (**Shift** steps by 5), default 15.
Ranges: Input / Output / Latch 0–15; Counter 1–config cap (default 16); Delay 0–config cap (default 20 ticks).
The value is drawn on the part's four sides only while you look at it, and shown in the anvil-hammer HUD.
:::

::: tip
Maybe you can even build a very small computer?
:::

## Doge Anvil growth

<ref item="anvilcraft_doge_plus:doge_anvil"/> has its own growth mechanic:

- Feed it **raw meat** by right-clicking (default +1 growth per piece).
- At the cap (default 128), it grows in place into <ref item="anvilcraft_doge_plus:giant_doge_anvil"/>, a 3×3×3 multiblock that inherits all the behaviors of the Giant Anvil.

Both values are server-configurable.
