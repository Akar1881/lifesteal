package com.lifesteal.listeners;

import com.lifesteal.LifeSteal;
import com.lifesteal.gui.RevivalGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Sound;

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

        ItemStack heartItem = plugin.getItemManager().getCustomItem("heart");
        ItemStack reviveItem = plugin.getItemManager().getCustomItem("revive");

        if (heartItem != null && item.isSimilar(heartItem)) {
            handleHeartItem(player, item);
            event.setCancelled(true);
        } else if (reviveItem != null && item.isSimilar(reviveItem)) {
            handleReviveItem(player, item);
            event.setCancelled(true);
        }
    }

    private void handleHeartItem(Player player, ItemStack item) {
        if (!player.hasPermission("lifesteal.item.use.heart")) {
            player.sendMessage("§cYou don't have permission to use this item!");
            return;
        }

        long cooldown = plugin.getConfigManager().getItemsConfig().getInt("heart-item.cooldown") * 1000L;
        long timeLeft = getCooldownTimeLeft(player.getUniqueId(), heartCooldowns, cooldown);

        if (timeLeft > 0) {
            player.sendMessage("§cYou must wait " + (timeLeft / 1000) + " seconds before using this again!");
            return;
        }

        int maxHearts = plugin.getConfigManager().getMaxHearts();
        int currentHearts = plugin.getHeartManager().getHearts(player);

        if (currentHearts >= maxHearts && plugin.getConfigManager().getItemsConfig().getBoolean("heart-item.max-hearts-limit")) {
            player.sendMessage("§cYou've reached the maximum number of hearts!");
            return;
        }

        plugin.getHeartManager().addHearts(player, 1);
        item.setAmount(item.getAmount() - 1);

        if (plugin.getConfigManager().getItemsConfig().getBoolean("heart-item.heal-on-use")) {
            player.setHealth(player.getHealth() + 2);
        }

        String sound = plugin.getConfigManager().getConfig().getString("sounds.heart-gain");
        if (sound != null) {
            player.playSound(player.getLocation(), Sound.valueOf(sound), 1.0f, 1.0f);
        }

        player.sendMessage(plugin.getConfigManager().getConfig().getString("messages.heart-gain")
            .replace("&", "§"));

        heartCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    private void handleReviveItem(Player player, ItemStack item) {
        if (!player.hasPermission("lifesteal.item.use.revive")) {
            player.sendMessage("§cYou don't have permission to use this item!");
            return;
        }

        // Open revival GUI
        new RevivalGUI(plugin, player).open();
        item.setAmount(item.getAmount() - 1);
    }

    private long getCooldownTimeLeft(UUID uuid, HashMap<UUID, Long> cooldowns, long cooldownTime) {
        if (!cooldowns.containsKey(uuid)) return 0;
        
        long timePassed = System.currentTimeMillis() - cooldowns.get(uuid);
        return Math.max(0, cooldownTime - timePassed);
    }
}