package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import com.lifesteal.utils.QueueWorld;
import io.papermc.lib.PaperLib;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FirstJoinManager {
    private final LifeSteal plugin;
    private final Set<UUID> pendingConfirmations = new HashSet<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();
    private QueueWorld queueWorld;

    public FirstJoinManager(LifeSteal plugin) {
        this.plugin = plugin;
        
        // Initialize PaperLib
        PaperLib.suggestPaper(plugin);
        
        // Initialize queue world if first join is enabled
        if (plugin.getConfigManager().isFirstJoinEnabled()) {
            this.queueWorld = new QueueWorld(plugin);
            this.queueWorld.initialize();
        }
    }

    public void handleFirstJoin(Player player) {
        if (!plugin.getConfigManager().isFirstJoinEnabled()) {
            return;
        }

        // Set gamemode to spectator temporarily
        player.setGameMode(GameMode.SPECTATOR);
        
        // Freeze player
        frozenPlayers.add(player.getUniqueId());
        
        // Send player to queue world
        queueWorld.sendToQueueWorld(player);
        
        // Send welcome messages
        for (String message : plugin.getConfigManager().getFirstJoinMessages()) {
            player.sendMessage(ColorUtils.colorize(message));
        }
        
        // Add to pending confirmations
        pendingConfirmations.add(player.getUniqueId());
    }

    public boolean isPendingConfirmation(UUID uuid) {
        return pendingConfirmations.contains(uuid);
    }

    public boolean isFrozen(UUID uuid) {
        return frozenPlayers.contains(uuid);
    }

    public void handleConfirmation(Player player) {
        if (!pendingConfirmations.contains(player.getUniqueId())) {
            return;
        }

        // Remove from pending confirmations
        pendingConfirmations.remove(player.getUniqueId());
        
        // Send confirmation message
        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getFirstJoinConfirmMessage()));

        // Handle confirmation in queue world
        queueWorld.handlePlayerConfirmation(player);
        
        // Remove from frozen players (will be done in the teleport method)
        frozenPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Clean up resources when the plugin is disabled
     */
    public void cleanup() {
        if (queueWorld != null) {
            queueWorld.cleanup();
        }
    }
    
    /**
     * Get the queue world manager
     * @return The queue world manager
     */
    public QueueWorld getQueueWorld() {
        return queueWorld;
    }
}
