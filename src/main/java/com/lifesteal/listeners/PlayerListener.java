package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
}