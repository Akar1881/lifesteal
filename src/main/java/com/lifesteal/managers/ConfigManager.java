package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

public class ConfigManager {
    private final LifeSteal plugin;
    private FileConfiguration config;
    private FileConfiguration itemsConfig;
    private File configFile;
    private File itemsFile;

    // Default values and bounds
    private static final int MIN_HEARTS = 1;
    private static final int MAX_HEARTS = 100;
    private static final int MIN_STARTING_HEARTS = 1;
    private static final int MAX_STARTING_HEARTS = 20;
    private static final int MIN_HEARTS_PER_KILL = 1;
    private static final int MAX_HEARTS_PER_KILL = 10;
    private static final int MIN_HEARTS_PER_DEATH = 1;
    private static final int MAX_HEARTS_PER_DEATH = 10;
    private static final int MIN_PVP_DURATION = 1;
    private static final int MAX_PVP_DURATION = 24;
    private static final int MIN_PVE_DURATION = 1;
    private static final int MAX_PVE_DURATION = 24;
    private static final double MIN_BORDER_SIZE = 100;
    private static final double MAX_BORDER_SIZE = 100000;
    private static final int MIN_BORDER_SHRINK_INTERVAL = 1;
    private static final int MAX_BORDER_SHRINK_INTERVAL = 1440;
    private static final double MIN_BORDER_SHRINK_AMOUNT = 1;
    private static final double MAX_BORDER_SHRINK_AMOUNT = 1000;
    private static final int MIN_BORDER_WARNING_TIME = 10;
    private static final int MAX_BORDER_WARNING_TIME = 300;
    private static final int MIN_BORDER_WARNING_DISTANCE = 10;
    private static final int MAX_BORDER_WARNING_DISTANCE = 1000;
    private static final double MIN_BORDER_DAMAGE = 0.5;
    private static final double MAX_BORDER_DAMAGE = 20.0;
    private static final double MIN_BORDER_BUFFER = 1.0;
    private static final double MAX_BORDER_BUFFER = 100.0;

    public ConfigManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadConfigs();
        validateConfigs();
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

    private void validateConfigs() {
        List<String> errors = new ArrayList<>();
        
        // Validate heart-related settings
        validateHeartSettings(errors);
        
        // Validate PvP cycle settings
        validatePvPCycleSettings(errors);
        
        // Validate world border settings
        validateWorldBorderSettings(errors);
        
        // Validate first join settings
        validateFirstJoinSettings(errors);
        
        // Log any validation errors
        if (!errors.isEmpty()) {
            plugin.getLogger().warning("Configuration validation found issues:");
            for (String error : errors) {
                plugin.getLogger().warning("- " + error);
            }
            plugin.getLogger().warning("Using default values for invalid settings.");
        }
    }

    private void validateHeartSettings(List<String> errors) {
        int startingHearts = getStartingHearts();
        if (startingHearts < MIN_STARTING_HEARTS || startingHearts > MAX_STARTING_HEARTS) {
            errors.add("Starting hearts must be between " + MIN_STARTING_HEARTS + " and " + MAX_STARTING_HEARTS);
            config.set("starting-hearts", 10);
        }

        int minHearts = getMinHearts();
        if (minHearts < MIN_HEARTS || minHearts > MAX_HEARTS) {
            errors.add("Minimum hearts must be between " + MIN_HEARTS + " and " + MAX_HEARTS);
            config.set("min-hearts", 1);
        }

        int maxHearts = getMaxHearts();
        if (maxHearts < MIN_HEARTS || maxHearts > MAX_HEARTS || maxHearts < minHearts) {
            errors.add("Maximum hearts must be between " + MIN_HEARTS + " and " + MAX_HEARTS + " and greater than minimum hearts");
            config.set("max-hearts", 20);
        }

        int heartsPerKill = getHeartsGainedPerKill();
        if (heartsPerKill < MIN_HEARTS_PER_KILL || heartsPerKill > MAX_HEARTS_PER_KILL) {
            errors.add("Hearts gained per kill must be between " + MIN_HEARTS_PER_KILL + " and " + MAX_HEARTS_PER_KILL);
            config.set("hearts-gained-per-kill", 1);
        }

        int heartsPerDeath = getHeartsLostPerDeath();
        if (heartsPerDeath < MIN_HEARTS_PER_DEATH || heartsPerDeath > MAX_HEARTS_PER_DEATH) {
            errors.add("Hearts lost per death must be between " + MIN_HEARTS_PER_DEATH + " and " + MAX_HEARTS_PER_DEATH);
            config.set("hearts-lost-per-death", 1);
        }
    }

    private void validatePvPCycleSettings(List<String> errors) {
        int pvpDuration = getPvPDuration();
        if (pvpDuration < MIN_PVP_DURATION || pvpDuration > MAX_PVP_DURATION) {
            errors.add("PvP duration must be between " + MIN_PVP_DURATION + " and " + MAX_PVP_DURATION + " hours");
            config.set("pvp-cycle.pvp-duration", 2);
        }

        int pveDuration = getPvEDuration();
        if (pveDuration < MIN_PVE_DURATION || pveDuration > MAX_PVE_DURATION) {
            errors.add("PvE duration must be between " + MIN_PVE_DURATION + " and " + MAX_PVE_DURATION + " hours");
            config.set("pvp-cycle.pve-duration", 2);
        }

        String eliminationMode = getEliminationMode();
        if (!eliminationMode.equalsIgnoreCase("ban") && !eliminationMode.equalsIgnoreCase("spectator")) {
            errors.add("Elimination mode must be either 'ban' or 'spectator'");
            config.set("elimination.mode", "spectator");
        }
    }

    private void validateWorldBorderSettings(List<String> errors) {
        if (isWorldBorderEnabled()) {
            double initialSize = getInitialBorderSize();
            if (initialSize < MIN_BORDER_SIZE || initialSize > MAX_BORDER_SIZE) {
                errors.add("Initial border size must be between " + MIN_BORDER_SIZE + " and " + MAX_BORDER_SIZE);
                config.set("world-border.initial-size", 5000);
            }

            if (isWorldBorderShrinkEnabled()) {
                int shrinkInterval = getWorldBorderShrinkInterval();
                if (shrinkInterval < MIN_BORDER_SHRINK_INTERVAL || shrinkInterval > MAX_BORDER_SHRINK_INTERVAL) {
                    errors.add("Border shrink interval must be between " + MIN_BORDER_SHRINK_INTERVAL + " and " + MAX_BORDER_SHRINK_INTERVAL + " minutes");
                    config.set("world-border.shrink.interval", 30);
                }

                double shrinkAmount = getWorldBorderShrinkAmount();
                if (shrinkAmount < MIN_BORDER_SHRINK_AMOUNT || shrinkAmount > MAX_BORDER_SHRINK_AMOUNT) {
                    errors.add("Border shrink amount must be between " + MIN_BORDER_SHRINK_AMOUNT + " and " + MAX_BORDER_SHRINK_AMOUNT);
                    config.set("world-border.shrink.amount", 100);
                }

                double minSize = getWorldBorderMinSize();
                if (minSize < MIN_BORDER_SIZE || minSize > MAX_BORDER_SIZE || minSize > initialSize) {
                    errors.add("Border minimum size must be between " + MIN_BORDER_SIZE + " and " + MAX_BORDER_SIZE + " and less than initial size");
                    config.set("world-border.shrink.min-size", 500);
                }

                int warningTime = getWorldBorderWarningTime();
                if (warningTime < MIN_BORDER_WARNING_TIME || warningTime > MAX_BORDER_WARNING_TIME) {
                    errors.add("Border warning time must be between " + MIN_BORDER_WARNING_TIME + " and " + MAX_BORDER_WARNING_TIME + " seconds");
                    config.set("world-border.shrink.warning-time", 60);
                }
            }

            int warningDistance = getWorldBorderWarningDistance();
            if (warningDistance < MIN_BORDER_WARNING_DISTANCE || warningDistance > MAX_BORDER_WARNING_DISTANCE) {
                errors.add("Border warning distance must be between " + MIN_BORDER_WARNING_DISTANCE + " and " + MAX_BORDER_WARNING_DISTANCE);
                config.set("world-border.shrink.warning-distance", 50);
            }

            double damageAmount = getWorldBorderDamageAmount();
            if (damageAmount < MIN_BORDER_DAMAGE || damageAmount > MAX_BORDER_DAMAGE) {
                errors.add("Border damage amount must be between " + MIN_BORDER_DAMAGE + " and " + MAX_BORDER_DAMAGE);
                config.set("world-border.damage.amount", 1.0);
            }

            double damageBuffer = getWorldBorderDamageBuffer();
            if (damageBuffer < MIN_BORDER_BUFFER || damageBuffer > MAX_BORDER_BUFFER) {
                errors.add("Border damage buffer must be between " + MIN_BORDER_BUFFER + " and " + MAX_BORDER_BUFFER);
                config.set("world-border.damage.buffer", 5.0);
            }
        }
    }

    private void validateFirstJoinSettings(List<String> errors) {
        if (isFirstJoinEnabled()) {
            List<String> messages = getFirstJoinMessages();
            if (messages.isEmpty()) {
                errors.add("First join messages list is empty");
                config.set("first-join.messages", List.of("Welcome to the server!"));
            }

            int minDistance = getSafeLocationMinDistance();
            int maxDistance = getSafeLocationMaxDistance();
            if (minDistance < 0 || maxDistance < minDistance) {
                errors.add("Safe location distances are invalid");
                config.set("first-join.safe-location.min-distance", 100);
                config.set("first-join.safe-location.max-distance", 1000);
            }

            int maxAttempts = getSafeLocationMaxAttempts();
            if (maxAttempts < 1 || maxAttempts > 100) {
                errors.add("Safe location max attempts must be between 1 and 100");
                config.set("first-join.safe-location.max-attempts", 10);
            }
        }
    }

    public void reloadConfigs() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
        validateConfigs();
        
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
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration files", e);
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public int getStartingHearts() {
        return config.contains("starting-hearts") ? config.getInt("starting-hearts") : 10;
    }

    public int getMinHearts() {
        return config.contains("min-hearts") ? config.getInt("min-hearts") : 1;
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
                config.getInt("safe-location.min-distance") : 100;
    }
    
    public int getSafeLocationMaxDistance() {
        return config.contains("safe-location.max-distance") ? 
                config.getInt("safe-location.max-distance") : 1000;
    }
    
    public int getSafeLocationMaxAttempts() {
        return config.contains("safe-location.max-attempts") ? 
                config.getInt("safe-location.max-attempts") : 10;
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

    /**
     * Gets an integer value from the configuration.
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The integer value
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    /**
     * Gets a double value from the configuration.
     * @param path The configuration path
     * @param defaultValue The default value if not found
     * @return The double value
     */
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    /**
     * Gets the maximum number of bounty kills allowed.
     * @return The maximum number of bounty kills
     */
    public int getMaxBountyKills() {
        return getInt("bounty.max_kills", 3);
    }

    /**
     * Gets the number of hearts gained from bounties.
     * @return The number of hearts gained
     */
    public int getBountyHeartsGained() {
        return getInt("bounty.hearts.gained", 1);
    }

    /**
     * Gets the world border initial size.
     * @return The initial size of the world border
     */
    public double getWorldBorderInitialSize() {
        return getDouble("world_border.initial_size", 1000.0);
    }
}