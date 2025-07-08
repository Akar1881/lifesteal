package com.lifesteal.commands;

import com.lifesteal.LifeSteal;
import com.lifesteal.gui.AllyListGUI;
import com.lifesteal.utils.ColorUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AllyCommand implements CommandExecutor, TabCompleter {
    private final LifeSteal plugin;
    private final List<String> subCommands = Arrays.asList("list", "accept", "deny");

    public AllyCommand(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.colorize("&cOnly players can use this command!"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            new AllyListGUI(plugin, player).open();
            return true;
        } else if (args[0].equalsIgnoreCase("accept")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtils.colorize("&cUsage: /ally accept <player>"));
                return true;
            }
            
            Player requester = Bukkit.getPlayer(args[1]);
            if (requester == null || !requester.isOnline()) {
                player.sendMessage(ColorUtils.colorize("&cPlayer not found or not online!"));
                return true;
            }
            
            if (plugin.getAllyManager().hasPendingIncomingRequest(player, requester)) {
                if (plugin.getAllyManager().acceptAllyRequest(player, requester)) {
                    player.sendMessage(ColorUtils.colorize("&aYou are now allies with " + requester.getName() + "!"));
                    requester.sendMessage(ColorUtils.colorize("&a" + player.getName() + " accepted your ally request!"));
                } else {
                    player.sendMessage(ColorUtils.colorize("&cFailed to accept ally request!"));
                }
            } else {
                player.sendMessage(ColorUtils.colorize("&cYou don't have a pending ally request from this player!"));
            }
            return true;
        } else if (args[0].equalsIgnoreCase("deny")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtils.colorize("&cUsage: /ally deny <player>"));
                return true;
            }
            
            Player requester = Bukkit.getPlayer(args[1]);
            if (requester == null || !requester.isOnline()) {
                player.sendMessage(ColorUtils.colorize("&cPlayer not found or not online!"));
                return true;
            }
            
            if (plugin.getAllyManager().hasPendingIncomingRequest(player, requester)) {
                if (plugin.getAllyManager().cancelAllyRequest(requester, player)) {
                    player.sendMessage(ColorUtils.colorize("&aYou denied " + requester.getName() + "'s ally request."));
                    requester.sendMessage(ColorUtils.colorize("&c" + player.getName() + " denied your ally request."));
                } else {
                    player.sendMessage(ColorUtils.colorize("&cFailed to deny ally request!"));
                }
            } else {
                player.sendMessage(ColorUtils.colorize("&cYou don't have a pending ally request from this player!"));
            }
            return true;
        } else {
            // Assume it's a player name for sending a request
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null || !target.isOnline()) {
                player.sendMessage(ColorUtils.colorize("&cPlayer not found or not online!"));
                return true;
            }
            
            if (target.equals(player)) {
                player.sendMessage(ColorUtils.colorize("&cYou cannot ally yourself!"));
                return true;
            }
            
            if (plugin.getAllyManager().isAlly(player, target)) {
                player.sendMessage(ColorUtils.colorize("&cYou are already allies with " + target.getName() + "!"));
                return true;
            }
            
            if (plugin.getAllyManager().hasPendingOutgoingRequest(player, target)) {
                player.sendMessage(ColorUtils.colorize("&cYou already sent an ally request to " + target.getName() + "!"));
                return true;
            }
            
            if (plugin.getAllyManager().hasPendingIncomingRequest(player, target)) {
                player.sendMessage(ColorUtils.colorize("&c" + target.getName() + " already sent you an ally request! Use /ally accept " + target.getName() + " to accept it."));
                return true;
            }
            
            if (plugin.getAllyManager().sendAllyRequest(player, target)) {
                player.sendMessage(ColorUtils.colorize("&aYou have sent an ally request to " + target.getName() + "."));
                
                // Send clickable message to target
                TextComponent message = new TextComponent(ColorUtils.colorize("&e" + player.getName() + " sent you an ally request. "));
                
                // Accept button
                TextComponent acceptButton = new TextComponent(ColorUtils.colorize("&a[Accept]"));
                acceptButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ally accept " + player.getName()));
                acceptButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(ColorUtils.colorize("&aClick to accept the ally request")).create()));
                
                // Deny button
                TextComponent denyButton = new TextComponent(ColorUtils.colorize("&c[Deny]"));
                denyButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ally deny " + player.getName()));
                denyButton.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder(ColorUtils.colorize("&cClick to deny the ally request")).create()));
                
                // Combine the message
                message.addExtra(acceptButton);
                message.addExtra(new TextComponent(" "));
                message.addExtra(denyButton);
                
                target.spigot().sendMessage(message);
            } else {
                player.sendMessage(ColorUtils.colorize("&cFailed to send ally request!"));
            }
            return true;
        }
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(ColorUtils.colorize("&6=== Ally System Help ==="));
        player.sendMessage(ColorUtils.colorize("&e/ally <player> &7- Send an ally request to a player"));
        player.sendMessage(ColorUtils.colorize("&e/ally list &7- View your allies"));
        player.sendMessage(ColorUtils.colorize("&e/ally accept <player> &7- Accept an ally request"));
        player.sendMessage(ColorUtils.colorize("&e/ally deny <player> &7- Deny an ally request"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(subCommands);
            
            // Add online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getName().equals(sender.getName())) {
                    completions.add(player.getName());
                }
            }
            
            return completions.stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("accept") || args[0].equalsIgnoreCase("deny"))) {
            Player player = (Player) sender;
            List<String> requesters = plugin.getAllyManager().getPendingIncomingRequests(player).stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
            
            return requesters;
        }
        
        return new ArrayList<>();
    }
}