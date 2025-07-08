package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;

public class DatabaseManager {
    private final LifeSteal plugin;
    private Connection connection;
    private final String storageType;
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String sqliteFile;

    public DatabaseManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        this.host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        this.database = plugin.getConfig().getString("storage.mysql.database", "lifesteal");
        this.username = plugin.getConfig().getString("storage.mysql.user", "root");
        this.password = plugin.getConfig().getString("storage.mysql.password", "password");
        this.sqliteFile = plugin.getConfig().getString("storage.sqlite.file", "plugins/Lifesteal/storage/lifesteal.db");
    }

    public void initialize() {

        if (storageType.equals("mysql")) {
            initializeMySql();
        } else {
            initializeSqlite();
        }
        createTables();
    }

    private void initializeMySql() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database,
                username,
                password
            );
            plugin.getLogger().info("Successfully connected to MySQL database!");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to connect to MySQL database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeSqlite() {
        try {
            Class.forName("org.sqlite.JDBC");
            File dataFolder = new File(sqliteFile).getParentFile();
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteFile);
            plugin.getLogger().info("Successfully connected to SQLite database!");
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createTables() {
        try {
            // Players table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        hearts INT NOT NULL DEFAULT 10
                    )
                """);
            }

            // Allies table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS allies (
                        player_uuid VARCHAR(36),
                        ally_uuid VARCHAR(36),
                        PRIMARY KEY (player_uuid, ally_uuid),
                        FOREIGN KEY (player_uuid) REFERENCES players(uuid),
                        FOREIGN KEY (ally_uuid) REFERENCES players(uuid)
                    )
                """);
            }

            // Ally requests table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS ally_requests (
                        sender_uuid VARCHAR(36),
                        receiver_uuid VARCHAR(36),
                        timestamp BIGINT NOT NULL,
                        PRIMARY KEY (sender_uuid, receiver_uuid),
                        FOREIGN KEY (sender_uuid) REFERENCES players(uuid),
                        FOREIGN KEY (receiver_uuid) REFERENCES players(uuid)
                    )
                """);
            }

            // World Border table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS world_border (
                        id INT PRIMARY KEY DEFAULT 1,
                        current_size DOUBLE NOT NULL,
                        initial_size DOUBLE NOT NULL,
                        last_shrink_time BIGINT NOT NULL,
                        next_shrink_time BIGINT NOT NULL,
                        CHECK (id = 1)
                    )
                """);
            }

            // Create cycle_timer table after connection is ready
            new com.lifesteal.managers.CycleTimerDatabase(plugin).createTable();
            plugin.getLogger().info("Successfully created database tables!");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create database tables: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void setHearts(UUID uuid, int hearts) {
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        String sql;
        if (storageType.equals("mysql")) {
            sql = "INSERT INTO players (uuid, hearts) VALUES (?, ?) ON DUPLICATE KEY UPDATE hearts = ?";
        } else {
            sql = "INSERT OR REPLACE INTO players (uuid, hearts) VALUES (?, ?)";
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, hearts);
            if (storageType.equals("mysql")) {
                stmt.setInt(3, hearts);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to set hearts for " + uuid, e);
        }
    }

    public int getHearts(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT hearts FROM players WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("hearts");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get hearts for " + uuid, e);
        }
        return plugin.getConfigManager().getStartingHearts();
    }

    public void addAlly(UUID player, UUID ally) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO allies (player_uuid, ally_uuid) VALUES (?, ?)")) {
            // Add both directions for mutual alliance
            stmt.setString(1, player.toString());
            stmt.setString(2, ally.toString());
            stmt.executeUpdate();
            
            stmt.setString(1, ally.toString());
            stmt.setString(2, player.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add ally relationship", e);
        }
    }

    public void removeAlly(UUID player, UUID ally) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM allies WHERE (player_uuid = ? AND ally_uuid = ?) OR (player_uuid = ? AND ally_uuid = ?)")) {
            stmt.setString(1, player.toString());
            stmt.setString(2, ally.toString());
            stmt.setString(3, ally.toString());
            stmt.setString(4, player.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove ally relationship", e);
        }
    }

    public List<UUID> getAllies(UUID player) {
        List<UUID> allies = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT ally_uuid FROM allies WHERE player_uuid = ?")) {
            stmt.setString(1, player.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                allies.add(UUID.fromString(rs.getString("ally_uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get allies for " + player, e);
        }
        return allies;
    }

    public void addAllyRequest(UUID sender, UUID receiver) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO ally_requests (sender_uuid, receiver_uuid, timestamp) VALUES (?, ?, ?)")) {
            stmt.setString(1, sender.toString());
            stmt.setString(2, receiver.toString());
            stmt.setLong(3, System.currentTimeMillis());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to add ally request", e);
        }
    }

    public void removeAllyRequest(UUID sender, UUID receiver) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM ally_requests WHERE sender_uuid = ? AND receiver_uuid = ?")) {
            stmt.setString(1, sender.toString());
            stmt.setString(2, receiver.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to remove ally request", e);
        }
    }

    public List<UUID> getPendingAllyRequests(UUID receiver) {
        List<UUID> requests = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT sender_uuid FROM ally_requests WHERE receiver_uuid = ?")) {
            stmt.setString(1, receiver.toString());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                requests.add(UUID.fromString(rs.getString("sender_uuid")));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get pending ally requests for " + receiver, e);
        }
        return requests;
    }

    public void cleanupTimedOutRequests(long timeout) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM ally_requests WHERE timestamp < ?")) {
            stmt.setLong(1, System.currentTimeMillis() - timeout);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to cleanup timed out requests", e);
        }
    }

    public void saveWorldBorderData(double currentSize, double initialSize, long lastShrinkTime, long nextShrinkTime) {
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        String sql;
        if (storageType.equals("mysql")) {
            sql = "INSERT INTO world_border (id, current_size, initial_size, last_shrink_time, next_shrink_time) " +
                  "VALUES (1, ?, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE current_size = ?, initial_size = ?, last_shrink_time = ?, next_shrink_time = ?";
        } else {
            sql = "INSERT OR REPLACE INTO world_border (id, current_size, initial_size, last_shrink_time, next_shrink_time) VALUES (1, ?, ?, ?, ?)";
        }
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, currentSize);
            stmt.setDouble(2, initialSize);
            stmt.setLong(3, lastShrinkTime);
            stmt.setLong(4, nextShrinkTime);
            if (storageType.equals("mysql")) {
                stmt.setDouble(5, currentSize);
                stmt.setDouble(6, initialSize);
                stmt.setLong(7, lastShrinkTime);
                stmt.setLong(8, nextShrinkTime);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save world border data", e);
        }
    }

    public Map<String, Object> getWorldBorderData() {
        Map<String, Object> data = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT current_size, initial_size, last_shrink_time, next_shrink_time FROM world_border WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                data.put("current_size", rs.getDouble("current_size"));
                data.put("initial_size", rs.getDouble("initial_size"));
                data.put("last_shrink_time", rs.getLong("last_shrink_time"));
                data.put("next_shrink_time", rs.getLong("next_shrink_time"));
            } else {
                // Initialize with default values
                double initialSize = plugin.getConfigManager().getInitialBorderSize();
                data.put("current_size", initialSize);
                data.put("initial_size", initialSize);
                data.put("last_shrink_time", 0L);
                data.put("next_shrink_time", 0L);
                saveWorldBorderData(initialSize, initialSize, 0L, 0L);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get world border data", e);
            // Return default values on error
            double initialSize = plugin.getConfigManager().getInitialBorderSize();
            data.put("current_size", initialSize);
            data.put("initial_size", initialSize);
            data.put("last_shrink_time", 0L);
            data.put("next_shrink_time", 0L);
        }
        return data;
    }

    public Connection getConnection() {
        return connection;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error closing database connection: " + e.getMessage());
            }
        }
    }
}