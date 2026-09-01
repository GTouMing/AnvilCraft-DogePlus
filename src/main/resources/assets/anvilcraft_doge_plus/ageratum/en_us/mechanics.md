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

<ref item="anvilcraft_doge_plus:inlay_table"/> has two inputs and two outputs:

- **Base material slot**: the item to be inlaid. How many **sockets** it has decides how many **inlay materials** can be embedded.
- **Inlay material slot**: the material to embed. It defines the **attribute** granted to the **base material**.
- **Product slot**: the finished, inlaid item.
- **Old inlay material slot**: the old inlay material replaced when re-inlaying a base material whose sockets are all full.

To perform an inlay:

1. Right-click the top of the table to place the **base material** and the **inlay material** in order (or throw items onto the table, dropped items are sucked into the table; or use chutes to automate it).
2. Make an **anvil strike the table** (or use an Anvil Hammer to hit the table).
3. The anvil hammers the inlay material into the base material, consuming **1 material + 1 base material** and producing **1 inlaid item**.
4. Right-click the side of the table with an empty hand to take the product or the old inlay material, or right-click the top to take everything.

### Filling and replacing

- As long as the base material has **free sockets**, each impact adds one more inlay.
- When the base material is **full**, the next inlay **replaces the inlay material in the socket corresponding to the anvil's fall height**, and the old inlay material is ejected into the old inlay material slot.
- If the replaced material carried the **Enchant** attribute, its enchantments are extracted back onto the old inlay material.

### Removing inlays

With the base material in place and the **material slot empty**, an anvil impact **removes** one inlay instead:

- The anvil's **fall distance** decides which socket the inlay is taken from: (0,1] corresponds to slot 1, (1,2] to slot 2, and so on.
- The removed material goes into the old inlay material slot, and the base material loses one inlay material.

### Data-driven design

The whole system is fully data-driven, so modpack authors can extend it freely:

- `data/<namespace>/material/base/*.json`: base material definitions (including socket counts).
- `data/<namespace>/material/inlay/*.json`: material definitions (including granted attributes).
- `data/<namespace>/recipe/inlay/*.json`: recipes binding a material to a base material.

### Base materials and sockets

A **base material** is any item defined in `data/<namespace>/material/base/*.json`. Its `sockets` value decides how many materials can be inlaid.

## Inlay materials and attributes

An **inlay material** is any item defined in `data/<namespace>/material/inlay/*.json`. Its `attributes` value decides which attributes it has.

### Item attributes

These affect the item (and block, see below) that carries them:

| Attribute       | Source material                             | Effect                                                                                                       |
|-----------------|---------------------------------------------|--------------------------------------------------------------------------------------------------------------|
| **Fire-proof**  | Netherite Ingot                             | Cannot be burned.                                                                                            |
| **Magnetic**    | <ref item="anvilcraft:magnet_ingot"/>       | Attracted by magnets.                                                                                        |
| **High Temp**   | <ref item="anvilcraft:ember_metal_ingot"/>  | The longer it stays in lava or fire, the more damage accumulates; attacking consumes the accumulated damage. |
| **Cold Forged** | <ref item="anvilcraft:frost_metal_ingot"/>  | Slowly repairs durability while in water or powder snow.                                                     |
| **Eternal**     | <ref item="anvilcraft:transcendium_ingot"/> | Indestructible: immune to fire, explosion, cactus, time, and the void.                                       |
| **Nirvana**     | Totem                                       | On death, triggers a totem, then the inlay material shatters.                                                |
| **Defense**     | Netherite Ingot                             | Grants +2 armor when held or equipped.                                                                       |
| **Life**        | <ref item="anvilcraft:royal_steel_ingot"/>  | Grants +2 max health when held or equipped.                                                                  |
| **Attack**      | <ref item="anvilcraft:cursed_gold_ingot"/>  | Grants +2 attack damage when held or equipped.                                                               |
| **Enchant**     | Enchanted Book / Book                       | Merges enchantments on inlay, extracts them on removal.                                                      |
| **Effect**      | Potion                                      | Grants potion effects when held or equipped.                                                                 |

### Logic-gate attributes

These turn an inlaid block into a logic gate:

| Attribute     | Source material                            | Effect                                                      |
|---------------|--------------------------------------------|-------------------------------------------------------------|
| **Direction** | <ref item="anvilcraft:multiphase_matter"/> | Makes sockets directional, active when there are 6 sockets. |
| **Output**    | Redstone                                   | Outputs redstone signals from this face.                    |
| **Input**     | Observer                                   | Inputs redstone signals from this face.                     |
| **NOT Gate**  | Redstone Torch                             | Outputs the inverted signal of the opposite face.           |
| **AND Gate**  | Repeater                                   | Outputs the AND of adjacent inputs, in order.               |
| **OR Gate**   | Comparator                                 | Outputs the OR of adjacent inputs, in order.                |
| **Generator** | <ref item="anvilcraft:supercapacitor"/>    | Produces 512 kW of power once placed.                       |

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

## Block-level inlays

Inlays are **not lost when a block is placed**. An inlaid block item keeps its attributes in the world:

- **Magnetic** blocks attract nearby items.
- **Fire-proof** blocks cannot burn.
- **Eternal** blocks resist explosions and cannot be mined.
- **High Temp** blocks burn entities that step on them.
- Breaking the block returns an item that keeps all its inlays.

## Logic gate blocks

Blocks with **Directional**, **Input**, **Output**, and various **gate** attributes.

### Directional sockets

When a base material has **6 sockets** and the **Direction** attribute, each face of the block corresponds to a socket. Inputs and outputs are then resolved per-face:

- **Input** inputs redstone signals from this face.
- **Output** outputs the maximum signal of all input faces from this face.
- **NOT Gate** reads the input face signal and outputs the inverted signal.
- **AND Gate** requires at least two inputs and outputs their minimum.
- **OR Gate** outputs the maximum of its inputs.

::: tip
Maybe you can even build a very small computer?
:::

## Doge Anvil growth

<ref item="anvilcraft_doge_plus:doge_anvil"/> has its own growth mechanic:

- Feed it **raw meat** by right-clicking (default +1 growth per piece).
- At the cap (default 128), it grows in place into <ref item="anvilcraft_doge_plus:giant_doge_anvil"/>, a 3×3×3 multiblock that inherits all the behaviors of the Giant Anvil.

Both values are server-configurable.
