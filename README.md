# CityJobs 💼

A lightweight, production-ready jobs and economy plugin for Paper 1.21.x. 
Designed for active survival and RPG servers, CityJobs handles job progression, economy integration, and localized work areas without relying on bloated external dependencies like WorldGuard or MySQL.

## Features

- **Built-in Occupations:** Lumberjack, Miner, and Farmer.
- **Dynamic Configuration:** Add any 1.21 material (like Pale Oak or Deepslate variants) to the `config.yml`, set a price, and type `/jobsadmin reload`. No code updates needed.
- **Standalone Work Areas:** Built-in 3D cuboid region system. Restrict job activities to specific zones, or let players work globally until you define an area.
- **Zero-Setup Database:** Uses local SQLite with WAL (Write-Ahead Logging) mode enabled for safe, asynchronous data saving. Progress survives crashes.
- **Vault Economy Integration:** Direct bridging with EssentialsX (or any Vault provider). Transactions are mathematically safe—items are only removed from the inventory *after* the bank deposit succeeds, preventing duplication exploits.
- **Death & Exploit Protection:** Players keep their job, level, and XP on death. Built-in cooldowns prevent `/job join/quit` spam and `/sell` command abuse.

## Installation

1. Download the latest `CityJobs.jar`.
2. Place it into your server's `plugins/` directory.
3. Ensure you have **Vault** and an economy provider (like **EssentialsX**) installed.
4. Restart your server.
5. Configure your prices, spawns, and areas in `plugins/CityJobs/config.yml`.

## Commands & Permissions

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/job` | Opens the interactive job selection GUI | `cityjobs.use` |
| `/job join <job>` | Joins a specific job (starts a 10m cooldown) | `cityjobs.use` |
| `/job quit` | Leaves the current job | `cityjobs.use` |
| `/job stats` | Displays player level, XP, and earnings | `cityjobs.use` |
| `/sellwood` | Sells all valid Lumberjack items | `cityjobs.use` |
| `/sellmines` | Sells all valid Miner items | `cityjobs.use` |
| `/sellfarm` | Sells all valid Farmer items | `cityjobs.use` |

### Admin Commands (`cityjobs.admin`)
| Command | Description |
|---------|-------------|
| `/jobsadmin reload` | Reloads config, prices, and areas instantly |
| `/jobsadmin info <player>`| Views live job stats for an online player |
| `/jobsadmin setprice <job> <mat> <price>` | Sets an item's sell price dynamically |
| `/jobsadmin setspawn <job>` | Sets the respawn location for a specific job |

## Setting Up a Work Area
You don't need external region plugins to restrict where players can work.

1. Stand at the first corner of your desired area and run: `/jobsadmin pos1 <job>`
2. Stand at the opposite (diagonal) corner and run: `/jobsadmin pos2 <job>`
3. Save the region: `/jobsadmin createarea <job>`

*Note: Once an area is created, players with that job can only gain XP and harvest resources inside this specific region. To remove the restriction, simply set `enabled: false` under that job's area in `config.yml`.*

## Compilation
To build this project from source, you need Java 21 and Maven.
```bash
mvn clean package
