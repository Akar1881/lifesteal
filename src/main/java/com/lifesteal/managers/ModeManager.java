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
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class ModeManager {
    private final LifeSteal plugin;
    private boolean isPvPMode = true;
    private BukkitTask rotationTask;
    private BukkitTask actionBarTask;
    private BukkitTask messageRotationTask;
    private long nextSwitch;
    private BossBar modeBar;
    private CycleTimerDatabase cycleTimerDatabase;
    private int currentMessageIndex = 0;
    private List<String> messages;
    private List<Long> durations;
    private static final int MAX_MESSAGES = 10;
    private static final long DEFAULT_DURATION = 30000L; // 30 seconds
    private static final long ROTATION_CHECK_INTERVAL = 20L * 60L; // 1 minute

    public ModeManager(LifeSteal plugin) {
        this.plugin = plugin;
        initializeBossBar();
        this.cycleTimerDatabase = new CycleTimerDatabase(plugin);
        loadTimerData();
        loadMessages();
    }

    private void initializeBossBar() {
        try {
            this.modeBar = Bukkit.createBossBar(
                ColorUtils.colorize("&aLifeSteal"),
                BarColor.GREEN,
                BarStyle.SOLID
            );
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize boss bar", e);
        }
    }

    public BossBar getBossBar() {
        return modeBar;
    }

    public void loadMessages() {
        try {
            messages = new ArrayList<>();
            durations = new ArrayList<>();

            ConfigurationSection bossBarSection = plugin.getConfigManager().getConfig().getConfigurationSection("boss-bar");
            if (bossBarSection != null && bossBarSection.getBoolean("enabled", true)) {
                List<?> messagesList = bossBarSection.getList("messages");
                if (messagesList != null) {
                    for (Object obj : messagesList) {
                        if (obj instanceof ConfigurationSection) {
                            ConfigurationSection section = (ConfigurationSection) obj;
                            addMessage(section.getString("text"), section.getString("duration"));
                        } else if (obj instanceof java.util.LinkedHashMap) {
                            @SuppressWarnings("unchecked")
                            java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) obj;
                            addMessage((String) map.get("text"), (String) map.get("duration"));
                        }
                    }
                }
            }
            
            // Limit messages and add default if empty
            limitMessages();
            addDefaultMessage();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading boss bar messages", e);
            addDefaultMessage();
        }
    }

    private void addMessage(String text, String duration) {
        if (text != null && !text.isEmpty()) {
            messages.add(text);
            durations.add(parseDuration(duration));
        }
    }

    private void limitMessages() {
        if (messages.size() > MAX_MESSAGES) {
            messages = messages.subList(0, MAX_MESSAGES);
            durations = durations.subList(0, MAX_MESSAGES);
        }
    }

    private void addDefaultMessage() {
        if (messages.isEmpty()) {
            messages.add(isPvPMode ? "&cMode: PvP" : "&aMode: PvE");
            durations.add(DEFAULT_DURATION);
        }
    }

    private long parseDuration(String duration) {
        if (duration == null) return DEFAULT_DURATION;
        
        try {
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
                return DEFAULT_DURATION;
            }

            return Long.parseLong(duration) * multiplier;
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Invalid duration format: " + duration);
            return DEFAULT_DURATION;
        }
    }

    private void loadTimerData() {
        try {
            java.util.Map<String, Object> data = cycleTimerDatabase.getCycleTimerData();
            if (data.containsKey("current_mode") && data.containsKey("next_switch")) {
                isPvPMode = "PVP".equals(data.get("current_mode"));
                nextSwitch = (long) data.get("next_switch");
            } else {
                initializeFirstRun();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error loading timer data", e);
            initializeFirstRun();
        }
    }

    private void initializeFirstRun() {
        isPvPMode = false;
        nextSwitch = System.currentTimeMillis() + (getPvEDuration() * 3600000L);
        saveTimerData();
    }

    private void saveTimerData() {
        try {
            cycleTimerDatabase.saveCycleTimerData(isPvPMode ? "PVP" : "PVE", nextSwitch);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error saving timer data", e);
        }
    }

    public void startRotation() {
        try {
            if (rotationTask != null) {
                stopRotation();
            }

            updateBossBarColor();
            announceMode();
            startRotationTask();
            startActionBar();
            startMessageRotation();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error starting rotation", e);
        }
    }

    private void updateBossBarColor() {
        if (modeBar != null) {
            modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);
        }
    }

    private void announceMode() {
        String initialMode = isPvPMode ? "PVP" : "PVE";
        for (String command : plugin.getConfigManager().getConfig().getStringList("pvp-cycle.on-switch")) {
            try {
                executeModeCommand(command, initialMode);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error executing mode command: " + command, e);
            }
        }
    }

    private void executeModeCommand(String command, String mode) {
        String processed = ColorUtils.colorize(command.replace("%mode%", mode));
        if (processed.toLowerCase().startsWith("broadcast ")) {
            Bukkit.broadcastMessage(processed.substring("broadcast ".length()));
        } else if (processed.toLowerCase().startsWith("playsound ")) {
            handlePlaySoundCommand(processed);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
        }
    }

    private void startRotationTask() {
        rotationTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    switchMode();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error in rotation task", e);
                }
            }
        }.runTaskTimer(plugin, 0L, ROTATION_CHECK_INTERVAL);

        if (modeBar != null) {
            Bukkit.getOnlinePlayers().forEach(modeBar::addPlayer);
        }
    }

    private void startMessageRotation() {
        if (messageRotationTask != null) {
            messageRotationTask.cancel();
        }

        if (!messages.isEmpty()) {
            messageRotationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        rotateMessage();
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Error in message rotation", e);
                    }
                }
            }.runTaskTimer(plugin, 0L, 20L);
        }
    }

    private void rotateMessage() {
        if (messages.isEmpty() || modeBar == null) return;
        
        String message = messages.get(currentMessageIndex);
        message = message.replace("%time%", formatTime((nextSwitch - System.currentTimeMillis()) / 1000));
        message = message.replace("%mode%", isPvPMode ? "PVP" : "PVE");
        modeBar.setTitle(ColorUtils.colorize(message));
        
        modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);
        
        long currentDuration = durations.get(currentMessageIndex);
        currentMessageIndex = (currentMessageIndex + 1) % messages.size();
        
        messageRotationTask.cancel();
        messageRotationTask = new BukkitRunnable() {
            @Override
            public void run() {
                rotateMessage();
            }
        }.runTaskLater(plugin, currentDuration / 50L);
    }

    public void stopRotation() {
        try {
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
            if (modeBar != null) {
                modeBar.removeAll();
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error stopping rotation", e);
        }
    }

    private void switchMode() {
        try {
            if (System.currentTimeMillis() >= nextSwitch) {
                isPvPMode = !isPvPMode;
                nextSwitch = System.currentTimeMillis() + 
                    ((isPvPMode ? getPvPDuration() : getPvEDuration()) * 3600000L);
                saveTimerData();
                
                updateBossBarColor();
                announceMode();
                
                // Update all online players' boss bars
                if (modeBar != null) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        modeBar.addPlayer(player);
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error switching mode", e);
        }
    }

    private void startActionBar() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
        }

        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    updateActionBar();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Error updating action bar", e);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void updateActionBar() {
        String mode = isPvPMode ? "PVP" : "PVE";
        String timeLeft = formatTime((nextSwitch - System.currentTimeMillis()) / 1000);
        String message = ColorUtils.colorize("&eMode: &f" + mode + " &e| &fTime Left: &e" + timeLeft);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    private int getPvPDuration() {
        return plugin.getConfigManager().getConfig().getInt("pvp-cycle.pvp-duration", 2);
    }

    private int getPvEDuration() {
        return plugin.getConfigManager().getConfig().getInt("pvp-cycle.pve-duration", 2);
    }

    public boolean isPvPMode() {
        return isPvPMode;
    }

    public void setMode(boolean pvp) {
        isPvPMode = pvp;
        nextSwitch = System.currentTimeMillis() + 
            ((isPvPMode ? getPvPDuration() : getPvEDuration()) * 3600000L);
        saveTimerData();
        updateBossBarColor();
    }

    public void addTime(long millis) {
        nextSwitch += millis;
        saveTimerData();
    }

    public void subtractTime(long millis) {
        nextSwitch = Math.max(System.currentTimeMillis(), nextSwitch - millis);
        saveTimerData();
    }

    public long getTimeLeftMillis() {
        return Math.max(0, nextSwitch - System.currentTimeMillis());
    }

    public boolean toggleBossBar() {
        if (modeBar != null) {
            boolean visible = !modeBar.isVisible();
            modeBar.setVisible(visible);
            return visible;
        }
        return false;
    }

    private void handlePlaySoundCommand(String command) {
        try {
            String[] parts = command.split(" ");
            if (parts.length >= 2) {
                String soundName = parts[1].toUpperCase();
                Sound sound = Sound.valueOf(soundName);
                float volume = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
                float pitch = parts.length > 3 ? Float.parseFloat(parts[3]) : 1.0f;
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error playing sound: " + command, e);
        }
    }
}