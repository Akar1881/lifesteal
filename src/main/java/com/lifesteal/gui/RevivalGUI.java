package com.lifesteal.gui;

import com.lifesteal.LifeSteal;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
        this.inventory = Bukkit.createInventory(null, 54, "§6Select Player to Revive");
        initializeItems();
    }

    private void initializeItems() {
        List<Player> eliminatedPlayers = new ArrayList<>();
        
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == org.bukkit.GameMode.SPECTATOR) {
                eliminatedPlayers.add(p);
            }
        }

        for (int i = 0; i < eliminatedPlayers.size() && i < 54; i++) {
            Player eliminated = eliminatedPlayers.get(i);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            
            meta.setDisplayName("§e" + eliminated.getName());
            meta.setOwningPlayer(eliminated);
            
            List<String> lore = new ArrayList<>();
            lore.add("§7Click to revive this player");
            meta.setLore(lore);
            
            skull.setItemMeta(meta);
            inventory.setItem(i, skull);
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