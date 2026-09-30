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

The core of AnvilCraft: Doge+ is the **inlay system**, a brand-new crafting method that embeds materials into items to grant them special properties. This page explains the whole system.

## Inlay System

<recipe id="anvilcraft_doge_plus:inlay/totems_crab_claw"/>

### How it works

<ref item="anvilcraft_doge_plus:inlay_table"/> has two slots, the **base material slot** and the **inlay material slot**:

- The **base material slot** accepts: the item to be inlaid, which has **sockets**.
- The **inlay material slot** accepts: the material to embed, which has **properties**.

When the items in the two slots match a recipe, an **inlay** can be performed.

### Filling and replacing

- While the base material is **not fully inlaid**, every inlay is appended.
- When the base material is **fully inlaid**, the next inlay **replaces the inlay material in the socket given by the anvil's fall height**, and the old inlay material drops below the table.

### Removing inlays

When **the base material is present and the material slot is empty**, it tries to remove one inlay:

- The anvil's **fall height** decides which socket is taken out: (0,1] is the 1st socket, (1,2] is the 2nd, and so on.
- The removed inlay material drops below the table together with the base material.

### Properties

| Property         | Source material                             | Effect                                                                                     |
|------------------|---------------------------------------------|--------------------------------------------------------------------------------------------|
| **Fire-proof**   | Netherite Ingot                             | Cannot be burned.                                                                          |
| **Magnetic**     | <ref item="anvilcraft:magnet_ingot"/>       | Has attraction, and repels when activated.                                                 |
| **High Temp**    | <ref item="anvilcraft:ember_metal_ingot"/>  | The longer it stays in lava or fire, the more accumulated damage; attacking consumes it.   |
| **Cold Forged**  | <ref item="anvilcraft:frost_metal_ingot"/>  | Slowly repairs durability in water or powder snow.                                         |
| **Eternal**      | <ref item="anvilcraft:transcendium_ingot"/> | Indestructible, immune to fire, explosion, cactus, time and the void.                      |
| **Nirvana**      | Totem                                       | Triggers a totem on death, then the inlay material shatters.                               |
| **Defense**      | Netherite Ingot                             | Grants +2 armor when held or equipped.                                                     |
| **Life**         | <ref item="anvilcraft:royal_steel_ingot"/>  | Grants +2 max health when held or equipped.                                                |
| **Attack**       | <ref item="anvilcraft:cursed_gold_ingot"/>  | Grants +2 attack damage when held or equipped.                                             |
| **Enchant**      | Enchanted Book / Book                       | Merges enchantments when inlaid, extracts them when removed.                               |
| **Effect**       | Potion                                      | Grants potion effects when held, equipped or placed.                                       |
| **Output**       | Redstone                                    | Outputs redstone signals from this face.                                                   |
| **Input**        | <ref item="anvilcraft:redstone_wire"/>      | Inputs redstone signals from this face.                                                    |
| **NOT Gate**     | Redstone Torch                              | Outputs the inverted signal of the opposite face.                                          |
| **AND Gate**     | Repeater                                    | ANDs adjacent inputs in order.                                                             |
| **Counter Gate** | Button                                      | Counts input pulses and outputs a 1-tick signal at the set count.                          |
| **Latch Gate**   | Lever                                       | Records and outputs a received signal; a second input clears it.                           |
| **Delay Gate**   | Pressure Plate                              | Outputs the received signal for the set number of ticks.                                   |
| **Delayed Input Gate** | Clock                                 | Records the input signal and, after the set number of ticks, feeds it to this block's other gates as a 1-tick input on that face. |
| **Remote Gate**  | <ref item="minecraft:ender_pearl"/>         | Remote faces with the same channel link up: forwards inputs on the redstone side, remote extract on the item side. |
| **Insert**       | <ref item="anvilcraft:chute"/>              | Puts items into the container it faces.                                                    |
| **Extract**      | <ref item="anvilcraft:magnetic_chute"/>     | Takes items from the container it faces.                                                   |
| **Generator**    | <ref item="anvilcraft:supercapacitor"/>     | Produces 512 kW of power once placed.                                                      |

### Resonance

The **Resonance** property does nothing on its own; it enhances the other inlay properties:

| Enhanced property | Resonance effect                                                                                              |
|-------------------|---------------------------------------------------------------------------------------------------------------|
| Defense           | Grants +4 armor when held or equipped.                                                                        |
| Life              | Grants +4 max health when held or equipped.                                                                   |
| Attack            | Grants +4 attack damage when held or equipped.                                                                |
| Enchant           | Merges enchantments with a 50% chance to raise the level by 1, and a 50% chance to extract them when removed. |
| Nirvana           | Triggers a totem on death, then the material has a 50% chance to shatter.                                     |
| High Temp         | The longer it stays in lava or fire, the more accumulated damage; attacking slowly consumes it.               |
| Cold Forged       | Repairs durability faster in water or powder snow (durable items only).                                       |

## Inlay Crafting

<recipe id="anvilcraft_doge_plus:inlay_crafting/magnet_ingot_from_hollow_magnet_block_4"/>

**Inlay Crafting** extends the inlay system: at the [Inlay Crafting Table](block/inlay_crafting_table.md), items/blocks carrying the specified inlays are combined into new items/blocks.

### The three kinds of inlay crafting

Inlay crafting recipes fall into three kinds:

| Kind               | `kind`     | Source                          |
|--------------------|------------|---------------------------------|
| **Inlay Crafting** | `crafting` | Added by this mod               |
| **Inlay Smithing** | `smithing` | Vanilla and AnvilCraft smithing |
| **Inlay Copying**  | `copying`  | AnvilCraft jewel copying        |


**Silent Inlay**: some items can take part in inlaying even without properties; these items are not written into inlay recipes, but defined through **inlay crafting recipes**.


### Inlay crafting recipes

Built-in/derived inlay crafting recipes of the mod:

- **Vanilla smithing**: a Netherite Upgrade Smithing Template inlaid with a Netherite Ingot + diamond gear → the matching netherite gear.
- **AnvilCraft smithing**: 2/4/8-to-one smithing templates and the various upgrade templates, following AnvilCraft's recipes.
- **Some AnvilCraft jewel copying**: the copied item is the mold, and the product carries Vanishing Curse I.
- **Vanilla armor trims**: 18 trim templates inlaid with "trimmable armor + trim material".
- **Hollow Magnet Block**: inlay n Iron Ingots → n Magnet Ingots (n = 1..4).
- **Confinement Chamber**: inlay 1 Charged Neutronium Ingot → 1 Confined Neutronium Ingot.

**Transcendium**: the add-on specifically adds a Transcendium inlay crafting recipe: inlay <ref item="anvilcraft:overheated_ember_metal_block"/> with <ref item="anvilcraft:charged_neutronium_ingot"/> and craft it:

| Enchantment count | Products                                                                             |
|-------------------|--------------------------------------------------------------------------------------|
| 0                 | 4 Transcendium Ingots                                                                |
| 1–10              | 4 Transcendium Ingots + 3n Transcendium Nuggets + (10n% chance) 1 Neutronium Ingot   |
| 11–14             | 4 Transcendium Ingots + 3n Transcendium Nuggets + 1 Neutronium Ingot                 |
| 15                | 1 Neutronium Ingot + 1 Block of Transcendium                                         |
| ≥16               | n Transcendium Nuggets + 1 Neutronium Ingot + 1 Block of Transcendium                |

## Block-level Inlays

Inlays **are not lost when a block is placed**. A block item that has been inlaid keeps its properties after being placed as a block:

- A **Magnetic** block can attract the anvil below and repel it when activated, which shows up as an increased fall height.
- A **Fire-proof** block is not flammable.
- An **Eternal** block is blast-proof and cannot be mined.
- An **Effect** block grants potion effects to creatures that step on it.
- Items dropped when breaking a block keep all of their inlays in almost every case.



## Carriers

**Inlay Carrier**: gains face properties from its sockets, and is used to craft the **Logic Carrier** and the **Logistics Carrier**.

<ref item="anvilcraft_doge_plus:logic_carrier"/>:

- The logic gate type of each face can be programmed; hold an Anvil Hammer and long-press right-click to open the wheel.
- Aim at the face where the gate is and adjust the set value with **Ctrl + scroll wheel**.
- Faces that have not been programmed neither accept redstone input nor output redstone signals.

<ref item="anvilcraft_doge_plus:logistics_carrier"/>:

- The transfer mode of each face can be programmed; hold an Anvil Hammer and long-press right-click to open the wheel.
- Aim at an **Insert** logistics face and adjust the **throughput** with **Ctrl + scroll wheel**; with an item in the offhand and an Anvil Hammer in the main hand, right-click a logistics to set a **filter**.
- **Throughput**: the number of items transported per trip; **filter**: only the specified items are allowed through, and filters are supported.
- Logistics Carriers can be connected into a transport chain; an **Insert** end records all **Extract** ends, and no matter how long the chain is, items travel end-to-end with a 7gt cooldown.
- The **Insert / Extract** target can be a block container or an entity container such as a [Doge Node](../entity/doge_node.md).
