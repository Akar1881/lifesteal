package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import com.lifesteal.utils.QueueWorld;
import io.papermc.lib.PaperLib;
import org.bukkit.GameMode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

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
            
            // Load saved player states
            loadPlayerStates();
        }
    }
    
    /**
     * Save player states to database
     */
    public void savePlayerStates() {
        if (!plugin.getConfigManager().isFirstJoinEnabled()) {
            return;
        }
        
        // Save pending confirmations and frozen players
        for (UUID uuid : pendingConfirmations) {
            plugin.getDatabaseManager().setQueueState(uuid, true, false, true);
        }
        
        // Save player confirmation states from queue world
        queueWorld.savePlayerConfirmationStates();
        
        plugin.getLogger().info("Saved queue player states to database");
    }
    
    /**
     * Load player states from database
     */
    private void loadPlayerStates() {
        // Get all players in the queue
        Map<UUID, Map<String, Boolean>> states = plugin.getDatabaseManager().getAllQueueStates();
        
        for (Map.Entry<UUID, Map<String, Boolean>> entry : states.entrySet()) {
            UUID uuid = entry.getKey();
            Map<String, Boolean> state = entry.getValue();
            
            // Add to pending confirmations if not confirmed
            if (!state.get("confirmed")) {
                pendingConfirmations.add(uuid);
            }
            
            // Add to frozen players if frozen
            if (state.get("frozen")) {
                frozenPlayers.add(uuid);
            }
        }
        
        // Load player confirmation states
        queueWorld.loadPlayerConfirmationStates();
        
        plugin.getLogger().info("Loaded queue player states: " + 
                pendingConfirmations.size() + " pending confirmations, " + 
                frozenPlayers.size() + " frozen players");
    }

    public void handleFirstJoin(Player player) {
        if (!plugin.getConfigManager().isFirstJoinEnabled()) {
            return;
        }

        // Set gamemode to spectator temporarily
        player.setGameMode(GameMode.SPECTATOR);
        
        // Freeze player
        frozenPlayers.add(player.getUniqueId());
        
        // Update database
        plugin.getDatabaseManager().setQueueState(player.getUniqueId(), true, false, true);
        
        // Send player to queue world
        queueWorld.sendToQueueWorld(player);
        
        // Send welcome messages
        for (String message : plugin.getConfigManager().getFirstJoinMessages()) {
            player.sendMessage(ColorUtils.colorize(message));
        }
        
        // Add clear instructions
        player.sendMessage("");
        player.sendMessage(ColorUtils.colorize("&6&l➤ &eType &6CONFIRM &ein chat to continue."));
        player.sendMessage(ColorUtils.colorize("&6&l➤ &eYou can disconnect and come back later if you wish."));
        player.sendMessage(ColorUtils.colorize("&6&l➤ &eType &6help &eif you need assistance."));
        player.sendMessage("");
        
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
        
        // Update database
        plugin.getDatabaseManager().setQueueState(player.getUniqueId(), true, true, true);
        
        // Send confirmation message
        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getFirstJoinConfirmMessage()));

        // Handle confirmation in queue world
        queueWorld.handlePlayerConfirmation(player);
        
        // Remove from frozen players (will be done in the teleport method)
        frozenPlayers.remove(player.getUniqueId());
    }
    
    /**
     * Handle a player reconnecting after being in the queue world
     * @param player The player who reconnected
     */
    public void handleReconnect(Player player) {
        // Set gamemode to spectator temporarily
        player.setGameMode(GameMode.SPECTATOR);
        
        // Make sure they're still frozen
        if (!frozenPlayers.contains(player.getUniqueId())) {
            frozenPlayers.add(player.getUniqueId());
        }
        
        // Send player to queue world
        queueWorld.sendToQueueWorld(player);
        
        // Send reconnect message
        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getReconnectMessage()));
        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getReconnectConfirmMessage()));
        
        // If they already confirmed before, show them the status
        if (queueWorld.isPlayerConfirmed(player.getUniqueId())) {
            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getReconnectAlreadyConfirmedMessage()));
            double progress = queueWorld.getGenerationProgress();
            String progressMsg = plugin.getConfigManager().getReconnectProgressMessage()
                .replace("%progress%", String.format("%.1f", progress) + "%");
            player.sendMessage(ColorUtils.colorize(progressMsg));
        }
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
