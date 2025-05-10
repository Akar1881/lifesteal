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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class ModeManager {
    private final LifeSteal plugin;
    private boolean isPvPMode = true;
    private BukkitTask rotationTask;
    private BukkitTask actionBarTask;
    private long nextSwitch;
    private BossBar modeBar;

    public ModeManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.modeBar = Bukkit.createBossBar(
            ColorUtils.colorize("&cMode: PvP"),
            BarColor.RED,
            BarStyle.SOLID
        );
    }

    public void startRotation() {
        if (rotationTask != null) {
            stopRotation();
        }

        nextSwitch = System.currentTimeMillis() + (getPvPDuration() * 3600000L);

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
    
    /**
     * Toggles the visibility of the boss bar
     * @return true if the boss bar is now visible, false if it's hidden
     */
    public boolean toggleBossBar() {
        if (modeBar.getPlayers().isEmpty()) {
            // Boss bar is currently hidden, show it to all players
            Bukkit.getOnlinePlayers().forEach(modeBar::addPlayer);
            return true;
        } else {
            // Boss bar is currently visible, hide it
            modeBar.removeAll();
            return false;
        }
    }
}