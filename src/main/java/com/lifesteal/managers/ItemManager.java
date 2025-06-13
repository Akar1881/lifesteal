package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.logging.Level;

public class ItemManager {
    private final LifeSteal plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();
    private final Set<NamespacedKey> registeredRecipes = new HashSet<>();
    
    // Constants for validation
    private static final int MAX_RECIPE_SIZE = 9;
    private static final int MAX_INGREDIENTS = 9;
    private static final int MAX_LORE_LENGTH = 50;
    private static final int MAX_NAME_LENGTH = 50;
    private static final Material DEFAULT_MATERIAL = Material.STONE;
    private static final String DEFAULT_NAME = "Custom Item";

    public ItemManager(LifeSteal plugin) {
        this.plugin = plugin;
    }

    public void registerItems() {
        try {
            // Clear existing recipes first
            clearRecipes();
            
            ConfigurationSection itemsConfig = plugin.getConfigManager().getItemsConfig();
            if (itemsConfig == null) {
                plugin.getLogger().warning("No items configuration found!");
                return;
            }

            for (String key : itemsConfig.getKeys(false)) {
                try {
                    ConfigurationSection config = itemsConfig.getConfigurationSection(key);
                    if (config == null || !config.getBoolean("enabled", false)) {
                        continue;
                    }

                    // Build the item
                    String materialName = config.getString("material", DEFAULT_MATERIAL.name());
                    Material material;
                    try {
                        material = Material.valueOf(materialName);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid material '" + materialName + "' for item '" + key + "', using " + DEFAULT_MATERIAL.name());
                        material = DEFAULT_MATERIAL;
                    }

                    ItemStack item = new ItemStack(material);
                    ItemMeta meta = item.getItemMeta();
                    if (meta == null) {
                        plugin.getLogger().warning("Could not get ItemMeta for item '" + key + "'");
                        continue;
                    }

                    // Name
                    String name = config.getString("name", DEFAULT_NAME);
                    if (name.length() > MAX_NAME_LENGTH) {
                        name = name.substring(0, MAX_NAME_LENGTH);
                        plugin.getLogger().warning("Item name for '" + key + "' was truncated due to length");
                    }
                    meta.setDisplayName(ColorUtils.colorize(name));

                    // Lore
                    List<String> lore = config.getStringList("lore");
                    List<String> coloredLore = new ArrayList<>();
                    for (String line : lore) {
                        if (line.length() > MAX_LORE_LENGTH) {
                            line = line.substring(0, MAX_LORE_LENGTH);
                            plugin.getLogger().warning("Lore line for item '" + key + "' was truncated due to length");
                        }
                        coloredLore.add(ColorUtils.colorize(line));
                    }
                    meta.setLore(coloredLore);

                    // Glow
                    if (config.getBoolean("glow", false)) {
                        meta.addEnchant(Enchantment.DURABILITY, 1, true);
                        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                    }

                    // CustomModelData
                    if (config.contains("custom-model-data")) {
                        int cmd = config.getInt("custom-model-data");
                        try {
                            meta.setCustomModelData(cmd);
                        } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
                            plugin.getLogger().info("CustomModelData not supported in this Minecraft version");
                        }
                    }

                    item.setItemMeta(meta);
                    customItems.put(key, item);

                    // Register recipe if present and not revival-heart
                    if (config.isConfigurationSection("recipe") && !key.equals("revival-heart")) {
                        ConfigurationSection recipeConfig = config.getConfigurationSection("recipe");
                        if (recipeConfig == null) {
                            plugin.getLogger().warning("Invalid recipe configuration for item '" + key + "'");
                            continue;
                        }

                        try {
                            if (recipeConfig.getBoolean("shaped", true)) {
                                registerShapedRecipe(key, recipeConfig, item);
                            } else {
                                registerShapelessRecipe(key, recipeConfig, item);
                            }
                        } catch (Exception e) {
                            plugin.getLogger().log(Level.WARNING, "Error registering recipe for item '" + key + "'", e);
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error processing item '" + key + "'", e);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error registering items", e);
        }
    }

    private void clearRecipes() {
        try {
            for (NamespacedKey key : registeredRecipes) {
                plugin.getServer().removeRecipe(key);
            }
            registeredRecipes.clear();
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error clearing recipes", e);
        }
    }

    private void registerShapedRecipe(String key, ConfigurationSection recipeConfig, ItemStack result) {
        try {
            List<String> shape = recipeConfig.getStringList("shape");
            if (shape.isEmpty() || shape.size() > 3) {
                throw new IllegalArgumentException("Invalid recipe shape for item '" + key + "'");
            }

            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key.toLowerCase()), result);
            recipe.shape(shape.toArray(new String[0]));

            ConfigurationSection ingredients = recipeConfig.getConfigurationSection("ingredients");
            if (ingredients == null) {
                throw new IllegalArgumentException("No ingredients found for item '" + key + "'");
            }

            for (String ingKey : ingredients.getKeys(false)) {
                if (ingKey.length() != 1) {
                    throw new IllegalArgumentException("Invalid ingredient key '" + ingKey + "' for item '" + key + "'");
                }

                String materialName = ingredients.getString(ingKey);
                if (materialName == null) {
                    throw new IllegalArgumentException("No material specified for ingredient '" + ingKey + "' in item '" + key + "'");
                }

                try {
                    Material material = Material.valueOf(materialName);
                    recipe.setIngredient(ingKey.charAt(0), material);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid material '" + materialName + "' for ingredient '" + ingKey + "' in item '" + key + "'");
                }
            }

            plugin.getServer().addRecipe(recipe);
            registeredRecipes.add(recipe.getKey());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error registering shaped recipe for item '" + key + "'", e);
            throw e;
        }
    }

    private void registerShapelessRecipe(String key, ConfigurationSection recipeConfig, ItemStack result) {
        try {
            List<String> ingredients = recipeConfig.getStringList("ingredients");
            if (ingredients.isEmpty() || ingredients.size() > MAX_INGREDIENTS) {
                throw new IllegalArgumentException("Invalid number of ingredients for item '" + key + "'");
            }

            ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, key.toLowerCase()), result);
            for (String ingredient : ingredients) {
                try {
                    Material material = Material.valueOf(ingredient);
                    recipe.addIngredient(material);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("Invalid material '" + ingredient + "' in shapeless recipe for item '" + key + "'");
                }
            }

            plugin.getServer().addRecipe(recipe);
            registeredRecipes.add(recipe.getKey());
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error registering shapeless recipe for item '" + key + "'", e);
            throw e;
        }
    }

    public ItemStack getCustomItem(String key) {
        if (key == null) {
            return null;
        }
        ItemStack item = customItems.get(key);
        return item != null ? item.clone() : null;
    }

    public boolean isCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) {
            return false;
        }
        String name = meta.getDisplayName();
        return customItems.values().stream()
            .anyMatch(customItem -> customItem.hasItemMeta() && 
                customItem.getItemMeta().hasDisplayName() && 
                customItem.getItemMeta().getDisplayName().equals(name));
    }
}
