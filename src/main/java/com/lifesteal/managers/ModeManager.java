package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;

public class ModeManager {
    private final LifeSteal plugin;
    private boolean isPvPMode = true;
    private BukkitTask rotationTask;
    private BukkitTask actionBarTask;
    private long nextSwitch;
    private BossBar modeBar;
    private File timerFile;
    private FileConfiguration timerConfig;

    public ModeManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.modeBar = Bukkit.createBossBar(
            ColorUtils.colorize("&cMode: PvP"),
            BarColor.RED,
            BarStyle.SOLID
        );
        setupTimerConfig();
        loadTimerData();
    }

    private void setupTimerConfig() {
        timerFile = new File(plugin.getDataFolder(), "cycle-timer.yml");
        if (!timerFile.exists()) {
            plugin.saveResource("cycle-timer.yml", false);
        }
        timerConfig = YamlConfiguration.loadConfiguration(timerFile);
    }

    private void loadTimerData() {
        isPvPMode = timerConfig.getString("current-mode", "PVP").equals("PVP");
        nextSwitch = timerConfig.getLong("next-switch", 0);
        
        // If the saved time is in the past or 0, set up a new cycle
        if (nextSwitch <= System.currentTimeMillis()) {
            nextSwitch = System.currentTimeMillis() + (getPvPDuration() * 3600000L);
            isPvPMode = true;
        }
    }

    private void saveTimerData() {
        timerConfig.set("current-mode", isPvPMode ? "PVP" : "PVE");
        timerConfig.set("next-switch", nextSwitch);
        try {
            timerConfig.save(timerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save cycle timer data!");
            e.printStackTrace();
        }
    }

    public void startRotation() {
        if (rotationTask != null) {
            stopRotation();
        }

        // Update the boss bar to match current mode
        modeBar.setTitle(ColorUtils.colorize(
            isPvPMode ? "&cMode: PvP" : "&aMode: PvE"));
        modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);

        rotationTask = new BukkitRunnable() {
            @Override
            public void run() {
                switchMode();
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L); // Check every minute

        // Show the boss bar to all players
        Bukkit.getOnlinePlayers().forEach(modeBar::addPlayer);
        
        startActionBar();
    }

    public void stopRotation() {
        if (rotationTask != null) {
            rotationTask.cancel();
            rotationTask = null;
        }
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        modeBar.removeAll();
        
        // Save timer data when stopping
        saveTimerData();
    }

    private void switchMode() {
        if (System.currentTimeMillis() >= nextSwitch) {
            isPvPMode = !isPvPMode;
            nextSwitch = System.currentTimeMillis() + 
                (isPvPMode ? getPvPDuration() : getPvEDuration()) * 3600000L;

            // Update boss bar
            modeBar.setTitle(ColorUtils.colorize(
                isPvPMode ? "&cMode: PvP" : "&aMode: PvE"));
            modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);

            // Execute switch commands
            for (String command : plugin.getConfigManager().getConfig()
                .getStringList("pvp-cycle.on-switch")) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    ColorUtils.colorize(command
                        .replace("%mode%", isPvPMode ? "PVP" : "PVE"))
                );
            }
            
            // Save timer data after mode switch
            saveTimerData();
        }
    }

    private void startActionBar() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }

        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                long timeLeft = (nextSwitch - System.currentTimeMillis()) / 1000;
                String format = plugin.getConfigManager().getConfig().getString(
                    isPvPMode ? "action-bar.format-pvp" : "action-bar.format-pve"
                );

                if (format != null) {
                    String message = ColorUtils.colorize(format
                        .replace("%time%", formatTime(timeLeft)));
                    
                    Bukkit.getOnlinePlayers().forEach(player -> {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                            new TextComponent(message));
                    });
                }
                
                // Save timer data every 5 minutes
                if (timeLeft % 300 == 0) {
                    saveTimerData();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    private int getPvPDuration() {
        return plugin.getConfigManager().getPvPDuration();
    }

    private int getPvEDuration() {
        return plugin.getConfigManager().getPvEDuration();
    }

    public boolean isPvPMode() {
        return isPvPMode;
    }
    
    public boolean toggleBossBar() {
        if (modeBar.getPlayers().isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(modeBar::addPlayer);
            return true;
        } else {
            modeBar.removeAll();
            return false;
        }
    }
}