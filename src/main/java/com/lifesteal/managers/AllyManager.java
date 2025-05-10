package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class AllyManager {
    private final LifeSteal plugin;
    private File alliesFile;
    private FileConfiguration alliesConfig;
    private final long REQUEST_TIMEOUT = 60000; // 1 minute in milliseconds

    public AllyManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    public void loadConfig() {
        alliesFile = new File(plugin.getDataFolder(), "allies.yml");
        if (!alliesFile.exists()) {
            plugin.saveResource("allies.yml", false);
        }
        alliesConfig = YamlConfiguration.loadConfiguration(alliesFile);
    }

    public void saveConfig() {
        try {
            alliesConfig.save(alliesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save allies.yml!");
            e.printStackTrace();
        }
    }

    public void reloadConfig() {
        alliesConfig = YamlConfiguration.loadConfiguration(alliesFile);
    }

    public boolean sendAllyRequest(Player sender, Player target) {
        String senderUUID = sender.getUniqueId().toString();
        String targetUUID = target.getUniqueId().toString();

        // Check if they are already allies
        if (isAlly(sender, target)) {
            return false;
        }

        // Check if there's already a pending request
        if (hasPendingRequest(sender, target)) {
            return false;
        }

        // Add to sender's outgoing requests
        String senderPath = senderUUID + ".pending-outgoing." + targetUUID;
        alliesConfig.set(senderPath, System.currentTimeMillis());

        // Add to target's incoming requests
        String targetPath = targetUUID + ".pending-incoming." + senderUUID;
        alliesConfig.set(targetPath, System.currentTimeMillis());

        saveConfig();
        return true;
    }

    public boolean acceptAllyRequest(Player player, Player requester) {
        String playerUUID = player.getUniqueId().toString();
        String requesterUUID = requester.getUniqueId().toString();

        // Check if there's a pending request
        if (!hasPendingIncomingRequest(player, requester)) {
            return false;
        }

        // Remove pending requests
        alliesConfig.set(playerUUID + ".pending-incoming." + requesterUUID, null);
        alliesConfig.set(requesterUUID + ".pending-outgoing." + playerUUID, null);

        // Add to allies lists
        List<String> playerAllies = alliesConfig.getStringList(playerUUID + ".allies");
        if (!playerAllies.contains(requesterUUID)) {
            playerAllies.add(requesterUUID);
        }
        alliesConfig.set(playerUUID + ".allies", playerAllies);

        List<String> requesterAllies = alliesConfig.getStringList(requesterUUID + ".allies");
        if (!requesterAllies.contains(playerUUID)) {
            requesterAllies.add(playerUUID);
        }
        alliesConfig.set(requesterUUID + ".allies", requesterAllies);

        saveConfig();
        return true;
    }

    public boolean cancelAllyRequest(Player player, Player target) {
        String playerUUID = player.getUniqueId().toString();
        String targetUUID = target.getUniqueId().toString();

        // Remove from player's outgoing requests
        alliesConfig.set(playerUUID + ".pending-outgoing." + targetUUID, null);

        // Remove from target's incoming requests
        alliesConfig.set(targetUUID + ".pending-incoming." + playerUUID, null);

        saveConfig();
        return true;
    }

    public boolean removeAlly(Player player, OfflinePlayer ally) {
        String playerUUID = player.getUniqueId().toString();
        String allyUUID = ally.getUniqueId().toString();

        // Remove from player's allies
        List<String> playerAllies = alliesConfig.getStringList(playerUUID + ".allies");
        if (playerAllies.contains(allyUUID)) {
            playerAllies.remove(allyUUID);
            alliesConfig.set(playerUUID + ".allies", playerAllies);
        } else {
            return false;
        }

        // Remove from ally's allies
        List<String> allyAllies = alliesConfig.getStringList(allyUUID + ".allies");
        if (allyAllies.contains(playerUUID)) {
            allyAllies.remove(playerUUID);
            alliesConfig.set(allyUUID + ".allies", allyAllies);
        }

        saveConfig();
        return true;
    }

    public boolean isAlly(Player player, Player target) {
        String playerUUID = player.getUniqueId().toString();
        String targetUUID = target.getUniqueId().toString();

        List<String> playerAllies = alliesConfig.getStringList(playerUUID + ".allies");
        return playerAllies.contains(targetUUID);
    }

    public boolean isAlly(Player player, OfflinePlayer target) {
        String playerUUID = player.getUniqueId().toString();
        String targetUUID = target.getUniqueId().toString();

        List<String> playerAllies = alliesConfig.getStringList(playerUUID + ".allies");
        return playerAllies.contains(targetUUID);
    }

    public boolean hasPendingRequest(Player player, Player target) {
        return hasPendingOutgoingRequest(player, target) || hasPendingIncomingRequest(player, target);
    }

    public boolean hasPendingOutgoingRequest(Player player, Player target) {
        String playerUUID = player.getUniqueId().toString();
        String targetUUID = target.getUniqueId().toString();

        return alliesConfig.contains(playerUUID + ".pending-outgoing." + targetUUID);
    }

    public boolean hasPendingIncomingRequest(Player player, Player requester) {
        String playerUUID = player.getUniqueId().toString();
        String requesterUUID = requester.getUniqueId().toString();

        return alliesConfig.contains(playerUUID + ".pending-incoming." + requesterUUID);
    }

    public List<OfflinePlayer> getAllies(Player player) {
        String playerUUID = player.getUniqueId().toString();
        List<String> allyUUIDs = alliesConfig.getStringList(playerUUID + ".allies");
        List<OfflinePlayer> allies = new ArrayList<>();

        for (String uuid : allyUUIDs) {
            OfflinePlayer ally = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            allies.add(ally);
        }

        return allies;
    }

    public List<Player> getOnlineAllies(Player player) {
        List<OfflinePlayer> allAllies = getAllies(player);
        List<Player> onlineAllies = new ArrayList<>();

        for (OfflinePlayer ally : allAllies) {
            if (ally.isOnline()) {
                onlineAllies.add(ally.getPlayer());
            }
        }

        return onlineAllies;
    }

    public List<Player> getPendingIncomingRequests(Player player) {
        String playerUUID = player.getUniqueId().toString();
        ConfigurationSection section = alliesConfig.getConfigurationSection(playerUUID + ".pending-incoming");
        List<Player> requesters = new ArrayList<>();

        if (section != null) {
            for (String requesterUUID : section.getKeys(false)) {
                long timestamp = section.getLong(requesterUUID);
                
                // Check if request has timed out
                if (System.currentTimeMillis() - timestamp > REQUEST_TIMEOUT) {
                    // Remove timed out request
                    alliesConfig.set(playerUUID + ".pending-incoming." + requesterUUID, null);
                    alliesConfig.set(requesterUUID + ".pending-outgoing." + playerUUID, null);
                    continue;
                }
                
                Player requester = Bukkit.getPlayer(UUID.fromString(requesterUUID));
                if (requester != null && requester.isOnline()) {
                    requesters.add(requester);
                }
            }
            saveConfig();
        }

        return requesters;
    }

    public void cleanupTimedOutRequests() {
        boolean needsSave = false;
        
        for (String playerUUID : alliesConfig.getKeys(false)) {
            ConfigurationSection incomingSection = alliesConfig.getConfigurationSection(playerUUID + ".pending-incoming");
            if (incomingSection != null) {
                for (String requesterUUID : incomingSection.getKeys(false)) {
                    long timestamp = incomingSection.getLong(requesterUUID);
                    
                    if (System.currentTimeMillis() - timestamp > REQUEST_TIMEOUT) {
                        alliesConfig.set(playerUUID + ".pending-incoming." + requesterUUID, null);
                        alliesConfig.set(requesterUUID + ".pending-outgoing." + playerUUID, null);
                        needsSave = true;
                    }
                }
            }
        }
        
        if (needsSave) {
            saveConfig();
        }
    }
}