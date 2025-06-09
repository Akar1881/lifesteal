package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class BountyManager implements Listener {
    private final LifeSteal plugin;
    private final ItemManager itemManager;
    private UUID targetPlayer;
    private BukkitTask locationTask;
    private BukkitTask bountyTask;
    private boolean bountyActive = false;
    private final Random random = new Random();
    private boolean isRareBounty = false;
    private static final double RARE_BOUNTY_CHANCE = 0.0001; // 0.01% chance

    public BountyManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager(); // Assumes you have a getter for ItemManager in LifeSteal
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Creates a special revival heart item (now config-driven)
     */
    private ItemStack createRevivalItem() {
        ItemStack revivalHeart = itemManager.getCustomItem("revival-heart");
        if (revivalHeart == null) {
            // fallback to a default if config is missing
            revivalHeart = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
            ItemMeta meta = revivalHeart.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtils.colorize("&c&l♥ &6&lRevival Heart &c&l♥"));
                List<String> lore = new ArrayList<>();
                lore.add(ColorUtils.colorize("&7A rare item obtained from a bounty"));
                lore.add(ColorUtils.colorize("&7Right-click to revive your allies"));
                lore.add(ColorUtils.colorize("&7from elimination."));
                lore.add("");
                lore.add(ColorUtils.colorize("&c&lRARE ITEM"));
                meta.setLore(lore);
                revivalHeart.setItemMeta(meta);
            }
        }
        return revivalHeart;
    }

    // --- ADDED: Bounty system control methods ---
    public boolean isBountyEnabled() {
        FileConfiguration config = plugin.getConfig();
        if (config.getConfigurationSection("bounty") == null) {
            return false;
        }
        
        ConfigurationSection bountySection = config.getConfigurationSection("bounty");
        return bountySection.contains("enabled") ? bountySection.getBoolean("enabled") : false;
    }

    public void startBountySystem() {
        // Start bounty system logic (set flag, start tasks, etc)
        this.bountyActive = true;
        // You may want to start scheduled tasks here if needed
    }

    public void stopBountySystem() {
        // Stop bounty system logic (unset flag, cancel tasks, etc)
        this.bountyActive = false;
        if (locationTask != null) {
            locationTask.cancel();
            locationTask = null;
        }
        if (bountyTask != null) {
            bountyTask.cancel();
            bountyTask = null;
        }
        // Any other cleanup if needed
    }
    // ... rest of the class ...
}
