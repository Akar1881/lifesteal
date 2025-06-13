package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class HeartManager {
    private final LifeSteal plugin;
    private final Map<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> heartCache = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> eliminatedPlayers = new ConcurrentHashMap<>();
    private static final int CACHE_CLEANUP_INTERVAL = 300; // 5 minutes

    public HeartManager(LifeSteal plugin) {
        this.plugin = plugin;
        startCacheCleanupTask();
    }

    private void startCacheCleanupTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                cleanup();
            }
        }.runTaskTimer(plugin, CACHE_CLEANUP_INTERVAL * 20L, CACHE_CLEANUP_INTERVAL * 20L);
    }

    private ReentrantLock getPlayerLock(UUID uuid) {
        return playerLocks.computeIfAbsent(uuid, k -> new ReentrantLock());
    }

    public void setHearts(Player player, int hearts) {
        if (player == null || !player.isOnline()) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            // Validate hearts value
            int minHearts = plugin.getConfigManager().getMinHearts();
            int maxHearts = plugin.getConfigManager().getMaxHearts();
            hearts = Math.max(minHearts, Math.min(hearts, maxHearts));

            int maxHealth = hearts * 2;
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            player.setHealth(maxHealth);
            
            // Update database and cache
            plugin.getDatabaseManager().setHearts(player.getUniqueId(), hearts);
            heartCache.put(player.getUniqueId(), hearts);
            
            // Update boss bar if enabled
            if (plugin.getConfigManager().isBossBarEnabled() && plugin.getModeManager() != null) {
                plugin.getModeManager().getBossBar().removePlayer(player);
                plugin.getModeManager().getBossBar().addPlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting hearts for player: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    public int getHearts(Player player) {
        if (player == null || !player.isOnline()) return 0;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            // Try cache first
            Integer cachedHearts = heartCache.get(player.getUniqueId());
            if (cachedHearts != null) {
                return cachedHearts;
            }
            
            // If not in cache, get from database
            int hearts = plugin.getDatabaseManager().getHearts(player.getUniqueId());
            heartCache.put(player.getUniqueId(), hearts);
            return hearts;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting hearts for player: " + player.getName(), e);
            return plugin.getConfigManager().getStartingHearts();
        } finally {
            lock.unlock();
        }
    }
    
    public void updatePlayerHearts(Player player) {
        if (player == null || !player.isOnline()) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            // Get hearts from database
            int hearts = plugin.getDatabaseManager().getHearts(player.getUniqueId());
            
            // Update player's max health
            int maxHealth = hearts * 2;
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
            
            // Update cache
            heartCache.put(player.getUniqueId(), hearts);
            
            // Update boss bar if enabled
            if (plugin.getConfigManager().isBossBarEnabled() && plugin.getModeManager() != null) {
                plugin.getModeManager().getBossBar().removePlayer(player);
                plugin.getModeManager().getBossBar().addPlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating hearts for player: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    public void addHearts(Player player, int amount) {
        if (player == null || !player.isOnline() || amount <= 0) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            int currentHearts = getHearts(player);
            int maxHearts = plugin.getConfigManager().getMaxHearts();
            int newHearts = currentHearts + amount;
            
            if (!player.hasPermission("lifesteal.bypass.maxhearts")) {
                newHearts = Math.min(newHearts, maxHearts);
            }
            
            setHearts(player, newHearts);
            
            // Play positive sound for heart gain
            playHeartGainSound(player);
            
            // Send message to player
            player.sendMessage(ColorUtils.colorize("&a+" + amount + " hearts!"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error adding hearts for player: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    private void playHeartGainSound(Player player) {
        try {
            String soundName = plugin.getConfigManager().getConfig()
                .getString("sounds.heart-gain", "ENTITY_PLAYER_LEVELUP");
            
            if (soundName.contains(".")) {
                soundName = soundName.toUpperCase().replace(".", "_");
            }
            
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public void removeHearts(Player player, int amount) {
        if (player == null || !player.isOnline() || amount <= 0) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            int currentHearts = getHearts(player);
            int minHearts = plugin.getConfigManager().getMinHearts();
            int newHearts = Math.max(currentHearts - amount, minHearts);
            
            setHearts(player, newHearts);
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
            
            // Send message to player
            player.sendMessage(ColorUtils.colorize("&c-" + amount + " hearts!"));

            if (newHearts <= 0) {
                eliminatePlayer(player);
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing hearts for player: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    public void eliminatePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            String eliminationMode = plugin.getConfigManager().getEliminationMode();
            
            // Play elimination sound to all players
            Bukkit.getOnlinePlayers().forEach(p -> 
                p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));
            
            // Broadcast elimination message
            String eliminationMessage = ColorUtils.colorize(plugin.getConfigManager().getConfig()
                .getString("messages.eliminated", "&4%player% has been eliminated!"));
            Bukkit.broadcastMessage(eliminationMessage.replace("%player%", player.getName()));
            
            if (eliminationMode.equalsIgnoreCase("ban")) {
                String banMessage = ColorUtils.colorize(plugin.getConfigManager().getConfig()
                    .getString("messages.banned", "&4You have been eliminated!"));
                player.kickPlayer(banMessage);
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                    player.getName(),
                    "Eliminated from LifeSteal",
                    null,
                    "LifeSteal Plugin"
                );
            } else {
                player.setGameMode(GameMode.SPECTATOR);
            }

            // Execute elimination command if configured
            if (plugin.getConfigManager().getConfig().contains("elimination.command")) {
                String cmd = plugin.getConfigManager().getConfig().getString("elimination.command");
                if (cmd != null && !cmd.isEmpty()) {
                    Bukkit.dispatchCommand(
                        Bukkit.getConsoleSender(),
                        cmd.replace("%player%", player.getName())
                    );
                }
            }
            
            // Mark player as eliminated
            eliminatedPlayers.put(player.getUniqueId(), true);
            
            // Clear cache for eliminated player
            heartCache.remove(player.getUniqueId());
            playerLocks.remove(player.getUniqueId());
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error eliminating player: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    public boolean isEliminated(Player player) {
        return player != null && eliminatedPlayers.getOrDefault(player.getUniqueId(), false);
    }

    public void revivePlayer(Player player) {
        if (player == null || !player.isOnline()) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            player.setGameMode(GameMode.SURVIVAL);
            setHearts(player, plugin.getConfigManager().getStartingHearts());
            
            if (player.isBanned()) {
                Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(player.getName());
            }
            
            // Remove from eliminated players
            eliminatedPlayers.remove(player.getUniqueId());
            
            // Play revival sound
            player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
            
            // Send revival message
            String revivalMessage = ColorUtils.colorize(plugin.getConfigManager().getConfig()
                .getString("messages.revived", "&aYou have been revived!"));
            player.sendMessage(revivalMessage);
            
            // Broadcast revival message
            String broadcastMessage = ColorUtils.colorize(plugin.getConfigManager().getConfig()
                .getString("messages.player-revived", "&a%player% has been revived!"));
            Bukkit.broadcastMessage(broadcastMessage.replace("%player%", player.getName()));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reviving player: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }

    public void cleanup() {
        try {
            // Remove locks and cache for offline players
            for (UUID uuid : playerLocks.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    playerLocks.remove(uuid);
                    heartCache.remove(uuid);
                    eliminatedPlayers.remove(uuid);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error during heart manager cleanup", e);
        }
    }

    public void savePlayerData(Player player) {
        if (player == null) return;
        
        ReentrantLock lock = getPlayerLock(player.getUniqueId());
        lock.lock();
        try {
            int hearts = getHearts(player);
            plugin.getDatabaseManager().setHearts(player.getUniqueId(), hearts);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving player data for: " + player.getName(), e);
        } finally {
            lock.unlock();
        }
    }
}