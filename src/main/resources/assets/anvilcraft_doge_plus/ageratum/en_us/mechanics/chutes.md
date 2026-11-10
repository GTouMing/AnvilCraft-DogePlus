---
navigation:
  title: "Chute Automation"
  icon: "anvilcraft_doge_plus:chute_dispenser"
categories:
  - mechanics
---

# Chute Automation

Block pages: [Chute Dispenser](../block/chute_dispenser.md) · [Chute Dropper](../block/chute_dropper.md) · [Magnetic Chute Dispenser](../block/magnetic_chute_dispenser.md) · [Magnetic Chute Dropper](../block/magnetic_chute_dropper.md)

All four variants automatically take in items and output on redstone pulses. Shapelessly combine the corresponding AnvilCraft chute with a vanilla dispenser or dropper; each recipe produces 2.

## Input and facing

| Type | Input position | Output direction |
|---|---|---|
| Normal variants | The cell directly above | Down or any horizontal direction, never up |
| Magnetic variants | The cell opposite the output | All six directions, including up |

1. Check the outlet, then put the input container on the correct side.

2. Automatic intake runs while unpowered. Shift reverses the placement facing; a magnetic variant moves its input to the opposite side of its outlet.

## Redstone and output

- A rising signal at the block or the block above schedules one activation after 4 ticks. Continuous power does not repeat output.

- Each activation visits all 9 slots and takes 1 item from each occupied slot, up to 9 items.

- Dispenser variants run the item's dispenser behavior, such as Bone Meal or buckets; ordinary items are tossed.

- Dropper variants always eject item entities. They do not use Bone Meal or buckets and do not insert directly into a container in front.

- These variants need a pulse; load while unpowered, then test the direction with one short pulse.

## Filtering and slots

- There are 9 slots. Hover a filter slot and scroll to adjust its limit from 1–64, in steps of 1, or hold Shift for steps of 5.

- Enabling filtering builds samples from the current contents of occupied slots; empty slots will not automatically accept items. With an empty cursor, Shift + click an empty slot to toggle its disabled state.

Choose normal or magnetic by input position, then dispenser or dropper by whether you need item behavior or simple ejection.
