package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WorldBorderManager {
    private final LifeSteal plugin;
    private FileConfiguration borderData;
    private File borderDataFile;
    private BukkitTask shrinkTask;
    private BukkitTask warningTask;
    private double currentSize;
    private long lastShrinkTime;
    private long nextShrinkTime;

    public WorldBorderManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadBorderData();
    }

    public void loadBorderData() {
        borderDataFile = new File(plugin.getDataFolder(), "border-data.yml");
        if (!borderDataFile.exists()) {
            plugin.saveResource("border-data.yml", false);
        }
        borderData = YamlConfiguration.loadConfiguration(borderDataFile);
        
        double configInitialSize = plugin.getConfigManager().getInitialBorderSize();
        
        if (!borderData.contains("current-size") || 
            !borderData.contains("initial-config-size") || 
            borderData.getDouble("initial-config-size") != configInitialSize) {
            
            currentSize = configInitialSize;
            borderData.set("current-size", currentSize);
            borderData.set("initial-config-size", configInitialSize);
            lastShrinkTime = 0;
            nextShrinkTime = 0;
            borderData.set("last-shrink-time", lastShrinkTime);
            borderData.set("next-shrink-time", nextShrinkTime);
            
            plugin.getLogger().info("Updated border size to match config: " + currentSize);
            
            try {
                borderData.save(borderDataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save border data: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            currentSize = borderData.getDouble("current-size", configInitialSize);
            lastShrinkTime = borderData.getLong("last-shrink-time", 0);
            nextShrinkTime = borderData.getLong("next-shrink-time", 0);
        }
    }

    public void saveBorderData() {
        borderData.set("current-size", currentSize);
        borderData.set("last-shrink-time", lastShrinkTime);
        borderData.set("next-shrink-time", nextShrinkTime);
        borderData.set("initial-config-size", plugin.getConfigManager().getInitialBorderSize());
        
        try {
            borderData.save(borderDataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save border data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void initializeBorder() {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }

        List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
        boolean useWorldSpawn = plugin.getConfigManager().useWorldSpawnAsCenter();
        double centerX = plugin.getConfigManager().getWorldBorderCenterX();
        double centerZ = plugin.getConfigManager().getWorldBorderCenterZ();
        
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
                plugin.getLogger().info("Initialized world border for world: " + worldName + " with size: " + currentSize);
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
    }

    public void shrinkBorder() {
        if (!plugin.getConfigManager().isWorldBorderEnabled() || !plugin.getConfigManager().isWorldBorderShrinkEnabled()) {
            return;
        }
        
        final double shrinkAmount = plugin.getConfigManager().getWorldBorderShrinkAmount();
        final double minSize = plugin.getConfigManager().getWorldBorderMinSize();
        final int warningTime = plugin.getConfigManager().getWorldBorderWarningTime();
        
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
        
        warningTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            double newSize = currentSize - finalShrinkAmount;
            if (newSize < minSize) {
                newSize = minSize;
            }
            
            List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
            for (String worldName : worldNames) {
                World world = Bukkit.getWorld(worldName);
                if (world != null) {
                    WorldBorder border = world.getWorldBorder();
                    border.setSize(newSize, 30);
                }
            }
            
            currentSize = newSize;
            lastShrinkTime = System.currentTimeMillis();
            nextShrinkTime = lastShrinkTime + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
            saveBorderData();
            
            String shrunkMessage = plugin.getConfigManager().getWorldBorderShrunkMessage()
                    .replace("%size%", String.valueOf((int) newSize));
            Bukkit.broadcastMessage(ColorUtils.colorize(shrunkMessage));
            
            plugin.getLogger().info("World border shrunk to: " + newSize);
        }, warningTime * 20L);
    }

    public void resetBorder() {
        double initialSize = plugin.getConfigManager().getInitialBorderSize();
        List<String> worldNames = plugin.getConfigManager().getWorldBorderWorlds();
        
        for (String worldName : worldNames) {
            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                WorldBorder border = world.getWorldBorder();
                border.setSize(initialSize);
            }
        }
        
        currentSize = initialSize;
        lastShrinkTime = 0;
        nextShrinkTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(plugin.getConfigManager().getWorldBorderShrinkInterval());
        
        borderData.set("initial-config-size", initialSize);
        saveBorderData();
        
        plugin.getLogger().info("World border reset to initial size: " + initialSize);
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
}