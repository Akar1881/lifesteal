# LifeSteal Plugin v1.3

A comprehensive LifeSteal plugin for Minecraft servers that adds an exciting heart-stealing mechanic along with advanced features including PvP/PvE cycles, ally system, bounty hunting, world border management, chunk pre-generation, and more.

## 🌟 Key Features

### 💗 Advanced Heart System
- Configurable starting, minimum, and maximum hearts
- Gain hearts by killing other players during PvP mode
- Lose hearts upon death (configurable for PvP and natural deaths)
- Heart Fragment items for gaining extra hearts with cooldowns
- Maximum hearts limit with permission bypass
- Enhanced heart management with visual feedback

### ⚔️ Dynamic PvP/PvE Cycle System
- Automatic switching between PvP and PvE modes
- Customizable durations for each mode (hours)
- Boss bar with rotating custom messages
- Action bar timers with real-time countdown
- Sound effects and announcements on mode switches
- Admin commands for cycle control and scheduling
- Persistent timer data across server restarts

### 👥 Advanced Ally System
- Send and receive ally requests with GUI management
- Interactive ally list with player head displays
- Mutual protection during PvP mode
- Revival system for eliminated allies using special items
- Persistent ally data storage in database
- Confirmation dialogs for ally removal
- Sound effects for ally interactions

### 🎯 Enhanced Bounty System
- Random bounty targets during PvP mode
- Location broadcasting with configurable intervals
- Special rewards including Revival Hearts
- Rare bounties (10% chance) with enhanced rewards
- Logout penalties for bounty targets
- Minimum player requirement for activation
- Statistics tracking for bounties placed/claimed

### 🌍 Dynamic World Border Management
- Configurable initial size and center point
- Automatic shrinking with customizable intervals
- Advanced warning system for border shrinks
- Damage system for players outside border
- Anti-exploit measures and teleport prevention
- Multi-world support with per-world configuration
- Real-time border status monitoring

### 🆕 First Join Queue System
- Dedicated queue world for new players
- Custom welcome messages with rule explanations
- Agreement confirmation requirement
- Safe spawn location finder with distance controls
- Background chunk pre-generation using Chunky
- Queue world music system with rotating discs
- Progress tracking for chunk generation
- Reconnection handling for interrupted sessions

### 📊 Database & Performance
- MySQL and SQLite support with connection pooling
- Efficient data management with HikariCP
- Persistent storage for all player data:
  - Player hearts and elimination status
  - Ally relationships and pending requests
  - World border data and shrink schedules
  - Queue states and confirmation status
  - Cycle timer data and mode history
- Async operations where possible
- Paper-specific optimizations
- Smart caching system for improved performance

### ⚡ Advanced Features
- **Statistics System**: Track kills, deaths, KDR, hearts stolen/lost, bounties
- **Custom Items**: Heart Fragments, Revival Totems, Revival Hearts
- **GUI Interfaces**: Ally management, revival selection
- **Chunky Integration**: Automatic chunk pre-generation for new players
- **Boss Bar System**: Rotating messages with customizable durations
- **Sound System**: Configurable sounds for all major events
- **Permission System**: Granular permissions for all features
- **Command System**: Comprehensive admin and player commands

## 📋 Requirements

- **Server**: Spigot/Paper 1.17.1+
- **Java**: 17+
- **Optional**: [Chunky](https://modrinth.com/plugin/chunky) plugin for chunk pre-generation
- **Database**: MySQL 8.0+ or SQLite 3.0+ (auto-configured)

## 💾 Installation

1. Download the latest release from [Modrinth](https://modrinth.com/plugin/lifesteal) or [GitHub](https://github.com/Akar1881/lifesteal)
2. Place the JAR file in your server's `plugins` folder
3. Start/restart your server
4. Configure the plugin in `config.yml` and `items.yml`
5. (Optional) Install Chunky for chunk pre-generation features

## 🔧 Configuration

The plugin is highly configurable through multiple files:

### config.yml
- Heart system settings (starting, min, max hearts)
- PvP/PvE cycle configuration (durations, commands)
- World border settings (size, shrinking, damage)
- First join system (messages, safe locations)
- Bounty system (rewards, penalties, intervals)
- Database configuration (MySQL/SQLite)
- Boss bar and action bar settings
- Messages, sounds, and elimination modes

### items.yml
- Heart Fragment configuration (cooldowns, effects)
- Revival Totem settings (crafting recipes)
- Revival Heart properties (rare bounty rewards)
- Custom model data and enchantment effects

## 📜 Commands

### General Commands
- `/ally <player>` - Send an ally request
- `/ally list` - View your allies in GUI
- `/ally accept <player>` - Accept an ally request
- `/ally deny <player>` - Deny an ally request

### Admin Commands
- `/lifesteal help` - Show comprehensive help menu
- `/lifesteal version` - Display plugin info and server status
- `/lifesteal reload` - Reload all configuration files
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Manage player hearts
- `/lifesteal revive <player>` - Revive an eliminated player
- `/lifesteal schedule <set|add|subtract|info>` - Control PvP/PvE cycle timing
- `/lifesteal togglebar` - Toggle boss bar visibility
- `/lifesteal border <info|reset|shrink|toggle>` - Manage world border

## 🔒 Permissions

- `lifesteal.admin` - Access to all admin commands
- `lifesteal.ally` - Use ally system commands
- `lifesteal.item.use.heart` - Allow using heart items
- `lifesteal.item.use.revive` - Allow using revive items
- `lifesteal.bypass.maxhearts` - Bypass maximum hearts limit

## 🎮 Perfect For

- **SMPs**: Enhanced survival multiplayer experience
- **PvP Servers**: Competitive heart-stealing gameplay
- **Survival Challenges**: Hardcore survival with consequences
- **Community Servers**: Social features with ally system
- **Competitive Gameplay**: Statistics and leaderboards

## 🔄 What's New in v1.3

### Major Features Added:
- **Enhanced Command System**: New help command with comprehensive information
- **Improved Admin Tools**: Better feedback and status information
- **Advanced Statistics**: Detailed player statistics tracking
- **Performance Optimizations**: Better database handling and caching
- **Bug Fixes**: Resolved various edge cases and improved stability

### Technical Improvements:
- Updated to support latest Minecraft versions
- Improved error handling and logging
- Better configuration validation
- Enhanced database connection management
- Optimized chunk loading and world management

## 🌐 Support & Community

- **Issues**: [GitHub Issues](https://github.com/Akar1881/lifesteal/issues)
- **Discord**: [Join our Community](https://discord.gg/K6tkSQcPfA)
- **Documentation**: [Wiki](https://github.com/Akar1881/lifesteal/wiki)
- **Updates**: Follow on [Modrinth](https://modrinth.com/plugin/lifesteal)

## 📈 Server Compatibility

This plugin has been tested and optimized for:
- **Small Servers**: 5-20 players
- **Medium Servers**: 20-50 players  
- **Large Servers**: 50+ players

Performance scales well with proper database configuration and server resources.

## 📜 License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

## 🙏 Credits

- **Developer**: Akar1881
- **Contributors**: Community feedback and testing
- **Special Thanks**: Paper team, Chunky developers, and the Minecraft modding community

---

**Make your server unique with this comprehensive LifeSteal plugin!**