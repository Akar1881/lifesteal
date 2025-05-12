package com.lifesteal;

import com.lifesteal.commands.AllyCommand;
import com.lifesteal.commands.LifeStealCommand;
import com.lifesteal.listeners.GUIListener;
import com.lifesteal.listeners.ItemListener;
import com.lifesteal.listeners.PlayerListener;
import com.lifesteal.managers.AllyManager;
import com.lifesteal.managers.BountyManager;
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
    private AllyManager allyManager;
    private BountyManager bountyManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.heartManager = new HeartManager(this);
        this.itemManager = new ItemManager(this);
        this.modeManager = new ModeManager(this);
        this.allyManager = new AllyManager(this);
        this.bountyManager = new BountyManager(this);

        // Register commands
        LifeStealCommand lifeStealCommand = new LifeStealCommand(this);
        getCommand("lifesteal").setExecutor(lifeStealCommand);
        getCommand("lifesteal").setTabCompleter(lifeStealCommand);
        
        AllyCommand allyCommand = new AllyCommand(this);
        getCommand("ally").setExecutor(allyCommand);
        getCommand("ally").setTabCompleter(allyCommand);

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
            
            // If we're starting in PvP mode, initialize the bounty system
            if (modeManager.isPvPMode() && bountyManager.isBountyEnabled()) {
                bountyManager.startBountySystem();
            }
        }
        
        // Schedule task to clean up timed out ally requests
        getServer().getScheduler().runTaskTimer(this, () -> allyManager.cleanupTimedOutRequests(), 1200L, 1200L); // Run every minute (20 ticks * 60)
    }

    @Override
    public void onDisable() {
        if (modeManager != null) {
            modeManager.stopRotation();
        }
        
        // Save ally data
        if (allyManager != null) {
            allyManager.saveConfig();
        }
        
        // Stop bounty system if active
        if (bountyManager != null) {
            bountyManager.stopBountySystem();
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
    
    public AllyManager getAllyManager() {
        return allyManager;
    }
    
    public BountyManager getBountyManager() {
        return bountyManager;
    }
}