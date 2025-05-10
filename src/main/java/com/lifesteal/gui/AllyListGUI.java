package com.lifesteal.gui;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

public class AllyListGUI {
    private final LifeSteal plugin;
    private final Player player;
    private final Inventory inventory;

    public AllyListGUI(LifeSteal plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 54, ColorUtils.colorize("&6Your Allies"));
        initializeItems();
    }

    private void initializeItems() {
        List<OfflinePlayer> allies = plugin.getAllyManager().getAllies(player);
        
        if (allies.isEmpty()) {
            ItemStack noAllies = new ItemStack(Material.BARRIER);
            ItemMeta meta = noAllies.getItemMeta();
            meta.setDisplayName(ColorUtils.colorize("&cYou have no allies"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7Use /ally <player> to send an ally request"));
            meta.setLore(lore);
            noAllies.setItemMeta(meta);
            inventory.setItem(22, noAllies);
        } else {
            for (int i = 0; i < allies.size() && i < 54; i++) {
                OfflinePlayer ally = allies.get(i);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                
                meta.setDisplayName(ColorUtils.colorize("&e" + ally.getName()));
                meta.setOwningPlayer(ally);
                
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7Status: " + (ally.isOnline() ? "&aOnline" : "&cOffline")));
                lore.add(ColorUtils.colorize("&7Right-click to remove ally"));
                meta.setLore(lore);
                
                skull.setItemMeta(meta);
                inventory.setItem(i, skull);
            }
        }

        // Fill empty slots with glass panes
        ItemStack filler = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void open() {
        player.openInventory(inventory);
    }
}