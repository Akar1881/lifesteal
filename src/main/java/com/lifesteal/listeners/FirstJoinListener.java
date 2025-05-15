package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
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
        if (!player.hasPlayedBefore()) {
            plugin.getFirstJoinManager().handleFirstJoin(player);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFirstJoinManager().isPendingConfirmation(player.getUniqueId()) &&
            event.getMessage().equalsIgnoreCase("CONFIRM")) {
            event.setCancelled(true);
            plugin.getFirstJoinManager().handleConfirmation(player);
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