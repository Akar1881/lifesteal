package com.lifesteal.utils;

import com.lifesteal.LifeSteal;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.WorldBorder;

public class QueueWorld {
    private static final String QUEUE_WORLD_NAME = "queue";
    private final LifeSteal plugin;
    private World queueWorld;
    private Object chunkyAPI;
    private Plugin chunkyPlugin;
    private boolean chunksGenerated = false;
    private boolean generationInProgress = false;
    private final Map<UUID, Boolean> playerConfirmed = new HashMap<>();
    private final Map<UUID, Integer> playerMusicTasks = new HashMap<>();
    private final Sound[] musicDiscs = {
        Sound.MUSIC_DISC_13,
        Sound.MUSIC_DISC_CAT,
        Sound.MUSIC_DISC_BLOCKS,
        Sound.MUSIC_DISC_CHIRP,
        Sound.MUSIC_DISC_FAR,
        Sound.MUSIC_DISC_MALL,
        Sound.MUSIC_DISC_MELLOHI,
        Sound.MUSIC_DISC_STAL,
        Sound.MUSIC_DISC_STRAD,
        Sound.MUSIC_DISC_WARD,
        Sound.MUSIC_DISC_11,
        Sound.MUSIC_DISC_WAIT,
        Sound.MUSIC_DISC_PIGSTEP
    };

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

        // Initialize Chunky API if Chunky is available
        if (plugin.isChunkyAvailable()) {
            try {
                chunkyPlugin = plugin.getServer().getPluginManager().getPlugin("Chunky");
                
                // Get the ChunkyAPI using ServicesManager
                RegisteredServiceProvider<?> provider = plugin.getServer().getServicesManager().getRegistration(Class.forName("org.popcraft.chunky.api.ChunkyAPI"));
                if (provider != null) {
                    chunkyAPI = provider.getProvider();
                    plugin.getLogger().info("Successfully hooked into Chunky for chunk pre-generation");
                } else {
                    plugin.getLogger().warning("Chunky API service not found. Chunk pre-generation will be disabled.");
                    chunkyAPI = null;
                    chunkyPlugin = null;
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into Chunky: " + e.getMessage());
                plugin.getLogger().warning("Chunk pre-generation will be disabled");
                chunkyAPI = null;
                chunkyPlugin = null;
            }
        } else {
            plugin.getLogger().warning("Chunky plugin not found. Chunk pre-generation will be disabled.");
            chunkyAPI = null;
            chunkyPlugin = null;
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
                queueWorld.setGameRule(org.bukkit.GameRule.DO_DAYLIGHT_CYCLE, false);
                queueWorld.setGameRule(org.bukkit.GameRule.DO_WEATHER_CYCLE, false);
                queueWorld.setGameRule(org.bukkit.GameRule.DO_MOB_SPAWNING, false);
                queueWorld.setTime(6000); // Set to midday
                
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
        
        // Start chunk pre-generation if Chunky is available
        if (chunkyAPI != null && plugin.getConfigManager().isChunkPreGenerationEnabled()) {
            startChunkGeneration();
        } else {
            // If Chunky is not available or pre-generation is disabled, mark as generated
            chunksGenerated = true;
        }
    }
    
    /**
     * Start chunk pre-generation using Chunky
     */
    public void startChunkGeneration() {
        if (chunkyAPI == null || generationInProgress || !plugin.isChunkyAvailable()) {
            if (!plugin.isChunkyAvailable()) {
                plugin.getLogger().warning("Cannot start chunk generation: Chunky plugin not found");
            }
            return;
        }
        
        World mainWorld = plugin.getServer().getWorlds().get(0);
        String worldName = mainWorld.getName();
        
        // Get radius from config or world border
        int radius;
        if (plugin.getConfigManager().isWorldBorderEnabled()) {
            // Use world border size
            double borderSize = mainWorld.getWorldBorder().getSize() / 2;
            radius = (int) borderSize;
        } else {
            // Use configured radius
            radius = plugin.getConfigManager().getChunkPreGenerationRadius();
            plugin.getLogger().info("Using configured chunk pre-generation radius: " + radius);
        }
        
        // Ensure radius is reasonable
        radius = Math.min(radius, 8000); // Cap at 8000 blocks
        
        plugin.getLogger().info("Starting chunk pre-generation for world " + worldName + " with radius " + radius);
        
        // Start the task
        generationInProgress = true;
        chunksGenerated = false;
        
        // Get world center
        double centerX, centerZ;
        if (plugin.getConfigManager().isWorldBorderEnabled() && !plugin.getConfigManager().useWorldSpawnAsCenter()) {
            centerX = plugin.getConfigManager().getWorldBorderCenterX();
            centerZ = plugin.getConfigManager().getWorldBorderCenterZ();
        } else {
            Location spawn = mainWorld.getSpawnLocation();
            centerX = spawn.getX();
            centerZ = spawn.getZ();
        }
        
        // Start the generation task
        try {
            // Use reflection to call startTask with the correct parameters
            Method startTaskMethod = chunkyAPI.getClass().getMethod("startTask", 
                String.class, String.class, double.class, double.class, double.class, double.class, String.class);
            startTaskMethod.invoke(chunkyAPI, worldName, "square", centerX, centerZ, (double)radius, (double)radius, "concentric");
            
            // Register a completion listener
            try {
                // Get the onGenerationComplete method
                Method onCompleteMethod = chunkyAPI.getClass().getMethod("onGenerationComplete", Consumer.class);
                
                // Create a consumer that will be called when generation completes
                Consumer<Object> consumer = event -> {
                    // Generation is complete
                    generationInProgress = false;
                    chunksGenerated = true;
                    plugin.getLogger().info("Chunk pre-generation completed for world " + worldName);
                    
                    // Reset progress tracking
                    startTime = 0;
                    totalChunks = 0;
                    estimatedTimeSeconds = 0;
                    
                    // Notify waiting players
                    for (UUID uuid : playerConfirmed.keySet()) {
                        Player player = Bukkit.getPlayer(uuid);
                        if (player != null && playerConfirmed.get(uuid)) {
                            // Stop music for player
                            stopMusicForPlayer(player);
                            
                            player.sendMessage(ColorUtils.colorize("&a&lCHUNK GENERATION COMPLETE! &aYou will be teleported to the main world now."));
                            findSafeLocationAndTeleport(player);
                        }
                    }
                };
                
                // Register the consumer
                onCompleteMethod.invoke(chunkyAPI, consumer);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to register completion listener: " + e.getMessage());
                
                // Fall back to polling for completion
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        try {
                            // Use reflection to get generation tasks
                            Method getTasksMethod = chunkyAPI.getClass().getMethod("getGenerationTasks");
                            Map<String, Object> tasks = (Map<String, Object>) getTasksMethod.invoke(chunkyAPI);
                            
                            if (!tasks.containsKey(worldName)) {
                                // Generation is complete
                                generationInProgress = false;
                                chunksGenerated = true;
                                plugin.getLogger().info("Chunk pre-generation completed for world " + worldName);
                                
                                // Cancel this task
                                cancel();
                                
                                // Notify waiting players
                                for (UUID uuid : playerConfirmed.keySet()) {
                                    Player player = Bukkit.getPlayer(uuid);
                                    if (player != null && playerConfirmed.get(uuid)) {
                                        player.sendMessage(ColorUtils.colorize("&aChunk generation completed! You will be teleported to the main world now."));
                                        findSafeLocationAndTeleport(player);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            plugin.getLogger().warning("Error checking chunk generation status: " + e.getMessage());
                            cancel();
                        }
                    }
                }.runTaskTimer(plugin, 200L, 200L); // Check every 10 seconds
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to start chunk generation: " + e.getMessage());
            e.printStackTrace();
            generationInProgress = false;
            chunksGenerated = true; // Mark as generated to avoid blocking players
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
        
        // Check if player was previously in the queue and chunks are now generated
        if (playerConfirmed.containsKey(player.getUniqueId()) && chunksGenerated) {
            player.sendMessage(ColorUtils.colorize("&a&lGOOD NEWS! &aChunks are now generated! Teleporting you to the main world..."));
            findSafeLocationAndTeleport(player);
            return;
        }
        
        // Set player to spectator mode temporarily to prevent fall damage
        player.setGameMode(GameMode.SPECTATOR);
        
        // Teleport player to queue world spawn
        Location spawnLoc = queueWorld.getSpawnLocation().clone().add(0.5, 1, 0.5);
        PaperLib.teleportAsync(player, spawnLoc).thenAccept(result -> {
            if (result) {
                plugin.getLogger().info("Player " + player.getName() + " sent to queue world successfully.");
                
                // Start playing music if enabled
                if (plugin.getConfigManager().isQueueMusicEnabled()) {
                    startMusicForPlayer(player);
                }
                
                // Add to player confirmed map (not confirmed yet) if not already there
                if (!playerConfirmed.containsKey(player.getUniqueId())) {
                    playerConfirmed.put(player.getUniqueId(), false);
                }
                
                // Show chunk generation status if applicable
                if (generationInProgress) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            double progress = getGenerationProgress();
                            player.sendMessage(ColorUtils.colorize("&6&lNOTE: &eChunk pre-generation is in progress."));
                            player.sendMessage(ColorUtils.colorize("&eCurrent progress: &6" + String.format("%.1f", progress) + "%"));
                            player.sendMessage(ColorUtils.colorize("&eYou can confirm now, but you'll need to wait for chunk generation to complete before being teleported."));
                            player.sendMessage(ColorUtils.colorize("&eYou can also disconnect and come back later if you wish."));
                        }
                    }.runTaskLater(plugin, 60L); // 3 seconds delay
                }
            } else {
                plugin.getLogger().warning("Failed to send player " + player.getName() + " to queue world!");
            }
        });
    }
    
    /**
     * Start playing music for a player
     * @param player The player to play music for
     */
    private void startMusicForPlayer(Player player) {
        // Cancel any existing music task
        stopMusicForPlayer(player);
        
        // Play a music disc immediately
        Sound initialDisc = musicDiscs[new java.util.Random().nextInt(musicDiscs.length)];
        player.playSound(player.getLocation(), initialDisc, SoundCategory.RECORDS, 1.0f, 1.0f);
        plugin.getLogger().info("Playing music disc " + initialDisc.name() + " for player " + player.getName());
        
        // Send message about music
        player.sendMessage(ColorUtils.colorize("&d♫ &5Now playing music while you wait... &d♫"));
        
        // Start a new music task for subsequent discs
        int taskId = new BukkitRunnable() {
            @Override
            public void run() {
                if (player.isOnline() && player.getWorld().equals(queueWorld)) {
                    // Play a random music disc
                    Sound musicDisc = musicDiscs[new java.util.Random().nextInt(musicDiscs.length)];
                    player.playSound(player.getLocation(), musicDisc, SoundCategory.RECORDS, 1.0f, 1.0f);
                    plugin.getLogger().info("Playing next music disc " + musicDisc.name() + " for player " + player.getName());
                } else {
                    // Player left, cancel task
                    cancel();
                    playerMusicTasks.remove(player.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, 20L * 180, 20L * 180).getTaskId(); // Play a new disc every 3 minutes
        
        // Store the task ID
        playerMusicTasks.put(player.getUniqueId(), taskId);
    }
    
    /**
     * Stop playing music for a player
     * @param player The player to stop music for
     */
    private void stopMusicForPlayer(Player player) {
        Integer taskId = playerMusicTasks.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        
        // Stop all sounds for the player
        for (Sound disc : musicDiscs) {
            player.stopSound(disc, SoundCategory.RECORDS);
        }
        
        // Also stop all record sounds in case we missed any
        player.stopSound(Sound.MUSIC_DISC_13, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_CAT, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_BLOCKS, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_CHIRP, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_FAR, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_MALL, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_MELLOHI, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_STAL, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_STRAD, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_WARD, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_11, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_WAIT, SoundCategory.RECORDS);
        player.stopSound(Sound.MUSIC_DISC_PIGSTEP, SoundCategory.RECORDS);
        
        plugin.getLogger().info("Stopped all music for player " + player.getName());
    }

    /**
     * Handle player confirmation
     * @param player The player who confirmed
     */
    public void handlePlayerConfirmation(Player player) {
        // Mark player as confirmed
        playerConfirmed.put(player.getUniqueId(), true);
        
        // Update database
        plugin.getDatabaseManager().setQueueState(player.getUniqueId(), true, true, true);
        
        // Check if chunks are generated
        if (chunksGenerated) {
            // Chunks are ready, teleport player
            findSafeLocationAndTeleport(player);
        } else {
            // Chunks are not ready, inform player
            double progress = getGenerationProgress();
            player.sendMessage(ColorUtils.colorize("&6Chunks are still being generated... &e" + String.format("%.1f", progress) + "%"));
            player.sendMessage(ColorUtils.colorize("&eYou will be teleported automatically when chunk generation is complete."));
            player.sendMessage(ColorUtils.colorize("&eYou can disconnect and come back later if you prefer."));
            
            // Show progress bar
            StringBuilder progressBar = new StringBuilder("&a");
            int barLength = 20;
            int filledBars = (int) (progress / 5); // 100% / 5 = 20 bars
            for (int i = 0; i < barLength; i++) {
                if (i < filledBars) {
                    progressBar.append("█");
                } else {
                    progressBar.append("&7█");
                }
            }
            player.sendMessage(ColorUtils.colorize(progressBar.toString()));
        }
    }
    
    /**
     * Find a safe location in the main world and teleport the player there
     * @param player The player to teleport
     */
    public void findSafeLocationAndTeleport(Player player) {
        if (player == null || !player.isOnline()) return;

        int minDistance = plugin.getConfigManager().getSafeLocationMinDistance();
        int maxDistance = plugin.getConfigManager().getSafeLocationMaxDistance();
        int maxAttempts = plugin.getConfigManager().getSafeLocationMaxAttempts();

        CompletableFuture.runAsync(() -> {
            try {
                World world = plugin.getServer().getWorlds().get(0);
                Location safeLocation = null;
                int attempts = 0;

                while (safeLocation == null && attempts < maxAttempts) {
                    double x = (Math.random() * 2 - 1) * maxDistance;
                    double z = (Math.random() * 2 - 1) * maxDistance;
                    
                    if (Math.abs(x) < minDistance) x = Math.signum(x) * minDistance;
                    if (Math.abs(z) < minDistance) z = Math.signum(z) * minDistance;

                    Location location = new Location(world, x, 0, z);
                    location.setY(world.getHighestBlockYAt(location) + 1);

                    if (isLocationSafe(location)) {
                        safeLocation = location;
                        break;
                    }
                    attempts++;
                }

                if (safeLocation != null) {
                    int chunkX = safeLocation.getBlockX() >> 4;
                    int chunkZ = safeLocation.getBlockZ() >> 4;
                    
                    for (int dx = -1; dx <= 1; dx++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            PaperLib.getChunkAtAsync(world, chunkX + dx, chunkZ + dz, true)
                                .thenAccept(chunk -> {
                                    if (chunk != null) {
                                        chunk.load();
                                    }
                                });
                        }
                    }

                    Location finalSafeLocation = safeLocation;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            PaperLib.teleportAsync(player, finalSafeLocation)
                                .thenAccept(success -> {
                                    if (success) {
                                        player.sendMessage(ColorUtils.colorize("&aYou have been teleported to a safe location!"));
                                        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                                    } else {
                                        player.sendMessage(ColorUtils.colorize("&cFailed to teleport to safe location!"));
                                    }
                                });
                        } catch (Exception e) {
                            plugin.getLogger().severe("Error teleporting player: " + e.getMessage());
                            player.sendMessage(ColorUtils.colorize("&cAn error occurred while teleporting!"));
                        }
                    });
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ColorUtils.colorize("&cCould not find a safe location!"));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error finding safe location: " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ColorUtils.colorize("&cAn error occurred while finding a safe location!"));
                });
            }
        });
    }

    private boolean isLocationSafe(Location location) {
        if (location == null || location.getWorld() == null) return false;

        try {
            // Check if the block below is solid
            if (!location.getBlock().getRelative(0, -1, 0).getType().isSolid()) {
                return false;
            }

            // Check if the block at the location and above are air
            if (!location.getBlock().getType().isAir() || 
                !location.getBlock().getRelative(0, 1, 0).getType().isAir()) {
                return false;
            }

            // Check if the location is within world border
            if (plugin.getConfigManager().isWorldBorderEnabled()) {
                WorldBorder border = location.getWorld().getWorldBorder();
                if (border != null && !border.isInside(location)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking location safety: " + e.getMessage());
            return false;
        }
    }

    /**
     * Clean up the queue world
     */
    public void cleanup() {
        // Stop chunk generation if in progress
        if (chunkyAPI != null && generationInProgress && plugin.isChunkyAvailable()) {
            try {
                World mainWorld = plugin.getServer().getWorlds().get(0);
                
                // Use reflection to call cancelTask
                Method cancelTaskMethod = chunkyAPI.getClass().getMethod("cancelTask", String.class);
                cancelTaskMethod.invoke(chunkyAPI, mainWorld.getName());
                
                plugin.getLogger().info("Cancelled chunk generation task");
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to cancel chunk generation: " + e.getMessage());
            }
        }
        
        // Stop music for all players
        for (UUID uuid : playerMusicTasks.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                stopMusicForPlayer(player);
            } else {
                // Just cancel the task
                Integer taskId = playerMusicTasks.get(uuid);
                if (taskId != null) {
                    Bukkit.getScheduler().cancelTask(taskId);
                }
            }
        }
        playerMusicTasks.clear();
        
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
        
        // Clear player maps
        playerConfirmed.clear();
    }

    /**
     * Get the queue world
     * @return The queue world
     */
    public World getQueueWorld() {
        return queueWorld;
    }
    
    /**
     * Check if chunks are generated
     * @return True if chunks are generated, false otherwise
     */
    public boolean areChunksGenerated() {
        return chunksGenerated;
    }
    
    /**
     * Check if chunk generation is in progress
     * @return True if chunk generation is in progress, false otherwise
     */
    public boolean isGenerationInProgress() {
        return generationInProgress;
    }
    
    // Track our own progress
    private long startTime = 0;
    private int totalChunks = 0;
    private int estimatedTimeSeconds = 0;
    
    /**
     * Check if a player has confirmed
     * @param uuid The player's UUID
     * @return True if the player has confirmed, false otherwise
     */
    public boolean isPlayerConfirmed(UUID uuid) {
        return playerConfirmed.containsKey(uuid) && playerConfirmed.get(uuid);
    }
    
    /**
     * Save player confirmation states to database
     */
    public void savePlayerConfirmationStates() {
        playerConfirmed.forEach((uuid, confirmed) -> {
            plugin.getDatabaseManager().setQueueState(uuid, true, confirmed, true);
        });
    }
    
    /**
     * Load player confirmation states from database
     */
    public void loadPlayerConfirmationStates() {
        Map<UUID, Map<String, Boolean>> states = plugin.getDatabaseManager().getAllQueueStates();
        
        for (Map.Entry<UUID, Map<String, Boolean>> entry : states.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Boolean> state = entry.getValue();
            
            // Add to player confirmed map
            playerConfirmed.put(uuid, state.get("confirmed"));
        }
        
        plugin.getLogger().info("Loaded " + playerConfirmed.size() + " player confirmation states from database");
    }
    
    /**
     * Get the generation progress
     * @return The generation progress as a percentage (0-100)
     */
    public double getGenerationProgress() {
        if (chunkyAPI == null || !generationInProgress || !plugin.isChunkyAvailable()) {
            return 100.0;
        }
        
        // If we don't have a start time, set it now
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
            
            // Calculate total chunks based on radius
            World mainWorld = plugin.getServer().getWorlds().get(0);
            int radius = plugin.getConfigManager().getChunkPreGenerationRadius();
            // Square shape has (radius*2)^2 chunks
            totalChunks = (int) Math.pow(radius/8 * 2, 2);
            
            // Estimate 20 chunks per second (this is a rough estimate)
            estimatedTimeSeconds = totalChunks / 20;
            
            return 0.0;
        }
        
        // Calculate progress based on elapsed time vs estimated time
        long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
        double progress = Math.min(100.0, (elapsedSeconds * 100.0) / estimatedTimeSeconds);
        
        return progress;
    }
    
    /**
     * Reload the queue world configuration
     * This should be called when the plugin's config is reloaded
     */
    public void reload() {
        plugin.getLogger().info("Reloading queue world configuration");
        
        // If chunk generation is in progress, restart it with the new radius
        if (generationInProgress && chunkyAPI != null && plugin.isChunkyAvailable()) {
            try {
                // Cancel the current task
                World mainWorld = plugin.getServer().getWorlds().get(0);
                Method cancelTaskMethod = chunkyAPI.getClass().getMethod("cancelTask", String.class);
                cancelTaskMethod.invoke(chunkyAPI, mainWorld.getName());
                
                plugin.getLogger().info("Cancelled current chunk generation task for reconfiguration");
                
                // Start a new task with the updated radius
                startChunkGeneration();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to restart chunk generation with new radius: " + e.getMessage());
            }
        }
    }
}