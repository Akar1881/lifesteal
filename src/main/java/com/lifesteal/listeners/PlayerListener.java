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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import com.lifesteal.events.PvPCycleChangeEvent;

public class PlayerListener implements Listener {
    private final LifeSteal plugin;
    private final Set<UUID> pendingHeartLoss = new HashSet<>();

    public PlayerListener(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Set player to survival mode
        player.setGameMode(GameMode.SURVIVAL);
        
        if (!player.hasPlayedBefore()) {
            plugin.getHeartManager().setHearts(player, plugin.getConfigManager().getStartingHearts());
        }

        // Check if player has pending heart loss from a previous death
        if (pendingHeartLoss.contains(player.getUniqueId())) {
            handleHeartLoss(player);
            pendingHeartLoss.remove(player.getUniqueId());
        }

        // Add player to boss bar if enabled
        if (plugin.getConfigManager().isBossBarEnabled()) {
            plugin.getModeManager().getBossBar().addPlayer(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Handle bounty quit penalty
        plugin.getBountyManager().handlePlayerQuit(player);
        
        // Handle heart loss on quit if applicable
        if (plugin.getHeartManager().shouldLoseHeartsOnQuit(player)) {
            plugin.getHeartManager().handleQuitHeartLoss(player);
        }
        
        // Remove player from boss bar if enabled
        if (plugin.getConfigManager().isBossBarEnabled()) {
            plugin.getModeManager().getBossBar().removePlayer(player);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer != null) {
            // Handle regular PvP kill
            plugin.getHeartManager().handleKill(killer, victim);
            
            // Handle bounty kill if applicable
            plugin.getBountyManager().handleBountyKill(killer, victim);
        } else if (plugin.getConfigManager().isNaturalDeathLoss()) {
            // Natural death (like falling, drowning, etc.)
            // Mark player for heart loss on respawn
            pendingHeartLoss.add(victim.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Handle heart loss if player was marked for it
        if (pendingHeartLoss.contains(player.getUniqueId())) {
            handleHeartLoss(player);
            pendingHeartLoss.remove(player.getUniqueId());
        }
    }

    private void handleHeartLoss(Player player) {
        // Get values from config
        int heartsLost = plugin.getConfigManager().getHeartsLostPerDeath();
        int minHearts = plugin.getConfigManager().getMinHearts();
        
        // Remove hearts
        plugin.getHeartManager().removeHearts(player, heartsLost);
        
        // Check for elimination
        if (plugin.getHeartManager().getHearts(player) <= minHearts) {
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

    @EventHandler
    public void onPvPCycleChange(PvPCycleChangeEvent event) {
        if (event.isPvPEnabled()) {
            // Start bounty system when PvP is enabled
            plugin.getBountyManager().startBountySystem();
        } else {
            // Handle bounty survival rewards when PvP is disabled
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (plugin.getBountyManager().hasBounty(player)) {
                    plugin.getBountyManager().handleBountySurvival(player);
                }
            }
        }
    }
}
