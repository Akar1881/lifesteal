# LifeSteal Plugin

A Minecraft plugin that adds a heart-stealing mechanic and ally system to your server. When players kill each other, they can steal hearts and potentially eliminate players from the game.
![Logo](logo/logo.png)

## Features

### Heart System
- Players start with configurable amount of hearts
- Kill players to steal their hearts
- Players can be eliminated when losing all hearts
- Configurable maximum and minimum hearts
- PvP/PvE cycle system with boss bar display

### Ally System
- Players can form alliances with other players
- Ally commands for managing relationships
- GUI interface for viewing and managing allies
- Ability to revive eliminated allies using revival items

### Custom Items
- Heart Fragment: Gain an extra heart
- Revival Totem: Bring back eliminated allies

## Commands

### LifeSteal Commands
- `/lifesteal reload` - Reload plugin configuration
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Modify player hearts
- `/lifesteal revive <player>` - Revive an eliminated player
- `/lifesteal schedule` - Restart the PvP/PvE cycle
- `/lifesteal togglebar` - Toggle the mode boss bar visibility

### Ally Commands
- `/ally <player>` - Send an ally request to a player
- `/ally list` - View your allies in a GUI
- `/ally accept <player>` - Accept an ally request
- `/ally deny <player>` - Deny an ally request

## Configuration

### config.yml
```yaml
starting-hearts: 10
min-hearts: 0
max-hearts: 20

# Heart gain/loss
hearts-gained-per-kill: 1
hearts-lost-per-death: 1
natural-death-loss: false

# PvP/PvE cycle
pvp-cycle:
  enabled: true
  pvp-duration: 2  # hours
  pve-duration: 2  # hours
```

### items.yml
```yaml
heart-item:
  enabled: true
  name: "&cHeart Fragment"
  material: RED_DYE
  cooldown: 30  # seconds

revive-item:
  enabled: true
  name: "&6Revival Totem"
  material: TOTEM_OF_UNDYING
```

## Permissions

- `lifesteal.admin` - Access to all admin commands
- `lifesteal.item.use.heart` - Permission to use heart items
- `lifesteal.item.use.revive` - Permission to use revival items
- `lifesteal.bypass.maxhearts` - Bypass the maximum hearts limit

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in the generated config files

## Requirements

- Spigot/Paper 1.17.1+
- Java 17+

## Support

If you encounter any issues or need help, please:
1. Check the configuration files
2. Verify permissions are set correctly
3. Check the console for error messages