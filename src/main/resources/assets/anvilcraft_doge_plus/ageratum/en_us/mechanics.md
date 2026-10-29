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

| Property               | Source material                             | Effect                                                                                                                            |
|------------------------|---------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------|
| **Fire-proof**         | Netherite Ingot                             | Cannot be burned.                                                                                                                 |
| **Magnetic**           | <ref item="anvilcraft:magnet_ingot"/>       | Has attraction, and repels when activated.                                                                                        |
| **High Temp**          | <ref item="anvilcraft:ember_metal_ingot"/>  | The longer it stays in lava or fire, the more accumulated damage; attacking consumes it.                                          |
| **Cold Forged**        | <ref item="anvilcraft:frost_metal_ingot"/>  | Slowly repairs durability in water or powder snow.                                                                                |
| **Eternal**            | <ref item="anvilcraft:transcendium_ingot"/> | Indestructible, immune to fire, explosion, cactus, time and the void.                                                             |
| **Nirvana**            | Totem                                       | Triggers a totem on death, then the inlay material shatters.                                                                      |
| **Defense**            | Netherite Ingot                             | Scales with the total number of Defense inlays equipped (see Attribute Levels).                                                   |
| **Life**               | <ref item="anvilcraft:royal_steel_ingot"/>  | Scales with the total number of Life inlays equipped (see Attribute Levels).                                                      |
| **Attack**             | <ref item="anvilcraft:cursed_gold_ingot"/>  | Scales with the total number of Attack inlays equipped (see Attribute Levels).                                                    |
| **Enchant**            | Enchanted Book / Book                       | Merges enchantments when inlaid, extracts them when removed.                                                                      |
| **Effect**             | Potion                                      | Provides potion effects when held, equipped, or placed (see Attribute Levels).                                                    |
| **Output**             | Redstone                                    | Outputs redstone signals from this face.                                                                                          |
| **Input**              | <ref item="anvilcraft:redstone_wire"/>      | Inputs redstone signals from this face.                                                                                           |
| **NOT Gate**           | Redstone Torch                              | Outputs the inverted signal of the opposite face.                                                                                 |
| **AND Gate**           | Repeater                                    | ANDs adjacent inputs in order.                                                                                                    |
| **Counter Gate**       | Button                                      | Counts input pulses and outputs a 1-tick signal at the set count.                                                                 |
| **Latch Gate**         | Lever                                       | Records and outputs a received signal; a second input clears it.                                                                  |
| **Delay Gate**         | Pressure Plate                              | Outputs the received signal for the set number of ticks.                                                                          |
| **Delayed Input Gate** | Clock                                       | Records the input signal and, after the set number of ticks, feeds it to this block's other gates as a 1-tick input on that face. |
| **Remote Gate**        | <ref item="minecraft:ender_pearl"/>         | Remote faces with the same channel link up: forwards inputs on the redstone side, remote extract on the item side.                |
| **Insert**             | <ref item="anvilcraft:chute"/>              | Puts items into the container it faces.                                                                                           |
| **Extract**            | <ref item="anvilcraft:magnetic_chute"/>     | Takes items from the container it faces.                                                                                          |
| **Generator**          | <ref item="anvilcraft:supercapacitor"/>     | Produces power once placed (512 kW by default, configurable).                                                                     |

### Attribute Levels

**Defense**, **Life**, **Attack** and **Effect** are handled by the **attribute level system**: it counts the total number of inlay entries carrying that attribute across all your equipment, and the count determines the attribute level.

### Resonance

The **Resonance** property does nothing on its own; it enhances the other inlay properties:

| Enhanced property | Resonance effect                                                                                              |
|-------------------|---------------------------------------------------------------------------------------------------------------|
| Defense           | This item's inlays count double toward the matching attribute level.                                          |
| Life              | This item's inlays count double toward the matching attribute level.                                          |
| Attack            | This item's inlays count double toward the matching attribute level.                                          |
| Enchant           | Merges enchantments with a 50% chance to raise the level by 1, and a 50% chance to extract them when removed. |
| Nirvana           | Triggers a totem on death, then the material has a 50% chance to shatter.                                     |
| High Temp         | The longer it stays in lava or fire, the more accumulated damage; attacking slowly consumes it.               |
| Cold Forged       | Repairs durability faster in water or powder snow (durable items only).                                       |

### Recipe Filtering

Hold an anvil hammer in your main hand and a sample item in your off hand, then right-click a **horizontal side of an Inlay Table / Inlay Crafting Table** to set a recipe filter (right-click with an empty off hand to clear):

- **Inlay Table**: the off-hand item is the base (possibly already inlaid). The base slot only accepts the same base; the material slot accepts the existing inlay materials when the base already has inlays, otherwise every material from that base's recipes.
- **Inlay Crafting Table**: the off-hand item is the target product. The base slot only accepts fully-inlaid bases matching a recipe that produces it.
- An <ref item="anvilcraft:filter"/> in the off hand is applied directly as an item filter.

Once set, the filter item is rendered on all four horizontal sides of the block.

## Inlay Crafting

<recipe id="anvilcraft_doge_plus:inlay_crafting/magnet_ingot_from_hollow_magnet_block_4"/>

**Inlay Crafting** extends the inlay system: at the [Inlay Crafting Table](block/inlay_crafting_table.md), items/blocks carrying the specified inlays are combined into new items/blocks.

### The three kinds of inlay crafting

Inlay crafting recipes fall into three kinds:

| Kind               | Source                          |
|--------------------|---------------------------------|
| **Inlay Crafting** | Added by this mod               |
| **Inlay Smithing** | Vanilla and AnvilCraft smithing |
| **Inlay Copying**  | AnvilCraft jewel copying        |


**Some materials have no properties**, yet can still be inlaid that usually use on an inlay crafting recipe.


### Inlay crafting recipes

Built-in/derived inlay crafting recipes of the mod:

- **Vanilla smithing**: a Netherite Upgrade Smithing Template inlaid with a Netherite Ingot + diamond gear → the matching netherite gear.
- **AnvilCraft smithing**: 2/4/8-to-one smithing templates and the various upgrade templates, following AnvilCraft's recipes.
- **Some AnvilCraft jewel copying**: the copied item is the mold, and the product carries Vanishing Curse I.
- **Vanilla armor trims**: 18 trim templates inlaid with "trimmable armor + trim material".
- **Hollow Magnet Block**: inlay n Iron Ingots → n Magnet Ingots (n = 1..4).
- **Confinement Chamber**: inlay 1 Charged Neutronium Ingot → 1 Confined Neutronium Ingot.
- **Shulker Box**: inlay 3 Shulker Boxes and 1 Void Matter Block → 1 Space Overcompressor.

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
- Logistics Carriers chain into a transport network: an **Insert** end records all **Extract** ends and moves items end to end, the whole chain **sharing one 7gt cooldown**. When several **Insert** faces draw from one **Extract** face, the amount is split **weighted by each face's throughput** (zero-share faces fall through). **Every Insert face's filter along the chain applies** (cumulative AND).
- The **Insert / Extract** target can be a block container or an entity container such as a [Doge Node](../entity/doge_node.md).
- **Craft Insert**: before inserting, tries to craft the **filtered item** from the materials of the **reachable extract sources**; throughput limits the products per round, and container items (e.g. empty buckets) stay in their respective source. Falls back to a plain insert when it cannot craft. Inlay a **Crafting Table** to gain this property, or select it directly in the wheel.

**Quick edit**: hold a carrier of the same type and right-click one of its faces; if another carrier of the same type is directly on the other side of that face, the two facing faces are completed into a path (when both are empty, the clicked face defaults to Input/Extract and the opposite to Output/Insert; hold **Ctrl** to invert; when only one side has a role, the other side gets the complementary role). If the face points at a **container** (logistics carrier) or **redstone dust** (logic carrier), only the clicked face is edited: an existing Input/Output/Extract/Insert becomes its opposite; an empty face is completed from the carrier's existing roles (prefer Input/Extract when both exist, add Output when only Input exists); hold **Ctrl** to invert.
