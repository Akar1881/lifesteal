# LifeSteal Plugin

A Minecraft plugin that adds a heart-stealing mechanic to your server. Players can gain or lose hearts through PvP combat, and eliminated players can be revived using special items.
[![Logo](logo/logo.png)]

## Features

- Heart stealing system in PvP
- Natural death heart loss
- Configurable PvP/PvE cycle
- Custom items (Heart Fragment and Revival Totem)
- Action bar timer display
- Boss bar mode indicator
- Persistent player data
- Elimination system (Spectator or Ban mode)

## Commands

- `/lifesteal reload` - Reload plugin configuration
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Modify player hearts
- `/lifesteal revive <player>` - Revive an eliminated player
- `/lifesteal schedule` - Restart mode rotation

## Permissions

- `lifesteal.admin` - Access to all admin commands
- `lifesteal.togglebar` - Toggle action bar display
- `lifesteal.item.use.heart` - Use heart items
- `lifesteal.item.use.revive` - Use revival items
- `lifesteal.bypass.maxhearts` - Bypass maximum hearts limit

## Configuration

### config.yml
```yaml
starting-hearts: 10
min-hearts: 0
max-hearts: 20

# Heart gain/loss settings
hearts-gained-per-kill: 1
hearts-lost-per-death: 1
natural-death-loss: true

# PvP/PvE cycle settings
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

## Installation

1. Download the plugin JAR file
2. Place it in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/LifeSteal/config.yml`

## Requirements

- Spigot/Paper 1.17.1+
- Java 17+

## Support

For issues and feature requests, please create an issue on our GitHub repository.