# LifeSteal v1.3 - Advanced Heart Stealing Plugin

Transform your Minecraft server into an intense survival experience with this feature-rich LifeSteal plugin. Players must fight to survive, form alliances, and hunt bounties while managing their precious hearts in a dynamic world with shrinking borders.

## 🌟 Key Features

### 💗 Advanced Heart System
- Configurable starting, minimum, and maximum hearts (1-100)
- Gain hearts through PvP kills during PvP mode
- Lose hearts upon death (configurable for PvP and natural deaths)
- Heart Fragment items for extra hearts with cooldowns
- Maximum hearts limit with permission bypass
- Visual feedback and sound effects for heart changes

### ⚔️ Dynamic PvP/PvE Cycle
- Automatic mode switching with customizable durations
- Boss bar with rotating custom messages and progress
- Action bar timers with real-time countdown
- Sound effects and announcements on mode switches
- Admin control commands for scheduling
- Persistent timer data across server restarts

### 👥 Advanced Ally System
- GUI-based ally management with player heads
- Send/receive ally requests with confirmation dialogs
- Mutual protection during PvP mode
- Revival system for eliminated allies using special items
- Persistent ally relationships in database
- Sound effects and visual feedback

### 🎯 Enhanced Bounty System
- Random bounty targets during PvP mode
- Location broadcasting with configurable intervals
- Special rewards including Revival Hearts
- Rare bounties (10% chance) with enhanced rewards
- Logout penalties for bounty targets
- Minimum player requirement (configurable)
- Statistics tracking for bounties placed/claimed

### 🌍 Dynamic World Border
- Configurable initial size and center point
- Automatic shrinking with warning system
- Damage system for players outside border
- Anti-exploit measures and teleport prevention
- Multi-world support
- Real-time status monitoring and admin controls

### 🆕 First Join Queue System
- Dedicated queue world for new players
- Custom welcome messages and rule explanations
- Agreement confirmation requirement
- Safe spawn location finder with distance controls
- Background chunk pre-generation using Chunky
- Queue world music system with rotating music discs
- Progress tracking and reconnection handling

### 📊 Database & Performance
- MySQL and SQLite support with HikariCP connection pooling
- Efficient data management and caching
- Persistent storage for all game data
- Async operations for optimal performance
- Paper-specific optimizations
- Smart world management

### ⚡ Additional Features
- **Statistics System**: Comprehensive player statistics (K/D, hearts, bounties)
- **Custom Items**: Heart Fragments, Revival Totems, Revival Hearts
- **GUI Interfaces**: Interactive menus for ally and revival management
- **Chunky Integration**: Automatic chunk pre-generation for smooth gameplay
- **Boss Bar System**: Rotating messages with customizable durations
- **Sound System**: Configurable sounds for all major events
- **Permission System**: Granular permissions for all features

## 📋 Requirements
- **Server**: Spigot/Paper 1.17.1+
- **Java**: 17+
- **Optional**: [Chunky](https://modrinth.com/plugin/chunky) plugin for chunk pre-generation

## 💾 Quick Setup
1. Download and place in plugins folder
2. Start/restart server
3. Configure in `config.yml` and `items.yml`
4. (Optional) Install Chunky for enhanced first-join experience
5. Enjoy your enhanced survival experience!

## 🎮 Commands

### Player Commands
- `/ally <player>` - Send ally request
- `/ally list` - View allies (GUI)
- `/ally accept/deny <player>` - Manage requests

### Admin Commands  
- `/lifesteal help` - Comprehensive help menu
- `/lifesteal version` - Plugin info and server status
- `/lifesteal hearts <set|add|remove> <player> <amount>` - Manage hearts
- `/lifesteal revive <player>` - Revive eliminated players
- `/lifesteal schedule <set|add|subtract|info>` - Control PvP/PvE timing
- `/lifesteal border <info|reset|shrink>` - Manage world border
- `/lifesteal reload` - Reload configuration

## 🔒 Permissions
- `lifesteal.admin` - All admin commands
- `lifesteal.ally` - Ally system access
- `lifesteal.item.use.heart` - Use heart items
- `lifesteal.item.use.revive` - Use revival items
- `lifesteal.bypass.maxhearts` - Bypass heart limits

## 🔄 What's New in v1.3

### Major Updates:
- **Enhanced Command System**: New comprehensive help and version commands
- **Improved Admin Tools**: Better feedback, status information, and error handling
- **Advanced Statistics**: Detailed player statistics tracking (K/D, hearts, bounties)
- **Performance Optimizations**: Better database handling, caching, and async operations
- **Bug Fixes**: Resolved edge cases and improved overall stability

### Technical Improvements:
- Updated for latest Minecraft versions (1.17.1+)
- Improved configuration validation and error handling
- Enhanced database connection management with HikariCP
- Optimized chunk loading and world management
- Better integration with Paper-specific features

### Quality of Life:
- More informative command feedback with before/after values
- Better error messages and troubleshooting information
- Enhanced visual feedback for all player actions
- Improved documentation and help system

## 🎯 Perfect For:
- **SMPs**: Enhanced survival multiplayer with social features
- **PvP Servers**: Competitive heart-stealing gameplay
- **Survival Challenges**: Hardcore survival with real consequences
- **Community Servers**: Social features with ally system and statistics
- **Competitive Gameplay**: Leaderboards and achievement tracking

## 📈 Server Compatibility
Tested and optimized for servers of all sizes:
- **Small**: 5-20 players
- **Medium**: 20-50 players
- **Large**: 50+ players

Performance scales well with proper configuration.

## 🔧 Configuration Highlights
- **Heart System**: Fully customizable heart mechanics
- **PvP Cycles**: Flexible timing and mode switching
- **World Border**: Dynamic shrinking with warnings
- **Database**: Choice between MySQL and SQLite
- **First Join**: Complete new player onboarding system
- **Bounty System**: Configurable rewards and penalties
- **Messages**: Fully customizable text and colors

## 🌐 Support
- **Discord**: [Join our Community](https://discord.gg/K6tkSQcPfA)
- **Issues**: Report bugs and request features
- **Documentation**: Comprehensive setup guides

## 🏆 Why Choose LifeSteal v1.3?
- **Most Feature-Complete**: Comprehensive LifeSteal experience
- **Highly Optimized**: Built for performance and stability
- **Actively Maintained**: Regular updates and bug fixes
- **Community Driven**: Features requested by server owners
- **Professional Quality**: Production-ready with extensive testing

Transform your server into an epic survival battleground where every heart matters!

---

**Download now and give your players the ultimate LifeSteal experience!**