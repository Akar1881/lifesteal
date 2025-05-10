package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.gui.AllyListGUI;
import com.lifesteal.gui.AllyRemoveConfirmGUI;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class GUIListener implements Listener {
    private final LifeSteal plugin;

    public GUIListener(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        
        // Handle Revival GUI
        if (title.equals(ColorUtils.colorize("&6Select Ally to Revive"))) {
            handleRevivalGUI(event);
        }
        // Handle Ally List GUI
        else if (title.equals(ColorUtils.colorize("&6Your Allies"))) {
            handleAllyListGUI(event);
        }
        // Handle Ally Remove Confirmation GUI
        else if (title.startsWith(ColorUtils.colorize("&cRemove Ally:"))) {
            handleAllyRemoveConfirmGUI(event);
        }
    }
    
    private void handleRevivalGUI(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        // Check if it's the "No allies" barrier item
        if (clicked.getType() == Material.BARRIER) {
            return;
        }
        
        // Check if it's a player head
        if (!(clicked.getItemMeta() instanceof SkullMeta)) {
            return;
        }

        Player clicker = (Player) event.getWhoClicked();
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        
        if (meta.getOwningPlayer() == null) {
            return;
        }

        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            clicker.sendMessage(ColorUtils.colorize("&cPlayer is no longer online!"));
            return;
        }
        
        // Check if they are allies
        if (!plugin.getAllyManager().isAlly(clicker, target)) {
            clicker.sendMessage(ColorUtils.colorize("&cYou can only revive your allies!"));
            return;
        }

        plugin.getHeartManager().revivePlayer(target);
        clicker.sendMessage(ColorUtils.colorize("&aSuccessfully revived " + target.getName() + "!"));
        target.sendMessage(ColorUtils.colorize("&aYou have been revived by " + clicker.getName() + "!"));
        
        clicker.closeInventory();
    }
    
    private void handleAllyListGUI(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        // Check if it's the "No allies" barrier item
        if (clicked.getType() == Material.BARRIER) {
            return;
        }
        
        // Check if it's a player head
        if (!(clicked.getItemMeta() instanceof SkullMeta)) {
            return;
        }
        
        // Check if it was a right-click (to remove ally)
        if (event.isRightClick()) {
            Player player = (Player) event.getWhoClicked();
            SkullMeta meta = (SkullMeta) clicked.getItemMeta();
            
            if (meta.getOwningPlayer() == null) {
                return;
            }
            
            OfflinePlayer ally = meta.getOwningPlayer();
            
            // Open confirmation GUI
            new AllyRemoveConfirmGUI(plugin, player, ally).open();
        }
    }
    
    private void handleAllyRemoveConfirmGUI(InventoryClickEvent event) {
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        String allyName = title.substring(ColorUtils.colorize("&cRemove Ally: ").length());
        OfflinePlayer ally = plugin.getServer().getOfflinePlayer(allyName);
        
        if (clicked.getType() == Material.LIME_WOOL) {
            // Confirm removal
            if (plugin.getAllyManager().removeAlly(player, ally)) {
                player.sendMessage(ColorUtils.colorize("&aRemoved " + allyName + " from your allies."));
                
                // If ally is online, notify them
                if (ally.isOnline()) {
                    ally.getPlayer().sendMessage(ColorUtils.colorize("&c" + player.getName() + " has removed you from their allies."));
                }
                
                // Close inventory and open ally list
                player.closeInventory();
                new AllyListGUI(plugin, player).open();
            } else {
                player.sendMessage(ColorUtils.colorize("&cFailed to remove ally!"));
                player.closeInventory();
            }
        } else if (clicked.getType() == Material.RED_WOOL) {
            // Cancel removal
            player.closeInventory();
            new AllyListGUI(plugin, player).open();
        }
    }
}