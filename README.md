# LifeSteal Plugin

A comprehensive LifeSteal plugin for Minecraft servers that adds an exciting heart-stealing mechanic along with PvP/PvE cycles, ally system, bounty hunting, world border, and more.

## 🌟 Key Features

### 💗 Heart System
- Configurable starting, minimum, and maximum hearts
- Gain hearts by killing other players
- Lose hearts upon death (configurable for PvP and natural deaths)
- Heart Fragment items for gaining extra hearts
- Maximum hearts limit with permission bypass

### ⚔️ PvP/PvE Cycle System
- Automatic switching between PvP and PvE modes
- Customizable durations for each mode
- Boss bar with rotating messages
- Action bar timers
- Sound effects and announcements
- Admin commands for cycle control

### 👥 Advanced Ally System
- Send and receive ally requests
- GUI-based ally management
- Mutual protection during PvP
- Revival system for eliminated allies
- Persistent ally data storage

### 🎯 Bounty System
- Random bounty targets during PvP mode
- Location broadcasting with configurable intervals
- Special rewards including Revival Hearts
- Rare bounties with enhanced rewards
- Logout penalties for targets
- Minimum player requirement for activation

### 🌍 Dynamic World Border
- Configurable initial size and center point
- Automatic shrinking with customizable intervals
- Warning system for border shrinks
- Damage system for players outside border
- Anti-exploit measures
- Multi-world support

### 🆕 First Join System
- Queue world for new players
- Custom welcome messages
- Agreement confirmation requirement
- Safe spawn location finder
- Background chunk pre-generation
- Queue world music system
- Progress tracking for chunk generation

### 📊 Database Support
- MySQL and SQLite support
- Efficient data management
- Persistent storage for:
  - Player hearts
  - Ally relationships
  - World border data
  - Queue states
  - Cycle timer data

### ⚡ Performance Optimizations
- Async operations where possible
- Paper-specific optimizations
- Efficient chunk loading
- Optimized world border handling
- Smart queue world management

## 📋 Requirements

- Server: Spigot/Paper 1.17.1+
- Java: 17+
- Optional: [Chunky](https://modrinth.com/plugin/chunky) plugin for chunk pre-generation

## 💾 Installation

1. Download the latest release
2. Place the JAR file in your server's `plugins` folder
3. Start/restart your server
4. Configure the plugin in `config.yml`

## 🔧 Configuration

The plugin is highly configurable through multiple files:

### config.yml
- Heart system settings
- PvP/PvE cycle configuration
- World border settings
- First join system
- Bounty system
- Database configuration
- Messages and sounds

### items.yml
- Heart Fragment configuration
- Revival Totem settings
- Revival Heart properties

## 📜 Commands

### General Commands
- `/ally <player>` - Send an ally request
- `/ally list` - View your allies (GUI)
- `/ally accept <player>` - Accept an ally request
- `/ally deny <player>` - Deny an ally request
- `/ally remove <player>` - Remove an ally

### Admin Commands
- `/lifesteal reload` - Reload configuration
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Manage player hearts
- `/lifesteal revive <player>` - Revive a player
- `/lifesteal schedule <set|add|subtract|info>` - Control PvP/PvE cycle
- `/lifesteal togglebar` - Toggle boss bar visibility
- `/lifesteal border <info|reset|shrink|toggle>` - Manage world border

## 🔒 Permissions

- `lifesteal.admin` - Access to all admin commands
- `lifesteal.item.use.heart` - Allow using heart items
- `lifesteal.item.use.revive` - Allow using revive items
- `lifesteal.bypass.maxhearts` - Bypass maximum hearts limit

## 🌐 Support

- Issues: [GitHub Issues](https://github.com/Akar1881/lifesteal/issues)
- Discord: [Join our Community](https://discord.gg/K6tkSQcPfA)

## 📜 License

This project is licensed under the GNU General Public License v3.0.