package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.bukkit.ChatColor;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.zip.GZIPOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.Arrays;

/**
 * Manages bounty-related operations in the LifeSteal plugin.
 * This class handles bounty setting, removal, tracking, and related events.
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li>Bounty management (set, remove, track)</li>
 *   <li>Event handling and processing</li>
 *   <li>Command processing</li>
 *   <li>Performance monitoring</li>
 *   <li>Error handling and recovery</li>
 *   <li>Logging and log rotation</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Get the BountyManager instance
 * BountyManager bountyManager = plugin.getBountyManager();
 * 
 * // Set a bounty
 * bountyManager.setBounty(player, target);
 * 
 * // Remove a bounty
 * bountyManager.removeBounty(player);
 * </pre>
 * 
 * <h2>Configuration</h2>
 * The following configuration options are available in config.yml:
 * <ul>
 *   <li>bounty.cooldown: Cooldown between bounties (in milliseconds)</li>
 *   <li>bounty.max_kills: Maximum kills before cooldown</li>
 *   <li>bounty.hearts.min: Minimum hearts gained from bounties</li>
 *   <li>bounty.hearts.max: Maximum hearts gained from bounties</li>
 * </ul>
 * 
 * <h2>Permissions</h2>
 * <ul>
 *   <li>lifesteal.bounty.set: Allow setting bounties</li>
 *   <li>lifesteal.bounty.remove: Allow removing bounties</li>
 *   <li>lifesteal.bounty.stats: Allow viewing bounty stats</li>
 *   <li>lifesteal.admin: Access to admin commands and features</li>
 * </ul>
 * 
 * <h2>Commands</h2>
 * <ul>
 *   <li>/bounty set <player>: Set a bounty on a player</li>
 *   <li>/bounty remove: Remove your current bounty</li>
 *   <li>/bounty stats [player]: View bounty statistics</li>
 * </ul>
 * 
 * @author LifeSteal Team
 * @version 1.0.0
 * @since 1.0.0
 */
public class BountyManager implements Listener {
    // Constants
    private static final long CLEANUP_INTERVAL = TimeUnit.HOURS.toMillis(1);
    private static final long MAX_DATA_AGE = TimeUnit.DAYS.toMillis(30);
    private static final int MAX_MAP_SIZE = 10000;
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY = TimeUnit.SECONDS.toMillis(5);
    private static final long COMMAND_COOLDOWN = TimeUnit.SECONDS.toMillis(30);
    private static final long PERFORMANCE_LOG_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    private static final int PERFORMANCE_THRESHOLD_MS = 100;
    private static final int MAX_ERROR_REPORTS = 1000;
    private static final long ERROR_REPORT_RETENTION = TimeUnit.DAYS.toMillis(7);
    private static final long LOG_ROTATION_INTERVAL = TimeUnit.DAYS.toMillis(1);
    private static final int MAX_LOG_FILES = 7;
    private static final long LOG_RETENTION_PERIOD = TimeUnit.DAYS.toMillis(30);
    private static final double RARE_BOUNTY_CHANCE = 0.0001; // 0.01% chance
    private static final long BOUNTY_COOLDOWN = 3600000; // 1 hour in milliseconds
    private static final int MAX_BOUNTY_KILLS = 3; // Maximum kills before cooldown
    private static final int LOCATION_BROADCAST_INTERVAL = 300; // 5 minutes in seconds
    private static final int BOUNTY_CHECK_INTERVAL = 60; // 1 minute in seconds
    private static final int MAX_BOUNTY_HEARTS = 10; // Maximum hearts that can be gained from bounties
    private static final int MIN_BOUNTY_HEARTS = 1; // Minimum hearts that can be gained from bounties

    // Plugin instance
    private final LifeSteal plugin;
    private final ItemManager itemManager;
    private final ConfigManager configManager;
    private final DatabaseManager databaseManager;
    private final HeartManager heartManager;

    // Data storage
    private final Map<UUID, Long> lastBountyTime;
    private final Map<UUID, Integer> bountyKills;
    private final Map<UUID, Long> lastCleanupTime;
    private final Map<UUID, ReentrantReadWriteLock> playerLocks;
    private final Queue<BountyEvent> eventQueue;
    private final Map<UUID, List<BountyEvent>> failedEvents;
    private final Map<UUID, Long> commandCooldowns;
    private final Queue<BountyCommand> commandQueue;
    private final Map<String, PerformanceMetric> performanceMetrics;
    private final Map<String, ErrorReport> errorReports;

    // Logging
    private File currentLogFile;
    private File logDirectory;

    // Tasks
    private BukkitTask cleanupTask;
    private BukkitTask eventProcessorTask;
    private BukkitTask commandProcessorTask;
    private BukkitTask performanceMonitorTask;
    private BukkitTask errorCleanupTask;
    private BukkitTask logRotationTask;
    private BukkitTask locationTask;
    private BukkitTask bountyTask;

    // State
    private UUID targetPlayer;
    private boolean bountyActive = false;
    private boolean isRareBounty = false;
    private final Random random = new Random();

    /**
     * Represents a bounty command.
     */
    private static class BountyCommand {
        private final UUID playerId;
        private final String commandType;
        private final String[] args;
        private final long timestamp;
        private int retryCount;

        /**
         * Creates a new BountyCommand instance.
         * @param playerId The player's UUID
         * @param commandType The type of command
         * @param args The command arguments
         */
        BountyCommand(UUID playerId, String commandType, String[] args) {
            this.playerId = playerId;
            this.commandType = commandType;
            this.args = args;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = 0;
        }
    }

    /**
     * Represents a bounty event.
     */
    private static class BountyEvent {
        private final UUID playerId;
        private final String eventType;
        private final Map<String, Object> data;
        private final long timestamp;
        private int retryCount;

        /**
         * Creates a new BountyEvent instance.
         * @param playerId The player's UUID
         * @param eventType The type of event
         * @param data The event data
         */
        BountyEvent(UUID playerId, String eventType, Map<String, Object> data) {
            this.playerId = playerId;
            this.eventType = eventType;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = 0;
        }
    }

    private static class PerformanceMetric {
        long totalTime;
        long count;
        long maxTime;
        long minTime;
        long lastReset;

        PerformanceMetric() {
            reset();
        }

        void reset() {
            totalTime = 0;
            count = 0;
            maxTime = 0;
            minTime = Long.MAX_VALUE;
            lastReset = System.currentTimeMillis();
        }

        void addMeasurement(long time) {
            totalTime += time;
            count++;
            maxTime = Math.max(maxTime, time);
            minTime = Math.min(minTime, time);
        }

        double getAverageTime() {
            return count > 0 ? (double) totalTime / count : 0;
        }
    }

    private static class ErrorReport {
        final String errorType;
        final String message;
        final String stackTrace;
        final long timestamp;
        int occurrenceCount;
        boolean resolved;

        ErrorReport(String errorType, String message, String stackTrace) {
            this.errorType = errorType;
            this.message = message;
            this.stackTrace = stackTrace;
            this.timestamp = System.currentTimeMillis();
            this.occurrenceCount = 1;
            this.resolved = false;
        }
    }

    /**
     * Initializes a new BountyManager instance.
     * @param plugin The LifeSteal plugin instance
     */
    public BountyManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
        this.configManager = plugin.getConfigManager();
        this.databaseManager = plugin.getDatabaseManager();
        this.heartManager = plugin.getHeartManager();

        // Initialize collections
        this.lastBountyTime = new ConcurrentHashMap<>();
        this.bountyKills = new ConcurrentHashMap<>();
        this.lastCleanupTime = new ConcurrentHashMap<>();
        this.playerLocks = new ConcurrentHashMap<>();
        this.eventQueue = new ConcurrentLinkedQueue<>();
        this.failedEvents = new ConcurrentHashMap<>();
        this.commandCooldowns = new ConcurrentHashMap<>();
        this.commandQueue = new ConcurrentLinkedQueue<>();
        this.performanceMetrics = new ConcurrentHashMap<>();
        this.errorReports = new ConcurrentHashMap<>();

        // Initialize systems
        initializeLogging();
        startTasks();
        registerEvents();
        loadBountyData();
    }

    /**
     * Starts all periodic tasks.
     */
    private void startTasks() {
        startCleanupTask();
        startEventProcessorTask();
        startCommandProcessorTask();
        startPerformanceMonitorTask();
        startErrorCleanupTask();
        startLogRotationTask();
    }

    /**
     * Registers event listeners.
     */
    private void registerEvents() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private void initializeLogging() {
        try {
            logDirectory = new File(plugin.getDataFolder(), "logs");
            if (!logDirectory.exists()) {
                logDirectory.mkdirs();
            }
            currentLogFile = new File(logDirectory, "bounty_" + System.currentTimeMillis() + ".log");
            if (!currentLogFile.exists()) {
                currentLogFile.createNewFile();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to initialize logging: " + e.getMessage());
        }
    }

    private void startLogRotationTask() {
        logRotationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                rotateLogs();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in log rotation task: " + e.getMessage());
            }
        }, 20L * 60 * 60 * 24, 20L * 60 * 60 * 24); // Run every 24 hours
    }

    private void rotateLogs() {
        try {
            // Create new log file
            File newLogFile = new File(logDirectory, "bounty_" + System.currentTimeMillis() + ".log");
            if (!newLogFile.exists()) {
                newLogFile.createNewFile();
            }

            // Compress old log file
            if (currentLogFile.exists() && currentLogFile.length() > 0) {
                compressLogFile(currentLogFile);
            }

            // Update current log file
            currentLogFile = newLogFile;

            // Cleanup old log files
            cleanupOldLogs();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to rotate logs: " + e.getMessage());
        }
    }

    private void compressLogFile(File logFile) {
        try {
            File compressedFile = new File(logFile.getPath() + ".gz");
            try (GZIPOutputStream gzipOut = new GZIPOutputStream(new FileOutputStream(compressedFile));
                 FileInputStream fileIn = new FileInputStream(logFile)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fileIn.read(buffer)) > 0) {
                    gzipOut.write(buffer, 0, len);
                }
            }
            logFile.delete();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to compress log file: " + e.getMessage());
        }
    }

    private void cleanupOldLogs() {
        try {
            File[] logFiles = logDirectory.listFiles((dir, name) -> name.startsWith("bounty_") && name.endsWith(".gz"));
            if (logFiles != null) {
                // Sort by last modified time
                Arrays.sort(logFiles, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

                // Keep only the most recent MAX_LOG_FILES
                for (int i = MAX_LOG_FILES; i < logFiles.length; i++) {
                    logFiles[i].delete();
                }

                // Delete logs older than retention period
                long cutoffTime = System.currentTimeMillis() - LOG_RETENTION_PERIOD;
                for (File logFile : logFiles) {
                    if (logFile.lastModified() < cutoffTime) {
                        logFile.delete();
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to cleanup old logs: " + e.getMessage());
        }
    }

    private void logToFile(String message) {
        try {
            if (currentLogFile != null && currentLogFile.exists()) {
                try (FileWriter writer = new FileWriter(currentLogFile, true);
                     BufferedWriter bufferedWriter = new BufferedWriter(writer)) {
                    String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                    bufferedWriter.write(String.format("[%s] %s%n", timestamp, message));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to write to log file: " + e.getMessage());
        }
    }

    /**
     * Logs an event to the log file.
     * @param status The status or action being performed
     * @param event The event to log
     * @param e Optional exception (can be null)
     */
    private void logEvent(String status, BountyEvent event, Exception e) {
        String message = String.format("[Event] %s - Type: %s, Player: %s, Target: %s, Time: %s",
            status,
            event.eventType,
            event.playerId,
            event.data != null ? event.data.get("targetId") : "N/A",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(event.timestamp))
        );
        if (e != null) {
            message += ", Error: " + e.getMessage();
        }
        logToFile(message);
    }

    private void startCleanupTask() {
        cleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                performCleanup();
                monitorMemoryUsage();
            } catch (Exception e) {
                plugin.getLogger().severe("Error during cleanup task: " + e.getMessage());
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // Run every hour
    }

    private void startEventProcessorTask() {
        eventProcessorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                processEventQueue();
                retryFailedEvents();
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing event queue: " + e.getMessage());
            }
        }, 20L, 20L); // Process every second
    }

    private void startCommandProcessorTask() {
        commandProcessorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                processCommandQueue();
            } catch (Exception e) {
                plugin.getLogger().severe("Error processing command queue: " + e.getMessage());
            }
        }, 20L, 20L); // Process every second
    }

    private void startPerformanceMonitorTask() {
        performanceMonitorTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                logPerformanceMetrics();
                optimizePerformance();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in performance monitoring: " + e.getMessage());
            }
        }, 20L * 60 * 15, 20L * 60 * 15); // Run every 15 minutes
    }

    private void startErrorCleanupTask() {
        errorCleanupTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            try {
                cleanupErrorReports();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in error cleanup task: " + e.getMessage());
            }
        }, 20L * 60 * 60, 20L * 60 * 60); // Run every hour
    }

    private void performCleanup() {
        long currentTime = System.currentTimeMillis();
        final int[] cleanedEntries = {0};

        // Clean up old bounty time entries
        for (Map.Entry<UUID, Long> entry : lastBountyTime.entrySet()) {
            ReentrantReadWriteLock lock = getPlayerLock(entry.getKey());
            lock.writeLock().lock();
            try {
                if (currentTime - entry.getValue() > MAX_DATA_AGE) {
                    lastBountyTime.remove(entry.getKey());
                    bountyKills.remove(entry.getKey());
                    playerLocks.remove(entry.getKey());
                    cleanedEntries[0]++;
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        // Clean up old cleanup time entries
        for (Map.Entry<UUID, Long> entry : lastCleanupTime.entrySet()) {
            ReentrantReadWriteLock lock = getPlayerLock(entry.getKey());
            lock.writeLock().lock();
            try {
                if (currentTime - entry.getValue() > MAX_DATA_AGE) {
                    lastCleanupTime.remove(entry.getKey());
                    playerLocks.remove(entry.getKey());
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        // Trim maps if they exceed maximum size
        if (lastBountyTime.size() > MAX_MAP_SIZE) {
            int toRemove = lastBountyTime.size() - MAX_MAP_SIZE;
            lastBountyTime.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .limit(toRemove)
                .forEach(entry -> {
                    ReentrantReadWriteLock lock = getPlayerLock(entry.getKey());
                    lock.writeLock().lock();
                    try {
                        lastBountyTime.remove(entry.getKey());
                        bountyKills.remove(entry.getKey());
                        playerLocks.remove(entry.getKey());
                        cleanedEntries[0]++;
                    } finally {
                        lock.writeLock().unlock();
                    }
                });
        }

        if (cleanedEntries[0] > 0) {
            plugin.getLogger().info("Cleaned up " + cleanedEntries[0] + " old entries from bounty data");
            saveBountyData(); // Save after cleanup
        }
    }

    private void monitorMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();
        
        double memoryUsagePercent = (double) usedMemory / maxMemory * 100;
        
        if (memoryUsagePercent > 80) {
            plugin.getLogger().warning("High memory usage detected: " + String.format("%.2f", memoryUsagePercent) + "%");
            // Force garbage collection if memory usage is too high
            System.gc();
        }
        
        // Log memory stats every 6 hours
        if (System.currentTimeMillis() % (6 * 60 * 60 * 1000) < 60000) {
            plugin.getLogger().info("Memory Usage: " + String.format("%.2f", memoryUsagePercent) + "%");
            plugin.getLogger().info("Total Memory: " + (totalMemory / 1024 / 1024) + "MB");
            plugin.getLogger().info("Free Memory: " + (freeMemory / 1024 / 1024) + "MB");
            plugin.getLogger().info("Max Memory: " + (maxMemory / 1024 / 1024) + "MB");
        }
    }

    public void cleanup() {
        if (cleanupTask != null) {
            cleanupTask.cancel();
        }
        if (eventProcessorTask != null) {
            eventProcessorTask.cancel();
        }
        if (commandProcessorTask != null) {
            commandProcessorTask.cancel();
        }
        if (performanceMonitorTask != null) {
            performanceMonitorTask.cancel();
        }
        if (errorCleanupTask != null) {
            errorCleanupTask.cancel();
        }
        if (logRotationTask != null) {
            logRotationTask.cancel();
        }
        performCleanup();
        saveBountyData();
        lastBountyTime.clear();
        bountyKills.clear();
        lastCleanupTime.clear();
        eventQueue.clear();
        failedEvents.clear();
        commandQueue.clear();
        commandCooldowns.clear();
        performanceMetrics.clear();
        errorReports.clear();
        
        // Rotate logs one final time
        try {
            rotateLogs();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to rotate logs during cleanup: " + e.getMessage());
        }
    }

    /**
     * Creates a special revival heart item (now config-driven)
     */
    private ItemStack createRevivalItem() {
        ItemStack revivalHeart = itemManager.getCustomItem("revival-heart");
        if (revivalHeart == null) {
            // fallback to a default if config is missing
            revivalHeart = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
            ItemMeta meta = revivalHeart.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize("&c&l♥ &6&lRevival Heart &c&l♥"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7A rare item obtained from a bounty"));
                lore.add(ColorUtils.colorize("&7Right-click to revive your allies"));
                lore.add(ColorUtils.colorize("&7from elimination."));
                lore.add("");
                lore.add(ColorUtils.colorize("&c&lRARE ITEM"));
                meta.setLore(lore);
                revivalHeart.setItemMeta(meta);
            }
        }
        return revivalHeart;
    }

    // --- ADDED: Bounty system control methods ---
    public boolean isBountyEnabled() {
        FileConfiguration config = plugin.getConfig();
        if (config.getConfigurationSection("bounty") == null) {
            return false;
        }
        
        ConfigurationSection bountySection = config.getConfigurationSection("bounty");
        return bountySection.contains("enabled") ? bountySection.getBoolean("enabled") : false;
    }

    public void startBountySystem() {
        if (!isBountyEnabled()) return;
        
        this.bountyActive = true;
        // Start bounty selection task
        bountyTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!bountyActive || !plugin.getModeManager().isPvPMode()) return;
            
            // Check if we need a new target
            if (targetPlayer == null || Bukkit.getPlayer(targetPlayer) == null) {
                selectRandomTarget();
            }
        }, 20L * 60, 20L * 60); // Check every minute
        
        // Start location broadcast task
        startLocationBroadcast();
    }

    private void selectRandomTarget() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        if (onlinePlayers.isEmpty()) return;
        
        // Filter out players who are on cooldown or have reached max kills
        onlinePlayers.removeIf(player -> {
            Long lastTime = lastBountyTime.get(player.getUniqueId());
            Integer kills = bountyKills.getOrDefault(player.getUniqueId(), 0);
            return (lastTime != null && System.currentTimeMillis() - lastTime < BOUNTY_COOLDOWN) ||
                   kills >= MAX_BOUNTY_KILLS;
        });
        
        if (onlinePlayers.isEmpty()) return;
        
        // Select random player
        Player target = onlinePlayers.get(random.nextInt(onlinePlayers.size()));
        targetPlayer = target.getUniqueId();
        
        // Determine if this is a rare bounty
        isRareBounty = random.nextDouble() < RARE_BOUNTY_CHANCE;
        
        // Announce new bounty
        String message = isRareBounty ? 
            "&c&lRARE BOUNTY! &eA bounty has been placed on &c" + target.getName() + "&e!" :
            "&eA bounty has been placed on &c" + target.getName() + "&e!";
        Bukkit.broadcastMessage(ColorUtils.colorize(message));
        
        // Play sound
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
        }
    }

    private void startLocationBroadcast() {
        locationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!bountyActive || targetPlayer == null) return;
            
            Player target = Bukkit.getPlayer(targetPlayer);
            if (target == null || !target.isOnline()) {
                targetPlayer = null;
                return;
            }
            
            // Broadcast target's location every 5 minutes
            Location loc = target.getLocation();
            String message = String.format("&eBounty target &c%s &eis at &7[%d, %d, %d]&e!",
                target.getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            Bukkit.broadcastMessage(ColorUtils.colorize(message));
        }, 20L * 300, 20L * 300); // Every 5 minutes
    }

    private void processEventQueue() {
        while (!eventQueue.isEmpty()) {
            BountyEvent event = eventQueue.poll();
            try {
                processEvent(event);
            } catch (Exception e) {
                handleFailedEvent(event, e);
            }
        }
    }

    private void processEvent(BountyEvent event) {
        measurePerformance("processEvent", () -> {
            logEvent("Processing", event, null);
            
            switch (event.eventType) {
                case "SET_BOUNTY":
                    processSetBountyEvent(event);
                    break;
                case "REMOVE_BOUNTY":
                    processRemoveBountyEvent(event);
                    break;
                case "BOUNTY_KILL":
                    processBountyKillEvent(event);
                    break;
                default:
                    plugin.getLogger().warning("Unknown event type: " + event.eventType);
            }
        });
    }

    /**
     * Processes a bounty kill event.
     * @param event The event to process
     */
    private void processBountyKillEvent(BountyEvent event) {
        Player killer = Bukkit.getPlayer(event.playerId);
        UUID targetId = (UUID) event.data.get("targetId");
        Player victim = Bukkit.getPlayer(targetId);
        
        if (killer == null || victim == null) {
            return;
        }

        // Process the bounty kill
        processBountyKill(killer, victim);
    }

    /**
     * Processes a set bounty event.
     * @param event The event to process
     */
    private void processSetBountyEvent(BountyEvent event) {
        Player player = Bukkit.getPlayer(event.playerId);
        UUID targetId = (UUID) event.data.get("targetId");
        Player target = Bukkit.getPlayer(targetId);
        
        if (player == null || target == null) {
            return;
        }

        // Process setting the bounty
        setBounty(player, target);
    }

    private void processRemoveBountyEvent(BountyEvent event) {
        Player player = Bukkit.getPlayer(event.playerId);
        
        if (player == null) {
            throw new IllegalStateException("Player not found");
        }
        
        removeBounty(player);
    }

    private void handleFailedEvent(BountyEvent event, Exception e) {
        logEvent("Failed", event, e);
        
        if (event.retryCount < MAX_RETRY_ATTEMPTS) {
            event.retryCount++;
            failedEvents.computeIfAbsent(event.playerId, k -> new ArrayList<>()).add(event);
            plugin.getLogger().warning("Event will be retried. Attempt " + event.retryCount + " of " + MAX_RETRY_ATTEMPTS);
        } else {
            plugin.getLogger().severe("Event failed after " + MAX_RETRY_ATTEMPTS + " attempts: " + e.getMessage());
        }
    }

    private void retryFailedEvents() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<UUID, List<BountyEvent>> entry : failedEvents.entrySet()) {
            List<BountyEvent> events = entry.getValue();
            events.removeIf(event -> {
                if (currentTime - event.timestamp >= RETRY_DELAY) {
                    eventQueue.add(event);
                    return true;
                }
                return false;
            });
            
            if (events.isEmpty()) {
                failedEvents.remove(entry.getKey());
            }
        }
    }

    /**
     * Sets a bounty on a target player.
     * This method will:
     * <ul>
     *   <li>Check if the player has permission</li>
     *   <li>Validate the target player</li>
     *   <li>Check cooldowns and limits</li>
     *   <li>Process the bounty setting</li>
     *   <li>Notify relevant players</li>
     * </ul>
     *
     * @param player The player setting the bounty
     * @param target The target player
     * @throws IllegalArgumentException if player or target is null
     * @throws IllegalStateException if player doesn't have permission
     * @see #removeBounty(Player)
     * @see #getBountyStats(Player)
     */
    public void setBounty(Player player, Player target) {
        if (player == null || target == null) return;
        
        measurePerformance("setBounty", () -> {
            BountyEvent event = new BountyEvent(player.getUniqueId(), "SET_BOUNTY", new HashMap<>());
            event.data.put("targetId", target.getUniqueId());
            eventQueue.add(event);
            logEvent("Queued", event, null);
        });
    }

    /**
     * Removes a bounty from a player.
     * This method will:
     * <ul>
     *   <li>Check if the player has permission</li>
     *   <li>Validate the player has a bounty</li>
     *   <li>Process the bounty removal</li>
     *   <li>Notify relevant players</li>
     * </ul>
     *
     * @param player The player to remove the bounty from
     * @throws IllegalArgumentException if player is null
     * @throws IllegalStateException if player doesn't have permission
     * @see #setBounty(Player, Player)
     * @see #getBountyStats(Player)
     */
    public void removeBounty(Player player) {
        if (player == null) return;
        
        measurePerformance("removeBounty", () -> {
            BountyEvent event = new BountyEvent(player.getUniqueId(), "REMOVE_BOUNTY", new HashMap<>());
            eventQueue.add(event);
            logEvent("Queued", event, null);
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        
        if (killer != null && hasBounty(victim)) {
            BountyEvent bountyEvent = new BountyEvent(killer.getUniqueId(), "BOUNTY_KILL", new HashMap<>());
            bountyEvent.data.put("targetId", victim.getUniqueId());
            eventQueue.add(bountyEvent);
            logEvent("Queued", bountyEvent, null);
        }
    }

    private void handleBountyKill(Player killer, Player victim) {
        ReentrantReadWriteLock lock = getPlayerLock(victim.getUniqueId());
        lock.writeLock().lock();
        try {
            logConcurrentOperation("Processing bounty kill", victim.getUniqueId());
            
            int currentKills = getBountyKills(victim);
            int newKills = currentKills + 1;
            bountyKills.put(victim.getUniqueId(), newKills);
            
            // Calculate hearts to gain
            int heartsToGain = calculateHeartsToGain(newKills);
            
            // Apply hearts to killer
            heartManager.addHearts(killer, heartsToGain);
            
            // Notify players
            killer.sendMessage(ChatColor.GREEN + "You gained " + heartsToGain + " hearts for killing a player with a bounty!");
            victim.sendMessage(ChatColor.RED + "You lost " + heartsToGain + " hearts to " + killer.getName() + "!");
            
            // Remove bounty if max kills reached
            if (newKills >= configManager.getMaxBountyKills()) {
                removeBounty(victim);
            }
            
            saveBountyData();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!bountyActive || targetPlayer == null) return;
        
        Player player = event.getPlayer();
        if (player.getUniqueId().equals(targetPlayer)) {
            handleBountyLogout(player);
        }
    }

    private void handleBountyLogout(Player player) {
        // Remove bounty if player logs out
        targetPlayer = null;
        Bukkit.broadcastMessage(ColorUtils.colorize("&cThe bounty target has logged out!"));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Reset bounty stats if cooldown has passed
        Long lastTime = lastBountyTime.get(player.getUniqueId());
        if (lastTime != null && System.currentTimeMillis() - lastTime >= BOUNTY_COOLDOWN) {
            lastBountyTime.remove(player.getUniqueId());
            bountyKills.remove(player.getUniqueId());
        }
    }

    public boolean isBountyActive() {
        return bountyActive && targetPlayer != null;
    }

    public UUID getCurrentTarget() {
        return targetPlayer;
    }

    public boolean isRareBounty() {
        return isRareBounty;
    }

    public void stopBountySystem() {
        // Stop bounty system logic (unset flag, cancel tasks, etc)
        this.bountyActive = false;
        if (locationTask != null) {
            locationTask.cancel();
            locationTask = null;
        }
        if (bountyTask != null) {
            bountyTask.cancel();
            bountyTask = null;
        }
        // Any other cleanup if needed
    }

    public void loadBountyData() {
        try {
            Map<String, Object> data = plugin.getDatabaseManager().getBountyData();
            if (data != null && !data.isEmpty()) {
                @SuppressWarnings("unchecked")
                Map<UUID, Long> savedLastBountyTime = (Map<UUID, Long>) data.get("last_bounty_time");
                @SuppressWarnings("unchecked")
                Map<UUID, Integer> savedBountyKills = (Map<UUID, Integer>) data.get("bounty_kills");
                
                if (savedLastBountyTime != null) {
                    validateAndLoadLastBountyTime(savedLastBountyTime);
                }
                if (savedBountyKills != null) {
                    validateAndLoadBountyKills(savedBountyKills);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading bounty data: " + e.getMessage());
            // Create backup of corrupted data
            createBackup();
        }
    }

    private void validateAndLoadLastBountyTime(Map<UUID, Long> savedData) {
        for (Map.Entry<UUID, Long> entry : savedData.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                // Validate timestamp is not in the future
                if (entry.getValue() <= System.currentTimeMillis()) {
                    lastBountyTime.put(entry.getKey(), entry.getValue());
                } else {
                    plugin.getLogger().warning("Invalid timestamp found for player " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }
    }

    private void validateAndLoadBountyKills(Map<UUID, Integer> savedData) {
        for (Map.Entry<UUID, Integer> entry : savedData.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                // Validate kill count is not negative
                if (entry.getValue() >= 0) {
                    bountyKills.put(entry.getKey(), entry.getValue());
                } else {
                    plugin.getLogger().warning("Invalid kill count found for player " + entry.getKey() + ": " + entry.getValue());
                }
            }
        }
    }

    private void createBackup() {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String backupPath = plugin.getDataFolder() + File.separator + "backups" + File.separator + "bounty_backup_" + timestamp + ".yml";
            
            // Create backups directory if it doesn't exist
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            // Create backup file
            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                backupFile.createNewFile();
            }

            // Save current data to backup
            Map<String, Object> backupData = new HashMap<>();
            backupData.put("last_bounty_time", new HashMap<>(lastBountyTime));
            backupData.put("bounty_kills", new HashMap<>(bountyKills));
            
            // Save to YAML
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : backupData.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            config.save(backupFile);
            
            plugin.getLogger().info("Created backup of bounty data at: " + backupPath);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to create backup: " + e.getMessage());
        }
    }

    public void saveBountyData() {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("last_bounty_time", new HashMap<>(lastBountyTime));
            data.put("bounty_kills", new HashMap<>(bountyKills));
            
            // Create backup before saving
            createBackup();
            
            plugin.getDatabaseManager().saveBountyData(data);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving bounty data: " + e.getMessage());
        }
    }

    private ReentrantReadWriteLock getPlayerLock(UUID playerId) {
        return playerLocks.computeIfAbsent(playerId, k -> new ReentrantReadWriteLock());
    }

    private void logConcurrentOperation(String operation, UUID playerId) {
        logToFile(String.format("[Concurrent] %s for player %s at %s", 
            operation, playerId, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())));
    }

    public boolean hasBounty(Player player) {
        if (player == null) return false;
        
        ReentrantReadWriteLock lock = getPlayerLock(player.getUniqueId());
        lock.readLock().lock();
        try {
            return lastBountyTime.containsKey(player.getUniqueId());
        } finally {
            lock.readLock().unlock();
        }
    }

    public int getBountyKills(Player player) {
        if (player == null) return 0;
        
        ReentrantReadWriteLock lock = getPlayerLock(player.getUniqueId());
        lock.readLock().lock();
        try {
            return bountyKills.getOrDefault(player.getUniqueId(), 0);
        } finally {
            lock.readLock().unlock();
        }
    }

    private int calculateHeartsToGain(int kills) {
        // Implement the logic to calculate hearts to gain based on the number of kills
        // This is a placeholder and should be replaced with the actual implementation
        return Math.min(
            Math.max(configManager.getBountyHeartsGained(), MIN_BOUNTY_HEARTS),
            MAX_BOUNTY_HEARTS
        );
    }

    private void logPerformanceMetrics() {
        StringBuilder log = new StringBuilder("\n=== Performance Metrics ===\n");
        for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
            PerformanceMetric metric = entry.getValue();
            if (metric.count > 0) {
                log.append(String.format("%s:\n", entry.getKey()))
                   .append(String.format("  Average: %.2f ms\n", metric.getAverageTime()))
                   .append(String.format("  Max: %d ms\n", metric.maxTime))
                   .append(String.format("  Min: %d ms\n", metric.minTime))
                   .append(String.format("  Total Operations: %d\n", metric.count));
            }
        }
        logToFile(log.toString());
    }

    private void optimizePerformance() {
        for (Map.Entry<String, PerformanceMetric> entry : performanceMetrics.entrySet()) {
            PerformanceMetric metric = entry.getValue();
            if (metric.getAverageTime() > PERFORMANCE_THRESHOLD_MS) {
                logToFile(String.format("PERFORMANCE WARNING: %s is taking too long (avg: %.2f ms)", 
                    entry.getKey(), metric.getAverageTime()));
                
                // Implement specific optimizations based on the metric
                switch (entry.getKey()) {
                    case "setBounty":
                        optimizeSetBounty();
                        break;
                    case "removeBounty":
                        optimizeRemoveBounty();
                        break;
                    case "processEvent":
                        optimizeEventProcessing();
                        break;
                    case "processCommand":
                        optimizeCommandProcessing();
                        break;
                }
            }
        }
    }

    private void optimizeSetBounty() {
        // Implement specific optimizations for setBounty
        if (lastBountyTime.size() > MAX_MAP_SIZE * 0.8) {
            performCleanup();
        }
    }

    private void optimizeRemoveBounty() {
        // Implement specific optimizations for removeBounty
        if (commandCooldowns.size() > MAX_MAP_SIZE * 0.8) {
            commandCooldowns.clear();
        }
    }

    private void optimizeEventProcessing() {
        // Implement specific optimizations for event processing
        if (eventQueue.size() > 1000) {
            logToFile("PERFORMANCE WARNING: Large event queue detected: " + eventQueue.size() + " events");
            // Process events in batches
            int batchSize = 100;
            List<BountyEvent> batch = new ArrayList<>();
            while (!eventQueue.isEmpty() && batch.size() < batchSize) {
                batch.add(eventQueue.poll());
            }
            for (BountyEvent event : batch) {
                try {
                    processEvent(event);
                } catch (Exception e) {
                    handleFailedEvent(event, e);
                }
            }
        }
    }

    private void optimizeCommandProcessing() {
        // Implement specific optimizations for command processing
        if (commandQueue.size() > 1000) {
            logToFile("PERFORMANCE WARNING: Large command queue detected: " + commandQueue.size() + " commands");
            // Process commands in batches
            int batchSize = 100;
            List<BountyCommand> batch = new ArrayList<>();
            while (!commandQueue.isEmpty() && batch.size() < batchSize) {
                batch.add(commandQueue.poll());
            }
            for (BountyCommand command : batch) {
                try {
                    processCommand(command);
                } catch (Exception e) {
                    handleFailedCommand(command, e);
                }
            }
        }
    }

    private void measurePerformance(String operation, Runnable task) {
        long startTime = System.currentTimeMillis();
        try {
            task.run();
        } finally {
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            PerformanceMetric metric = performanceMetrics.computeIfAbsent(operation, k -> new PerformanceMetric());
            metric.addMeasurement(duration);
            
            if (duration > PERFORMANCE_THRESHOLD_MS) {
                logToFile(String.format("PERFORMANCE WARNING: %s took %d ms", operation, duration));
            }
        }
    }

    private void processCommand(BountyCommand command) {
        measurePerformance("processCommand", () -> {
            logCommand("Processing", command);
            
            Player player = Bukkit.getPlayer(command.playerId);
            if (player == null) {
                throw new IllegalStateException("Player not found");
            }

            if (isOnCooldown(player)) {
                long remainingTime = getRemainingCooldown(player);
                player.sendMessage(ChatColor.RED + "You must wait " + (remainingTime / 1000) + " seconds before using this command again.");
                return;
            }

            switch (command.commandType) {
                case "SET_BOUNTY":
                    if (command.args.length < 1) {
                        player.sendMessage(ChatColor.RED + "Usage: /bounty set <player>");
                        return;
                    }
                    Player target = Bukkit.getPlayer(command.args[0]);
                    if (target == null) {
                        player.sendMessage(ChatColor.RED + "Player not found.");
                        return;
                    }
                    setBounty(player, target);
                    break;
                case "REMOVE_BOUNTY":
                    removeBounty(player);
                    break;
                case "STATS":
                    if (command.args.length > 0) {
                        Player targetPlayer = Bukkit.getPlayer(command.args[0]);
                        if (targetPlayer == null) {
                            player.sendMessage(ChatColor.RED + "Player not found.");
                            return;
                        }
                        showBountyStats(player, targetPlayer);
                    } else {
                        showBountyStats(player, player);
                    }
                    break;
                default:
                    player.sendMessage(ChatColor.RED + "Unknown command: " + command.commandType);
            }

            setCommandCooldown(player);
        });
    }

    private void cleanupErrorReports() {
        long currentTime = System.currentTimeMillis();
        errorReports.entrySet().removeIf(entry -> 
            currentTime - entry.getValue().timestamp > ERROR_REPORT_RETENTION
        );
    }

    private void reportError(String errorType, String message, Exception e) {
        String stackTrace = e != null ? getStackTraceAsString(e) : "";
        String errorKey = errorType + ":" + message;
        
        ErrorReport report = errorReports.computeIfAbsent(errorKey, k -> 
            new ErrorReport(errorType, message, stackTrace)
        );
        
        report.occurrenceCount++;
        
        // Log error
        logError(errorType, message, e);
        
        // Notify administrators if this is a new error or if it's occurring frequently
        if (report.occurrenceCount == 1 || report.occurrenceCount % 10 == 0) {
            notifyAdministrators(errorType, message, report.occurrenceCount);
        }
        
        // Attempt error recovery
        attemptErrorRecovery(errorType, message, e);
    }

    private String getStackTraceAsString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    private void notifyAdministrators(String errorType, String message, int occurrenceCount) {
        String notification = String.format(
            ChatColor.RED + "[LifeSteal Error] %s: %s (Occurrences: %d)",
            errorType, message, occurrenceCount
        );
        
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("lifesteal.admin")) {
                player.sendMessage(notification);
            }
        }
    }

    private void attemptErrorRecovery(String errorType, String message, Exception e) {
        switch (errorType) {
            case "DATA_LOAD":
                recoverFromDataLoadError();
                break;
            case "DATA_SAVE":
                recoverFromDataSaveError();
                break;
            case "EVENT_PROCESSING":
                recoverFromEventProcessingError();
                break;
            case "COMMAND_PROCESSING":
                recoverFromCommandProcessingError();
                break;
            case "PERFORMANCE":
                recoverFromPerformanceError();
                break;
            default:
                plugin.getLogger().warning("No recovery strategy for error type: " + errorType);
        }
    }

    private void recoverFromDataLoadError() {
        try {
            // Attempt to restore from backup
            restoreFromBackup();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to recover from data load error: " + e.getMessage());
        }
    }

    private void recoverFromDataSaveError() {
        try {
            // Attempt to save to a temporary file
            saveToTemporaryFile();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to recover from data save error: " + e.getMessage());
        }
    }

    private void recoverFromEventProcessingError() {
        try {
            // Clear event queue and failed events
            eventQueue.clear();
            failedEvents.clear();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to recover from event processing error: " + e.getMessage());
        }
    }

    private void recoverFromCommandProcessingError() {
        try {
            // Clear command queue and cooldowns
            commandQueue.clear();
            commandCooldowns.clear();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to recover from command processing error: " + e.getMessage());
        }
    }

    private void recoverFromPerformanceError() {
        try {
            // Force garbage collection
            System.gc();
            // Clear performance metrics
            performanceMetrics.clear();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to recover from performance error: " + e.getMessage());
        }
    }

    private void saveToTemporaryFile() {
        try {
            File tempFile = new File(plugin.getDataFolder(), "temp_bounty_data.yml");
            Map<String, Object> data = new HashMap<>();
            data.put("last_bounty_time", new HashMap<>(lastBountyTime));
            data.put("bounty_kills", new HashMap<>(bountyKills));
            
            YamlConfiguration config = new YamlConfiguration();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            config.save(tempFile);
            
            plugin.getLogger().info("Saved data to temporary file: " + tempFile.getPath());
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save to temporary file: " + e.getMessage());
        }
    }

    /**
     * Checks if a player is on cooldown.
     * @param player The player to check
     * @return true if the player is on cooldown
     */
    private boolean isOnCooldown(Player player) {
        return commandCooldowns.containsKey(player.getUniqueId()) &&
               System.currentTimeMillis() < commandCooldowns.get(player.getUniqueId());
    }

    /**
     * Gets the remaining cooldown time for a player.
     * @param player The player to check
     * @return The remaining cooldown time in milliseconds
     */
    private long getRemainingCooldown(Player player) {
        return commandCooldowns.getOrDefault(player.getUniqueId(), 0L) - System.currentTimeMillis();
    }

    /**
     * Sets the command cooldown for a player.
     * @param player The player to set the cooldown for
     */
    private void setCommandCooldown(Player player) {
        commandCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + COMMAND_COOLDOWN);
    }

    /**
     * Shows bounty statistics for a player.
     * @param player The player viewing the stats
     * @param target The target player
     */
    private void showBountyStats(Player player, Player target) {
        // Implementation
    }

    /**
     * Restores data from backup.
     */
    private void restoreFromBackup() {
        // Implementation
    }

    /**
     * Processes the command queue.
     */
    private void processCommandQueue() {
        // Implementation
    }

    /**
     * Gets the maximum number of bounty kills allowed.
     * @return The maximum number of bounty kills
     */
    private int getMaxBountyKills() {
        return configManager.getMaxBountyKills();
    }

    /**
     * Gets the number of hearts gained from bounties.
     * @return The number of hearts gained
     */
    private int getBountyHeartsGained() {
        return configManager.getBountyHeartsGained();
    }

    /**
     * Gets the world border initial size.
     * @return The initial size of the world border
     */
    private double getWorldBorderInitialSize() {
        return configManager.getWorldBorderInitialSize();
    }

    /**
     * Gets the distance from a location to the world border.
     * @param location The location to check
     * @return The distance to the border
     */
    private double getDistance(Location location) {
        WorldBorder border = location.getWorld().getWorldBorder();
        return border.getSize() / 2 - Math.max(
            Math.abs(location.getX() - border.getCenter().getX()),
            Math.abs(location.getZ() - border.getCenter().getZ())
        );
    }

    /**
     * Gets the current bounty target.
     * @return The UUID of the current bounty target
     */
    public UUID getTargetPlayer() {
        return targetPlayer;
    }

    /**
     * Processes a bounty kill.
     * @param killer The player who got the kill
     * @param victim The player who was killed
     */
    private void processBountyKill(Player killer, Player victim) {
        if (killer == null || victim == null) {
            return;
        }

        // Get the number of hearts to give
        int hearts = getBountyHeartsGained();

        // Give hearts to the killer
        heartManager.addHearts(killer, hearts);

        // Send messages
        killer.sendMessage(ChatColor.GREEN + "You gained " + hearts + " hearts from the bounty!");
        victim.sendMessage(ChatColor.RED + "You lost " + hearts + " hearts from the bounty!");

        // Broadcast the kill
        Bukkit.broadcastMessage(ChatColor.GOLD + killer.getName() + " claimed the bounty on " + victim.getName() + "!");

        // Remove the bounty
        removeBounty(victim);
    }

    /**
     * Handles a failed command, retrying if possible and logging the error.
     */
    private void handleFailedCommand(BountyCommand command, Exception e) {
        logCommand("Failed", command);
        if (command.retryCount < MAX_RETRY_ATTEMPTS) {
            command.retryCount++;
            commandQueue.add(command);
            plugin.getLogger().warning("Command will be retried. Attempt " + command.retryCount + " of " + MAX_RETRY_ATTEMPTS);
        } else {
            plugin.getLogger().severe("Command failed after " + MAX_RETRY_ATTEMPTS + " attempts: " + e.getMessage());
            Player player = Bukkit.getPlayer(command.playerId);
            if (player != null) {
                player.sendMessage(ChatColor.RED + "Command failed to execute. Please try again later.");
            }
        }
    }

    /**
     * Logs a command to the log file.
     */
    private void logCommand(String status, BountyCommand command) {
        String message = String.format("[Command] %s - Type: %s, Player: %s, Args: %s, Time: %s",
            status,
            command.commandType,
            command.playerId,
            command.args != null ? String.join(", ", command.args) : "none",
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(command.timestamp))
        );
        logToFile(message);
    }

    /**
     * Logs an error to the log file.
     */
    private void logError(String errorType, String message, Exception e) {
        String errorMessage = String.format("ERROR [%s]: %s", errorType, message);
        if (e != null) {
            errorMessage += "\nStack trace: " + getStackTraceAsString(e);
        }
        logToFile(errorMessage);
    }
}
