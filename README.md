# Map Upgrades

## Project Identity
- **Name:** mapupgrades
- **Mod ID:** `mapupgrades`
- **Version:** `${version}` (Resolved at build time)

## Technical Summary
The **mapupgrades** mod implements a persistent, incremental player upgrade system optimized for custom RPG or adventure maps. Using Mixins, it injects custom NBT data storage (`UpgradeDataHolder`) into the player entity to maintain upgrade progression across sessions and respawns (`ServerPlayerEvents.COPY_FROM`). The mod mathematically calculates upgrade costs based on configurable base prices and multipliers, automatically deducting funds from a required vanilla scoreboard objective named `money`. It also provides optional compatibility with `placeholder-api` to display player upgrade data.

## Feature Breakdown
- **Persistent Player Data:** Modifies player NBT to permanently store their respective upgrade levels safely across dimension changes and deaths.
- **Dynamic Configurable Upgrades:** Uses JSON-based configuration to precisely balance base prices, scaling cost curves (`cost = base_price * factor^level`), maximum levels, and statistical value increments.
- **Core Upgrade Types:** Natively supports modifying `WALK_SPEED`, `MAX_HEALTH`, `STONE_SPEED`, `DIRT_SPEED`, and `WOOD_SPEED`.
- **Scoreboard Economy:** Directly hooks into the `money` scoreboard objective to validate and process upgrade purchases.
- **Visual Feedback:** Triggers specific particle bursts (Happy Villager and Totem of Undying) alongside sound effects upon successful purchases.

## Command Registry

| Command | Description | Permission Level |
| :--- | :--- | :--- |
| `/upgrade reload` | Reloads the `mapupgrades.json` configuration file. | OP (2) |
| `/upgrade info [<targets>]` | Displays the current level, max level, and next cost of all upgrades. | Player (0) / Target: OP (2) |
| `/upgrade buy <targets> <type> [<cost>]` | Purchases an upgrade for the target. Optionally overrides default cost. | OP (2) |
| `/upgrade add <targets> <type> <amount>` | Directly adds the specified number of levels to an upgrade. | OP (2) |
| `/upgrade set <targets> <type> <level>` | Sets an upgrade to a specific numerical level. | OP (2) |
| `/upgrade reset <targets> [<type>]` | Resets a specific or all upgrades to level 0. | OP (2) |

## Configuration Schema
The mod expects/generates the main configuration file at `config/mapupgrades.json`:

```json
{
  "walk_speed": {
    "base_price": 10,
    "price_factor": 1.5,
    "max_level": 30,
    "value_per_level": 0.025
  },
  "stone_speed": {
    "base_price": 10,
    "price_factor": 1.5,
    "max_level": 30,
    "value_per_level": 0.025
  },
  "dirt_speed": {
    "base_price": 10,
    "price_factor": 1.5,
    "max_level": 30,
    "value_per_level": 0.025
  },
  "wood_speed": {
    "base_price": 10,
    "price_factor": 1.5,
    "max_level": 30,
    "value_per_level": 0.025
  },
  "max_health": {
    "base_price": 15,
    "price_factor": 1.8,
    "max_level": 30,
    "value_per_level": 1.0
  }
}
```

## Developer Info
- **Author:** el_this_boy
- **Platform:** Fabric 1.21.1
