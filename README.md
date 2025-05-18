# LifeSteal Plugin

A feature-rich LifeSteal plugin for Minecraft servers that adds an exciting heart-stealing mechanic with PvP/PvE cycles, ally system, bounty hunting, and more.

## Features

- **Heart Stealing**: Players gain hearts by killing others and lose hearts upon death
- **PvP/PvE Cycle**: Automatic switching between PvP and PvE modes with customizable durations
- **Ally System**: Form alliances with other players to help each other survive
- **First Join System**: 
  - Welcome new players with customizable messages
  - Require confirmation before joining the server
  - Teleport new players to a safe location after confirmation
- **Bounty System**: 
  - Hunt down players with bounties for special rewards
  - Rare bounties with special rewards like Revival Hearts
  - Location broadcasting for bounty targets
  - Logout penalties for bounty targets who disconnect
- **World Border System**: Dynamic world border that can shrink over time
  - Configurable initial size and center point
  - Automatic shrinking at configurable intervals
  - Players take damage when outside the border
  - Prevents item throwing and teleportation beyond the border
- **Custom Items**: 
  - Heart Fragments to gain extra hearts
  - Revival Totems to bring back eliminated players
  - Rare Revival Hearts from bounty hunting
- **Revival System**:
  - Revive eliminated allies using Revival Totems or Hearts
  - GUI-based selection of allies to revive
  - Works with both spectator mode and banned players
- **Boss Bar**: Display server information and mode timers
- **Action Bar**: Shows current mode (PvP/PvE) and remaining time
- **Elimination System**: Players with 0 hearts are eliminated (configurable: ban or spectator mode)

## Requirements

- Spigot/Paper 1.17.1+
- Java 17+

## Installation

1. Download the latest release from [Modrinth](https://modrinth.com/plugin/lifesteal_akar1881/versions)
2. Place the JAR file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/LifeSteal/config.yml`

## Commands

### General Commands
- `/ally <player>` - Send an ally request
- `/ally list` - View your allies
- `/ally accept <player>` - Accept an ally request
- `/ally deny <player>` - Deny an ally request
- `/ally remove <player>` - Remove a player from your allies

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

## Configuration

The plugin is highly configurable through the following files:

### config.yml
Contains the main configuration for the plugin, including:
- Heart settings (starting, min, max)
- PvP/PvE cycle settings
- First join system
- World border settings
- Bounty system
- Boss bar and action bar settings
- Elimination settings

### items.yml
Configure the custom items:
- Heart Fragment
- Revival Totem
- Revival Heart

### Example Configuration
```yaml
# Heart settings
starting-hearts: 10
min-hearts: 0
max-hearts: 20

# Heart gain/loss
hearts-gained-per-kill: 1
hearts-lost-per-death: 1
natural-death-loss: true

# First Join System
first-join:
  enabled: true
  messages:
    - "&6Welcome to our LifeSteal SMP!"
    - "&eThis is a hardcore survival experience where:"
    - "&c- You lose hearts when you die"
    - "&a- You gain hearts by killing other players"
    - "&e- Players with 0 hearts are eliminated"
    - "&6Type &lCONFIRM &6in chat if you agree to these rules"
  confirm-message: "&aCongratulations! Welcome to the server!"
  teleport-message: "&aYou have been teleported to a safe location. Good luck!"
```

## Support

For bug reports and feature suggestions, please use [GitHub Issues](https://github.com/Akar1881/lifesteal/issues).
[Discord Community](https://discord.gg/K6tkSQcPfA).

## Credits

- Plugin developed by Akar1881
- Special thanks to the Minecraft plugin development community