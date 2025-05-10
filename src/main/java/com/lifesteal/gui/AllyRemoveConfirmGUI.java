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

import java.util.ArrayList;
import java.util.List;

public class AllyRemoveConfirmGUI {
    private final LifeSteal plugin;
    private final Player player;
    private final OfflinePlayer ally;
    private final Inventory inventory;

    public AllyRemoveConfirmGUI(LifeSteal plugin, Player player, OfflinePlayer ally) {
        this.plugin = plugin;
        this.player = player;
        this.ally = ally;
        this.inventory = Bukkit.createInventory(null, 27, ColorUtils.colorize("&cRemove Ally: " + ally.getName()));
        initializeItems();
    }

    private void initializeItems() {
        // Confirm button
        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName(ColorUtils.colorize("&a&lCONFIRM"));
        List<String> confirmLore = new ArrayList<>();
        confirmLore.add(ColorUtils.colorize("&7Click to remove " + ally.getName() + " from your allies"));
        confirmMeta.setLore(confirmLore);
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(11, confirm);

        // Cancel button
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName(ColorUtils.colorize("&c&lCANCEL"));
        List<String> cancelLore = new ArrayList<>();
        cancelLore.add(ColorUtils.colorize("&7Click to cancel"));
        cancelMeta.setLore(cancelLore);
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(15, cancel);

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