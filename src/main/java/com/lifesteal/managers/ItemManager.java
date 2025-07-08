package com.lifesteal.managers;

import com.lifesteal.LifeSteal;
import com.lifesteal.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemManager {
    private final LifeSteal plugin;
    private final Map<String, ItemStack> customItems = new HashMap<>();

    public ItemManager(LifeSteal plugin) {
        this.plugin = plugin;
    }

    public void registerItems() {
        ConfigurationSection itemsConfig = plugin.getConfigManager().getItemsConfig();
        for (String key : itemsConfig.getKeys(false)) {
            ConfigurationSection config = itemsConfig.getConfigurationSection(key);
            if (config == null || !config.getBoolean("enabled", false)) continue;

            // Build the item
            Material material = Material.valueOf(config.getString("material", "STONE"));
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();

            // Name
            meta.setDisplayName(ColorUtils.colorize(config.getString("name", key)));

            // Lore
            List<String> lore = config.getStringList("lore");
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
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
                    // For older MC versions, ignore
                }
            }

            item.setItemMeta(meta);
            customItems.put(key, item);

            // Register recipe if present and not revival-heart
            if (config.isConfigurationSection("recipe") && !key.equals("revival-heart")) {
                ConfigurationSection recipeConfig = config.getConfigurationSection("recipe");
                if (recipeConfig.getBoolean("shaped", true)) {
                    registerShapedRecipe(key, recipeConfig, item);
                } else {
                    registerShapelessRecipe(key, recipeConfig, item);
                }
            }
        }
    }

    private void registerShapedRecipe(String key, ConfigurationSection recipeConfig, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, key.toLowerCase()), result);
        List<String> shape = recipeConfig.getStringList("shape");
        recipe.shape(shape.toArray(new String[0]));

        ConfigurationSection ingredients = recipeConfig.getConfigurationSection("ingredients");
        for (String ingKey : ingredients.getKeys(false)) {
            recipe.setIngredient(ingKey.charAt(0), Material.valueOf(ingredients.getString(ingKey)));
        }
        plugin.getServer().addRecipe(recipe);
    }

    private void registerShapelessRecipe(String key, ConfigurationSection recipeConfig, ItemStack result) {
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, key.toLowerCase()), result);
        List<String> ingredients = recipeConfig.getStringList("ingredients");
        for (String ingredient : ingredients) {
            recipe.addIngredient(Material.valueOf(ingredient));
        }
        plugin.getServer().addRecipe(recipe);
    }

    public ItemStack getCustomItem(String key) {
        return customItems.get(key) != null ? customItems.get(key).clone() : null;
    }

    public void reloadItems() {
        // Clear existing items and recipes
        customItems.clear();
        plugin.getServer().resetRecipes();
        
        // Re-register all items
        registerItems();
    }

    public void giveItem(Player player, String itemId, int amount) {
        ItemStack item = getCustomItem(itemId);
        if (item != null) {
            item.setAmount(amount);
            player.getInventory().addItem(item);
        }
    }
}
