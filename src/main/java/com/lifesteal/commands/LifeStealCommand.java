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
    private final List<String> mainCommands = Arrays.asList("reload", "hearts", "revive", "schedule", "togglebar", "border");
    private final List<String> heartsOperations = Arrays.asList("set", "add", "remove");
    private final List<String> borderOperations = Arrays.asList("info", "reset", "shrink", "toggle");

    public LifeStealCommand(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ColorUtils.colorize("&6&lLifeSteal Commands:"));
            sender.sendMessage(ColorUtils.colorize("&7/reload &f- Reloads the plugin configuration"));
            sender.sendMessage(ColorUtils.colorize("&7/hearts <set|add|remove> <player> <amount> &f- Manage player hearts"));
            sender.sendMessage(ColorUtils.colorize("&7/revive <player> &f- Revive a player"));
            sender.sendMessage(ColorUtils.colorize("&7/schedule <set|add|subtract|info> &f- Control PvP/PvE cycle"));
            sender.sendMessage(ColorUtils.colorize("&7/togglebar &f- Toggle the boss bar visibility"));
            sender.sendMessage(ColorUtils.colorize("&7/border <info|reset|shrink|toggle> &f- Manage world border"));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                
                sender.sendMessage(ColorUtils.colorize("&6Reloading LifeSteal configuration..."));
                
                // Stop running tasks first
                if (plugin.getModeManager() != null) {
                    plugin.getModeManager().stopRotation();
                }
                
                if (plugin.getBountyManager() != null) {
                    plugin.getBountyManager().stopBountySystem();
                }
                
                if (plugin.getWorldBorderManager() != null) {
                    plugin.getWorldBorderManager().stopShrinkTask();
                }
                
                // Reload configurations
                plugin.getConfigManager().reloadConfigs();
                
                // Re-register items with new config values
                plugin.getItemManager().registerItems();
                
                // Reload mode manager messages
                plugin.getModeManager().loadMessages();
                
                // Reload the world border data
                plugin.getWorldBorderManager().loadBorderData();
                
                // If the border is enabled, reinitialize it
                if (plugin.getConfigManager().isWorldBorderEnabled()) {
                    plugin.getWorldBorderManager().initializeBorder();
                }
                
                // Restart mode rotation if enabled
                if (plugin.getConfigManager().isPvPCycleEnabled()) {
                    plugin.getModeManager().startRotation();
                    
                    // If we're in PvP mode, restart the bounty system
                    if (plugin.getModeManager().isPvPMode() && plugin.getBountyManager().isBountyEnabled()) {
                        plugin.getBountyManager().startBountySystem();
                    }
                }
                
                sender.sendMessage(ColorUtils.colorize("&aLifeSteal configuration reloaded successfully!"));
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
                // /lifesteal schedule <set|add|subtract|info> ...
                if (args.length == 1) {
                    sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule <set|add|subtract|info> ..."));
                    return true;
                }
                switch (args[1].toLowerCase()) {
                    case "set":
                        if (args.length != 3 || !(args[2].equalsIgnoreCase("pvp") || args[2].equalsIgnoreCase("pve"))) {
                            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule set <pvp|pve>"));
                            return true;
                        }
                        boolean toPvP = args[2].equalsIgnoreCase("pvp");
                        plugin.getModeManager().setMode(toPvP);
                        sender.sendMessage(ColorUtils.colorize("&aMode set to " + (toPvP ? "PvP" : "PvE") + "!"));
                        return true;
                    case "add":
                        if (args.length != 3) {
                            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule add <minutes>"));
                            return true;
                        }
                        try {
                            long addMinutes = Long.parseLong(args[2]);
                            plugin.getModeManager().addTime(addMinutes * 60 * 1000L);
                            sender.sendMessage(ColorUtils.colorize("&aAdded " + addMinutes + " minutes to the current mode timer!"));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ColorUtils.colorize("&cInvalid number!"));
                        }
                        return true;
                    case "subtract":
                        if (args.length != 3) {
                            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule subtract <minutes>"));
                            return true;
                        }
                        try {
                            long subMinutes = Long.parseLong(args[2]);
                            plugin.getModeManager().subtractTime(subMinutes * 60 * 1000L);
                            sender.sendMessage(ColorUtils.colorize("&aSubtracted " + subMinutes + " minutes from the current mode timer!"));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ColorUtils.colorize("&cInvalid number!"));
                        }
                        return true;
                    case "info":
                        boolean isPvP = plugin.getModeManager().isPvPMode();
                        long millis = plugin.getModeManager().getTimeLeftMillis();
                        long seconds = millis / 1000;
                        long minutes = seconds / 60;
                        long remSeconds = seconds % 60;
                        sender.sendMessage(ColorUtils.colorize("&eCurrent mode: " + (isPvP ? "&cPvP" : "&aPvE")));
                        sender.sendMessage(ColorUtils.colorize("&eTime left: &b" + minutes + "m " + remSeconds + "s"));
                        return true;
                    default:
                        sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule <set|add|subtract|info>"));
                        sender.sendMessage(ColorUtils.colorize("&7/set <pvp|pve> &f- Instantly switch to PvP or PvE mode"));
                        sender.sendMessage(ColorUtils.colorize("&7/add <minutes> &f- Add minutes to the current mode timer"));
                        sender.sendMessage(ColorUtils.colorize("&7/subtract <minutes> &f- Subtract minutes from the current mode timer"));
                        sender.sendMessage(ColorUtils.colorize("&7/info &f- Show the current mode and time left"));
                        return true;
                }
                
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
                
            case "border":
                if (!sender.hasPermission("lifesteal.admin")) {
                    sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command!"));
                    return true;
                }
                
                if (args.length == 1) {
                    sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal border <info|reset|shrink|toggle>"));
                    return true;
                }
                
                return handleBorderCommand(sender, args);

            default:
                sender.sendMessage(ColorUtils.colorize("&cUnknown command. Use /lifesteal for help."));
                return true;
        }
    }

    private boolean handleBorderCommand(CommandSender sender, String[] args) {
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "info":
                // Show border information
                boolean isEnabled = plugin.getConfigManager().isWorldBorderEnabled();
                double currentSize = plugin.getWorldBorderManager().getCurrentSize();
                boolean isShrinking = plugin.getConfigManager().isWorldBorderShrinkEnabled();
                
                sender.sendMessage(ColorUtils.colorize("&6&lWorld Border Information:"));
                sender.sendMessage(ColorUtils.colorize("&eStatus: " + (isEnabled ? "&aEnabled" : "&cDisabled")));
                sender.sendMessage(ColorUtils.colorize("&eCurrent Size: &b" + (int)currentSize + " blocks"));
                sender.sendMessage(ColorUtils.colorize("&eShrinking: " + (isShrinking ? "&aEnabled" : "&cDisabled")));
                
                if (isShrinking) {
                    sender.sendMessage(ColorUtils.colorize("&eNext Shrink: &b" + 
                            plugin.getWorldBorderManager().getFormattedTimeUntilNextShrink()));
                    sender.sendMessage(ColorUtils.colorize("&eShrink Amount: &b" + 
                            (int)plugin.getConfigManager().getWorldBorderShrinkAmount() + " blocks"));
                    sender.sendMessage(ColorUtils.colorize("&eMinimum Size: &b" + 
                            (int)plugin.getConfigManager().getWorldBorderMinSize() + " blocks"));
                }
                return true;
                
            case "reset":
                // Reset the border to initial size
                plugin.getWorldBorderManager().resetBorder();
                sender.sendMessage(ColorUtils.colorize("&aWorld border has been reset to initial size!"));
                return true;
                
            case "shrink":
                // Force a border shrink
                if (!plugin.getConfigManager().isWorldBorderEnabled()) {
                    sender.sendMessage(ColorUtils.colorize("&cWorld border is not enabled in the configuration!"));
                    return true;
                }
                
                plugin.getWorldBorderManager().shrinkBorder();
                sender.sendMessage(ColorUtils.colorize("&aForced world border shrink initiated!"));
                return true;
                
            case "toggle":
                // Toggle border on/off in config
                boolean currentState = plugin.getConfigManager().isWorldBorderEnabled();
                plugin.getConfigManager().getConfig().set("world-border.enabled", !currentState);
                plugin.getConfigManager().saveConfigs();
                
                if (!currentState) {
                    // If we're enabling it, initialize the border
                    plugin.getWorldBorderManager().initializeBorder();
                    sender.sendMessage(ColorUtils.colorize("&aWorld border has been enabled!"));
                } else {
                    // If we're disabling it, stop the shrink task
                    plugin.getWorldBorderManager().stopShrinkTask();
                    sender.sendMessage(ColorUtils.colorize("&cWorld border has been disabled!"));
                }
                return true;
                
            default:
                sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal border <info|reset|shrink|toggle>"));
                sender.sendMessage(ColorUtils.colorize("&7/info &f- Show information about the world border"));
                sender.sendMessage(ColorUtils.colorize("&7/reset &f- Reset the border to its initial size"));
                sender.sendMessage(ColorUtils.colorize("&7/shrink &f- Force the border to shrink immediately"));
                sender.sendMessage(ColorUtils.colorize("&7/toggle &f- Toggle the world border on/off"));
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
            } else if (subCommand.equals("schedule")) {
                // For schedule command, suggest subcommands
                List<String> scheduleOps = Arrays.asList("set", "add", "subtract", "info");
                for (String op : scheduleOps) {
                    if (op.startsWith(partialArg)) {
                        completions.add(op);
                    }
                }
            } else if (subCommand.equals("border")) {
                // For border command, suggest operations
                for (String op : borderOperations) {
                    if (op.startsWith(partialArg)) {
                        completions.add(op);
                    }
                }
            }
        } else if (args.length == 3 && args[0].toLowerCase().equals("hearts")) {
            // Third argument for hearts command - player name
            return getOnlinePlayerNames(args[2].toLowerCase());
        } else if (args.length == 3 && args[0].toLowerCase().equals("schedule")) {
            // Third argument for schedule command
            String subCommand = args[1].toLowerCase();
            String partialArg = args[2].toLowerCase();
            if (subCommand.equals("set")) {
                List<String> modes = Arrays.asList("pvp", "pve");
                for (String mode : modes) {
                    if (mode.startsWith(partialArg)) {
                        completions.add(mode);
                    }
                }
            } else if (subCommand.equals("add") || subCommand.equals("subtract")) {
                List<String> mins = Arrays.asList("1", "5", "10", "30");
                for (String min : mins) {
                    if (min.startsWith(partialArg)) {
                        completions.add(min);
                    }
                }
            }
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