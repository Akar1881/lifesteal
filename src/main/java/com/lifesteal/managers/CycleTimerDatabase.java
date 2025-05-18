package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CycleTimerDatabase {
    private final LifeSteal plugin;
    private final DatabaseManager db;

    public CycleTimerDatabase(LifeSteal plugin) {
        this.plugin = plugin;
        this.db = plugin.getDatabaseManager();
    }

    public void createTable() {
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS cycle_timer (
                    id INT PRIMARY KEY DEFAULT 1,
                    current_mode VARCHAR(8) NOT NULL,
                    next_switch BIGINT NOT NULL,
                    CHECK (id = 1)
                )
            """);
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to create cycle_timer table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveCycleTimerData(String mode, long nextSwitch) {
        String storageType = plugin.getConfig().getString("storage.type", "sqlite").toLowerCase();
        String sql;
        if (storageType.equals("mysql")) {
            sql = "INSERT INTO cycle_timer (id, current_mode, next_switch) VALUES (1, ?, ?) " +
                  "ON DUPLICATE KEY UPDATE current_mode = ?, next_switch = ?";
        } else {
            sql = "INSERT OR REPLACE INTO cycle_timer (id, current_mode, next_switch) VALUES (1, ?, ?)";
        }
        try (PreparedStatement stmt = db.getConnection().prepareStatement(sql)) {
            stmt.setString(1, mode);
            stmt.setLong(2, nextSwitch);
            if (storageType.equals("mysql")) {
                stmt.setString(3, mode);
                stmt.setLong(4, nextSwitch);
            }
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save cycle timer data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Object> getCycleTimerData() {
        Map<String, Object> data = new HashMap<>();
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT current_mode, next_switch FROM cycle_timer WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                data.put("current_mode", rs.getString("current_mode"));
                data.put("next_switch", rs.getLong("next_switch"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to get cycle timer data: " + e.getMessage());
        }
        return data;
    }

    public boolean hasCycleTimerData() {
        try (PreparedStatement stmt = db.getConnection().prepareStatement(
                "SELECT 1 FROM cycle_timer WHERE id = 1")) {
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to check cycle timer data: " + e.getMessage());
        }
        return false;
    }
}
