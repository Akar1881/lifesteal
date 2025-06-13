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
import java.util.logging.Level;

public class LifeStealCommand implements CommandExecutor, TabCompleter {
    private final LifeSteal plugin;
    private final List<String> mainCommands = Arrays.asList("reload", "hearts", "revive", "schedule", "togglebar", "border", "help", "version", "info");
    private final List<String> heartsOperations = Arrays.asList("set", "add", "remove");
    private final List<String> borderOperations = Arrays.asList("info", "reset", "shrink", "toggle");
    private final List<String> scheduleOperations = Arrays.asList("set", "add", "subtract", "info");
    
    // Constants for validation
    private static final String ADMIN_PERMISSION = "lifesteal.admin";
    private static final int MAX_HEARTS = 100;
    private static final int MIN_HEARTS = 1;
    private static final long MAX_SCHEDULE_TIME = 24 * 60 * 60 * 1000L; // 24 hours in milliseconds
    private static final long MIN_SCHEDULE_TIME = 60 * 1000L; // 1 minute in milliseconds

    public LifeStealCommand(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase();
        try {
            switch (subCommand) {
                case "help":
                    sendHelpMessage(sender);
                    return true;
                case "version":
                case "info":
                    sendVersionInfo(sender);
                    return true;
                case "hearts":
                    return handleHeartsCommand(sender, args);
                case "revive":
                    return handleReviveCommand(sender, args);
                case "schedule":
                    return handleScheduleCommand(sender, args);
                case "togglebar":
                    return handleToggleBarCommand(sender);
                case "border":
                    return handleBorderCommand(sender, args);
                case "reload":
                    return handleReloadCommand(sender);
                default:
                    sender.sendMessage(ColorUtils.colorize("&cUnknown command. Use &e/lifesteal help &cfor help."));
                    return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error executing command: " + subCommand, e);
            sender.sendMessage(ColorUtils.colorize("&cAn error occurred while executing the command. Please check the console for details."));
            return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6&l=== LifeSteal v1.3 Commands ==="));
        sender.sendMessage(ColorUtils.colorize("&e/lifesteal help &7- Show this help message"));
        sender.sendMessage(ColorUtils.colorize("&e/lifesteal version &7- Show plugin version and info"));
        
        if (sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&c&lAdmin Commands:"));
            sender.sendMessage(ColorUtils.colorize("&e/lifesteal reload &7- Reload plugin configuration"));
            sender.sendMessage(ColorUtils.colorize("&e/lifesteal hearts <set|add|remove> <player> <amount> &7- Manage player hearts"));
            sender.sendMessage(ColorUtils.colorize("&e/lifesteal revive <player> &7- Revive an eliminated player"));
            sender.sendMessage(ColorUtils.colorize("&e/lifesteal schedule <set|add|subtract|info> &7- Control PvP/PvE cycle"));
            sender.sendMessage(ColorUtils.colorize("&e/lifesteal togglebar &7- Toggle boss bar visibility"));
            sender.sendMessage(ColorUtils.colorize("&e/lifesteal border <info|reset|shrink|toggle> &7- Manage world border"));
        }
        
        sender.sendMessage(ColorUtils.colorize("&a&lPlayer Commands:"));
        sender.sendMessage(ColorUtils.colorize("&e/ally <player> &7- Send an ally request"));
        sender.sendMessage(ColorUtils.colorize("&e/ally list &7- View your allies"));
        sender.sendMessage(ColorUtils.colorize("&e/ally accept <player> &7- Accept an ally request"));
        sender.sendMessage(ColorUtils.colorize("&e/ally deny <player> &7- Deny an ally request"));
        
        sender.sendMessage(ColorUtils.colorize("&b&lFeatures:"));
        sender.sendMessage(ColorUtils.colorize("&7• Heart stealing system with PvP/PvE cycles"));
        sender.sendMessage(ColorUtils.colorize("&7• Advanced ally system with revival"));
        sender.sendMessage(ColorUtils.colorize("&7• Dynamic world border with shrinking"));
        sender.sendMessage(ColorUtils.colorize("&7• Bounty system during PvP mode"));
        sender.sendMessage(ColorUtils.colorize("&7• First join queue with chunk pre-generation"));
        sender.sendMessage(ColorUtils.colorize("&7• Statistics tracking and leaderboards"));
    }

    private void sendVersionInfo(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&6&l=== LifeSteal Plugin Information ==="));
        sender.sendMessage(ColorUtils.colorize("&eVersion: &f1.3-RELEASE"));
        sender.sendMessage(ColorUtils.colorize("&eAuthor: &fAkar1881"));
        sender.sendMessage(ColorUtils.colorize("&eMinecraft Version: &f1.17.1+"));
        sender.sendMessage(ColorUtils.colorize("&eJava Version: &f17+"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&a&lFeatures in v1.3:"));
        sender.sendMessage(ColorUtils.colorize("&7• Enhanced heart management system"));
        sender.sendMessage(ColorUtils.colorize("&7• Advanced ally system with GUI"));
        sender.sendMessage(ColorUtils.colorize("&7• Dynamic world border management"));
        sender.sendMessage(ColorUtils.colorize("&7• Bounty system with rare rewards"));
        sender.sendMessage(ColorUtils.colorize("&7• First join queue with chunk pre-generation"));
        sender.sendMessage(ColorUtils.colorize("&7• MySQL and SQLite database support"));
        sender.sendMessage(ColorUtils.colorize("&7• Statistics and performance tracking"));
        sender.sendMessage(ColorUtils.colorize("&7• Chunky integration for chunk pre-generation"));
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&b&lServer Status:"));
        sender.sendMessage(ColorUtils.colorize("&7Online Players: &e" + Bukkit.getOnlinePlayers().size()));
        sender.sendMessage(ColorUtils.colorize("&7Current Mode: &e" + (plugin.getModeManager().isPvPMode() ? "PvP" : "PvE")));
        sender.sendMessage(ColorUtils.colorize("&7World Border: &e" + (plugin.getConfigManager().isWorldBorderEnabled() ? "Enabled" : "Disabled")));
        sender.sendMessage(ColorUtils.colorize("&7Bounty System: &e" + (plugin.getBountyManager().isBountyEnabled() ? "Active" : "Inactive")));
        sender.sendMessage(ColorUtils.colorize("&7Database: &e" + plugin.getConfigManager().getConfig().getString("storage.type", "sqlite").toUpperCase()));
        
        if (plugin.isChunkyAvailable()) {
            sender.sendMessage(ColorUtils.colorize("&7Chunky Integration: &aAvailable"));
        } else {
            sender.sendMessage(ColorUtils.colorize("&7Chunky Integration: &cNot Available"));
        }
        
        sender.sendMessage("");
        sender.sendMessage(ColorUtils.colorize("&6Support: &fhttps://discord.gg/K6tkSQcPfA"));
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        try {
            sender.sendMessage(ColorUtils.colorize("&eReloading LifeSteal configuration..."));
            
            plugin.getConfigManager().reloadConfigs();
            plugin.getModeManager().loadMessages();
            plugin.getWorldBorderManager().loadBorderData();
            plugin.getBountyManager().loadBountyData();
            
            // Reload first join manager if enabled
            if (plugin.getConfigManager().isFirstJoinEnabled() && plugin.getFirstJoinManager() != null) {
                plugin.getFirstJoinManager().getQueueWorld().reload();
            }
            
            sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aConfiguration reloaded successfully!"));
            sender.sendMessage(ColorUtils.colorize("&7• Config files reloaded"));
            sender.sendMessage(ColorUtils.colorize("&7• Boss bar messages updated"));
            sender.sendMessage(ColorUtils.colorize("&7• World border data refreshed"));
            sender.sendMessage(ColorUtils.colorize("&7• Bounty system updated"));
            
            if (plugin.getConfigManager().isFirstJoinEnabled()) {
                sender.sendMessage(ColorUtils.colorize("&7• Queue world configuration reloaded"));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reloading configuration", e);
            sender.sendMessage(ColorUtils.colorize("&c&lERROR! &cAn error occurred while reloading the configuration. Please check the console for details."));
        }
        return true;
    }

    private boolean handleHeartsCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 4) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal hearts <set|add|remove> <player> <amount>"));
            return true;
        }

        String operation = args[1].toLowerCase();
        if (!heartsOperations.contains(operation)) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid operation. Use set, add, or remove."));
            return true;
        }

        String playerName = args[2];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize("&cPlayer not found: " + playerName));
            return true;
        }

        try {
            int amount = Integer.parseInt(args[3]);
            if (amount < MIN_HEARTS || amount > MAX_HEARTS) {
                sender.sendMessage(ColorUtils.colorize("&cAmount must be between " + MIN_HEARTS + " and " + MAX_HEARTS));
                return true;
            }

            int currentHearts = plugin.getHeartManager().getHearts(target);
            
            switch (operation) {
                case "set":
                    plugin.getHeartManager().setHearts(target, amount);
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aSet " + target.getName() + "'s hearts to &c" + amount + " &a(was " + currentHearts + ")"));
                    break;
                case "add":
                    plugin.getHeartManager().addHearts(target, amount);
                    int newHeartsAdd = plugin.getHeartManager().getHearts(target);
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aAdded &c" + amount + " &ahearts to " + target.getName() + " &7(" + currentHearts + " → " + newHeartsAdd + ")"));
                    break;
                case "remove":
                    plugin.getHeartManager().removeHearts(target, amount);
                    int newHeartsRemove = plugin.getHeartManager().getHearts(target);
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aRemoved &c" + amount + " &ahearts from " + target.getName() + " &7(" + currentHearts + " → " + newHeartsRemove + ")"));
                    break;
            }
            
            // Notify the target player
            target.sendMessage(ColorUtils.colorize("&6&lADMIN ACTION: &eYour hearts have been " + operation + " by an administrator."));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid amount specified. Please enter a valid number."));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error managing hearts", e);
            sender.sendMessage(ColorUtils.colorize("&cAn error occurred while managing hearts. Please check the console for details."));
        }
        return true;
    }

    private boolean handleReviveCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal revive <player>"));
            return true;
        }

        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        if (target == null) {
            sender.sendMessage(ColorUtils.colorize("&cPlayer not found: " + playerName));
            return true;
        }

        try {
            plugin.getHeartManager().revivePlayer(target);
            sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aRevived " + target.getName() + " and restored them to " + plugin.getConfigManager().getStartingHearts() + " hearts!"));
            
            // Broadcast revival message
            Bukkit.broadcastMessage(ColorUtils.colorize("&6&lREVIVAL! &e" + target.getName() + " has been revived by an administrator!"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error reviving player", e);
            sender.sendMessage(ColorUtils.colorize("&cAn error occurred while reviving the player. Please check the console for details."));
        }
        return true;
    }

    private boolean handleScheduleCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule <set|add|subtract|info> [time]"));
            return true;
        }

        String operation = args[1].toLowerCase();
        
        if (operation.equals("info")) {
            long timeLeft = plugin.getModeManager().getTimeLeftMillis();
            String currentMode = plugin.getModeManager().isPvPMode() ? "PvP" : "PvE";
            String nextMode = plugin.getModeManager().isPvPMode() ? "PvE" : "PvP";
            
            sender.sendMessage(ColorUtils.colorize("&6&l=== PvP/PvE Schedule Info ==="));
            sender.sendMessage(ColorUtils.colorize("&eCurrent Mode: &f" + currentMode));
            sender.sendMessage(ColorUtils.colorize("&eNext Mode: &f" + nextMode));
            sender.sendMessage(ColorUtils.colorize("&eTime Remaining: &f" + formatTime(timeLeft / 1000)));
            sender.sendMessage(ColorUtils.colorize("&ePvP Duration: &f" + plugin.getConfigManager().getPvPDuration() + " hours"));
            sender.sendMessage(ColorUtils.colorize("&ePvE Duration: &f" + plugin.getConfigManager().getPvEDuration() + " hours"));
            return true;
        }

        if (args.length != 3) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal schedule <set|add|subtract> <minutes>"));
            return true;
        }

        try {
            int minutes = Integer.parseInt(args[2]);
            if (minutes < 1 || minutes > 1440) { // 1 minute to 24 hours
                sender.sendMessage(ColorUtils.colorize("&cTime must be between 1 and 1440 minutes (24 hours)."));
                return true;
            }

            long millis = minutes * 60 * 1000L;
            
            switch (operation) {
                case "set":
                    plugin.getModeManager().addTime(millis - plugin.getModeManager().getTimeLeftMillis());
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aSet time until next mode switch to &e" + minutes + " minutes"));
                    break;
                case "add":
                    plugin.getModeManager().addTime(millis);
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aAdded &e" + minutes + " minutes &ato the current cycle"));
                    break;
                case "subtract":
                    plugin.getModeManager().subtractTime(millis);
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aSubtracted &e" + minutes + " minutes &afrom the current cycle"));
                    break;
                default:
                    sender.sendMessage(ColorUtils.colorize("&cInvalid operation. Use set, add, subtract, or info."));
                    return true;
            }
            
            // Show updated info
            long newTimeLeft = plugin.getModeManager().getTimeLeftMillis();
            sender.sendMessage(ColorUtils.colorize("&7New time remaining: &e" + formatTime(newTimeLeft / 1000)));
        } catch (NumberFormatException e) {
            sender.sendMessage(ColorUtils.colorize("&cInvalid time specified. Please enter a valid number of minutes."));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error managing schedule", e);
            sender.sendMessage(ColorUtils.colorize("&cAn error occurred while managing the schedule. Please check the console for details."));
        }
        return true;
    }

    private boolean handleToggleBarCommand(CommandSender sender) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        try {
            boolean isVisible = plugin.getModeManager().toggleBossBar();
            sender.sendMessage(ColorUtils.colorize(isVisible ? 
                "&a&lSUCCESS! &aBoss bar is now &evisible&a!" : 
                "&a&lSUCCESS! &aBoss bar is now &ehidden&a!"));
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error toggling boss bar", e);
            sender.sendMessage(ColorUtils.colorize("&cAn error occurred while toggling the boss bar. Please check the console for details."));
        }
        return true;
    }

    private boolean handleBorderCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            sender.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ColorUtils.colorize("&cUsage: /lifesteal border <info|reset|shrink|toggle>"));
            return true;
        }

        String operation = args[1].toLowerCase();
        
        try {
            switch (operation) {
                case "info":
                    double currentSize = plugin.getWorldBorderManager().getCurrentSize();
                    String timeUntilShrink = plugin.getWorldBorderManager().getFormattedTimeUntilNextShrink();
                    boolean enabled = plugin.getConfigManager().isWorldBorderEnabled();
                    boolean shrinkEnabled = plugin.getConfigManager().isWorldBorderShrinkEnabled();
                    
                    sender.sendMessage(ColorUtils.colorize("&6&l=== World Border Info ==="));
                    sender.sendMessage(ColorUtils.colorize("&eStatus: &f" + (enabled ? "Enabled" : "Disabled")));
                    sender.sendMessage(ColorUtils.colorize("&eCurrent Size: &f" + (int)currentSize + " blocks"));
                    sender.sendMessage(ColorUtils.colorize("&eShrinking: &f" + (shrinkEnabled ? "Enabled" : "Disabled")));
                    if (shrinkEnabled) {
                        sender.sendMessage(ColorUtils.colorize("&eNext Shrink: &f" + timeUntilShrink));
                        sender.sendMessage(ColorUtils.colorize("&eShrink Amount: &f" + (int)plugin.getConfigManager().getWorldBorderShrinkAmount() + " blocks"));
                        sender.sendMessage(ColorUtils.colorize("&eMinimum Size: &f" + (int)plugin.getConfigManager().getWorldBorderMinSize() + " blocks"));
                    }
                    break;
                case "reset":
                    plugin.getWorldBorderManager().resetBorder();
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aWorld border has been reset to initial size!"));
                    break;
                case "shrink":
                    plugin.getWorldBorderManager().shrinkBorder();
                    sender.sendMessage(ColorUtils.colorize("&a&lSUCCESS! &aForced world border shrink initiated!"));
                    break;
                case "toggle":
                    // This would require config modification, so just show current status
                    boolean currentlyEnabled = plugin.getConfigManager().isWorldBorderEnabled();
                    sender.sendMessage(ColorUtils.colorize("&eWorld border is currently: &f" + (currentlyEnabled ? "Enabled" : "Disabled")));
                    sender.sendMessage(ColorUtils.colorize("&7To toggle, modify the config.yml file and use /lifesteal reload"));
                    break;
                default:
                    sender.sendMessage(ColorUtils.colorize("&cInvalid operation. Use info, reset, shrink, or toggle."));
                    return true;
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error managing world border", e);
            sender.sendMessage(ColorUtils.colorize("&cAn error occurred while managing the world border. Please check the console for details."));
        }
        return true;
    }

    private String formatTime(long seconds) {
        if (seconds <= 0) return "0s";
        
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append("s");
        }
        
        return sb.toString().trim();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(ADMIN_PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            return mainCommands.stream()
                .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "hearts":
                    return heartsOperations.stream()
                        .filter(op -> op.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "schedule":
                    return scheduleOperations.stream()
                        .filter(op -> op.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
                case "border":
                    return borderOperations.stream()
                        .filter(op -> op.startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "hearts":
                case "revive":
                    return getOnlinePlayerNames(args[2]);
                case "schedule":
                    if (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("subtract")) {
                        return Arrays.asList("5", "10", "15", "30", "60", "120");
                    }
                    break;
            }
        }

        return Collections.emptyList();
    }

    private List<String> getOnlinePlayerNames(String partialName) {
        return Bukkit.getOnlinePlayers().stream()
            .map(Player::getName)
            .filter(name -> name.toLowerCase().startsWith(partialName.toLowerCase()))
            .collect(Collectors.toList());
    }
}