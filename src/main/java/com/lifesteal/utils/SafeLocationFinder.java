package com.lifesteal.utils;

import com.lifesteal.LifeSteal;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;

import java.util.Random;

public class SafeLocationFinder {
    private final LifeSteal plugin;
    private final Random random = new Random();
    private final int maxAttempts;
    private static final int MIN_Y = 60;
    private static final int MAX_Y = 120;
    private static final int BORDER_BUFFER = 50; // Buffer from world border

    public SafeLocationFinder(LifeSteal plugin) {
        this.plugin = plugin;
        // Read max attempts from config, fallback to 100 if not set
        this.maxAttempts = plugin.getConfig().getInt("safe-location.max-attempts", 100);
    }

    public Location findSafeLocation() {
        World world = plugin.getServer().getWorlds().get(0); // Get overworld
        WorldBorder border = world.getWorldBorder();
        Location center = border.getCenter();
        double borderSize = border.getSize();
        
        // Calculate actual radius we can use (half of border size minus buffer)
        double radius = (borderSize / 2) - BORDER_BUFFER;
        
        // Calculate bounds
        double minX = center.getX() - radius;
        double maxX = center.getX() + radius;
        double minZ = center.getZ() - radius;
        double maxZ = center.getZ() + radius;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // Generate random coordinates within bounds
            double x = minX + (random.nextDouble() * (maxX - minX));
            double z = minZ + (random.nextDouble() * (maxZ - minZ));

            Location loc = findSafeY(world, x, z);
            if (loc != null && isWithinBorder(loc, border)) {
                return loc;
            }
        }

        // If no safe location found, try near spawn as fallback
        return findSafeNearSpawn(world);
    }

    private Location findSafeNearSpawn(World world) {
        Location spawn = world.getSpawnLocation();
        int radius = 100; // Search within 100 blocks of spawn

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double x = spawn.getX() + (random.nextDouble() * 2 - 1) * radius;
            double z = spawn.getZ() + (random.nextDouble() * 2 - 1) * radius;

            Location loc = findSafeY(world, x, z);
            if (loc != null && isWithinBorder(loc, world.getWorldBorder())) {
                return loc;
            }
        }

        // If all else fails, return spawn location
        return world.getSpawnLocation();
    }

    private boolean isWithinBorder(Location loc, WorldBorder border) {
        Location center = border.getCenter();
        double radius = border.getSize() / 2;
        
        return Math.abs(loc.getX() - center.getX()) <= (radius - BORDER_BUFFER) &&
               Math.abs(loc.getZ() - center.getZ()) <= (radius - BORDER_BUFFER);
    }

    private Location findSafeY(World world, double x, double z) {
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            Location loc = new Location(world, x, y, z);
            if (isSafeLocation(loc)) {
                // Move to center of block and face random direction
                loc.add(0.5, 0, 0.5);
                loc.setYaw(random.nextFloat() * 360);
                loc.setPitch(0);
                return loc;
            }
        }
        return null;
    }

    private boolean isSafeLocation(Location loc) {
        Block ground = loc.getBlock();
        Block feet = ground.getRelative(0, 1, 0);
        Block head = ground.getRelative(0, 2, 0);
        Block above = ground.getRelative(0, 3, 0);

        // Check if ground is solid and safe
        if (!ground.getType().isSolid() || !isSafeGround(ground.getType())) {
            return false;
        }

        // Check if feet, head, and above positions are safe
        return isAirOrSafeBlock(feet.getType()) && 
               isAirOrSafeBlock(head.getType()) && 
               isAirOrSafeBlock(above.getType());
    }

    private boolean isSafeGround(Material type) {
        switch (type) {
            case GRASS_BLOCK:
            case DIRT:
            case STONE:
            case GRANITE:
            case DIORITE:
            case ANDESITE:
            case PODZOL:
            case COARSE_DIRT:
            case ROOTED_DIRT:
                return true;
            default:
                return false;
        }
    }

    private boolean isAirOrSafeBlock(Material type) {
        switch (type) {
            case AIR:
            case CAVE_AIR:
            case GRASS:
            case TALL_GRASS:
            case SNOW:
            case FERN:
            case SMALL_DRIPLEAF:
                return true;
            default:
                return false;
        }
    }
}
