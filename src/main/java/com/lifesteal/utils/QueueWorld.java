package com.lifesteal.utils;

import com.lifesteal.LifeSteal;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.concurrent.CompletableFuture;

public class QueueWorld {
    private static final String QUEUE_WORLD_NAME = "queue";
    private final LifeSteal plugin;
    private World queueWorld;

    public QueueWorld(LifeSteal plugin) {
        this.plugin = plugin;
    }

    /**
     * Initialize the queue world if first join is enabled
     */
    public void initialize() {
        if (!plugin.getConfigManager().isFirstJoinEnabled()) {
            return;
        }

        // Check if the world already exists
        queueWorld = Bukkit.getWorld(QUEUE_WORLD_NAME);
        
        if (queueWorld == null) {
            // Create a new empty world
            WorldCreator creator = new WorldCreator(QUEUE_WORLD_NAME);
            creator.generateStructures(false); // No structures needed for queue world
            creator.environment(World.Environment.NORMAL);
            creator.type(org.bukkit.WorldType.FLAT); // Use flat world for better performance
            
            plugin.getLogger().info("Creating queue world for first join system...");
            queueWorld = creator.createWorld();
            
            if (queueWorld != null) {
                // Set world settings
                queueWorld.setSpawnLocation(0, 100, 0);
                queueWorld.setKeepSpawnInMemory(true);
                queueWorld.setAutoSave(false); // No need to save this world
                
                // Create a simple platform at spawn
                createSpawnPlatform();
                
                plugin.getLogger().info("Queue world created successfully!");
            } else {
                plugin.getLogger().severe("Failed to create queue world!");
            }
        } else {
            plugin.getLogger().info("Queue world already exists, using existing world.");
            // Ensure spawn platform exists
            createSpawnPlatform();
        }
    }

    /**
     * Create a simple platform at spawn point
     */
    private void createSpawnPlatform() {
        if (queueWorld == null) return;
        
        Location center = queueWorld.getSpawnLocation();
        int y = 100;
        center.setY(y);
        
        // Create a 5x5 platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                Location loc = new Location(queueWorld, center.getX() + x, y, center.getZ() + z);
                loc.getBlock().setType(org.bukkit.Material.STONE);
            }
        }
        
        // Set air above the platform
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                for (int yOffset = 1; yOffset <= 3; yOffset++) {
                    Location loc = new Location(queueWorld, center.getX() + x, y + yOffset, center.getZ() + z);
                    loc.getBlock().setType(org.bukkit.Material.AIR);
                }
            }
        }
    }

    /**
     * Send a player to the queue world
     * @param player The player to send
     */
    public void sendToQueueWorld(Player player) {
        if (queueWorld == null) {
            plugin.getLogger().warning("Queue world is null! Cannot send player to queue world.");
            return;
        }
        
        // Set player to spectator mode temporarily to prevent fall damage
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport player to queue world spawn
        Location spawnLoc = queueWorld.getSpawnLocation().clone().add(0.5, 1, 0.5);
        PaperLib.teleportAsync(player, spawnLoc).thenAccept(result -> {
            if (result) {
                plugin.getLogger().info("Player " + player.getName() + " sent to queue world successfully.");
            } else {
                plugin.getLogger().warning("Failed to send player " + player.getName() + " to queue world!");
            }
        });
    }

    /**
     * Find a safe location in the main world and teleport the player there
     * @param player The player to teleport
     */
    public void findSafeLocationAndTeleport(Player player) {
        player.sendTitle(
            ColorUtils.colorize("&6Searching for safe location..."),
            ColorUtils.colorize("&eThis may take a few seconds"),
            10, 60, 10
        );
        
        // Run the safe location finder asynchronously
        new BukkitRunnable() {
            @Override
            public void run() {
                // Try to find multiple safe locations in parallel for better performance
                CompletableFuture<Location>[] futures = new CompletableFuture[10];
                for (int i = 0; i < 10; i++) {
                    futures[i] = CompletableFuture.supplyAsync(() -> {
                        SafeLocationFinder finder = new SafeLocationFinder(plugin);
                        return finder.findSafeLocation();
                    });
                }
                
                // Wait for the first safe location to be found
                CompletableFuture<Object> firstCompleted = CompletableFuture.anyOf(futures);
                
                try {
                    Location safeLoc = (Location) firstCompleted.get();
                    boolean usedFallback = false;
                    
                    if (safeLoc != null && safeLoc.equals(plugin.getServer().getWorlds().get(0).getSpawnLocation())) {
                        usedFallback = true;
                    }
                    
                    if (safeLoc != null) {
                        boolean finalUsedFallback = usedFallback;
                        
                        // Show loading message
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendTitle(
                                    ColorUtils.colorize("&6Loading world..."),
                                    ColorUtils.colorize("&ePreparing your adventure"),
                                    10, 40, 10
                                );
                            }
                        }.runTask(plugin);
                        
                        // Use PaperLib for async chunk loading and teleportation
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                // Ensure the chunk is loaded
                                PaperLib.getChunkAtAsync(safeLoc, true).thenAccept(chunk -> {
                                    // Set the player's spawn point to this location
                                    player.setBedSpawnLocation(safeLoc, true);
                                    
                                    // Teleport the player
                                    PaperLib.teleportAsync(player, safeLoc).thenAccept(result -> {
                                        if (result) {
                                            // Set to survival mode
                                            player.setGameMode(GameMode.SURVIVAL);
                                            
                                            // Send messages
                                            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getFirstJoinTeleportMessage()));
                                            if (finalUsedFallback) {
                                                player.sendMessage(ColorUtils.colorize("&eNo perfect safe spot found, so you were sent to spawn!"));
                                            }
                                            
                                            // Kick player if configured to do so
                                            if (plugin.getConfigManager().shouldKickAfterFirstJoin()) {
                                                new BukkitRunnable() {
                                                    @Override
                                                    public void run() {
                                                        player.kickPlayer(ColorUtils.colorize(plugin.getConfigManager().getFirstJoinKickMessage()));
                                                    }
                                                }.runTaskLater(plugin, 60L); // 3 seconds delay
                                            }
                                        } else {
                                            player.sendMessage(ColorUtils.colorize("&cFailed to teleport to safe location. Please contact an administrator."));
                                        }
                                    });
                                });
                            }
                        }.runTask(plugin);
                    } else {
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                player.sendMessage(ColorUtils.colorize("&cFailed to find safe location. Please contact an administrator."));
                            }
                        }.runTask(plugin);
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Error finding safe location: " + e.getMessage());
                    e.printStackTrace();
                    
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.sendMessage(ColorUtils.colorize("&cAn error occurred while finding a safe location. Please contact an administrator."));
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Clean up the queue world
     */
    public void cleanup() {
        if (queueWorld != null) {
            plugin.getLogger().info("Cleaning up queue world...");
            
            // Remove all players from the queue world
            for (Player player : queueWorld.getPlayers()) {
                plugin.getLogger().info("Teleporting player " + player.getName() + " out of queue world");
                player.teleport(plugin.getServer().getWorlds().get(0).getSpawnLocation());
            }
            
            // Unload the world without saving changes
            plugin.getLogger().info("Unloading queue world");
            boolean unloaded = Bukkit.unloadWorld(queueWorld, false);
            
            if (unloaded) {
                plugin.getLogger().info("Queue world unloaded successfully");
            } else {
                plugin.getLogger().warning("Failed to unload queue world");
            }
            
            queueWorld = null;
        }
    }

    /**
     * Get the queue world
     * @return The queue world
     */
    public World getQueueWorld() {
        return queueWorld;
    }
}