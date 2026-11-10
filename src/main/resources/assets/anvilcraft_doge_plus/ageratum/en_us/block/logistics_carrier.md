---
navigation:
  title: "Logistics Carrier"
  icon: "anvilcraft_doge_plus:logistics_carrier"
categories:
  - items
items:
  - anvilcraft_doge_plus:logistics_carrier
---

# Logistics Carrier

<block id="anvilcraft_doge_plus:logistics_carrier"/>

A socketless programmable item-transfer block. Set faces to Insert, Craft Insert, Extract or Remote; it does not participate in redstone.

## Use

1. Place it between the source and destination containers. Hold an Anvil Hammer, long-press right-click the source-facing side and select Extract, then set the destination side to Insert.
2. Aim at the Insert face and use Ctrl + scroll to adjust throughput; short right-click with a sample item or filter in the offhand to set its filter, and an empty offhand clears it.
3. Extend a route by connecting an upstream Insert face to a downstream Extract face.

Insert faces draw from reachable Extract sources, and every filter along the route must pass; Craft Insert needs an ordinary product sample. See [Carrier mechanics](../mechanics/carriers.md) for the full rules.

<recipe id="anvilcraft_doge_plus:inlay_crafting/logistics_carrier"/>
