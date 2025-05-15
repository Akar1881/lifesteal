package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.gui.RevivalGUI;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.HashMap;
import java.util.UUID;

public class ItemListener implements Listener {
    private final LifeSteal plugin;
    private final HashMap<UUID, Long> heartCooldowns = new HashMap<>();
    private final HashMap<UUID, Long> reviveCooldowns = new HashMap<>();

    public ItemListener(LifeSteal plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        ItemStack heartItem = plugin.getItemManager().getCustomItem("heart-item");
        ItemStack reviveItem = plugin.getItemManager().getCustomItem("revive-item");
        ItemStack revivalHeartItem = plugin.getItemManager().getCustomItem("revival-heart");

        if (heartItem != null && item.isSimilar(heartItem)) {
            handleHeartItem(player, item);
            event.setCancelled(true);
        } else if (reviveItem != null && item.isSimilar(reviveItem)) {
            handleReviveItem(player, item);
            event.setCancelled(true);
        } else if (revivalHeartItem != null && item.isSimilar(revivalHeartItem)) {
            handleRevivalHeartItem(player, item);
            event.setCancelled(true);
        } else if (isRevivalHeartItem(item)) {
            // Fallback check for revival heart items that might not match exactly
            handleRevivalHeartItem(player, item);
            event.setCancelled(true);
        }
    }
    
    /**
     * Checks if an item is a Revival Heart (from rare bounty)
     */
    private boolean isRevivalHeartItem(ItemStack item) {
        if (item.getType() != Material.TOTEM_OF_UNDYING) return false;
        
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        
        String displayName = item.getItemMeta().getDisplayName();
        return displayName.contains("Revival Heart");
    }

    private void handleHeartItem(Player player, ItemStack item) {
        if (!player.hasPermission("lifesteal.item.use.heart")) {
            player.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this item!"));
            return;
        }

        long cooldown = plugin.getConfigManager().getItemsConfig().getInt("heart-item.cooldown") * 1000L;
        long timeLeft = getCooldownTimeLeft(player.getUniqueId(), heartCooldowns, cooldown);

        if (timeLeft > 0) {
            player.sendMessage(ColorUtils.colorize("&cYou must wait " + (timeLeft / 1000) + " seconds before using this again!"));
            return;
        }

        int maxHearts = plugin.getConfigManager().getMaxHearts();
        int currentHearts = plugin.getHeartManager().getHearts(player);

        if (currentHearts >= maxHearts && plugin.getConfigManager().getItemsConfig().getBoolean("heart-item.max-hearts-limit")) {
            player.sendMessage(ColorUtils.colorize("&cYou've reached the maximum number of hearts!"));
            return;
        }

        plugin.getHeartManager().addHearts(player, 1);
        item.setAmount(item.getAmount() - 1);

        if (plugin.getConfigManager().getItemsConfig().getBoolean("heart-item.heal-on-use")) {
            // Calculate the new health without exceeding the maximum
            double maxHealth = player.getMaxHealth();
            double newHealth = Math.min(player.getHealth() + 2, maxHealth);
            player.setHealth(newHealth);
        }

        // Play a sound for heart gain
        try {
            String soundName = plugin.getConfigManager().getConfig().getString("sounds.heart-gain", "ENTITY_PLAYER_LEVELUP");
            // Make sure the sound name is properly formatted for the Sound enum
            if (soundName.contains(".")) {
                soundName = soundName.toUpperCase().replace(".", "_");
            }
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (Exception e) {
            // Fallback to a safe sound if there's an error
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            plugin.getLogger().warning("Invalid sound name in config. Using default sound instead.");
        }

        player.sendMessage(ColorUtils.colorize(plugin.getConfigManager().getConfig().getString("messages.heart-gain", "&aYou gained a heart!")));

        heartCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void handleReviveItem(Player player, ItemStack item) {
        if (!player.hasPermission("lifesteal.item.use.revive")) {
            player.sendMessage(ColorUtils.colorize("&cYou don't have permission to use this item!"));
            return;
        }

        // Store the item in player's metadata to be consumed only when a revival happens
        player.setMetadata("lifesteal_revival_item", new org.bukkit.metadata.FixedMetadataValue(plugin, "revive-item"));
        
        // Open revival GUI
        new RevivalGUI(plugin, player).open();
    }

    private long getCooldownTimeLeft(UUID uuid, HashMap<UUID, Long> cooldowns, long cooldownTime) {
        if (!cooldowns.containsKey(uuid)) return 0;
        
        long timePassed = System.currentTimeMillis() - cooldowns.get(uuid);
        return Math.max(0, cooldownTime - timePassed);
    }
    
    /**
     * Handles the use of a Revival Heart item (from rare bounty)
     */
    private void handleRevivalHeartItem(Player player, ItemStack item) {
        // Store the item in player's metadata to be consumed only when a revival happens
        player.setMetadata("lifesteal_revival_item", new org.bukkit.metadata.FixedMetadataValue(plugin, "revival-heart"));
        
        // Open the revival GUI
        new RevivalGUI(plugin, player).open();
        
        // Play special sound
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        
        // Send message
        player.sendMessage(ColorUtils.colorize("&6&l⚠ &eYou opened the &c&lRevival Heart&e menu! Select a player to revive."));
    }
}