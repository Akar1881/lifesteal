package com.lifesteal;

import com.lifesteal.commands.AllyCommand;
import com.lifesteal.commands.LifeStealCommand;
import com.lifesteal.listeners.*;
import com.lifesteal.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class LifeSteal extends JavaPlugin {
    private static LifeSteal instance;
    private ConfigManager configManager;
    private HeartManager heartManager;
    private ItemManager itemManager;
    private ModeManager modeManager;
    private AllyManager allyManager;
    private BountyManager bountyManager;
    private WorldBorderManager worldBorderManager;
    private FirstJoinManager firstJoinManager;
    private DatabaseManager databaseManager;
    private StatisticsManager statisticsManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Check for Chunky plugin
        boolean chunkyAvailable = getServer().getPluginManager().getPlugin("Chunky") != null;
        if (chunkyAvailable) {
            getLogger().info("Chunky plugin found! Chunk pre-generation will be available.");
        } else {
            getLogger().warning("Chunky plugin not found. Chunk pre-generation will be disabled.");
            getLogger().warning("Download Chunky from: https://www.spigotmc.org/resources/chunky.81534/");
        }
        
        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();
        this.heartManager = new HeartManager(this);
        this.itemManager = new ItemManager(this);
        this.modeManager = new ModeManager(this);
        this.allyManager = new AllyManager(this);
        this.bountyManager = new BountyManager(this);
        this.worldBorderManager = new WorldBorderManager(this);
        this.firstJoinManager = new FirstJoinManager(this);
        this.statisticsManager = new StatisticsManager(this);

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
        getServer().getPluginManager().registerEvents(new BorderListener(this), this);
        getServer().getPluginManager().registerEvents(new FirstJoinListener(this), this);

        // Load configurations
        configManager.loadConfigs();
        
        // Initialize items
        itemManager.registerItems();
        
        // Always load the world border manager to ensure data is updated
        worldBorderManager.loadBorderData();
        
        // Initialize world border if enabled
        if (configManager.isWorldBorderEnabled()) {
            worldBorderManager.initializeBorder();
        }
        
        // Start mode rotation if enabled
        if (configManager.isPvPCycleEnabled()) {
            modeManager.startRotation();
            
            // If we're starting in PvP mode, initialize the bounty system
            if (modeManager.isPvPMode() && bountyManager.isBountyEnabled()) {
                bountyManager.startBountySystem();
            }
        }
        
        // Schedule task to clean up timed out ally requests
        getServer().getScheduler().runTaskTimer(this, () -> allyManager.cleanupTimedOutRequests(), 1200L, 1200L);
    }

    @Override
    public void onDisable() {
        if (modeManager != null) {
            modeManager.stopRotation();
        }
        
        if (bountyManager != null) {
            bountyManager.stopBountySystem();
        }
        
        if (worldBorderManager != null) {
            worldBorderManager.stopShrinkTask();
            worldBorderManager.saveBorderData();
        }
        
        if (firstJoinManager != null) {
            firstJoinManager.savePlayerStates();
            firstJoinManager.cleanup();
        }

        if (databaseManager != null) {
            databaseManager.close();
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
    
    public WorldBorderManager getWorldBorderManager() {
        return worldBorderManager;
    }

    public FirstJoinManager getFirstJoinManager() {
        return firstJoinManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public StatisticsManager getStatisticsManager() {
        return statisticsManager;
    }
    
    /**
     * Check if the Chunky plugin is available
     * @return True if Chunky is available, false otherwise
     */
    public boolean isChunkyAvailable() {
        return getServer().getPluginManager().getPlugin("Chunky") != null;
    }
}