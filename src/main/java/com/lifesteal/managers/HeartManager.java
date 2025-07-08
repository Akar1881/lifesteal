package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public class HeartManager {
    private final LifeSteal plugin;

    public HeartManager(LifeSteal plugin) {
        this.plugin = plugin;
    }

    public void setHearts(Player player, int hearts) {
        int maxHealth = hearts * 2;
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        plugin.getDatabaseManager().setHearts(player.getUniqueId(), hearts);
    }

    public int getHearts(Player player) {
        return plugin.getDatabaseManager().getHearts(player.getUniqueId());
    }

    public void addHearts(Player player, int amount) {
        int currentHearts = getHearts(player);
        int maxHearts = plugin.getConfigManager().getMaxHearts();
        int newHearts = Math.min(currentHearts + amount, maxHearts);
        
        if (!player.hasPermission("lifesteal.bypass.maxhearts")) {
            newHearts = Math.min(newHearts, maxHearts);
        }
        
        setHearts(player, newHearts);
        
        // Play positive sound for heart gain
        try {
            String soundName = plugin.getConfigManager().getConfig().getString("sounds.heart-gain", "ENTITY_PLAYER_LEVELUP");
            if (soundName.contains(".")) {
                soundName = soundName.toUpperCase().replace(".", "_");
            }
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public void removeHearts(Player player, int amount) {
        int currentHearts = getHearts(player);
        int minHearts = plugin.getConfigManager().getMinHearts();
        int newHearts = Math.max(currentHearts - amount, minHearts);
        setHearts(player, newHearts);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

        if (newHearts <= 0) {
            eliminatePlayer(player);
        }
    }

    public void eliminatePlayer(Player player) {
        String eliminationMode = plugin.getConfigManager().getEliminationMode();
        Bukkit.getOnlinePlayers().forEach(p -> 
            p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));
        
        if (eliminationMode.equalsIgnoreCase("ban")) {
            String banMessage = ColorUtils.colorize(plugin.getConfigManager().getConfig()
                .getString("messages.banned", "&4You have been eliminated!"));
            player.kickPlayer(banMessage);
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).addBan(
                player.getName(),
                "Eliminated from LifeSteal",
                null,
                "LifeSteal Plugin"
            );
        } else {
            player.setGameMode(GameMode.SPECTATOR);
        }

        String cmd = plugin.getConfigManager().getConfig().getString("elimination.command");
        if (cmd != null && !cmd.isEmpty()) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                cmd.replace("%player%", player.getName())
            );
        }
    }

    public void revivePlayer(Player player) {
        player.setGameMode(GameMode.SURVIVAL);
        setHearts(player, plugin.getConfigManager().getStartingHearts());
        if (player.isBanned()) {
            Bukkit.getBanList(org.bukkit.BanList.Type.NAME).pardon(player.getName());
        }
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
    }

    public void updateConfig() {
        // Get new values from config
        int startingHearts = plugin.getConfigManager().getStartingHearts();
        int maxHearts = plugin.getConfigManager().getMaxHearts();
        int minHearts = plugin.getConfigManager().getMinHearts();
        
        // Update hearts for all online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Set all players to the new starting hearts value
            setHearts(player, startingHearts);
            
            // If current hearts are outside new limits, adjust them
            int currentHearts = getHearts(player);
            if (currentHearts > maxHearts) {
                setHearts(player, maxHearts);
            } else if (currentHearts < minHearts) {
                setHearts(player, minHearts);
            }
        }
    }

    public boolean shouldLoseHeartsOnQuit(Player player) {
        return plugin.getConfigManager().getConfig().getBoolean("quit-heart-loss.enabled", false);
    }

    public void handleQuitHeartLoss(Player player) {
        int heartsLost = plugin.getConfigManager().getConfig().getInt("quit-heart-loss.amount", 1);
        removeHearts(player, heartsLost);
    }

    public void handleKill(Player killer, Player victim) {
        // Get hearts to steal from config
        int heartsToSteal = plugin.getConfigManager().getHeartsLostPerDeath();
        
        // Remove hearts from victim
        removeHearts(victim, heartsToSteal);
        
        // Add hearts to killer
        addHearts(killer, heartsToSteal);
        
        // Send messages
        killer.sendMessage(ColorUtils.colorize("&aYou stole " + heartsToSteal + " heart(s) from " + victim.getName() + "!"));
        victim.sendMessage(ColorUtils.colorize("&cYou lost " + heartsToSteal + " heart(s) to " + killer.getName() + "!"));
    }
}