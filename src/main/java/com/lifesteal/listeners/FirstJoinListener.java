package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class FirstJoinListener implements Listener {
    private final LifeSteal plugin;

    public FirstJoinListener(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Handle first-time players
        if (!player.hasPlayedBefore()) {
            plugin.getFirstJoinManager().handleFirstJoin(player);
            return;
        }
        
        // Handle returning players who were in the queue world
        if (plugin.getFirstJoinManager().isPendingConfirmation(player.getUniqueId()) || 
            plugin.getFirstJoinManager().isFrozen(player.getUniqueId())) {
            
            // Send them back to the queue world
            plugin.getLogger().info("Player " + player.getName() + " reconnected while in queue. Sending back to queue world.");
            plugin.getFirstJoinManager().handleReconnect(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFirstJoinManager().isPendingConfirmation(player.getUniqueId())) {
            String message = event.getMessage().trim();
            
            // Accept various forms of confirmation
            if (message.equalsIgnoreCase("CONFIRM") || 
                message.equalsIgnoreCase("confirm") || 
     message.equalsIgnoreCase("yes") || 
                message.equalsIgnoreCase("ok") || 
                message.equalsIgnoreCase("ready")) {
                
                event.setCancelled(true);
                plugin.getFirstJoinManager().handleConfirmation(player);
            } else if (message.equalsIgnoreCase("help") || 
                       message.contains("stuck") || 
                       message.contains("how") || 
                       message.contains("?")) {
                
                // Provide help
                event.setCancelled(true);
                player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getHelpMessage()));
                player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getHelpExplanationMessage()));
                player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getHelpTeleportMessage()));
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (plugin.getFirstJoinManager().isFrozen(event.getPlayer().getUniqueId())) {
            // Only cancel if actual movement occurred (not just head rotation)
            if (event.getTo().getX() != event.getFrom().getX() ||
                event.getTo().getY() != event.getFrom().getY() ||
                event.getTo().getZ() != event.getFrom().getZ()) {
                event.setCancelled(true);
            }
        }
    }
}