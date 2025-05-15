package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import com.lifesteal.utils.SafeLocationFinder;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class FirstJoinManager {
    private final LifeSteal plugin;
    private final Set<UUID> pendingConfirmations = new HashSet<>();
    private final Set<UUID> frozenPlayers = new HashSet<>();

    public FirstJoinManager(LifeSteal plugin) {
        this.plugin = plugin;
    }

    public void handleFirstJoin(Player player) {
        if (!plugin.getConfigManager().isFirstJoinEnabled()) {
            return;
        }

        // Set gamemode to spectator and teleport to Y=250
        player.setGameMode(GameMode.SPECTATOR);
        Location spawnLoc = player.getLocation().clone();
        spawnLoc.setY(250);
        player.teleport(spawnLoc);
        
        // Freeze player
        frozenPlayers.add(player.getUniqueId());
        
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

        pendingConfirmations.remove(player.getUniqueId());
        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getFirstJoinConfirmMessage()));

        // Find safe location and teleport player
        new BukkitRunnable() {
            @Override
            public void run() {
                SafeLocationFinder finder = new SafeLocationFinder(plugin);
                Location safeLoc = finder.findSafeLocation();
                
                if (safeLoc != null) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            player.setGameMode(GameMode.SURVIVAL);
                            player.teleport(safeLoc);
                            frozenPlayers.remove(player.getUniqueId());
                            player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getFirstJoinTeleportMessage()));
                        }
                    }.runTask(plugin);
                } else {
                    player.sendMessage(ColorUtils.colorize("&cFailed to find safe location. Please contact an administrator."));
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}