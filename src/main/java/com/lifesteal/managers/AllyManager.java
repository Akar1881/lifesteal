package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AllyManager {
    private final LifeSteal plugin;
    private static final long REQUEST_TIMEOUT = 60000; // 1 minute in milliseconds

    public AllyManager(LifeSteal plugin) {
        this.plugin = plugin;
    }

    public boolean sendAllyRequest(Player sender, Player target) {
        sender.playSound(sender.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        target.playSound(target.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        if (isAlly(sender, target)) {
            return false;
        }

        plugin.getDatabaseManager().addAllyRequest(sender.getUniqueId(), target.getUniqueId());
        return true;
    }

    public boolean acceptAllyRequest(Player player, Player requester) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        requester.playSound(requester.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);

        if (!hasPendingIncomingRequest(player, requester)) {
            return false;
        }

        plugin.getDatabaseManager().removeAllyRequest(requester.getUniqueId(), player.getUniqueId());
        plugin.getDatabaseManager().addAlly(player.getUniqueId(), requester.getUniqueId());
        return true;
    }

    public boolean cancelAllyRequest(Player player, Player target) {
        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
        target.playSound(target.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);

        plugin.getDatabaseManager().removeAllyRequest(player.getUniqueId(), target.getUniqueId());
        return true;
    }

    public boolean removeAlly(Player player, OfflinePlayer ally) {
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        if (ally.isOnline()) {
            ally.getPlayer().playSound(ally.getPlayer().getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        plugin.getDatabaseManager().removeAlly(player.getUniqueId(), ally.getUniqueId());
        return true;
    }

    public boolean isAlly(Player player, Player target) {
        List<UUID> allies = plugin.getDatabaseManager().getAllies(player.getUniqueId());
        return allies.contains(target.getUniqueId());
    }

    public boolean isAlly(Player player, OfflinePlayer target) {
        List<UUID> allies = plugin.getDatabaseManager().getAllies(player.getUniqueId());
        return allies.contains(target.getUniqueId());
    }

    public List<OfflinePlayer> getAllies(Player player) {
        List<UUID> allyUUIDs = plugin.getDatabaseManager().getAllies(player.getUniqueId());
        List<OfflinePlayer> allies = new ArrayList<>();

        for (UUID uuid : allyUUIDs) {
            allies.add(Bukkit.getOfflinePlayer(uuid));
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

    public boolean hasPendingIncomingRequest(Player player, Player requester) {
        List<UUID> requests = plugin.getDatabaseManager().getPendingAllyRequests(player.getUniqueId());
        return requests.contains(requester.getUniqueId());
    }

    public List<Player> getPendingIncomingRequests(Player player) {
        List<UUID> requestUUIDs = plugin.getDatabaseManager().getPendingAllyRequests(player.getUniqueId());
        List<Player> requesters = new ArrayList<>();

        for (UUID uuid : requestUUIDs) {
            Player requester = Bukkit.getPlayer(uuid);
            if (requester != null && requester.isOnline()) {
                requesters.add(requester);
            }
        }

        return requesters;
    }

    public void cleanupTimedOutRequests() {
        plugin.getDatabaseManager().cleanupTimedOutRequests(REQUEST_TIMEOUT);
    }

    public boolean hasPendingOutgoingRequest(Player sender, Player target) {
    List<UUID> requests = plugin.getDatabaseManager().getPendingAllyRequests(target.getUniqueId());
    return requests.contains(sender.getUniqueId());
    }
}