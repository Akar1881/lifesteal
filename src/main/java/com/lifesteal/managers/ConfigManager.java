package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfigManager {
    private final LifeSteal plugin;
    private FileConfiguration config;
    private FileConfiguration itemsConfig;
    private File configFile;
    private File itemsFile;

    public ConfigManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    public void loadConfigs() {
        // Main config
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        configFile = new File(plugin.getDataFolder(), "config.yml");

        // Items config
        itemsFile = new File(plugin.getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            plugin.saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    public void saveConfigs() {
        try {
            config.save(configFile);
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getStartingHearts() {
        return config.getInt("starting-hearts", 10);
    }

    public int getMinHearts() {
        return config.getInt("min-hearts", 0);
    }

    public int getMaxHearts() {
        return config.getInt("max-hearts", 20);
    }

    public int getHeartsGainedPerKill() {
        return config.getInt("hearts-gained-per-kill", 1);
    }

    public int getHeartsLostPerDeath() {
        return config.getInt("hearts-lost-per-death", 1);
    }

    public boolean isNaturalDeathLoss() {
        return config.getBoolean("natural-death-loss", true);
    }

    public boolean isPvPCycleEnabled() {
        return config.getBoolean("pvp-cycle.enabled", true);
    }

    public int getPvPDuration() {
        return config.getInt("pvp-cycle.pvp-duration", 2);
    }

    public int getPvEDuration() {
        return config.getInt("pvp-cycle.pve-duration", 2);
    }

    public String getEliminationMode() {
        return config.getString("elimination.mode", "spectator");
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }
    
    public boolean isBossBarEnabled() {
        return getConfig().getBoolean("boss-bar.enabled", true);
    }
    
    // World Border Configuration Methods
    
    public boolean isWorldBorderEnabled() {
        return config.getBoolean("world-border.enabled", false);
    }
    
    public List<String> getWorldBorderWorlds() {
        return config.getStringList("world-border.worlds");
    }
    
    public double getInitialBorderSize() {
        return config.getDouble("world-border.initial-size", 5000);
    }
    
    public boolean useWorldSpawnAsCenter() {
        return config.getBoolean("world-border.center.use-world-spawn", true);
    }
    
    public double getWorldBorderCenterX() {
        return config.getDouble("world-border.center.x", 0);
    }
    
    public double getWorldBorderCenterZ() {
        return config.getDouble("world-border.center.z", 0);
    }
    
    public boolean isWorldBorderShrinkEnabled() {
        return config.getBoolean("world-border.shrink.enabled", false);
    }
    
    public int getWorldBorderShrinkInterval() {
        return config.getInt("world-border.shrink.interval", 30);
    }
    
    public double getWorldBorderShrinkAmount() {
        return config.getDouble("world-border.shrink.amount", 100);
    }
    
    public double getWorldBorderMinSize() {
        return config.getDouble("world-border.shrink.min-size", 500);
    }
    
    public int getWorldBorderWarningTime() {
        return config.getInt("world-border.shrink.warning-time", 60);
    }
    
    public int getWorldBorderWarningDistance() {
        return config.getInt("world-border.shrink.warning-distance", 50);
    }
    
    public double getWorldBorderDamageAmount() {
        return config.getDouble("world-border.damage.amount", 1.0);
    }
    
    public double getWorldBorderDamageBuffer() {
        return config.getDouble("world-border.damage.buffer", 5.0);
    }
    
    public String getWorldBorderShrinkingMessage() {
        return config.getString("world-border.messages.border-shrinking", 
                "&c&lWARNING! &fThe world border is shrinking in &e%time% &fseconds!");
    }
    
    public String getWorldBorderShrunkMessage() {
        return config.getString("world-border.messages.border-shrunk", 
                "&c&lBORDER SHRUNK! &fThe world border has shrunk to &e%size% &fblocks!");
    }
    
    public String getWorldBorderOutsideMessage() {
        return config.getString("world-border.messages.outside-border", 
                "&c&lWARNING! &fYou are outside the world border! Return immediately or take damage!");
    }

    public boolean isFirstJoinEnabled() {
        return config.getBoolean("first-join.enabled", true);
    }

    public List<String> getFirstJoinMessages() {
        return config.getStringList("first-join.messages");
    }

    public String getFirstJoinConfirmMessage() {
        return config.getString("first-join.confirm-message", "&aCongratulations! Welcome to the server!");
    }

    public String getFirstJoinTeleportMessage() {
        return config.getString("first-join.teleport-message", "&aYou have been teleported to a safe location. Good luck!");
    }
    
    public String getFirstJoinKickMessage() {
        return config.getString("first-join.reconnect-kick-message", "&aPlease reconnect for optimal performance");
    }
    
    public boolean shouldKickAfterFirstJoin() {
        return config.getBoolean("first-join.kick-after-teleport", false);
    }
    
    public int getSafeLocationMinDistance() {
        return config.getInt("safe-location.min-distance", 700);
    }
    
    public int getSafeLocationMaxDistance() {
        return config.getInt("safe-location.max-distance", 1300);
    }
    
    public int getSafeLocationMaxAttempts() {
        return config.getInt("safe-location.max-attempts", 20);
    }
}