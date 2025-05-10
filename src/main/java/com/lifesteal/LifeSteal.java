package com.lifesteal;

import com.lifesteal.commands.LifeStealCommand;
import com.lifesteal.listeners.GUIListener;
import com.lifesteal.listeners.ItemListener;
import com.lifesteal.listeners.PlayerListener;
import com.lifesteal.managers.ConfigManager;
import com.lifesteal.managers.HeartManager;
import com.lifesteal.managers.ItemManager;
import com.lifesteal.managers.ModeManager;
import org.bukkit.plugin.java.JavaPlugin;

public class LifeSteal extends JavaPlugin {
    private static LifeSteal instance;
    private ConfigManager configManager;
    private HeartManager heartManager;
    private ItemManager itemManager;
    private ModeManager modeManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.heartManager = new HeartManager(this);
        this.itemManager = new ItemManager(this);
        this.modeManager = new ModeManager(this);

        // Register commands
        getCommand("lifesteal").setExecutor(new LifeStealCommand(this));

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemListener(this), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        // Load configurations
        configManager.loadConfigs();
        
        // Initialize items
        itemManager.registerItems();
        
        // Start mode rotation if enabled
        if (configManager.isPvPCycleEnabled()) {
            modeManager.startRotation();
        }
    }

    @Override
    public void onDisable() {
        if (modeManager != null) {
            modeManager.stopRotation();
        }
    }

    public static LifeSteal getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public HeartManager getHeartManager() {
        return heartManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }

    public ModeManager getModeManager() {
        return modeManager;
    }
}