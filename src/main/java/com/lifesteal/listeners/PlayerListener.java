package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
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

public class PlayerListener implements Listener {
    private final LifeSteal plugin;

    public PlayerListener(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPlayedBefore()) {
            plugin.getHeartManager().setHearts(player, plugin.getConfigManager().getStartingHearts());
        }
        // Add player to boss bar if enabled
        if (plugin.getConfigManager().isBossBarEnabled()) {
            plugin.getModeManager().getBossBar().addPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(org.bukkit.event.player.PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Remove player from boss bar if enabled
        if (plugin.getConfigManager().isBossBarEnabled()) {
            plugin.getModeManager().getBossBar().removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Handle PvP deaths
        if (killer != null && plugin.getModeManager().isPvPMode()) {
            plugin.getHeartManager().addHearts(killer, plugin.getConfigManager().getHeartsGainedPerKill());
            
            // Drop heart item only on PvP kills
            ItemStack heartItem = plugin.getItemManager().getCustomItem("heart");
            if (heartItem != null && plugin.getConfigManager().getItemsConfig()
                .getBoolean("heart-item.drop-on-death")) {
                event.getDrops().add(heartItem);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Player killer = player.getKiller();

        if (killer != null && plugin.getModeManager().isPvPMode()) {
            // PvP death
            plugin.getHeartManager().removeHearts(player, plugin.getConfigManager().getHeartsLostPerDeath());
        } else if (plugin.getConfigManager().isNaturalDeathLoss()) {
            // Natural death (like Warden)
            plugin.getHeartManager().removeHearts(player, plugin.getConfigManager().getHeartsLostPerDeath());
        }

        // Check for elimination after heart removal
        if (plugin.getHeartManager().getHearts(player) <= 0) {
            plugin.getHeartManager().eliminatePlayer(player);
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Check if both entities are players
        if (!(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }
        
        Player attacker = (Player) event.getDamager();
        Player victim = (Player) event.getEntity();
        
        // If we're in PVE mode, cancel player vs player damage
        if (!plugin.getModeManager().isPvPMode()) {
            // Cancel the damage event
            event.setCancelled(true);
            
            // Send a message to the attacker
            attacker.sendMessage(org.bukkit.ChatColor.RED + "PvP is currently disabled! You can only attack players during PvP mode.");
        }
    }
}
