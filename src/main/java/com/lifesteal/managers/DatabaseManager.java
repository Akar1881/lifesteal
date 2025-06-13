package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class DatabaseManager {
    private final LifeSteal plugin;
    private HikariDataSource dataSource;
    private String storageType;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String sqliteFile;
    private static final int MAX_POOL_SIZE = 10;
    private static final int MIN_IDLE = 5;
    private static final long MAX_LIFETIME = TimeUnit.MINUTES.toMillis(30);
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private static final long VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY = TimeUnit.SECONDS.toMillis(5);
    private static final int BATCH_SIZE = 1000;
    private static final long CONNECTION_TEST_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    public DatabaseManager(LifeSteal plugin) {
        this.plugin = plugin;
        loadConfiguration();
        startConnectionTestTask();
    }

    private void loadConfiguration() {
        // Storage type with fallback
        this.storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        
        // MySQL configuration with fallbacks
        this.host = plugin.getConfig().getString("storage.mysql.host", "localhost");
        this.port = plugin.getConfig().getInt("storage.mysql.port", 3306);
        this.database = plugin.getConfig().getString("storage.mysql.database", "lifesteal");
        this.username = plugin.getConfig().getString("storage.mysql.user", "root");
        this.password = plugin.getConfig().getString("storage.mysql.password", "password");
        
        // SQLite configuration with fallback
        this.sqliteFile = plugin.getConfig().getString("storage.sqlite.file", 
            "plugins/Lifesteal/storage/lifesteal.db");
    }

    private void startConnectionTestTask() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                testConnection();
            }
        }, CONNECTION_TEST_INTERVAL, CONNECTION_TEST_INTERVAL);
    }

    private void testConnection() {
        try (Connection conn = getConnection()) {
            validateConnection(conn);
        } catch (SQLException e) {
            plugin.getLogger().warning("Database connection test failed: " + e.getMessage());
            reconnect();
        }
    }

    private void reconnect() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
            initialize();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to reconnect to database: " + e.getMessage());
        }
    }

    public void initialize() {
        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                if (storageType.equals("mysql")) {
                    initializeMySql();
                } else {
                    initializeSqlite();
                }
                createTables();
                return;
            } catch (Exception e) {
                retries++;
                plugin.getLogger().severe("Failed to initialize database (attempt " + retries + "/" + MAX_RETRIES + "): " + e.getMessage());
                if (retries < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        plugin.getLogger().severe("Failed to initialize database after " + MAX_RETRIES + " attempts!");
    }

    private void initializeMySql() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + 
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        configureHikariPool(config);
        
        // MySQL-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Successfully initialized MySQL connection pool!");
    }

    private void initializeSqlite() {
        HikariConfig config = new HikariConfig();
        File dataFolder = new File(sqliteFile).getParentFile();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        config.setJdbcUrl("jdbc:sqlite:" + sqliteFile);
        configureHikariPool(config);
        
        // SQLite-specific optimizations
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        
        dataSource = new HikariDataSource(config);
        plugin.getLogger().info("Successfully initialized SQLite connection pool!");
    }

    private void configureHikariPool(HikariConfig config) {
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setMaxLifetime(MAX_LIFETIME);
        config.setConnectionTimeout(CONNECTION_TIMEOUT);
        config.setValidationTimeout(VALIDATION_TIMEOUT);
        config.setLeakDetectionThreshold(TimeUnit.SECONDS.toMillis(30));
        config.setAutoCommit(true);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            initialize();
        }
        return dataSource.getConnection();
    }

    private void validateConnection(Connection conn) throws SQLException {
        if (conn == null || conn.isClosed()) {
            throw new SQLException("Connection is null or closed");
        }
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1");
        }
    }

    public void executeQuery(String sql, QueryCallback callback) {
        try (Connection conn = getConnection()) {
            validateConnection(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                callback.execute(stmt);
            }
        } catch (SQLException e) {
            handleDatabaseError("Error executing query: " + sql, e);
        }
    }

    public <T> T executeQuery(String sql, ResultSetCallback<T> callback) {
        try (Connection conn = getConnection()) {
            validateConnection(conn);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                try (ResultSet rs = stmt.executeQuery()) {
                    return callback.execute(rs);
                }
            }
        } catch (SQLException e) {
            handleDatabaseError("Error executing query: " + sql, e);
            return null;
        }
    }

    private void handleDatabaseError(String message, SQLException e) {
        plugin.getLogger().log(Level.SEVERE, message, e);
        if (e.getMessage().contains("Communications link failure") || 
            e.getMessage().contains("Connection is closed")) {
            reconnect();
        }
    }

    public void executeBatch(String sql, List<Object[]> batchData) {
        try (Connection conn = getConnection()) {
            validateConnection(conn);
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int count = 0;
                for (Object[] data : batchData) {
                    for (int i = 0; i < data.length; i++) {
                        stmt.setObject(i + 1, data[i]);
                    }
                    stmt.addBatch();
                    count++;
                    
                    if (count % BATCH_SIZE == 0) {
                        stmt.executeBatch();
                    }
                }
                if (count % BATCH_SIZE != 0) {
                    stmt.executeBatch();
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            handleDatabaseError("Error executing batch query: " + sql, e);
        }
    }

    public void close() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing database connection", e);
        }
    }

    // Functional interfaces for callbacks
    @FunctionalInterface
    public interface QueryCallback {
        void execute(PreparedStatement stmt) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultSetCallback<T> {
        T execute(ResultSet rs) throws SQLException;
    }

    // Example of using the new methods for setHearts
    public void setHearts(UUID uuid, int hearts) {
        String sql = storageType.equals("mysql") ?
            "INSERT INTO players (uuid, hearts) VALUES (?, ?) ON DUPLICATE KEY UPDATE hearts = ?" :
            "INSERT OR REPLACE INTO players (uuid, hearts) VALUES (?, ?)";
            
        executeQuery(sql, stmt -> {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, hearts);
            if (storageType.equals("mysql")) {
                stmt.setInt(3, hearts);
            }
            stmt.executeUpdate();
        });
    }

    // Example of using the new methods for getHearts
    public int getHearts(UUID uuid) {
        String sql = "SELECT hearts FROM players WHERE uuid = ?";
        return executeQuery(sql, rs -> {
            if (rs.next()) {
                return rs.getInt("hearts");
            }
            return plugin.getConfigManager().getStartingHearts();
        });
    }

    private void createTables() {
        try (Connection connection = getConnection()) {
            // Players table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS players (
                        uuid VARCHAR(36) PRIMARY KEY,
                        hearts INT NOT NULL DEFAULT 10
                    )
                """);
            }

            // Queue states table
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS queue_states (
                        uuid VARCHAR(36) PRIMARY KEY,
                        in_queue BOOLEAN NOT NULL DEFAULT 0,
                        confirmed BOOLEAN NOT NULL DEFAULT 0,
                        frozen BOOLEAN NOT NULL DEFAULT 0,
                        FOREIGN KEY (uuid) REFERENCES players(uuid)
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

    public void addAlly(UUID player, UUID ally) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
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
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
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
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "SELECT ally_uuid FROM allies WHERE player_uuid = ?")) {
            stmt.setString(1, player.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    allies.add(UUID.fromString(rs.getString("ally_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get allies for " + player, e);
        }
        return allies;
    }

    public void addAllyRequest(UUID sender, UUID receiver) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
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
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
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
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "SELECT sender_uuid FROM ally_requests WHERE receiver_uuid = ?")) {
            stmt.setString(1, receiver.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(UUID.fromString(rs.getString("sender_uuid")));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get pending ally requests for " + receiver, e);
        }
        return requests;
    }

    public void cleanupTimedOutRequests(long timeout) {
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "DELETE FROM ally_requests WHERE timestamp < ?")) {
            stmt.setLong(1, System.currentTimeMillis() - timeout);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to cleanup timed out requests", e);
        }
    }

    public void saveWorldBorderData(double currentSize, long lastShrinkTime, long nextShrinkTime) {
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        String sql;
        if (storageType.equals("mysql")) {
            sql = "INSERT INTO world_border (id, current_size, last_shrink_time, next_shrink_time) " +
                  "VALUES (1, ?, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE current_size = ?, last_shrink_time = ?, next_shrink_time = ?";
        } else {
            sql = "INSERT OR REPLACE INTO world_border (id, current_size, last_shrink_time, next_shrink_time) VALUES (1, ?, ?, ?)";
        }
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, currentSize);
            stmt.setLong(2, lastShrinkTime);
            stmt.setLong(3, nextShrinkTime);
            if (storageType.equals("mysql")) {
                stmt.setDouble(4, currentSize);
                stmt.setLong(5, lastShrinkTime);
                stmt.setLong(6, nextShrinkTime);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save world border data", e);
        }
    }

    public Map<String, Object> getWorldBorderData() {
        Map<String, Object> data = new HashMap<>();
        try (Connection connection = getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                "SELECT current_size, last_shrink_time, next_shrink_time FROM world_border WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                data.put("current_size", rs.getDouble("current_size"));
                data.put("last_shrink_time", rs.getLong("last_shrink_time"));
                data.put("next_shrink_time", rs.getLong("next_shrink_time"));
            } else {
                // Initialize with default values
                double initialSize = plugin.getConfigManager().getInitialBorderSize();
                data.put("current_size", initialSize);
                data.put("last_shrink_time", 0L);
                data.put("next_shrink_time", 0L);
                saveWorldBorderData(initialSize, 0L, 0L);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to get world border data", e);
            // Return default values on error
            data.put("current_size", plugin.getConfigManager().getInitialBorderSize());
            data.put("last_shrink_time", 0L);
            data.put("next_shrink_time", 0L);
        }
        return data;
    }

    /**
     * Ensure a player exists in the players table
     * @param uuid The player's UUID
     */
    private void ensurePlayerExists(UUID uuid) {
        try {
            // Check if player exists
            String checkSql = "SELECT uuid FROM players WHERE uuid = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (!rs.next()) {
                    // Player doesn't exist, insert with default hearts
                    String insertSql = "INSERT INTO players (uuid, hearts) VALUES (?, ?)";
                    try (Connection insertConnection = getConnection();
                         PreparedStatement insertStmt = insertConnection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, uuid.toString());
                        insertStmt.setInt(2, plugin.getConfigManager().getStartingHearts());
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error ensuring player exists: " + uuid, e);
        }
    }
    
    /**
     * Set a player's queue state
     * @param uuid The player's UUID
     * @param inQueue Whether the player is in the queue
     * @param confirmed Whether the player has confirmed
     * @param frozen Whether the player is frozen
     */
    public void setQueueState(UUID uuid, boolean inQueue, boolean confirmed, boolean frozen) {
        try {
            // Make sure player exists in players table
            ensurePlayerExists(uuid);
            
            // Check if player already has a queue state
            String checkSql = "SELECT uuid FROM queue_states WHERE uuid = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(checkSql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    // Update existing queue state
                    String updateSql = "UPDATE queue_states SET in_queue = ?, confirmed = ?, frozen = ? WHERE uuid = ?";
                    try (Connection updateConnection = getConnection();
                         PreparedStatement updateStmt = updateConnection.prepareStatement(updateSql)) {
                        updateStmt.setBoolean(1, inQueue);
                        updateStmt.setBoolean(2, confirmed);
                        updateStmt.setBoolean(3, frozen);
                        updateStmt.setString(4, uuid.toString());
                        updateStmt.executeUpdate();
                    }
                } else {
                    // Insert new queue state
                    String insertSql = "INSERT INTO queue_states (uuid, in_queue, confirmed, frozen) VALUES (?, ?, ?, ?)";
                    try (Connection insertConnection = getConnection();
                         PreparedStatement insertStmt = insertConnection.prepareStatement(insertSql)) {
                        insertStmt.setString(1, uuid.toString());
                        insertStmt.setBoolean(2, inQueue);
                        insertStmt.setBoolean(3, confirmed);
                        insertStmt.setBoolean(4, frozen);
                        insertStmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error setting queue state for player " + uuid, e);
        }
    }
    
    /**
     * Get a player's queue state
     * @param uuid The player's UUID
     * @return A map containing the player's queue state, or null if not found
     */
    public Map<String, Boolean> getQueueState(UUID uuid) {
        try {
            String sql = "SELECT in_queue, confirmed, frozen FROM queue_states WHERE uuid = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    Map<String, Boolean> state = new HashMap<>();
                    state.put("in_queue", rs.getBoolean("in_queue"));
                    state.put("confirmed", rs.getBoolean("confirmed"));
                    state.put("frozen", rs.getBoolean("frozen"));
                    return state;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting queue state for player " + uuid, e);
        }
        return null;
    }
    
    /**
     * Get all players in the queue
     * @return A map of player UUIDs to their queue states
     */
    public Map<UUID, Map<String, Boolean>> getAllQueueStates() {
        Map<UUID, Map<String, Boolean>> states = new HashMap<>();
        try {
            String sql = "SELECT uuid, in_queue, confirmed, frozen FROM queue_states WHERE in_queue = 1";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    Map<String, Boolean> state = new HashMap<>();
                    state.put("in_queue", rs.getBoolean("in_queue"));
                    state.put("confirmed", rs.getBoolean("confirmed"));
                    state.put("frozen", rs.getBoolean("frozen"));
                    states.put(uuid, state);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting all queue states", e);
        }
        return states;
    }
    
    /**
     * Remove a player's queue state
     * @param uuid The player's UUID
     */
    public void removeQueueState(UUID uuid) {
        try {
            String sql = "DELETE FROM queue_states WHERE uuid = ?";
            try (Connection connection = getConnection();
                 PreparedStatement stmt = connection.prepareStatement(sql)) {
                stmt.setString(1, uuid.toString());
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error removing queue state for player " + uuid, e);
        }
    }

    public void saveBountyData(Map<String, Object> data) {
        try {
            File dataFile = new File(plugin.getDataFolder(), "bounty_data.yml");
            if (!dataFile.exists()) {
                dataFile.createNewFile();
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                config.set(entry.getKey(), entry.getValue());
            }
            config.save(dataFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Error saving bounty data: " + e.getMessage());
        }
    }

    public Map<String, Object> getBountyData() {
        try {
            File dataFile = new File(plugin.getDataFolder(), "bounty_data.yml");
            if (!dataFile.exists()) {
                return new HashMap<>();
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            Map<String, Object> data = new HashMap<>();
            
            if (config.contains("last_bounty_time")) {
                data.put("last_bounty_time", config.get("last_bounty_time"));
            }
            if (config.contains("bounty_kills")) {
                data.put("bounty_kills", config.get("bounty_kills"));
            }
            
            return data;
        } catch (Exception e) {
            plugin.getLogger().severe("Error loading bounty data: " + e.getMessage());
            return new HashMap<>();
        }
    }
}