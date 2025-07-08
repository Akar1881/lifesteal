package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WorldBorderManager {
    private final LifeSteal plugin;
    private BukkitTask shrinkTask;
    private BukkitTask warningTask;
    private double currentSize;
    private double initialSize;
    private long lastShrinkTime;
    private long nextShrinkTime;
    private final int warningTime; // Warning time in seconds

    public WorldBorderManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.warningTime = plugin.getConfigManager().getConfig().getInt("world-border.warning-time", 10); // Default 10 seconds
        loadBorderData();
    }

    public void loadBorderData() {
        Map<String, Object> data = plugin.getDatabaseManager().getWorldBorderData();
        this.currentSize = (double) data.get("current_size");
        this.initialSize = (double) data.get("initial_size");
        this.lastShrinkTime = (long) data.get("last_shrink_time");
        this.nextShrinkTime = (long) data.get("next_shrink_time");
        
        // Check if config size has changed
        double configSize = plugin.getConfigManager().getInitialBorderSize();
        if (configSize != this.initialSize) {
            // Config size has changed, reset to new size
            this.initialSize = configSize;
            this.currentSize = configSize;
            this.lastShrinkTime = System.currentTimeMillis();
            this.nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
            saveBorderData();
        }
    }

    public void saveBorderData() {
        plugin.getDatabaseManager().saveWorldBorderData(currentSize, initialSize, lastShrinkTime, nextShrinkTime);
    }

    public void initializeBorder() {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }

        List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
        boolean useWorldSpawn = plugin.getConfigManager().useWorldSpawnAsCenter();
        double centerX = plugin.getConfigManager().getWorldBorderCenterX();
        double centerZ = plugin.getConfigManager().getWorldBorderCenterZ();
        
        // Get the initial size from config
        double configSize = plugin.getConfigManager().getInitialBorderSize();
        
        // If config size has changed, update both initial and current size
        if (configSize != this.initialSize) {
            this.initialSize = configSize;
            this.currentSize = configSize;
            this.lastShrinkTime = System.currentTimeMillis();
            this.nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
            saveBorderData();
        }
        
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                
                if (useWorldSpawn) {
                    Location spawnLocation = world.getSpawnLocation();
                    border.setCenter(spawnLocation.getX(), spawnLocation.getZ());
                    plugin.getLogger().info("Using world spawn as border center: " + 
                            spawnLocation.getX() + ", " + spawnLocation.getZ());
                } else {
                    border.setCenter(centerX, centerZ);
                }
                
                border.setSize(currentSize);
                border.setDamageAmount(plugin.getConfigManager().getWorldBorderDamageAmount());
                border.setDamageBuffer(plugin.getConfigManager().getWorldBorderDamageBuffer());
                border.setWarningDistance(plugin.getConfigManager().getWorldBorderWarningDistance());
                plugin.getLogger().info("Initialized world border for world: " + worldName + " with size: " + currentSize + " (initial: " + initialSize + ")");
            } else {
                plugin.getLogger().warning("Could not find world: " + worldName + " for world border initialization");
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
        long intervalTicks = intervalMinutes * 60 * 20;
        
        long now = System.currentTimeMillis();
        long initialDelay = 0;
        
        if (nextShrinkTime > now) {
            initialDelay = (nextShrinkTime - now) / 50;
        } else if (nextShrinkTime == 0) {
            nextShrinkTime = now + TimeUnit.MINUTES.toMillis(intervalMinutes);
            saveBorderData();
        }
        
        shrinkTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (plugin.getConfigManager().isWorldBorderEnabled() && 
                plugin.getConfigManager().isWorldBorderShrinkEnabled()) {
                shrinkBorder(false);
            }
        }, initialDelay, intervalTicks);
        
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
    }

    public void cancelWarningTask() {
        if (warningTask != null) {
            warningTask.cancel();
            warningTask = null;
        }
    }

    public void updateConfig() {
        // Stop existing tasks
        stopShrinkTask();
        
        // Reinitialize border if enabled
        if (plugin.getConfigManager().isWorldBorderEnabled()) {
            initializeBorder();
            startShrinkTask();
        }
    }

    public void shrinkBorder(boolean immediate) {
        if (!plugin.getConfigManager().isWorldBorderEnabled() || !plugin.getConfigManager().isWorldBorderShrinkEnabled()) {
            return;
        }

        final double currentSize = getCurrentSize();
        double minSize = plugin.getConfigManager().getWorldBorderMinSize();

        // Check if we've reached minimum size
        if (currentSize <= minSize) {
            // Stop the shrinking task since we've reached minimum
            if (shrinkTask != null) {
                shrinkTask.cancel();
                shrinkTask = null;
            }
            // Send message only once
            String message = plugin.getConfigManager().getConfig().getString("world-border.messages.min-size-reached");
            if (message != null) {
                Bukkit.broadcastMessage(ColorUtils.colorize(message));
            }
            return;
        }

        double shrinkAmount = plugin.getConfigManager().getWorldBorderShrinkAmount();
        final double actualShrinkAmount = shrinkAmount;

        if (immediate) {
            // Calculate new size
            double newSize = currentSize - actualShrinkAmount;
            if (newSize < minSize) {
                newSize = minSize;
            }
            
            // Update current size
            this.currentSize = newSize;
            
            // Apply to world border
            List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
            for (String worldName : worldNames) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    WorldBorder border = world.getWorldBorder();
                    border.setSize(newSize, 0);
                    plugin.getLogger().info("World border size changed to: " + newSize + " in world: " + worldName);
                }
            }
            
            lastShrinkTime = System.currentTimeMillis();
            nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
            saveBorderData();
            
            String shrunkMessage = plugin.getConfigManager().getConfig().getString("world-border.messages.border-shrunk", "&c&lBORDER SHRUNK! &fThe world border has shrunk to &e%size% &fblocks!")
                    .replace("%size%", String.valueOf((int) newSize));
            Bukkit.broadcastMessage(ColorUtils.colorize(shrunkMessage));
            
            // Play scary sound
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), "entity.ender_dragon.death", 1.0f, 0.5f);
            }
        } else {
            // Schedule all warning messages
            int totalSeconds = plugin.getConfigManager().getWorldBorderShrinkInterval() * 60;
            List<ConfigManager.WarningTime> warningTimes = plugin.getConfigManager().getWorldBorderWarningTimes();
            for (ConfigManager.WarningTime warning : warningTimes) {
                int delay = totalSeconds - warning.seconds;
                if (delay < 0) continue;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    Bukkit.broadcastMessage(ColorUtils.colorize(warning.message));
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), "entity.ender_dragon.growl", 1.0f, 1.0f);
                    }
                }, delay * 20L);
            }
            // Schedule the actual shrink at the end of the interval
            warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Calculate new size
                double newSize = currentSize - actualShrinkAmount;
                if (newSize < minSize) {
                    newSize = minSize;
                }
                // Update current size
                WorldBorderManager.this.currentSize = newSize;
                // Apply to world border
                List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
                for (String worldName : worldNames) {
                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        WorldBorder border = world.getWorldBorder();
                        border.setSize(newSize, 0);
                        plugin.getLogger().info("World border size changed to: " + newSize + " in world: " + worldName);
                    }
                }
                lastShrinkTime = System.currentTimeMillis();
                nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
                saveBorderData();
                String shrunkMessage = plugin.getConfigManager().getConfig().getString("world-border.messages.border-shrunk", "&c&lBORDER SHRUNK! &fThe world border has shrunk to &e%size% &fblocks!")
                        .replace("%size%", String.valueOf((int) newSize));
                Bukkit.broadcastMessage(ColorUtils.colorize(shrunkMessage));
                // Play scary sound
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), "entity.ender_dragon.death", 1.0f, 0.5f);
                }
            }, totalSeconds * 20L);
        }
    }

    public void resetBorder() {
        double configSize = plugin.getConfigManager().getInitialBorderSize();
        List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
        
        // Update both initial and current size
        this.initialSize = configSize;
        this.currentSize = configSize;
        this.lastShrinkTime = System.currentTimeMillis();
        this.nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
        saveBorderData();
        
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                border.setSize(configSize);
            }
        }
        
        plugin.getLogger().info("World border reset to initial size: " + configSize);
    }

    public boolean isOutsideBorder(Location location) {
        World world = location.getWorld();
        if (world == null) return false;
        
        List<String> configuredWorlds = plugin.getConfigManager().getWorldBorderWorlds();
        if (!configuredWorlds.contains(world.getName())) {
            return false;
        }
        
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double size = border.getSize() / 2;
        
        double x = location.getX();
        double z = location.getZ();
        
        return Math.abs(x - center.getX()) > size || Math.abs(z - center.getZ()) > size;
    }
    
    public double getDistanceOutsideBorder(Location location) {
        World world = location.getWorld();
        if (world == null) return 0;
        
        List<String> configuredWorlds = plugin.getConfigManager().getWorldBorderWorlds();
        if (!configuredWorlds.contains(world.getName())) {
            return 0;
        }
        
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2;
        
        double x = location.getX();
        double z = location.getZ();
        
        double dx = Math.abs(x - center.getX()) - radius;
        double dz = Math.abs(z - center.getZ()) - radius;
        
        if (dx <= 0 && dz <= 0) {
            return 0;
        }
        
        if (dx > 0 && dz > 0) {
            return Math.sqrt(dx * dx + dz * dz);
        } else {
            return Math.max(dx, dz);
        }
    }
    
    public boolean isNearBorder(Location location, double distance) {
        World world = location.getWorld();
        if (world == null) return false;
        
        List<String> configuredWorlds = plugin.getConfigManager().getWorldBorderWorlds();
        if (!configuredWorlds.contains(world.getName())) {
            return false;
        }
        
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double radius = border.getSize() / 2;
        
        double x = location.getX();
        double z = location.getZ();
        
        double dx = Math.abs(x - center.getX());
        double dz = Math.abs(z - center.getZ());
        
        return (Math.abs(dx - radius) < distance || Math.abs(dz - radius) < distance);
    }

    public double getCurrentSize() {
        return currentSize;
    }

    public long getTimeUntilNextShrink() {
        return Math.max(0, nextShrinkTime - System.currentTimeMillis());
    }

    public String getFormattedTimeUntilNextShrink() {
        long millis = getTimeUntilNextShrink();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) - TimeUnit.MINUTES.toSeconds(minutes);
        
        return String.format("%02d:%02d", minutes, seconds);
    }

    public String formatTime(int seconds) {
        if (seconds >= 86400) { // 1 day or more
            int days = seconds / 86400;
            return days + " day" + (days > 1 ? "s" : "");
        } else if (seconds >= 3600) { // 1 hour or more
            int hours = seconds / 3600;
            return hours + " hour" + (hours > 1 ? "s" : "");
        } else if (seconds >= 60) { // 1 minute or more
            int minutes = seconds / 60;
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds > 1 ? "s" : "");
        }
    }
}