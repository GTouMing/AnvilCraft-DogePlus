---
navigation:
  title: "Doge Line"
  icon: "anvilcraft_doge_plus:doge_anvil"
categories:
  - mechanics
---

# Doge Line

This page covers growth, magnet controls and node handling; recipes and structure previews are on the individual entries.

## Growth

- Right-click a [Doge Anvil](../block/doge_anvil.md) with raw meat to feed it; accepted items use the common raw-meat tag (beef, porkchops, chicken, mutton, rabbit and similar). Each piece adds 1 growth by default, and 128 growth attempts to produce a [Giant Doge Anvil](../block/giant_doge_anvil.md); survival feeding consumes 1 piece.

- The small anvil is the bottom-center of the giant structure: leave 1 block on each horizontal side and 2 layers above, for a 3×3×3 space. The other 26 cells must be replaceable; blocked growth keeps its progress.

- If growth is full but space is blocked, clear the obstruction and right-click again without feeding more meat. Before full growth, an ordinary non-feeding right-click opens the anvil interface.

- Feeding and steel-block conversion are separate routes; a growing small anvil needs no steel-block shell.

These values are defaults and can be changed by the server.

## Magnet controls

The [Hand Doge Magnet](../item/doge_magnet.md) chooses its action from its stored anvil, the clicked target and Shift, with no separate mode menu.

| Condition and control | Result |
|---|---|
| Empty, other hand empty or holding an anvil, right-click a storable anvil | Store 1 anvil |
| Empty, anvil in the other hand, right-click air without Shift | Load 1 from the other hand |
| Loaded, right-click a valid placement position | Place the anvil and clear storage |
| Loaded, hold right-click on air or a living entity without Shift, then release | Launch a Flying Anvil, clear storage, 10-tick cooldown |
| Empty with no anvil to load, right-click air without Shift | Attract nearby items and XP orbs, using 1 durability |
| Shift + right-click a suitable support block | Operate a Doge Node: remove an existing one first; creation tries anvil pickup and placement first |

- Storage keeps only the anvil type, not growth or other block-state values; do not use it to move an anvil while preserving growth.

- The magnet has 255 durability and is repaired with Doge Steel Ingots; attraction range and cooldown use AnvilCraft's handheld-magnet settings.

## Node storage and retrieval

1. Hold an empty magnet in the main hand and Shift + right-click a support with a suitable collision top. The node appears at the top-center.

2. Bring items near the node or insert them through logistics that support its item interface. It holds up to 8 stacks, each limited by the item's own stack size.

3. With an empty main hand, right-click the support of a node containing items to retrieve them; anything that does not fit is released near the player, and the node remains.

4. Hold a Doge Magnet in the main hand and Shift + right-click to remove the node and release its contents as pickup items.

The node stores its contents as item entities, even through direct logistics insertion. Brief support movement has a grace period, but persistent support loss removes the node and releases items.

## Flying anvils and marks

- Right-click a target along your line of sight with an AnvilCraft Magnet Ingot; each mark adds 1 and consumes 1 ingot. The Doge Magnet is not the marking tool.

- Default damage is 10 + marks × 2. Each flying anvil hits a given living entity at most once, excludes its owner, and can continue flying after a hit.

- Hitting a block or exceeding the lifetime drops a recoverable anvil item; it does not place an anvil block.
