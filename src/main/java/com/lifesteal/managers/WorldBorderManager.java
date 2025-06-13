package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class WorldBorderManager {
    private final LifeSteal plugin;
    private BukkitTask shrinkTask;
    private BukkitTask warningTask;
    private double currentSize;
    private long lastShrinkTime;
    private long nextShrinkTime;
    private final AtomicBoolean isShrinking = new AtomicBoolean(false);
    
    // Constants for validation
    private static final double MIN_BORDER_SIZE = 1.0;
    private static final double MAX_BORDER_SIZE = 60000000.0; // Minecraft's maximum world size
    private static final int MAX_WARNING_TIME = 3600; // 1 hour in seconds
    private static final int MIN_WARNING_TIME = 5; // 5 seconds
    private static final int SHRINK_DURATION = 30; // Duration in seconds for border shrinking animation

    public WorldBorderManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadBorderData();
    }

    public void loadBorderData() {
        try {
            Map<String, Object> data = plugin.getDatabaseManager().getWorldBorderData();
            if (data == null || data.isEmpty()) {
                plugin.getLogger().warning("No world border data found in database, using defaults");
                initializeDefaultData();
                return;
            }

            this.currentSize = validateBorderSize((double) data.get("current_size"));
            this.lastShrinkTime = (long) data.get("last_shrink_time");
            this.nextShrinkTime = (long) data.get("next_shrink_time");

            if (this.currentSize <= 0 || this.lastShrinkTime < 0 || this.nextShrinkTime < 0) {
                plugin.getLogger().warning("Invalid world border data found, resetting to defaults");
                initializeDefaultData();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading world border data: " + e.getMessage());
            initializeDefaultData();
        }
    }

    private void initializeDefaultData() {
        this.currentSize = plugin.getConfigManager().getWorldBorderInitialSize();
        this.lastShrinkTime = System.currentTimeMillis();
        this.nextShrinkTime = this.lastShrinkTime + TimeUnit.MINUTES.toMillis(
            plugin.getConfigManager().getWorldBorderShrinkInterval()
        );
        saveBorderData();
    }

    private double validateBorderSize(double size) {
        if (size < MIN_BORDER_SIZE) {
            plugin.getLogger().warning("Border size " + size + " is below minimum, using " + MIN_BORDER_SIZE);
            return MIN_BORDER_SIZE;
        }
        if (size > MAX_BORDER_SIZE) {
            plugin.getLogger().warning("Border size " + size + " exceeds maximum, using " + MAX_BORDER_SIZE);
            return MAX_BORDER_SIZE;
        }
        return size;
    }

    public void saveBorderData() {
        try {
            plugin.getDatabaseManager().saveWorldBorderData(currentSize, lastShrinkTime, nextShrinkTime);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving world border data: " + e.getMessage());
        }
    }

    public void initializeBorder() {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            plugin.getLogger().info("World border is disabled in configuration");
            return;
        }

        List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
        if (worldNames == null || worldNames.isEmpty()) {
            plugin.getLogger().warning("No worlds configured for world border!");
            return;
        }

        boolean useWorldSpawn = plugin.getConfigManager().useWorldSpawnAsCenter();
        double centerX = plugin.getConfigManager().getWorldBorderCenterX();
        double centerZ = plugin.getConfigManager().getWorldBorderCenterZ();
        
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Could not find world: " + worldName + " for world border initialization");
                continue;
            }

            WorldBorder border = world.getWorldBorder();
            if (border == null) {
                plugin.getLogger().warning("Could not get world border for world: " + worldName);
                continue;
            }
            
            try {
                if (useWorldSpawn) {
                    Location spawnLocation = world.getSpawnLocation();
                    if (spawnLocation == null) {
                        plugin.getLogger().warning("Could not get spawn location for world: " + worldName);
                        continue;
                    }
                    border.setCenter(spawnLocation.getX(), spawnLocation.getZ());
                    plugin.getLogger().info("Using world spawn as border center: " + 
                            spawnLocation.getX() + ", " + spawnLocation.getZ());
                } else {
                    border.setCenter(centerX, centerZ);
                }
                
                double validatedSize = validateBorderSize(currentSize);
                border.setSize(validatedSize);
                border.setDamageAmount(plugin.getConfigManager().getWorldBorderDamageAmount());
                border.setDamageBuffer(plugin.getConfigManager().getWorldBorderDamageBuffer());
                border.setWarningDistance(plugin.getConfigManager().getWorldBorderWarningDistance());
                plugin.getLogger().info("Initialized world border for world: " + worldName + " with size: " + validatedSize);
            } catch (Exception e) {
                plugin.getLogger().severe("Error initializing world border for world " + worldName + ": " + e.getMessage());
            }
        }
        
        if (plugin.getConfigManager().isWorldBorderShrinkEnabled()) {
            startShrinkTask();
        }
    }

    public void startShrinkTask() {
        if (shrinkTask != null) {
            shrinkTask.cancel();
        }
        
        long intervalMinutes = plugin.getConfigManager().getWorldBorderShrinkInterval();
        if (intervalMinutes <= 0) {
            plugin.getLogger().warning("Invalid shrink interval: " + intervalMinutes + " minutes");
            return;
        }
        
        long intervalTicks = intervalMinutes * 60 * 20;
        long now = System.currentTimeMillis();
        long initialDelay = 0;
        
        if (nextShrinkTime > now) {
            initialDelay = (nextShrinkTime - now) / 50;
        } else if (nextShrinkTime == 0) {
            nextShrinkTime = now + TimeUnit.MINUTES.toMillis(intervalMinutes);
            saveBorderData();
        }
        
        shrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, this::shrinkBorder, initialDelay, intervalTicks);
        plugin.getLogger().info("Started world border shrink task with interval: " + intervalMinutes + " minutes");
    }

    public void stopShrinkTask() {
        if (shrinkTask != null) {
            shrinkTask.cancel();
            shrinkTask = null;
        }
        
        if (warningTask != null) {
            warningTask.cancel();
            warningTask = null;
        }
        
        isShrinking.set(false);
    }

    public void shrinkBorder() {
        if (!plugin.getConfigManager().isWorldBorderEnabled() || 
            !plugin.getConfigManager().isWorldBorderShrinkEnabled() ||
            isShrinking.get()) {
            return;
        }
        
        final double shrinkAmount = plugin.getConfigManager().getWorldBorderShrinkAmount();
        final double minSize = plugin.getConfigManager().getWorldBorderMinSize();
        final int warningTime = Math.min(
            Math.max(plugin.getConfigManager().getWorldBorderWarningTime(), MIN_WARNING_TIME),
            MAX_WARNING_TIME
        );
        
        if (shrinkAmount <= 0 || minSize < MIN_BORDER_SIZE) {
            plugin.getLogger().warning("Invalid world border shrink configuration!");
            return;
        }
        
        double actualShrinkAmount = shrinkAmount;
        if (currentSize - shrinkAmount < minSize) {
            actualShrinkAmount = currentSize - minSize;
            if (actualShrinkAmount <= 0) {
                plugin.getLogger().info("World border has reached minimum size. No further shrinking.");
                return;
            }
        }
        
        if (warningTask != null) {
            warningTask.cancel();
        }
        
        String warningMessage = plugin.getConfigManager().getWorldBorderShrinkingMessage()
                .replace("%time%", String.valueOf(warningTime));
        
        Bukkit.broadcastMessage(ColorUtils.colorize(warningMessage));
        
        final double finalShrinkAmount = actualShrinkAmount;
        isShrinking.set(true);
        
        warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            try {
                double newSize = validateBorderSize(currentSize - finalShrinkAmount);
                if (newSize < minSize) {
                    newSize = minSize;
                }
                
                List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
                if (worldNames == null || worldNames.isEmpty()) {
                    plugin.getLogger().warning("No worlds configured for world border shrinking!");
                    return;
                }

                for (String worldName : worldNames) {
                    World world = Bukkit.getWorld(worldName);
                    if (world == null) {
                        plugin.getLogger().warning("Could not find world: " + worldName + " for world border shrinking");
                        continue;
                    }

                    WorldBorder border = world.getWorldBorder();
                    if (border == null) {
                        plugin.getLogger().warning("Could not get world border for world: " + worldName);
                        continue;
                    }

                    try {
                        border.setSize(newSize, SHRINK_DURATION);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error shrinking world border for world " + worldName + ": " + e.getMessage());
                    }
                }
                
                currentSize = newSize;
                lastShrinkTime = System.currentTimeMillis();
                nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
                saveBorderData();
            } finally {
                isShrinking.set(false);
            }
        }, warningTime * 20L);
    }

    public void resetBorder() {
        stopShrinkTask();
        
        double initialSize = plugin.getConfigManager().getWorldBorderInitialSize();
        currentSize = validateBorderSize(initialSize);
        lastShrinkTime = System.currentTimeMillis();
        nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
        saveBorderData();
        
        List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
        if (worldNames == null || worldNames.isEmpty()) {
            plugin.getLogger().warning("No worlds configured for world border reset!");
            return;
        }

        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("Could not find world: " + worldName + " for world border reset");
                continue;
            }

            WorldBorder border = world.getWorldBorder();
            if (border == null) {
                plugin.getLogger().warning("Could not get world border for world: " + worldName);
                continue;
            }

            try {
                border.setSize(currentSize);
                plugin.getLogger().info("Reset world border for world: " + worldName + " to size: " + currentSize);
            } catch (Exception e) {
                plugin.getLogger().severe("Error resetting world border for world " + worldName + ": " + e.getMessage());
            }
        }
        
        if (plugin.getConfigManager().isWorldBorderShrinkEnabled()) {
            startShrinkTask();
        }
    }

    public boolean isOutsideBorder(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }

        WorldBorder border = location.getWorld().getWorldBorder();
        if (border == null) {
            return false;
        }

        try {
            return !border.isInside(location);
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if location is outside border: " + e.getMessage());
            return false;
        }
    }

    public double getDistanceOutsideBorder(Location location) {
        if (location == null || location.getWorld() == null) {
            return 0.0;
        }

        WorldBorder border = location.getWorld().getWorldBorder();
        if (border == null) {
            return 0.0;
        }

        try {
            // Calculate distance manually since getDistance might not be available
            Location center = border.getCenter();
            double size = border.getSize() / 2.0;
            double dx = Math.abs(center.getX() - location.getX());
            double dz = Math.abs(center.getZ() - location.getZ());
            
            // If inside border, return 0
            if (dx <= size && dz <= size) {
                return 0.0;
            }
            
            // Calculate distance outside border
            double distanceOutside = 0.0;
            if (dx > size) {
                distanceOutside += dx - size;
            }
            if (dz > size) {
                distanceOutside += dz - size;
            }
            
            return distanceOutside;
        } catch (Exception e) {
            plugin.getLogger().warning("Error calculating distance outside border: " + e.getMessage());
            return 0.0;
        }
    }

    public boolean isNearBorder(Location location, double distance) {
        if (location == null || location.getWorld() == null || distance <= 0) {
            return false;
        }

        WorldBorder border = location.getWorld().getWorldBorder();
        if (border == null) {
            return false;
        }

        try {
            // Calculate distance manually since getDistance might not be available
            Location center = border.getCenter();
            double size = border.getSize() / 2.0;
            double dx = Math.abs(center.getX() - location.getX());
            double dz = Math.abs(center.getZ() - location.getZ());
            
            // If inside border, check if near the edge
            if (dx <= size && dz <= size) {
                return (size - dx <= distance) || (size - dz <= distance);
            }
            
            // If outside, it's definitely near the border
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking if location is near border: " + e.getMessage());
            return false;
        }
    }

    public double getCurrentSize() {
        return currentSize;
    }

    public long getTimeUntilNextShrink() {
        return Math.max(0, nextShrinkTime - System.currentTimeMillis());
    }

    public String getFormattedTimeUntilNextShrink() {
        long timeLeft = getTimeUntilNextShrink();
        if (timeLeft <= 0) {
            return "0s";
        }

        long seconds = timeLeft / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours % 24 > 0) {
            sb.append(hours % 24).append("h ");
        }
        if (minutes % 60 > 0) {
            sb.append(minutes % 60).append("m ");
        }
        if (seconds % 60 > 0) {
            sb.append(seconds % 60).append("s");
        }

        return sb.toString().trim();
    }
}