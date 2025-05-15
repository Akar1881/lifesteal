package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HeartManager {
    private final LifeSteal plugin;
    private final Map<UUID, Integer> playerHearts = new HashMap<>();
    private final File dataFile;
    private FileConfiguration data;

    public HeartManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        loadData();
    }

    private void loadData() {
        if (!dataFile.exists()) {
            plugin.saveResource("playerdata.yml", false);
        }
        data = YamlConfiguration.loadConfiguration(dataFile);
        
        // Load saved hearts into memory
        if (data.contains("players")) {
            for (String uuidStr : data.getConfigurationSection("players").getKeys(false)) {
                UUID uuid = UUID.fromString(uuidStr);
                int hearts = data.getInt("players." + uuidStr);
                playerHearts.put(uuid, hearts);
            }
        }
    }

    private void saveData() {
        for (Map.Entry<UUID, Integer> entry : playerHearts.entrySet()) {
            data.set("players." + entry.getKey().toString(), entry.getValue());
        }
        
        try {
            data.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data!");
            e.printStackTrace();
        }
    }
    
    /**
     * Saves data for a specific player
     * @param uuid The UUID of the player to save
     */
    public void savePlayerData(UUID uuid) {
        if (playerHearts.containsKey(uuid)) {
            data.set("players." + uuid.toString(), playerHearts.get(uuid));
            
            try {
                data.save(dataFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Could not save player data for " + uuid + "!");
                e.printStackTrace();
            }
        }
    }

    public void setHearts(Player player, int hearts) {
        int maxHealth = hearts * 2;
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
        player.setHealth(maxHealth);
        playerHearts.put(player.getUniqueId(), hearts);
        saveData();
    }

    public int getHearts(Player player) {
        return playerHearts.getOrDefault(player.getUniqueId(), 
            plugin.getConfigManager().getStartingHearts());
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
            // Make sure the sound name is properly formatted for the Sound enum
            if (soundName.contains(".")) {
                soundName = soundName.toUpperCase().replace(".", "_");
            }
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            // Fallback to a safe sound if there's an error
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    public void removeHearts(Player player, int amount) {
        int currentHearts = getHearts(player);
        int minHearts = plugin.getConfigManager().getMinHearts();
        int newHearts = Math.max(currentHearts - amount, minHearts);
        setHearts(player, newHearts);
        // Play negative sound for heart loss
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);

        if (newHearts <= 0) {
            eliminatePlayer(player);
        }
    }

    public void eliminatePlayer(Player player) {
        String eliminationMode = plugin.getConfigManager().getEliminationMode();
        // Play dramatic sound for elimination to all players
        org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f));
        
        if (eliminationMode.equalsIgnoreCase("ban")) {
            String banMessage = ColorUtils.colorize(plugin.getConfigManager().getConfig().getString("messages.banned", "&4You have been eliminated!"));
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
        // Play magical sound for revival
        player.playSound(player.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
    }
}