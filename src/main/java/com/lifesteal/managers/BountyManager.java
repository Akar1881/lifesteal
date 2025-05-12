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
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class BountyManager implements Listener {
    private final LifeSteal plugin;
    private UUID targetPlayer;
    private BukkitTask locationTask;
    private BukkitTask bountyTask;
    private boolean bountyActive = false;
    private final Random random = new Random();
    private boolean isRareBounty = false;
    private static final double RARE_BOUNTY_CHANCE = 0.0001; // 0.01% chance

    public BountyManager(LifeSteal plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Starts the bounty system when PvP mode begins
     */
    public void startBountySystem() {
        if (!isBountyEnabled()) {
            return;
        }

        // Check if there are enough players online
        if (Bukkit.getOnlinePlayers().size() < getMinPlayers()) {
            Bukkit.broadcastMessage(ColorUtils.colorize(
                    plugin.getConfigManager().getConfig().getString("bounty.messages.not-enough-players", 
                    "&c&lBOUNTY SYSTEM! &fNot enough players online for bounty system to activate.")));
            return;
        }

        // Select a random player as the target
        selectRandomTarget();
    }

    /**
     * Stops the bounty system when PvP mode ends
     */
    public void stopBountySystem() {
        if (bountyActive && targetPlayer != null) {
            Player target = Bukkit.getPlayer(targetPlayer);
            if (target != null && target.isOnline()) {
                if (isRareBounty) {
                    // Target survived a rare bounty, give them a special reward
                    plugin.getHeartManager().addHearts(target, getRewardHearts() * 2);
                    
                    // Give them a revival item too
                    ItemStack revivalItem = createRevivalItem();
                    if (target.getInventory().firstEmpty() != -1) {
                        target.getInventory().addItem(revivalItem);
                    } else {
                        // Drop at player's feet if inventory is full
                        target.getWorld().dropItem(target.getLocation(), revivalItem);
                    }
                    
                    // Broadcast rare survival message
                    String rareSurvivedMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.rare-survived", 
                            "&c&l⚠ RARE BOUNTY SURVIVED! &6&l%player% has survived the rare bounty and earned extra hearts and a Revival Heart!");
                    Bukkit.broadcastMessage(ColorUtils.colorize(rareSurvivedMessage.replace("%player%", target.getName())));
                    
                    // Play special sound for rare bounty survival
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                } else {
                    // Target survived a normal bounty, give them a reward
                    plugin.getHeartManager().addHearts(target, getRewardHearts());
                    
                    // Broadcast survival message
                    String survivedMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.survived", 
                            "&c&lBOUNTY SURVIVED! &f%player% has survived the bounty and earned a heart!");
                    Bukkit.broadcastMessage(ColorUtils.colorize(survivedMessage.replace("%player%", target.getName())));
                    
                    // Play sound for all players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 1.0f);
                    }
                }
            }
        }

        // Cancel tasks
        if (locationTask != null) {
            locationTask.cancel();
            locationTask = null;
        }
        
        if (bountyTask != null) {
            bountyTask.cancel();
            bountyTask = null;
        }

        bountyActive = false;
        targetPlayer = null;
        isRareBounty = false;
    }

    /**
     * Selects a target player for the bounty, prioritizing players with more hearts
     */
    private void selectRandomTarget() {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        if (onlinePlayers.isEmpty()) {
            return;
        }

        // Determine if this will be a rare bounty (0.01% chance)
        isRareBounty = random.nextDouble() < RARE_BOUNTY_CHANCE;
        
        // Sort players by heart count (descending)
        List<Player> sortedPlayers = onlinePlayers.stream()
                .sorted(Comparator.comparingInt((Player p) -> plugin.getHeartManager().getHearts(p)).reversed())
                .collect(Collectors.toList());
        
        Player target;
        
        // Prioritize players with more than 10 hearts
        List<Player> highHeartPlayers = sortedPlayers.stream()
                .filter(p -> plugin.getHeartManager().getHearts(p) > 10)
                .collect(Collectors.toList());
        
        if (!highHeartPlayers.isEmpty()) {
            // 80% chance to pick from players with high hearts
            if (random.nextDouble() < 0.8) {
                // Pick from top 3 players with most hearts, or fewer if not enough players
                int selectionSize = Math.min(3, highHeartPlayers.size());
                target = highHeartPlayers.get(random.nextInt(selectionSize));
            } else {
                // 20% chance to pick any player
                target = sortedPlayers.get(random.nextInt(sortedPlayers.size()));
            }
        } else {
            // If no players have more than 10 hearts, pick randomly
            target = sortedPlayers.get(random.nextInt(sortedPlayers.size()));
        }
        
        targetPlayer = target.getUniqueId();
        bountyActive = true;

        // Broadcast bounty start message
        String startMessage;
        if (isRareBounty) {
            startMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.rare-start", 
                    "&c&l⚠ RARE BOUNTY! &6&lA rare bounty has been placed on &c%player%&6&l! Hunt them down for a special reward!");
        } else {
            startMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.start", 
                    "&c&lBOUNTY! &fA bounty has been placed on &c%player%&f! Hunt them down for a reward!");
        }
        
        Bukkit.broadcastMessage(ColorUtils.colorize(startMessage.replace("%player%", target.getName())));

        // Play sound for all players
        Sound bountySound = isRareBounty ? Sound.ENTITY_ENDER_DRAGON_GROWL : Sound.ENTITY_WITHER_SPAWN;
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), bountySound, 1.0f, 1.0f);
        }

        // Schedule location broadcasts
        startLocationBroadcasts();
    }

    /**
     * Starts periodic broadcasts of the target's location
     */
    private void startLocationBroadcasts() {
        int interval = getLocationBroadcastInterval() * 60 * 20; // Convert minutes to ticks
        
        locationTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!bountyActive || targetPlayer == null) {
                return;
            }

            Player target = Bukkit.getPlayer(targetPlayer);
            if (target == null || !target.isOnline()) {
                return;
            }

            Location loc = target.getLocation();
            String locationMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.location", 
                    "&c&lBOUNTY LOCATION! &f%player% is at &c%x%, %y%, %z% &fin &c%world%");
            
            // Get friendly world name
            String worldName = getFriendlyWorldName(loc.getWorld());
            
            locationMessage = locationMessage
                    .replace("%player%", target.getName())
                    .replace("%x%", String.valueOf(loc.getBlockX()))
                    .replace("%y%", String.valueOf(loc.getBlockY()))
                    .replace("%z%", String.valueOf(loc.getBlockZ()))
                    .replace("%world%", worldName);
            
            Bukkit.broadcastMessage(ColorUtils.colorize(locationMessage));
            
            // Play sound for all players
            Sound locationSound = isRareBounty ? Sound.BLOCK_BELL_USE : Sound.BLOCK_NOTE_BLOCK_PLING;
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), locationSound, 1.0f, 2.0f);
            }
        }, interval, interval);
    }
    
    /**
     * Converts a Minecraft world to a friendly display name
     */
    private String getFriendlyWorldName(World world) {
        String worldName = world.getName();
        
        if (worldName.equalsIgnoreCase("world")) {
            return "OVERWORLD";
        } else if (worldName.equalsIgnoreCase("world_nether") || worldName.endsWith("_nether")) {
            return "THE NETHER";
        } else if (worldName.equalsIgnoreCase("world_the_end") || worldName.endsWith("_the_end") || worldName.endsWith("_end")) {
            return "THE END";
        }
        
        // If it's a custom world, format it nicely
        return worldName.replace("_", " ").toUpperCase();
    }

    /**
     * Event handler for player deaths
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        
        // Check if the killed player was the bounty target
        if (bountyActive && targetPlayer != null && victim.getUniqueId().equals(targetPlayer)) {
            Player killer = victim.getKiller();
            
            if (killer != null) {
                // Give the killer a reward
                if (isRareBounty) {
                    // Give rare revival item
                    ItemStack revivalItem = createRevivalItem();
                    if (killer.getInventory().firstEmpty() != -1) {
                        killer.getInventory().addItem(revivalItem);
                    } else {
                        // Drop at player's feet if inventory is full
                        killer.getWorld().dropItem(killer.getLocation(), revivalItem);
                    }
                    
                    // Also give extra hearts for rare bounty
                    plugin.getHeartManager().addHearts(killer, getRewardHearts() * 2);
                    
                    // Broadcast rare killed message
                    String rareKilledMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.rare-killed", 
                            "&c&l⚠ RARE BOUNTY CLAIMED! &6&l%killer% has killed %player% and claimed the rare bounty! They received a Revival Heart!");
                    
                    rareKilledMessage = rareKilledMessage
                            .replace("%killer%", killer.getName())
                            .replace("%player%", victim.getName());
                    
                    Bukkit.broadcastMessage(ColorUtils.colorize(rareKilledMessage));
                    
                    // Play special sound for rare bounty claim
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
                    }
                } else {
                    // Normal bounty reward
                    plugin.getHeartManager().addHearts(killer, getRewardHearts());
                    
                    // Broadcast killed message
                    String killedMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.killed", 
                            "&c&lBOUNTY CLAIMED! &f%killer% has killed %player% and claimed the bounty!");
                    
                    killedMessage = killedMessage
                            .replace("%killer%", killer.getName())
                            .replace("%player%", victim.getName());
                    
                    Bukkit.broadcastMessage(ColorUtils.colorize(killedMessage));
                    
                    // Play sound for all players
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
                    }
                }
                
                // End the bounty
                bountyActive = false;
                targetPlayer = null;
                isRareBounty = false;
                
                if (locationTask != null) {
                    locationTask.cancel();
                    locationTask = null;
                }
            }
        }
    }
    
    /**
     * Creates a special revival heart item
     */
    private ItemStack createRevivalItem() {
        ItemStack revivalHeart = new ItemStack(Material.TOTEM_OF_UNDYING, 1);
        ItemMeta meta = revivalHeart.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtils.colorize("&c&l♥ &6&lRevival Heart &c&l♥"));
            
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtils.colorize("&7A rare item obtained from a bounty"));
            lore.add(ColorUtils.colorize("&7Right-click to revive yourself or"));
            lore.add(ColorUtils.colorize("&7give to another player to revive them"));
            lore.add(ColorUtils.colorize("&7from elimination."));
            lore.add("");
            lore.add(ColorUtils.colorize("&c&lRARE ITEM"));
            
            meta.setLore(lore);
            revivalHeart.setItemMeta(meta);
        }
        
        return revivalHeart;
    }

    /**
     * Event handler for player quit
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // If the bounty target logs out
        if (bountyActive && targetPlayer != null && event.getPlayer().getUniqueId().equals(targetPlayer)) {
            Player player = event.getPlayer();
            
            // Apply logout penalty if enabled
            if (isLogoutPenaltyEnabled()) {
                // Store the player's UUID and hearts lost for when they rejoin
                applyLogoutPenalty(player);
                
                // Broadcast the penalty message
                String penaltyMessage = plugin.getConfigManager().getConfig().getString("bounty.messages.logout-penalty", 
                        "&c&lBOUNTY ESCAPED! &f%player% has left the server while having a bounty and lost %hearts% hearts!");
                
                penaltyMessage = penaltyMessage
                        .replace("%player%", player.getName())
                        .replace("%hearts%", String.valueOf(getLogoutPenaltyHearts()));
                
                Bukkit.broadcastMessage(ColorUtils.colorize(penaltyMessage));
                
                // Play sound for all players
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    onlinePlayer.playSound(onlinePlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 0.5f);
                }
            } else {
                // If penalty is disabled, just announce that the target left
                Bukkit.broadcastMessage(ColorUtils.colorize("&c&lBOUNTY TARGET LEFT! &fSelecting a new target..."));
            }
            
            // Cancel current tasks
            if (locationTask != null) {
                locationTask.cancel();
                locationTask = null;
            }
            
            // End the current bounty
            bountyActive = false;
            targetPlayer = null;
            isRareBounty = false;
            
            // Schedule a new target selection after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, this::selectRandomTarget, 60L); // 3 seconds
        }
    }
    
    /**
     * Applies the logout penalty to a player
     */
    private void applyLogoutPenalty(Player player) {
        int heartsToRemove = getLogoutPenaltyHearts();
        
        // Remove hearts from the player
        plugin.getHeartManager().removeHearts(player, heartsToRemove);
        
        // Save player data to ensure the heart change persists
        plugin.getHeartManager().savePlayerData(player.getUniqueId());
    }

    /**
     * Event handler for player join
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // If we don't have a bounty active and we now have enough players, consider starting one
        // Only do this if we're in PvP mode
        if (!bountyActive && plugin.getModeManager().isPvPMode() && 
                Bukkit.getOnlinePlayers().size() >= getMinPlayers() && isBountyEnabled()) {
            // Schedule a new target selection after a short delay
            Bukkit.getScheduler().runTaskLater(plugin, this::selectRandomTarget, 100L); // 5 seconds
        }
    }

    /**
     * Checks if the bounty system is enabled in config
     */
    public boolean isBountyEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("bounty.enabled", true);
    }

    /**
     * Gets the minimum number of players required for bounty system
     */
    public int getMinPlayers() {
        return plugin.getConfigManager().getConfig().getInt("bounty.min-players", 10);
    }

    /**
     * Gets the interval for location broadcasts in minutes
     */
    public int getLocationBroadcastInterval() {
        return plugin.getConfigManager().getConfig().getInt("bounty.location-broadcast-interval", 5);
    }

    /**
     * Gets the number of hearts to reward
     */
    public int getRewardHearts() {
        return plugin.getConfigManager().getConfig().getInt("bounty.reward-hearts", 1);
    }
    
    /**
     * Checks if the logout penalty is enabled
     */
    public boolean isLogoutPenaltyEnabled() {
        return plugin.getConfigManager().getConfig().getBoolean("bounty.logout-penalty.enabled", true);
    }
    
    /**
     * Gets the number of hearts to remove as a logout penalty
     */
    public int getLogoutPenaltyHearts() {
        return plugin.getConfigManager().getConfig().getInt("bounty.logout-penalty.hearts-lost", 2);
    }

    /**
     * Checks if a bounty is currently active
     */
    public boolean isBountyActive() {
        return bountyActive;
    }

    /**
     * Gets the current target player UUID
     */
    public UUID getTargetPlayer() {
        return targetPlayer;
    }
}