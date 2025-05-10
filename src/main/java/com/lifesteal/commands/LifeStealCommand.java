package com.lifesteal.commands;

import com.lifesteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class LifeStealCommand implements CommandExecutor {
    private final LifeSteal plugin;

    public LifeStealCommand(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§c/lifesteal <reload|hearts|revive|schedule>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage("§aConfiguration reloaded!");
                return true;

            case "hearts":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (args.length != 4) {
                    sender.sendMessage("§c/lifesteal hearts <set|add|remove> <player> <amount>");
                    return true;
                }
                handleHeartsCommand(sender, args);
                return true;

            case "revive":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage("§c/lifesteal revive <player>");
                    return true;
                }
                handleReviveCommand(sender, args[1]);
                return true;

            case "schedule":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage("§cYou don't have permission to use this command!");
                    return true;
                }
                plugin.getModeManager().startRotation();
                sender.sendMessage("§aMode rotation restarted!");
                return true;

            default:
                sender.sendMessage("§cUnknown command. Use /lifesteal for help.");
                return true;
        }
    }

    private void handleHeartsCommand(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid number!");
            return;
        }

        switch (args[1].toLowerCase()) {
            case "set":
                plugin.getHeartManager().setHearts(target, amount);
                break;
            case "add":
                plugin.getHeartManager().addHearts(target, amount);
                break;
            case "remove":
                plugin.getHeartManager().removeHearts(target, amount);
                break;
            default:
                sender.sendMessage("§cInvalid operation! Use set, add, or remove.");
                return;
        }
        sender.sendMessage("§aUpdated " + target.getName() + "'s hearts!");
    }

    private void handleReviveCommand(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return;
        }

        plugin.getHeartManager().revivePlayer(target);
        sender.sendMessage("§aRevived " + target.getName() + "!");
    }
}