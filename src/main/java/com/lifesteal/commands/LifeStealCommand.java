package com.lifesteal.commands;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class LifeStealCommand implements CommandExecutor, TabCompleter {
    private final LifeSteal plugin;
    private final List<String> mainCommands = Arrays.asList("reload", "hearts", "revive", "schedule", "togglebar");
    private final List<String> heartsOperations = Arrays.asList("set", "add", "remove");

    public LifeStealCommand(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize("&c/lifesteal <reload|hearts|revive|schedule|togglebar>"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                plugin.getConfigManager().reloadConfigs();
                sender.sendMessage(ColorUtils.colorize("&aConfiguration reloaded!"));
                return true;

            case "hearts":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                if (args.length != 4) {
                    sender.sendMessage(ColorUtils.colorize("&c/lifesteal hearts <set|add|remove> <player> <amount>"));
                    return true;
                }
                handleHeartsCommand(sender, args);
                return true;

            case "revive":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                if (args.length != 2) {
                    sender.sendMessage(ColorUtils.colorize("&c/lifesteal revive <player>"));
                    return true;
                }
                handleReviveCommand(sender, args[1]);
                return true;

            case "schedule":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                plugin.getModeManager().startRotation();
                sender.sendMessage(ColorUtils.colorize("&aMode rotation restarted!"));
                return true;
                
            case "togglebar":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                // Toggle the boss bar visibility
                boolean isVisible = plugin.getModeManager().toggleBossBar();
                sender.sendMessage(ColorUtils.colorize(isVisible ? 
                    "&aBoss bar is now visible!" : 
                    "&cBoss bar is now hidden!"));
                return true;

            default:
                sender.sendMessage(ColorUtils.colorize("&cUnknown command. Use /lifesteal for help."));
                return true;
        }
    }

    private void handleHeartsCommand(CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize("&cPlayer not found!"));
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid number!"));
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
                sender.sendMessage(ColorUtils.colorize("&cInvalid operation! Use set, add, or remove."));
                return;
        }
        sender.sendMessage(ColorUtils.colorize("&aUpdated " + target.getName() + "'s hearts!"));
    }

    private void handleReviveCommand(CommandSender sender, String playerName) {
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize("&cPlayer not found!"));
            return;
        }

        plugin.getHeartManager().revivePlayer(target);
        sender.sendMessage(ColorUtils.colorize("&aRevived " + target.getName() + "!"));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("lifesteal.admin")) {
            return Collections.emptyList();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            // First argument - main commands
            String partialCommand = args[0].toLowerCase();
            for (String cmd : mainCommands) {
                if (cmd.startsWith(partialCommand)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2) {
            // Second argument - depends on first argument
            String subCommand = args[0].toLowerCase();
            String partialArg = args[1].toLowerCase();
            
            if (subCommand.equals("hearts")) {
                // For hearts command, suggest operations
                for (String op : heartsOperations) {
                    if (op.startsWith(partialArg)) {
                        completions.add(op);
                    }
                }
            } else if (subCommand.equals("revive")) {
                // For revive command, suggest player names
                return getOnlinePlayerNames(partialArg);
            }
        } else if (args.length == 3 && args[0].toLowerCase().equals("hearts")) {
            // Third argument for hearts command - player name
            return getOnlinePlayerNames(args[2].toLowerCase());
        } else if (args.length == 4 && args[0].toLowerCase().equals("hearts")) {
            // Fourth argument for hearts command - suggest some common values
            String partialAmount = args[3].toLowerCase();
            List<String> amounts = Arrays.asList("1", "5", "10", "20");
            for (String amount : amounts) {
                if (amount.startsWith(partialAmount)) {
                    completions.add(amount);
                }
            }
        }
        
        return completions;
    }
    
    private List<String> getOnlinePlayerNames(String partialName) {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase().startsWith(partialName.toLowerCase()))
                .collect(Collectors.toList());
    }
}