package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class StatisticsManager {
    private final LifeSteal plugin;
    private final Map<UUID, PlayerStats> playerStats;
    private final File statsFile;
    private FileConfiguration statsConfig;

    public StatisticsManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.playerStats = new ConcurrentHashMap<>();
        this.statsFile = new File(plugin.getDataFolder(), "statistics.yml");
        loadStatistics();
    }

    public void loadStatistics() {
        if (!statsFile.exists()) {
            plugin.saveResource("statistics.yml", false);
        }
        statsConfig = YamlConfiguration.loadConfiguration(statsFile);
        
        if (statsConfig.contains("players")) {
            for (String uuidStr : statsConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String path = "players." + uuidStr;
                    PlayerStats stats = new PlayerStats(
                        statsConfig.getInt(path + ".kills", 0),
                        statsConfig.getInt(path + ".deaths", 0),
                        statsConfig.getInt(path + ".hearts_stolen", 0),
                        statsConfig.getInt(path + ".hearts_lost", 0),
                        statsConfig.getInt(path + ".bounties_placed", 0),
                        statsConfig.getInt(path + ".bounties_claimed", 0)
                    );
                    playerStats.put(uuid, stats);
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in statistics file: " + uuidStr);
                }
            }
        }
    }

    public void saveStatistics() {
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            String path = "players." + entry.getKey().toString();
            PlayerStats stats = entry.getValue();
            statsConfig.set(path + ".kills", stats.getKills());
            statsConfig.set(path + ".deaths", stats.getDeaths());
            statsConfig.set(path + ".hearts_stolen", stats.getHeartsStolen());
            statsConfig.set(path + ".hearts_lost", stats.getHeartsLost());
            statsConfig.set(path + ".bounties_placed", stats.getBountiesPlaced());
            statsConfig.set(path + ".bounties_claimed", stats.getBountiesClaimed());
        }

        try {
            statsConfig.save(statsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save statistics: " + e.getMessage());
        }
    }

    public PlayerStats getPlayerStats(Player player) {
        return playerStats.computeIfAbsent(player.getUniqueId(), uuid -> new PlayerStats());
    }

    public void incrementKills(Player player) {
        getPlayerStats(player).incrementKills();
        saveStatistics();
    }

    public void incrementDeaths(Player player) {
        getPlayerStats(player).incrementDeaths();
        saveStatistics();
    }

    public void addHeartsStolen(Player player, int amount) {
        getPlayerStats(player).addHeartsStolen(amount);
        saveStatistics();
    }

    public void addHeartsLost(Player player, int amount) {
        getPlayerStats(player).addHeartsLost(amount);
        saveStatistics();
    }

    public void incrementBountiesPlaced(Player player) {
        getPlayerStats(player).incrementBountiesPlaced();
        saveStatistics();
    }

    public void incrementBountiesClaimed(Player player) {
        getPlayerStats(player).incrementBountiesClaimed();
        saveStatistics();
    }

    public static class PlayerStats {
        private int kills;
        private int deaths;
        private int heartsStolen;
        private int heartsLost;
        private int bountiesPlaced;
        private int bountiesClaimed;

        public PlayerStats() {
            this(0, 0, 0, 0, 0, 0);
        }

        public PlayerStats(int kills, int deaths, int heartsStolen, int heartsLost, int bountiesPlaced, int bountiesClaimed) {
            this.kills = kills;
            this.deaths = deaths;
            this.heartsStolen = heartsStolen;
            this.heartsLost = heartsLost;
            this.bountiesPlaced = bountiesPlaced;
            this.bountiesClaimed = bountiesClaimed;
        }

        public int getKills() { return kills; }
        public int getDeaths() { return deaths; }
        public int getHeartsStolen() { return heartsStolen; }
        public int getHeartsLost() { return heartsLost; }
        public int getBountiesPlaced() { return bountiesPlaced; }
        public int getBountiesClaimed() { return bountiesClaimed; }

        public void incrementKills() { kills++; }
        public void incrementDeaths() { deaths++; }
        public void addHeartsStolen(int amount) { heartsStolen += amount; }
        public void addHeartsLost(int amount) { heartsLost += amount; }
        public void incrementBountiesPlaced() { bountiesPlaced++; }
        public void incrementBountiesClaimed() { bountiesClaimed++; }

        public double getKDR() {
            return deaths == 0 ? kills : (double) kills / deaths;
        }
    }
} 