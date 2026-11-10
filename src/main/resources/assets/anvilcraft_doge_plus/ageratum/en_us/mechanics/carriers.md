---
navigation:
  title: "Carriers"
  icon: "anvilcraft_doge_plus:inlay_carrier"
categories:
  - mechanics
---

# Carriers

The [Inlay Carrier](../block/inlay_carrier.md) gains face properties from 6 sockets. The <ref item="anvilcraft_doge_plus:logic_carrier"/> and <ref item="anvilcraft_doge_plus:logistics_carrier"/> have no sockets: program their faces directly. Material sources are in [Inlay and Inlay Crafting](inlay.md).

## Basic connections

- **Redstone:** set the face toward the signal source to Input, and the face toward the target to Output or another output gate. Adjacent carriers connect as “output gate → Input”; unprogrammed faces neither receive nor output signals.

- **Logistics:** set the face toward the source container to Extract and the face toward the destination to Insert. Extend a route by connecting an upstream Insert face to a downstream Extract face.

- Logistics Carriers do not participate in redstone or connect to redstone dust or wire. Remote faces do not replace the Extract/Insert faces toward containers or directly power neighbors.

## Programming and adjustment

Hold an Anvil Hammer in the main hand and aim at the face. Logic and Logistics Carriers open a mode wheel on long right-click; Inlay Carriers change faces with materials, see their [block page](../block/inlay_carrier.md).

| Action | Result |
|---|---|
| Ctrl + scroll | Adjust the aimed face's gate setting, throughput or remote channel number. |
| Ctrl + Shift + scroll | Gate settings and channel numbers by 5, throughput by 10. |
| Offhand item + short right-click on an Insert face | Set its item filter; empty offhand clears it. |
| Offhand item + short right-click on a Remote face | Set the channel's item identifier; empty offhand clears the identifier but keeps the number. |

Filters accept ordinary samples and filter items. Throughput limits items moved per round, or products for Craft Insert; the default range is 1–64.

## Logic gates

Except for Delayed Input, which reads the neighbor facing it separately, output gates use this block's Input faces, Remote faces and delayed input pulses, not only the opposite face.

| Gate | Behavior | Setting |
|---|---|---|
| Input | Reads its neighbor's redstone signal, clipped to the setting. | Signal limit 0–15 |
| Output | Outputs the maximum input, clipped to the setting. | Signal limit 0–15 |
| NOT | Outputs 0 if any input is powered, otherwise 15. | Not adjustable |
| AND | Needs at least two input faces and outputs their minimum signal; fewer than two gives 0. | Not adjustable |
| Counter | Counts arrivals, emits signal 15 for 1 tick at the threshold, then resets. | Default maximum 16 |
| Latch | Each arrival alternates between recording/outputting that signal and clearing it; scrolling can also edit the stored value. | Stored value 0–15 |
| Delay | With a positive setting, an arrival updates the output immediately and starts a timer; later arrivals update the output without restarting it; expiry clears the output. | Default 0–20 ticks |
| Delayed Input | Records its neighbor's signal, waits, then supplies a 1-tick input to this block's other gates, not an outward output; retriggering restarts the timer. | Default 0–20 ticks |

An “arrival” is an increase on an input face, including 0 becoming positive; a steady signal does not retrigger. Setting either delay mode to 0 neither passes through instantly nor clears a running Delay output. 20 ticks equal 1 second.

## Remote channels

A channel is identified by “item type + number,” with an empty identifier and number 0 by default. A sender with Input and Remote faces can transmit redstone to a receiver with a matching Remote face and an output gate; a source with Extract and Remote faces can supply a destination with a matching Remote face and an Insert face.

## Logistics and Craft Insert

- Extract/Insert targets may be block containers or entity containers such as a [Doge Node](../entity/doge_node.md). Face roles must match.

- Insert ends move items end-to-end from reachable Extract sources.

- **Craft Insert:** use an ordinary product sample as its filter to craft from materials in reachable Extract sources. Throughput must fit a recipe's full output. An empty filter or filter item cannot specify a target; failed crafting falls back to ordinary insertion.
