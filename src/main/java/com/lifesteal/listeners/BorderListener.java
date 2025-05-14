package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BorderListener implements Listener {
    private final LifeSteal plugin;
    private final Map<UUID, Long> lastWarningTime = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();
    private static final long WARNING_COOLDOWN = 5000; // 5 seconds cooldown between warnings
    private static final long DAMAGE_COOLDOWN = 1000; // 1 second cooldown between damage
    private static final double INSTANT_KILL_DISTANCE = 100.0; // Distance beyond border for instant kill

    public BorderListener(LifeSteal plugin) {
        this.plugin = plugin;
        
        // Start a task to check for players outside the border and apply damage
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!plugin.getConfigManager().isWorldBorderEnabled()) {
                    return;
                }
                
                long currentTime = System.currentTimeMillis();
                
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (plugin.getWorldBorderManager().isOutsideBorder(player.getLocation())) {
                        UUID playerId = player.getUniqueId();
                        
                        // Check if we should apply damage (with cooldown)
                        if (!lastDamageTime.containsKey(playerId) || 
                            currentTime - lastDamageTime.get(playerId) > DAMAGE_COOLDOWN) {
                            
                            // Calculate distance from border
                            double distanceFromBorder = plugin.getWorldBorderManager().getDistanceOutsideBorder(player.getLocation());
                            
                            // If player is way outside the border, kill them instantly
                            if (distanceFromBorder > INSTANT_KILL_DISTANCE) {
                                player.setHealth(0);
                                player.sendMessage(ColorUtils.colorize("&c&lYou ventured too far outside the world border and died!"));
                            } else {
                                // Apply damage based on config
                                double damage = plugin.getConfigManager().getWorldBorderDamageAmount();
                                player.damage(damage);
                                lastDamageTime.put(playerId, currentTime);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 20L); // Check every second
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }

        // Only check if the player has moved to a new block
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Check if player is outside the border
        if (plugin.getWorldBorderManager().isOutsideBorder(event.getTo())) {
            // Send warning message (with cooldown)
            UUID playerId = player.getUniqueId();
            long currentTime = System.currentTimeMillis();
            
            if (!lastWarningTime.containsKey(playerId) || 
                currentTime - lastWarningTime.get(playerId) > WARNING_COOLDOWN) {
                
                player.sendMessage(ColorUtils.colorize(
                        plugin.getConfigManager().getWorldBorderOutsideMessage()));
                lastWarningTime.put(playerId, currentTime);
            }
            
            // If player is trying to move way outside the border, cancel the move
            double distanceFromBorder = plugin.getWorldBorderManager().getDistanceOutsideBorder(event.getTo());
            if (distanceFromBorder > 20.0) { // If more than 20 blocks outside, cancel movement
                event.setCancelled(true);
                player.sendMessage(ColorUtils.colorize("&c&lYou cannot go that far outside the world border!"));
            }
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }

        Player player = event.getPlayer();
        
        // Check if teleport destination is outside the border
        if (plugin.getWorldBorderManager().isOutsideBorder(event.getTo())) {
            // Cancel all teleports outside the border
            player.sendMessage(ColorUtils.colorize("&cYou cannot teleport outside the world border!"));
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }
        
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();
        
        // If player is near the border, check if they're trying to throw items outside
        if (plugin.getWorldBorderManager().isNearBorder(playerLoc, 5.0)) {
            // Get player's looking direction
            Location targetLoc = playerLoc.clone().add(player.getLocation().getDirection().multiply(5));
            
            // Check if the target location would be outside the border
            if (plugin.getWorldBorderManager().isOutsideBorder(targetLoc)) {
                event.setCancelled(true);
                player.sendMessage(ColorUtils.colorize("&cYou cannot throw items outside the world border!"));
            }
        }
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }
        
        // Ensure players don't respawn outside the border
        if (plugin.getWorldBorderManager().isOutsideBorder(event.getRespawnLocation())) {
            // If respawn location is outside border, set it to world spawn
            event.setRespawnLocation(event.getRespawnLocation().getWorld().getSpawnLocation());
        }
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfigManager().isWorldBorderEnabled()) {
            return;
        }
        
        // Check if player died outside the border
        if (plugin.getWorldBorderManager().isOutsideBorder(event.getEntity().getLocation())) {
            // Customize death message for border deaths
            String deathMessage = event.getDeathMessage();
            if (deathMessage != null) {
                event.setDeathMessage(event.getEntity().getName() + " died outside the world border");
            }
        }
    }
}