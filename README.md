# LifeSteal Plugin

![LifeSteal Logo](logo/logo.png)

[![Version](https://img.shields.io/badge/version-1.0--VLTS-blue.svg)](https://github.com/Akar1881/lifesteal/releases)
[![Minecraft](https://img.shields.io/badge/minecraft-1.17.1+-green.svg)](https://www.spigotmc.org/)
[![License](https://img.shields.io/badge/license-GPL--3.0-red.svg)](LICENSE)
[![Discord](https://img.shields.io/discord/YOUR_DISCORD_ID?color=7289da&logo=discord&logoColor=white)](https://discord.gg/K6tkSQcPfA)

A comprehensive LifeSteal plugin that brings intense PvP mechanics to your Minecraft server. Steal hearts from your enemies, form alliances, and survive in a world where every death matters.

## 🌟 Features

### 💖 Heart System
- **Dynamic Health**: Players start with configurable hearts (default: 10)
- **Heart Stealing**: Kill players to steal their hearts
- **Heart Items**: Craft or find heart fragments to gain extra hearts
- **Elimination**: Players with 0 hearts are eliminated (spectator mode or banned)
- **Revival System**: Revive eliminated allies with special items

### ⚔️ PvP/PvE Cycle
- **Timed Cycles**: Automatic switching between PvP and PvE modes
- **Configurable Duration**: Set custom durations for each mode
- **Boss Bar Display**: Real-time countdown with customizable messages
- **Action Bar Timer**: Shows remaining time in current mode
- **Sound Effects**: Audio cues for mode switches

### 🤝 Alliance System
- **Ally Management**: Form alliances with other players
- **Clickable Requests**: Accept/deny ally requests with interactive messages
- **Ally Protection**: Allies can revive each other
- **GUI Interface**: User-friendly inventory-based ally management
- **Request Timeout**: Automatic cleanup of expired requests

### 🎯 Bounty System
- **Dynamic Bounties**: Random players get bounties during PvP mode
- **Location Broadcasting**: Periodic location updates for bounty targets
- **Rare Bounties**: Special bounties with unique rewards (Revival Hearts)
- **Survival Rewards**: Bounty targets get rewards for surviving
- **Quit Penalties**: Heart loss for logging out with active bounty

### 🌍 World Border
- **Shrinking Border**: Configurable border that shrinks over time
- **Multiple Worlds**: Support for multiple world borders
- **Warning System**: Timed warnings before border shrinks
- **Damage System**: Configurable damage for players outside border
- **Admin Controls**: Manual border management commands

### 📦 Custom Items
- **Heart Fragments**: Consumable items that grant extra hearts
- **Revival Totems**: Items to revive eliminated players
- **Revival Hearts**: Rare items from bounties for ally revival
- **Custom Recipes**: Configurable crafting recipes
- **Cooldown System**: Prevent item spam with configurable cooldowns

### 🗄️ Database Support
- **SQLite**: Built-in SQLite support (default)
- **MySQL**: Optional MySQL support for larger servers
- **Data Persistence**: All player data, allies, and world border state saved
- **Automatic Migration**: Seamless database upgrades

## 📋 Requirements

- **Minecraft Version**: 1.17.1 or higher
- **Server Software**: Spigot, Paper, or compatible forks
- **Java Version**: Java 17 or higher
- **RAM**: Minimum 1GB (2GB+ recommended for larger servers)

## 🚀 Installation

1. **Download** the latest release from [GitHub Releases](https://github.com/Akar1881/lifesteal/releases)
2. **Place** the JAR file in your server's `plugins` folder
3. **Start** your server to generate configuration files
4. **Configure** the plugin by editing `config.yml` and `items.yml`
5. **Restart** your server to apply changes

## ⚙️ Configuration

### Main Configuration (`config.yml`)

```yaml
# Heart System
starting-hearts: 10      # Hearts players start with
min-hearts: 0           # Minimum hearts before elimination
max-hearts: 20          # Maximum hearts a player can have
hearts-gained-per-kill: 1   # Hearts gained per kill
hearts-lost-per-death: 1    # Hearts lost per death
natural-death-loss: true    # Lose hearts from natural deaths

# PvP/PvE Cycle
pvp-cycle:
  enabled: true
  pvp-duration: 2       # Hours of PvP mode
  pve-duration: 2       # Hours of PvE mode

# World Border
world-border:
  enabled: true
  initial-size: 1000    # Starting border size
  shrink:
    enabled: true
    interval: 90        # Minutes between shrinks
    amount: 100         # Blocks to shrink each time
    min-size: 500       # Minimum border size

# Bounty System
bounty:
  enabled: true
  min-players: 10       # Minimum players for bounties
  reward-hearts: 1      # Hearts rewarded for bounty kills
```

### Items Configuration (`items.yml`)

```yaml
heart-item:
  enabled: true
  name: "&cHeart Fragment"
  material: RED_DYE
  cooldown: 30          # Seconds between uses
  
revive-item:
  enabled: true
  name: "&6Revival Totem"
  material: TOTEM_OF_UNDYING
  recipe:               # Crafting recipe
    shaped: false
    ingredients:
      - NETHER_STAR
      - TOTEM_OF_UNDYING
      - DIAMOND
```

## 🎮 Commands

### Player Commands
- `/ally <player>` - Send an ally request
- `/ally list` - View your allies
- `/ally accept <player>` - Accept an ally request
- `/ally deny <player>` - Deny an ally request
- `/shrink` - Show time until next border shrink

### Admin Commands
- `/lifesteal reload` - Reload plugin configuration
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Manage player hearts
- `/lifesteal revive <player>` - Revive an eliminated player
- `/lifesteal schedule <set|add|subtract|info>` - Control PvP/PvE cycle
- `/lifesteal togglebar` - Toggle boss bar visibility
- `/lifesteal border <info|reset|shrink|toggle>` - Manage world border
- `/lifesteal bounty <on|off>` - Toggle bounty system

## 🔐 Permissions

### Player Permissions
- `lifesteal.item.use.heart` - Use heart items (default: true)
- `lifesteal.item.use.revive` - Use revival items (default: true)

### Admin Permissions
- `lifesteal.admin` - Access to all admin commands (default: op)
- `lifesteal.bypass.maxhearts` - Bypass maximum hearts limit (default: op)

## 🎯 Game Mechanics

### Heart Stealing
When a player kills another player:
1. Victim loses configured hearts (default: 1)
2. Killer gains the same amount of hearts
3. If victim reaches 0 hearts, they are eliminated
4. Eliminated players enter spectator mode or get banned

### Alliance System
- Players can form alliances for mutual protection
- Allies can revive each other using revival items
- Alliance requests expire after 1 minute
- Removing an ally notifies both players

### Bounty Mechanics
- Random players get bounties during PvP mode
- Bounty locations are broadcast every 10 minutes
- Killing a bounty target rewards extra hearts
- Rare bounties (0.1% chance) reward Revival Hearts
- Bounty targets get rewards for surviving the PvP cycle

### World Border Shrinking
- Border shrinks at configured intervals
- Players receive warnings before shrinking
- Being outside the border causes damage
- Border stops shrinking at minimum size

## 🛠️ Database Setup

### SQLite (Default)
No additional setup required. The plugin automatically creates a SQLite database in the plugin folder.

### MySQL (Optional)
1. Create a MySQL database
2. Update `config.yml`:
```yaml
storage:
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: lifesteal
    user: your_username
    password: your_password
```

## 🎨 Customization

### Custom Items
Create custom items by editing `items.yml`:
- Change materials, names, and lore
- Add custom model data for resource packs
- Configure crafting recipes
- Set cooldowns and effects

### Boss Bar Messages
Customize the rotating boss bar messages:
```yaml
boss-bar:
  messages:
    - text: "&6&lYour Server Name"
      duration: "30s"
    - text: "&a&lLifeSteal Season 1"
      duration: "30s"
```

### Sounds and Effects
Configure sounds for various events:
```yaml
sounds:
  heart-gain: "entity.player.levelup"
  heart-loss: "entity.player.hurt"
```

## 🐛 Troubleshooting

### Common Issues

**Plugin not loading:**
- Check server version compatibility (1.17.1+)
- Verify Java version (17+)
- Check console for error messages

**Database connection errors:**
- Verify MySQL credentials if using MySQL
- Check file permissions for SQLite
- Ensure database exists and is accessible

**Hearts not updating:**
- Check if player has required permissions
- Verify configuration values are valid
- Restart server after config changes

**World border not working:**
- Ensure world names in config match actual world names
- Check if world border is enabled in config
- Verify shrink settings are properly configured

### Getting Help

1. **Check the logs** - Look for error messages in your server console
2. **Verify configuration** - Ensure all config values are valid
3. **Test permissions** - Make sure players have required permissions
4. **Join our Discord** - Get help from the community: [Discord Server](https://discord.gg/K6tkSQcPfA)
5. **Report bugs** - Create an issue on [GitHub](https://github.com/Akar1881/lifesteal/issues)

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch
3. **Make** your changes
4. **Test** thoroughly
5. **Submit** a pull request

### Development Setup
1. Clone the repository
2. Import into your IDE (IntelliJ IDEA recommended)
3. Set up a test server with Spigot 1.17.1+
4. Build with Maven: `mvn clean package`

## 📄 License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Plugin Developer**: [Akar1881](https://github.com/Akar1881)
- **Community Contributors**: Thanks to all who have contributed code, ideas, and feedback
- **Beta Testers**: Special thanks to our beta testing community

## 📞 Support

- **Discord Community**: [Join our Discord](https://discord.gg/K6tkSQcPfA)
- **Bug Reports**: [GitHub Issues](https://github.com/Akar1881/lifesteal/issues)
- **Feature Requests**: [GitHub Discussions](https://github.com/Akar1881/lifesteal/discussions)

---

**Made with ❤️ for the Minecraft community**

*LifeSteal Plugin - Where every heart matters and every death counts.*