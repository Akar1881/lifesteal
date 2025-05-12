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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModeManager {
    private final LifeSteal plugin;
    private boolean isPvPMode = true;
    private BukkitTask rotationTask;
    private BukkitTask actionBarTask;
    private BukkitTask messageRotationTask;
    private long nextSwitch;
    private BossBar modeBar;
    private File timerFile;
    private FileConfiguration timerConfig;
    private int currentMessageIndex = 0;
    private List<String> messages;
    private List<Long> durations;

    public ModeManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.modeBar = Bukkit.createBossBar(
            ColorUtils.colorize("&aMode: PvE"),
            BarColor.GREEN,
            BarStyle.SOLID
        );
        setupTimerConfig();
        loadTimerData();
        loadMessages();
    }

    private void setupTimerConfig() {
        timerFile = new File(plugin.getDataFolder(), "cycle-timer.yml");
        if (!timerFile.exists()) {
            plugin.saveResource("cycle-timer.yml", false);
        }
        timerConfig = YamlConfiguration.loadConfiguration(timerFile);
    }

    private void loadMessages() {
        messages = new ArrayList<>();
        durations = new ArrayList<>();

        ConfigurationSection bossBarSection = plugin.getConfigManager().getConfig().getConfigurationSection("boss-bar");
        if (bossBarSection != null && bossBarSection.getBoolean("enabled", true)) {
            List<?> messagesList = bossBarSection.getList("messages");
            if (messagesList != null) {
                for (Object obj : messagesList) {
                    if (obj instanceof ConfigurationSection) {
                        ConfigurationSection section = (ConfigurationSection) obj;
                        messages.add(section.getString("text"));
                        durations.add(parseDuration(section.getString("duration")));
                    }
                }
            }
        }
    }

    private long parseDuration(String duration) {
        if (duration == null) return 30000L; // Default 30 seconds
        
        long multiplier;
        if (duration.endsWith("h")) {
            multiplier = 3600000L;
            duration = duration.substring(0, duration.length() - 1);
        } else if (duration.endsWith("m")) {
            multiplier = 60000L;
            duration = duration.substring(0, duration.length() - 1);
        } else if (duration.endsWith("s")) {
            multiplier = 1000L;
            duration = duration.substring(0, duration.length() - 1);
        } else {
            return 30000L; // Default if invalid format
        }

        try {
            return Long.parseLong(duration) * multiplier;
        } catch (NumberFormatException e) {
            return 30000L;
        }
    }

    private void loadTimerData() {
        // Check if this is a fresh install or first run
        boolean isFirstRun = !timerFile.exists() || timerConfig.getLong("next-switch", 0) == 0;
        
        // Load current mode from config
        isPvPMode = timerConfig.getString("current-mode", "PVE").equals("PVP");
        nextSwitch = timerConfig.getLong("next-switch", 0);
        
        // If this is the first run or timer expired, start with PvE mode
        if (isFirstRun || nextSwitch <= System.currentTimeMillis()) {
            isPvPMode = false; // Start with PvE mode
            nextSwitch = System.currentTimeMillis() + (getPvEDuration() * 3600000L);
            saveTimerData(); // Save the initial state
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

        // Update the boss bar to match the current mode
        modeBar.setTitle(ColorUtils.colorize(
            isPvPMode ? "&cMode: PvP" : "&aMode: PvE"));
        modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);

        // Announce the initial mode
        String initialMode = isPvPMode ? "PVP" : "PVE";
        for (String command : plugin.getConfigManager().getConfig()
            .getStringList("pvp-cycle.on-switch")) {
            Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(),
                ColorUtils.colorize(command.replace("%mode%", initialMode))
            );
        }

        // Start the rotation task
        rotationTask = new BukkitRunnable() {
            @Override
            public void run() {
                switchMode();
            }
        }.runTaskTimer(plugin, 0L, 20L * 60L);

        Bukkit.getOnlinePlayers().forEach(modeBar::addPlayer);
        
        startActionBar();
        startMessageRotation();
    }

    private void startMessageRotation() {
        if (messageRotationTask != null) {
            messageRotationTask.cancel();
        }

        if (!messages.isEmpty()) {
            messageRotationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (messages.isEmpty()) return;
                    
                    String message = messages.get(currentMessageIndex);
                    message = message.replace("%time%", formatTime((nextSwitch - System.currentTimeMillis()) / 1000));
                    modeBar.setTitle(ColorUtils.colorize(message));
                    
                    currentMessageIndex = (currentMessageIndex + 1) % messages.size();
                    
                    // Schedule next message
                    long nextDuration = durations.get(currentMessageIndex);
                    messageRotationTask.cancel();
                    messageRotationTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            startMessageRotation();
                        }
                    }.runTaskLater(plugin, nextDuration / 50); // Convert to ticks
                }
            }.runTask(plugin);
        }
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
        if (messageRotationTask != null) {
            messageRotationTask.cancel();
            messageRotationTask = null;
        }
        modeBar.removeAll();
        
        saveTimerData();
    }

    private void switchMode() {
        if (System.currentTimeMillis() >= nextSwitch) {
            isPvPMode = !isPvPMode;
            nextSwitch = System.currentTimeMillis() + 
                (isPvPMode ? getPvPDuration() : getPvEDuration()) * 3600000L;

            modeBar.setTitle(ColorUtils.colorize(
                isPvPMode ? "&cMode: PvP" : "&aMode: PvE"));
            modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);

            for (String command : plugin.getConfigManager().getConfig()
                .getStringList("pvp-cycle.on-switch")) {
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    ColorUtils.colorize(command
                        .replace("%mode%", isPvPMode ? "PVP" : "PVE"))
                );
            }
            
            // Handle bounty system
            if (isPvPMode) {
                // Start bounty system when switching to PvP mode
                if (plugin.getBountyManager() != null) {
                    plugin.getBountyManager().startBountySystem();
                }
            } else {
                // Stop bounty system when switching to PvE mode
                if (plugin.getBountyManager() != null) {
                    plugin.getBountyManager().stopBountySystem();
                }
            }
            
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