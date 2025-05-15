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

public class RevivalGUI {
    private final LifeSteal plugin;
    private final Player player;
    private final Inventory inventory;

    public RevivalGUI(LifeSteal plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.inventory = Bukkit.createInventory(null, 54, ColorUtils.colorize("&6Select Ally to Revive"));
        initializeItems();
    }

    private void initializeItems() {
        List<Player> eliminatedAllies = new ArrayList<>();
        List<org.bukkit.OfflinePlayer> bannedAllies = new ArrayList<>();
        
        // Get all online players in spectator mode who are allies
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR && plugin.getAllyManager().isAlly(player, p)) {
                eliminatedAllies.add(p);
            }
        }
        
        // Get all banned players who are allies
        for (org.bukkit.OfflinePlayer offlinePlayer : plugin.getAllyManager().getAllies(player)) {
            // Check if the player is banned
            if (offlinePlayer.getName() != null && Bukkit.getBanList(org.bukkit.BanList.Type.NAME).isBanned(offlinePlayer.getName())) {
                // Make sure they're not already in the eliminated list (if they're online)
                boolean alreadyIncluded = false;
                for (Player p : eliminatedAllies) {
                    if (p.getUniqueId().equals(offlinePlayer.getUniqueId())) {
                        alreadyIncluded = true;
                        break;
                    }
                }
                
                if (!alreadyIncluded) {
                    bannedAllies.add(offlinePlayer);
                }
            }
        }

        if (eliminatedAllies.isEmpty() && bannedAllies.isEmpty()) {
            // No allies to revive
            ItemStack noAllies = new ItemStack(Material.BARRIER);
            ItemMeta meta = noAllies.getItemMeta();
            meta.setDisplayName(ColorUtils.colorize("&cNo allies to revive"));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7None of your allies need revival"));
            lore.add(ColorUtils.colorize("&7Use /ally to add more allies"));
            meta.setLore(lore);
            noAllies.setItemMeta(meta);
            inventory.setItem(22, noAllies);
        } else {
            int slot = 0;
            
            // Add online eliminated ally heads to the inventory
            for (int i = 0; i < eliminatedAllies.size() && slot < 54; i++) {
                Player eliminated = eliminatedAllies.get(i);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                
                meta.setDisplayName(ColorUtils.colorize("&e" + eliminated.getName()));
                meta.setOwningPlayer(eliminated);
                
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7Click to revive this ally"));
                lore.add(ColorUtils.colorize("&7Status: &eEliminated (Spectator)"));
                meta.setLore(lore);
                
                skull.setItemMeta(meta);
                inventory.setItem(slot++, skull);
            }
            
            // Add banned ally heads to the inventory
            for (int i = 0; i < bannedAllies.size() && slot < 54; i++) {
                org.bukkit.OfflinePlayer banned = bannedAllies.get(i);
                ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                
                meta.setDisplayName(ColorUtils.colorize("&c" + banned.getName()));
                meta.setOwningPlayer(banned);
                
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7Click to revive this ally"));
                lore.add(ColorUtils.colorize("&7Status: &cBanned"));
                meta.setLore(lore);
                
                skull.setItemMeta(meta);
                inventory.setItem(slot++, skull);
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