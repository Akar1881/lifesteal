package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

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
}