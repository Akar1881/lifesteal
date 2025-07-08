package com.lifesteal.commands;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ShrinkCommand implements CommandExecutor {
    private final LifeSteal plugin;

    public ShrinkCommand(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        long millis = plugin.getWorldBorderManager().getTimeUntilNextShrink();
        long seconds = millis / 1000;
        String formatted = plugin.getWorldBorderManager().formatTime((int)seconds);
        sender.sendMessage(ColorUtils.colorize("&eTime until next world border shrink: &b" + formatted));
        return true;
    }
} 