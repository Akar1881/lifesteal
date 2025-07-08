package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.Set;

public class BountyManager implements Listener {
    private final LifeSteal plugin;
    private final ItemManager itemManager;
    private final Map<UUID, Boolean> activeBounties;
    private final Set<UUID> playersToLoseHearts;
    private BukkitTask locationTask;
    private BukkitTask bountyTask;
    private boolean enabled;
    private boolean bountyActive = false;
    private final Random random = new Random();
    private boolean isRareBounty = false;
    private static final double RARE_BOUNTY_CHANCE = 0.0001; // 0.01% chance

    public BountyManager(LifeSteal plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager(); // Assumes you have a getter for ItemManager in LifeSteal
        this.activeBounties = new HashMap<>();
        this.playersToLoseHearts = new HashSet<>();
        this.enabled = plugin.getConfigManager().getConfig().getBoolean("bounty.enabled", true);
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
        return config.getConfigurationSection("bounty") != null &&
               config.getConfigurationSection("bounty").getBoolean("enabled", false);
    }

    public void startBountySystem() {
        if (!enabled) return;
        
        int minPlayers = plugin.getConfigManager().getConfig().getInt("bounty.min-players", 10);
        if (Bukkit.getOnlinePlayers().size() < minPlayers) {
            String message = plugin.getConfigManager().getConfig().getString("bounty.messages.not-enough-players");
            if (message != null) {
                Bukkit.broadcastMessage(ColorUtils.colorize(message));
            }
            return;
        }

        // Clear any existing bounties
        clearBounties();

        // Start location broadcasting
        startLocationBroadcasting();

        // Select a random player for bounty
        selectRandomBounty();
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

    private void selectRandomBounty() {
        List<Player> eligiblePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!activeBounties.containsKey(player.getUniqueId())) {
                eligiblePlayers.add(player);
            }
        }

        if (eligiblePlayers.isEmpty()) return;

        // Check for rare bounty (0.1% chance)
        boolean isRareBounty = Math.random() < 0.001;
        Player target;

        if (isRareBounty) {
            // For rare bounty, only select players with high hearts
            int minHearts = plugin.getConfigManager().getConfig().getInt("bounty.rare-bounty.min-hearts", 10);
            List<Player> highHeartPlayers = eligiblePlayers.stream()
                .filter(p -> plugin.getHeartManager().getHearts(p) >= minHearts)
                .toList();

            if (highHeartPlayers.isEmpty()) {
                // If no high heart players, fall back to regular bounty
                isRareBounty = false;
                target = eligiblePlayers.get(new Random().nextInt(eligiblePlayers.size()));
            } else {
                target = highHeartPlayers.get(new Random().nextInt(highHeartPlayers.size()));
            }
        } else {
            target = eligiblePlayers.get(new Random().nextInt(eligiblePlayers.size()));
        }

        activeBounties.put(target.getUniqueId(), isRareBounty);
        playersToLoseHearts.add(target.getUniqueId());

        // Broadcast bounty message
        String message = isRareBounty ? 
            plugin.getConfigManager().getConfig().getString("bounty.messages.rare-start") :
            plugin.getConfigManager().getConfig().getString("bounty.messages.start");
        
        if (message != null) {
            message = message.replace("%player%", target.getName());
            Bukkit.broadcastMessage(ColorUtils.colorize(message));
        }
    }

    private void startLocationBroadcasting() {
        if (locationTask != null) {
            locationTask.cancel();
        }

        int interval = plugin.getConfigManager().getConfig().getInt("bounty.location-broadcast-interval", 10) * 1200; // Convert minutes to ticks
        locationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID targetId : activeBounties.keySet()) {
                Player target = Bukkit.getPlayer(targetId);
                if (target != null && target.isOnline()) {
                    Location loc = target.getLocation();
                    String message = plugin.getConfigManager().getConfig().getString("bounty.messages.location");
                    if (message != null) {
                        message = message.replace("%player%", target.getName())
                            .replace("%x%", String.valueOf(loc.getBlockX()))
                            .replace("%y%", String.valueOf(loc.getBlockY()))
                            .replace("%z%", String.valueOf(loc.getBlockZ()))
                            .replace("%world%", loc.getWorld().getName());
                        Bukkit.broadcastMessage(ColorUtils.colorize(message));
                    }
                }
            }
        }, interval, interval);
    }

    public void handleBountyKill(Player killer, Player victim) {
        if (!activeBounties.containsKey(victim.getUniqueId())) return;

        boolean isRareBounty = activeBounties.get(victim.getUniqueId());
        activeBounties.remove(victim.getUniqueId());
        playersToLoseHearts.remove(victim.getUniqueId());

        if (isRareBounty) {
            // Give Revival Heart for rare bounty
            plugin.getItemManager().giveItem(killer, "revival-heart", 1);
            
            String message = plugin.getConfigManager().getConfig().getString("bounty.messages.rare-killed");
            if (message != null) {
                message = message.replace("%killer%", killer.getName())
                    .replace("%player%", victim.getName());
                Bukkit.broadcastMessage(ColorUtils.colorize(message));
            }
        } else {
            // Give regular bounty reward
            int rewardHearts = plugin.getConfigManager().getConfig().getInt("bounty.reward-hearts", 1);
            plugin.getHeartManager().addHearts(killer, rewardHearts);
            
            String message = plugin.getConfigManager().getConfig().getString("bounty.messages.killed");
            if (message != null) {
                message = message.replace("%killer%", killer.getName())
                    .replace("%player%", victim.getName());
                Bukkit.broadcastMessage(ColorUtils.colorize(message));
            }
        }
    }

    public void handleBountySurvival(Player player) {
        if (!activeBounties.containsKey(player.getUniqueId())) return;

        boolean isRareBounty = activeBounties.get(player.getUniqueId());
        activeBounties.remove(player.getUniqueId());
        playersToLoseHearts.remove(player.getUniqueId());

        if (isRareBounty) {
            // Give Revival Heart for surviving rare bounty
            plugin.getItemManager().giveItem(player, "revival-heart", 1);
            
            String message = plugin.getConfigManager().getConfig().getString("bounty.messages.rare-survived");
            if (message != null) {
                message = message.replace("%player%", player.getName());
                Bukkit.broadcastMessage(ColorUtils.colorize(message));
            }
        } else {
            // Give heart reward for surviving regular bounty
            int rewardHearts = plugin.getConfigManager().getConfig().getInt("bounty.reward-hearts", 1);
            plugin.getHeartManager().addHearts(player, rewardHearts);
            
            String message = plugin.getConfigManager().getConfig().getString("bounty.messages.survived");
            if (message != null) {
                message = message.replace("%player%", player.getName());
                Bukkit.broadcastMessage(ColorUtils.colorize(message));
            }
        }
    }

    public void handlePlayerQuit(Player player) {
        if (playersToLoseHearts.contains(player.getUniqueId())) {
            boolean isRareBounty = activeBounties.getOrDefault(player.getUniqueId(), false);
            activeBounties.remove(player.getUniqueId());
            playersToLoseHearts.remove(player.getUniqueId());

            if (plugin.getConfigManager().getConfig().getBoolean("bounty.logout-penalty.enabled", true)) {
                int heartsLost = plugin.getConfigManager().getConfig().getInt("bounty.logout-penalty.hearts-lost", 2);
                plugin.getHeartManager().removeHearts(player, heartsLost);

                String message = plugin.getConfigManager().getConfig().getString("bounty.messages.logout-penalty");
                if (message != null) {
                    message = message.replace("%player%", player.getName())
                        .replace("%hearts%", String.valueOf(heartsLost));
                    Bukkit.broadcastMessage(ColorUtils.colorize(message));
                }
            }
        }
    }

    public void clearBounties() {
        activeBounties.clear();
        playersToLoseHearts.clear();
        if (locationTask != null) {
            locationTask.cancel();
            locationTask = null;
        }
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearBounties();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasBounty(Player player) {
        return activeBounties.containsKey(player.getUniqueId());
    }

    public boolean isRareBounty(Player player) {
        return activeBounties.getOrDefault(player.getUniqueId(), false);
    }
}
