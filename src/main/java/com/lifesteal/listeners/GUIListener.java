package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
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
        if (!event.getView().getTitle().equals("§6Select Player to Revive")) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta() || !(clicked.getItemMeta() instanceof SkullMeta)) {
            return;
        }

        Player clicker = (Player) event.getWhoClicked();
        SkullMeta meta = (SkullMeta) clicked.getItemMeta();
        
        if (meta.getOwningPlayer() == null) {
            return;
        }

        Player target = meta.getOwningPlayer().getPlayer();
        if (target == null || !target.isOnline()) {
            clicker.sendMessage("§cPlayer is no longer online!");
            return;
        }

        plugin.getHeartManager().revivePlayer(target);
        clicker.sendMessage("§aSuccessfully revived " + target.getName() + "!");
        target.sendMessage("§aYou have been revived by " + clicker.getName() + "!");
        
        clicker.closeInventory();
    }
}