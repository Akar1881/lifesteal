package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Level;

public class PlayerListener implements Listener {
    private final LifeSteal plugin;

    public PlayerListener(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            // Handle first-time players
            if (!player.hasPlayedBefore()) {
                plugin.getHeartManager().setHearts(player, plugin.getConfigManager().getStartingHearts());
                plugin.getLogger().info("Set initial hearts for new player: " + player.getName());
            }

            // Add player to boss bar if enabled
            if (plugin.getConfigManager().isBossBarEnabled()) {
                plugin.getModeManager().getBossBar().addPlayer(player);
            }

            // Schedule delayed heart update to ensure proper sync
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        plugin.getHeartManager().updatePlayerHearts(player);
                    }
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player join for " + player.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        try {
            // Remove player from boss bar if enabled
            if (plugin.getConfigManager().isBossBarEnabled()) {
                plugin.getModeManager().getBossBar().removePlayer(player);
            }

            // Save player data
            plugin.getHeartManager().savePlayerData(player);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player quit for " + player.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        try {
            // Increment deaths for victim
            plugin.getStatisticsManager().incrementDeaths(victim);
            
            // Handle PvP deaths
            if (killer != null && plugin.getModeManager().isPvPMode()) {
                // Increment kills for killer
                plugin.getStatisticsManager().incrementKills(killer);
                
                // Check if we should drop heart fragments or directly add hearts
                boolean dropHeartItem = plugin.getConfigManager().getItemsConfig()
                    .getBoolean("heart-item.drop-on-death", true);
                
                if (dropHeartItem) {
                    // Drop heart fragment item only on PvP kills
                    ItemStack heartItem = plugin.getItemManager().getCustomItem("heart-item");
                    if (heartItem != null) {
                        event.getDrops().add(heartItem);
                        plugin.getLogger().info("Dropped heart item for " + victim.getName() + " killed by " + killer.getName());
                    }
                } else {
                    // If not dropping heart items, directly add hearts to the killer
                    int heartsGained = plugin.getConfigManager().getHeartsGainedPerKill();
                    plugin.getHeartManager().addHearts(killer, heartsGained);
                    killer.sendMessage(ChatColor.GREEN + "You gained " + heartsGained + " hearts!");
                    // Increment hearts stolen for killer, hearts lost for victim
                    plugin.getStatisticsManager().addHeartsStolen(killer, heartsGained);
                    plugin.getStatisticsManager().addHeartsLost(victim, heartsGained);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player death for " + victim.getName(), e);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Player killer = player.getKiller();

        try {
            if (killer != null && plugin.getModeManager().isPvPMode()) {
                // PvP death
                int heartsLost = plugin.getConfigManager().getHeartsLostPerDeath();
                plugin.getHeartManager().removeHearts(player, heartsLost);
                player.sendMessage(ChatColor.RED + "You lost " + heartsLost + " hearts from PvP death!");
            } else if (plugin.getConfigManager().isNaturalDeathLoss()) {
                // Natural death (like Warden)
                int heartsLost = plugin.getConfigManager().getHeartsLostPerDeath();
                plugin.getHeartManager().removeHearts(player, heartsLost);
                player.sendMessage(ChatColor.RED + "You lost " + heartsLost + " hearts from natural death!");
            }

            // Check for elimination after heart removal
            if (plugin.getHeartManager().getHearts(player) <= 0) {
                plugin.getHeartManager().eliminatePlayer(player);
            }

            // Schedule delayed heart update
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        plugin.getHeartManager().updatePlayerHearts(player);
                    }
                }
            }.runTaskLater(plugin, 20L); // 1 second delay
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling player respawn for " + player.getName(), e);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        try {
            // Check if both entities are players
            if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
                return;
            }
            
            Player attacker = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();
            
            // Check for PvP mode
            if (!plugin.getModeManager().isPvPMode()) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "PvP is currently disabled! You can only attack players during PvP mode.");
                return;
            }
            
            // Check if players are allies
            if (plugin.getAllyManager().isAlly(attacker, victim)) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "You cannot attack your ally!");
                return;
            }

            // Check if either player is eliminated
            if (plugin.getHeartManager().isEliminated(attacker) || plugin.getHeartManager().isEliminated(victim)) {
                event.setCancelled(true);
                attacker.sendMessage(ChatColor.RED + "Eliminated players cannot participate in PvP!");
                return;
            }

            // Safe zone check is disabled as SafeZoneManager is not implemented yet
            // TODO: Implement SafeZoneManager for safe zone functionality
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error handling entity damage event", e);
            event.setCancelled(true);
        }
    }
}
