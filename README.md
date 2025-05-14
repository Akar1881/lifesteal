![Logo](logo/logo.png)

# LifeSteal Plugin

A feature-rich LifeSteal plugin for Minecraft servers that adds an exciting heart-stealing mechanic with PvP/PvE cycles, ally system, and bounty hunting.

## Features

- **Heart Stealing**: Players gain hearts by killing others and lose hearts upon death
- **PvP/PvE Cycle**: Automatic switching between PvP and PvE modes
- **Ally System**: Form alliances with other players
- **Bounty System**: Hunt down players with bounties for special rewards
- **World Border System**: Dynamic world border that can shrink over time
  - Configurable initial size and center point
  - Automatic shrinking at configurable intervals
  - Players take damage when outside the border
  - Prevents item throwing and teleportation beyond the border
- **World Border System**: Dynamic world border that can shrink over time
  - Configurable initial size and center point
  - Automatic shrinking at configurable intervals
  - Players take damage when outside the border
  - Prevents item throwing and teleportation beyond the border
- **Custom Items**: 
  - Heart Fragments to gain extra hearts
  - Revival Totems to bring back eliminated players
  - Rare Revival Hearts from bounty hunting
- **Boss Bar**: Display server information and mode timers
- **Elimination System**: Players with 0 hearts are eliminated (configurable: ban or spectator mode)

## Requirements

- Spigot/Paper 1.17.1+
- Java 17+

## Installation

1. Download the latest release from Modrinth
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/LifeSteal/config.yml`

## Commands

### General Commands
- `/ally <player>` - Send an ally request
- `/ally list` - View your allies
- `/ally accept <player>` - Accept an ally request
- `/ally deny <player>` - Deny an ally request

### Admin Commands
- `/lifesteal reload` - Reload configuration
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Manage player hearts
- `/lifesteal revive <player>` - Revive a player
- `/lifesteal schedule <set|add|subtract|info>` - Control PvP/PvE cycle
- `/lifesteal togglebar` - Toggle the boss bar visibility
- `/lifesteal border info` - Display world border information
- `/lifesteal border reset` - Reset the border to its initial size
- `/lifesteal border shrink` - Force an immediate border shrink
- `/lifesteal border toggle` - Enable or disable the world border

## Permissions

- `lifesteal.admin` - Access to all admin commands
- `lifesteal.item.use.heart` - Allow using heart items
- `lifesteal.item.use.revive` - Allow using revive items
- `lifesteal.bypass.maxhearts` - Bypass maximum hearts limit

## Support

For bug reports and feature suggestions, please use [GitHub Issues](https://github.com/Akar1881/lifesteal/issues).