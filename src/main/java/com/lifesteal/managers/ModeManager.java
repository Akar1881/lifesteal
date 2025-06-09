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
    public org.bukkit.boss.BossBar getBossBar() {
        return modeBar;
    }
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

    public ModeManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.modeBar = Bukkit.createBossBar(
            ColorUtils.colorize("&aLifeSteal"), // Default text, will be updated by message rotation
            BarColor.GREEN,
            BarStyle.SOLID
        );
        this.cycleTimerDatabase = new CycleTimerDatabase(plugin);
        loadTimerData();
        loadMessages();
    }

    public void loadMessages() {
        messages = new ArrayList<>();
        durations = new ArrayList<>();

        ConfigurationSection bossBarSection = plugin.getConfigManager().getConfig().getConfigurationSection("boss-bar");
        if (bossBarSection != null && 
            (bossBarSection.contains("enabled") ? bossBarSection.getBoolean("enabled") : true)) {
            List<?> messagesList = bossBarSection.getList("messages");
            if (messagesList != null) {
                for (Object obj : messagesList) {
                    if (obj instanceof ConfigurationSection) {
                        ConfigurationSection section = (ConfigurationSection) obj;
                        messages.add(section.getString("text"));
                        durations.add(parseDuration(section.getString("duration")));
                    } else if (obj instanceof java.util.LinkedHashMap) {
                        // Handle YAML map format
                        @SuppressWarnings("unchecked")
                        java.util.LinkedHashMap<String, Object> map = (java.util.LinkedHashMap<String, Object>) obj;
                        messages.add((String) map.get("text"));
                        durations.add(parseDuration((String) map.get("duration")));
                    }
                }
            }
        }
        
        // Limit to 10 messages maximum
        if (messages.size() > 10) {
            messages = messages.subList(0, 10);
            durations = durations.subList(0, 10);
        }
        
        // If no messages were loaded, add a default one
        if (messages.isEmpty()) {
            messages.add(isPvPMode ? "&cMode: PvP" : "&aMode: PvE");
            durations.add(30000L); // 30 seconds
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
        // Try to load from DB
        java.util.Map<String, Object> data = cycleTimerDatabase.getCycleTimerData();
        if (data.containsKey("current_mode") && data.containsKey("next_switch")) {
            isPvPMode = "PVP".equals(data.get("current_mode"));
            nextSwitch = (long) data.get("next_switch");
        } else {
            // First run: start with PvE mode
            isPvPMode = false;
            nextSwitch = System.currentTimeMillis() + (getPvEDuration() * 3600000L);
            saveTimerData();
        }
    }

    private void saveTimerData() {
        cycleTimerDatabase.saveCycleTimerData(isPvPMode ? "PVP" : "PVE", nextSwitch);
    }

    public void startRotation() {
        if (rotationTask != null) {
            stopRotation();
        }

        // Update the boss bar color to match the current mode
        // The title will be set by the message rotation
        modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);

        // Announce the initial mode
        String initialMode = isPvPMode ? "PVP" : "PVE";
        for (String command : plugin.getConfigManager().getConfig()
            .getStringList("pvp-cycle.on-switch")) {
            String processed = ColorUtils.colorize(command.replace("%mode%", initialMode));
            if (processed.toLowerCase().startsWith("broadcast ")) {
                // Use Bukkit API for broadcasting
                Bukkit.broadcastMessage(processed.substring("broadcast ".length()));
            } else if (processed.toLowerCase().startsWith("playsound ")) {
                // Handle playsound command directly
                handlePlaySoundCommand(processed);
            } else {
                // Run as a normal command
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
            }
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
                    message = message.replace("%mode%", isPvPMode ? "PVP" : "PVE");
                    modeBar.setTitle(ColorUtils.colorize(message));
                    
                    // Always maintain the correct color based on the current mode
                    modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);
                    
                    // Get the current duration before changing the index
                    long currentDuration = durations.get(currentMessageIndex);
                    
                    // Update index for next message
                    currentMessageIndex = (currentMessageIndex + 1) % messages.size();
                    
                    // Cancel current task and schedule the next rotation
                    messageRotationTask.cancel();
                    messageRotationTask = new BukkitRunnable() {
                        @Override
                        public void run() {
                            startMessageRotation();
                        }
                    }.runTaskLater(plugin, currentDuration / 50); // Convert to ticks
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

            // Update the boss bar color based on the current mode
            // The title will be updated by the message rotation
            modeBar.setColor(isPvPMode ? BarColor.RED : BarColor.GREEN);

            // Play beacon activation sound to all players
            Bukkit.getOnlinePlayers().forEach(player -> {
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_BEACON_ACTIVATE, 1.0f, 1.0f);
            });

            for (String command : plugin.getConfigManager().getConfig()
                .getStringList("pvp-cycle.on-switch")) {
                String processed = ColorUtils.colorize(command.replace("%mode%", isPvPMode ? "PVP" : "PVE"));
                if (processed.toLowerCase().startsWith("broadcast ")) {
                    // Use Bukkit API for broadcasting
                    Bukkit.broadcastMessage(processed.substring("broadcast ".length()));
                } else if (processed.toLowerCase().startsWith("playsound ")) {
                    // Handle playsound command directly
                    handlePlaySoundCommand(processed);
                } else {
                    // Run as a normal command
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processed);
                }
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

    // Instantly set mode and reset timer
    public void setMode(boolean pvp) {
        this.isPvPMode = pvp;
        this.nextSwitch = System.currentTimeMillis() + (pvp ? getPvPDuration() : getPvEDuration()) * 3600000L;
        // Only update the color, the title will be handled by message rotation
        modeBar.setColor(pvp ? BarColor.RED : BarColor.GREEN);
        saveTimerData();
    }

    // Add time (in millis) to current mode
    public void addTime(long millis) {
        this.nextSwitch += millis;
        saveTimerData();
    }

    // Subtract time (in millis) from current mode
    public void subtractTime(long millis) {
        this.nextSwitch -= millis;
        if (this.nextSwitch < System.currentTimeMillis()) {
            this.nextSwitch = System.currentTimeMillis();
        }
        saveTimerData();
    }

    // Get time left in millis
    public long getTimeLeftMillis() {
        return nextSwitch - System.currentTimeMillis();
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
    
    /**
     * Handles playsound commands directly using the Bukkit API
     * Format: playsound <sound> [source] [player] [x] [y] [z] [volume] [pitch]
     */
    private void handlePlaySoundCommand(String command) {
        try {
            // Remove "playsound " prefix
            String[] parts = command.substring("playsound ".length()).split(" ");
            
            // Get the sound name
            String soundName = parts[0];
            Sound sound;
            try {
                sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            } catch (IllegalArgumentException e) {
                // Try with minecraft namespace
                if (soundName.startsWith("minecraft:")) {
                    soundName = soundName.substring("minecraft:".length());
                }
                sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            }
            
            // Default values
            String target = "@a";
            float volume = 1.0f;
            float pitch = 1.0f;
            
            // Parse additional parameters if provided
            if (parts.length > 1) {
                // Skip source (category) parameter if present
                int startIndex = 1;
                if (!parts[1].startsWith("@")) {
                    startIndex = 2; // Skip source parameter
                }
                
                if (parts.length > startIndex) {
                    target = parts[startIndex];
                }
                
                // If volume is specified
                if (parts.length > startIndex + 4) {
                    try {
                        volume = Float.parseFloat(parts[startIndex + 4]);
                    } catch (NumberFormatException ignored) {}
                }
                
                // If pitch is specified
                if (parts.length > startIndex + 5) {
                    try {
                        pitch = Float.parseFloat(parts[startIndex + 5]);
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // Play the sound to all players or specific targets
            if (target.equals("@a")) {
                for (org.bukkit.entity.Player player : Bukkit.getOnlinePlayers()) {
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            } else if (target.startsWith("@p")) {
                // Just play to the first player
                if (!Bukkit.getOnlinePlayers().isEmpty()) {
                    org.bukkit.entity.Player player = Bukkit.getOnlinePlayers().iterator().next();
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            } else {
                // Try to find the player by name
                org.bukkit.entity.Player player = Bukkit.getPlayer(target);
                if (player != null) {
                    player.playSound(player.getLocation(), sound, volume, pitch);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to execute playsound command: " + command);
            plugin.getLogger().warning("Error: " + e.getMessage());
        }
    }
}