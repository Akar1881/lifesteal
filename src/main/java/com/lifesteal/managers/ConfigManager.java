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
        
        // Reload queue world configuration if first join is enabled
        if (isFirstJoinEnabled() && plugin.getFirstJoinManager() != null) {
            plugin.getFirstJoinManager().getQueueWorld().reload();
        }
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
        return config.contains("starting-hearts") ? config.getInt("starting-hearts") : 10;
    }

    public int getMinHearts() {
        return config.contains("min-hearts") ? config.getInt("min-hearts") : 0;
    }

    public int getMaxHearts() {
        return config.contains("max-hearts") ? config.getInt("max-hearts") : 20;
    }

    public int getHeartsGainedPerKill() {
        return config.contains("hearts-gained-per-kill") ? config.getInt("hearts-gained-per-kill") : 1;
    }

    public int getHeartsLostPerDeath() {
        return config.contains("hearts-lost-per-death") ? config.getInt("hearts-lost-per-death") : 1;
    }

    public boolean isNaturalDeathLoss() {
        return config.contains("natural-death-loss") ? config.getBoolean("natural-death-loss") : true;
    }

    public boolean isPvPCycleEnabled() {
        return config.contains("pvp-cycle.enabled") ? config.getBoolean("pvp-cycle.enabled") : true;
    }

    public int getPvPDuration() {
        return config.contains("pvp-cycle.pvp-duration") ? config.getInt("pvp-cycle.pvp-duration") : 2;
    }

    public int getPvEDuration() {
        return config.contains("pvp-cycle.pve-duration") ? config.getInt("pvp-cycle.pve-duration") : 2;
    }

    public String getEliminationMode() {
        return config.contains("elimination.mode") ? config.getString("elimination.mode") : "spectator";
    }

    public FileConfiguration getItemsConfig() {
        return itemsConfig;
    }
    
    public boolean isBossBarEnabled() {
        return config.contains("boss-bar.enabled") ? config.getBoolean("boss-bar.enabled") : true;
    }
    
    // World Border Configuration Methods
    
    public boolean isWorldBorderEnabled() {
        return config.contains("world-border.enabled") ? config.getBoolean("world-border.enabled") : false;
    }
    
    public List<String> getWorldBorderWorlds() {
        return config.getStringList("world-border.worlds");
        // No fallback needed for getStringList as it returns an empty list if the path doesn't exist
    }
    
    public double getInitialBorderSize() {
        return config.contains("world-border.initial-size") ? config.getDouble("world-border.initial-size") : 5000;
    }
    
    public boolean useWorldSpawnAsCenter() {
        return config.contains("world-border.center.use-world-spawn") ? config.getBoolean("world-border.center.use-world-spawn") : true;
    }
    
    public double getWorldBorderCenterX() {
        return config.contains("world-border.center.x") ? config.getDouble("world-border.center.x") : 0;
    }
    
    public double getWorldBorderCenterZ() {
        return config.contains("world-border.center.z") ? config.getDouble("world-border.center.z") : 0;
    }
    
    public boolean isWorldBorderShrinkEnabled() {
        return config.contains("world-border.shrink.enabled") ? config.getBoolean("world-border.shrink.enabled") : false;
    }
    
    public int getWorldBorderShrinkInterval() {
        return config.contains("world-border.shrink.interval") ? config.getInt("world-border.shrink.interval") : 30;
    }
    
    public double getWorldBorderShrinkAmount() {
        return config.contains("world-border.shrink.amount") ? config.getDouble("world-border.shrink.amount") : 100;
    }
    
    public double getWorldBorderMinSize() {
        return config.contains("world-border.shrink.min-size") ? config.getDouble("world-border.shrink.min-size") : 500;
    }
    
    public int getWorldBorderWarningTime() {
        return config.contains("world-border.shrink.warning-time") ? config.getInt("world-border.shrink.warning-time") : 60;
    }
    
    public int getWorldBorderWarningDistance() {
        return config.contains("world-border.shrink.warning-distance") ? config.getInt("world-border.shrink.warning-distance") : 50;
    }
    
    public double getWorldBorderDamageAmount() {
        return config.contains("world-border.damage.amount") ? config.getDouble("world-border.damage.amount") : 1.0;
    }
    
    public double getWorldBorderDamageBuffer() {
        return config.contains("world-border.damage.buffer") ? config.getDouble("world-border.damage.buffer") : 5.0;
    }
    
    public String getWorldBorderShrinkingMessage() {
        return config.contains("world-border.messages.border-shrinking") ? 
                config.getString("world-border.messages.border-shrinking") : 
                "&c&lWARNING! &fThe world border is shrinking in &e%time% &fseconds!";
    }
    
    public String getWorldBorderShrunkMessage() {
        return config.contains("world-border.messages.border-shrunk") ? 
                config.getString("world-border.messages.border-shrunk") : 
                "&c&lBORDER SHRUNK! &fThe world border has shrunk to &e%size% &fblocks!";
    }
    
    public String getWorldBorderOutsideMessage() {
        return config.contains("world-border.messages.outside-border") ? 
                config.getString("world-border.messages.outside-border") : 
                "&c&lWARNING! &fYou are outside the world border! Return immediately or take damage!";
    }

    public boolean isFirstJoinEnabled() {
        return config.contains("first-join.enabled") ? config.getBoolean("first-join.enabled") : true;
    }

    public List<String> getFirstJoinMessages() {
        return config.getStringList("first-join.messages");
        // No fallback needed for getStringList as it returns an empty list if the path doesn't exist
    }

    public String getFirstJoinConfirmMessage() {
        return config.contains("first-join.confirm-message") ? 
                config.getString("first-join.confirm-message") : 
                "&aCongratulations! Welcome to the server!";
    }

    public String getFirstJoinTeleportMessage() {
        return config.contains("first-join.teleport-message") ? 
                config.getString("first-join.teleport-message") : 
                "&aYou have been teleported to a safe location. Good luck!";
    }
    
    public String getFirstJoinKickMessage() {
        return config.contains("first-join.reconnect-kick-message") ? 
                config.getString("first-join.reconnect-kick-message") : 
                "&aPlease reconnect for optimal performance";
    }
    
    public boolean shouldKickAfterFirstJoin() {
        return config.contains("first-join.kick-after-teleport") ? 
                config.getBoolean("first-join.kick-after-teleport") : false;
    }
    
    public String getReconnectMessage() {
        return config.contains("first-join.reconnect-message") ? 
                config.getString("first-join.reconnect-message") : 
                "&6Welcome back! You were previously in the queue world.";
    }
    
    public String getReconnectConfirmMessage() {
        return config.contains("first-join.reconnect-confirm-message") ? 
                config.getString("first-join.reconnect-confirm-message") : 
                "&eType &6CONFIRM &ein chat to continue.";
    }
    
    public String getReconnectAlreadyConfirmedMessage() {
        return config.contains("first-join.reconnect-already-confirmed") ? 
                config.getString("first-join.reconnect-already-confirmed") : 
                "&aYou have already confirmed. Waiting for chunk generation to complete.";
    }
    
    public String getReconnectProgressMessage() {
        return config.contains("first-join.reconnect-progress") ? 
                config.getString("first-join.reconnect-progress") : 
                "&6Current progress: &e%progress%";
    }
    
    public String getHelpMessage() {
        return config.contains("first-join.help-message") ? 
                config.getString("first-join.help-message") : 
                "&6&lHELP: &eType &6CONFIRM &ein chat to continue.";
    }
    
    public String getHelpExplanationMessage() {
        return config.contains("first-join.help-explanation") ? 
                config.getString("first-join.help-explanation") : 
                "&eYou are currently in the queue world waiting for chunk generation.";
    }
    
    public String getHelpTeleportMessage() {
        return config.contains("first-join.help-teleport") ? 
                config.getString("first-join.help-teleport") : 
                "&eOnce you confirm, you'll be teleported to the main world when chunks are ready.";
    }
    
    public int getSafeLocationMinDistance() {
        return config.contains("safe-location.min-distance") ? 
                config.getInt("safe-location.min-distance") : 700;
    }
    
    public int getSafeLocationMaxDistance() {
        return config.contains("safe-location.max-distance") ? 
                config.getInt("safe-location.max-distance") : 1300;
    }
    
    public int getSafeLocationMaxAttempts() {
        return config.contains("safe-location.max-attempts") ? 
                config.getInt("safe-location.max-attempts") : 20;
    }
    
    public boolean isChunkPreGenerationEnabled() {
        return config.contains("chunk-pregeneration.enabled") ? 
                config.getBoolean("chunk-pregeneration.enabled") : true;
    }

    public int getChunkPreGenerationRadius() {
        return config.contains("chunk-pregeneration.radius") ? 
                config.getInt("chunk-pregeneration.radius") : 1000;
    }
    
    public boolean isQueueMusicEnabled() {
        return config.contains("first-join.queue-music") ? 
                config.getBoolean("first-join.queue-music") : true;
    }
}