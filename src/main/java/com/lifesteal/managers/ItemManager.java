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
        registerHeartItem();
        registerReviveItem();
    }

    private void registerHeartItem() {
        ConfigurationSection config = plugin.getConfigManager().getItemsConfig().getConfigurationSection("heart-item");
        if (config == null || !config.getBoolean("enabled")) return;

        ItemStack item = new ItemStack(Material.valueOf(config.getString("material", "RED_DYE")));
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ColorUtils.colorize(config.getString("name", "&cHeart Fragment")));
        
        List<String> lore = config.getStringList("lore");
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ColorUtils.colorize(line));
        }
        meta.setLore(coloredLore);

        if (config.getBoolean("glow")) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        customItems.put("heart", item);

        if (config.getConfigurationSection("recipe") != null) {
            registerHeartRecipe(config.getConfigurationSection("recipe"), item);
        }
    }

    private void registerReviveItem() {
        ConfigurationSection config = plugin.getConfigManager().getItemsConfig().getConfigurationSection("revive-item");
        if (config == null || !config.getBoolean("enabled")) return;

        ItemStack item = new ItemStack(Material.valueOf(config.getString("material", "TOTEM_OF_UNDYING")));
        ItemMeta meta = item.getItemMeta();
        
        meta.setDisplayName(ColorUtils.colorize(config.getString("name", "&6Revival Totem")));
        
        List<String> lore = config.getStringList("lore");
        List<String> coloredLore = new ArrayList<>();
        for (String line : lore) {
            coloredLore.add(ColorUtils.colorize(line));
        }
        meta.setLore(coloredLore);

        if (config.getBoolean("glow")) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        item.setItemMeta(meta);
        customItems.put("revive", item);

        if (config.getConfigurationSection("recipe") != null) {
            registerReviveRecipe(config.getConfigurationSection("recipe"), item);
        }
    }

    private void registerHeartRecipe(ConfigurationSection recipeConfig, ItemStack result) {
        if (recipeConfig.getBoolean("shaped", true)) {
            ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, "heart_item"), result);
            List<String> shape = recipeConfig.getStringList("shape");
            recipe.shape(shape.toArray(new String[0]));

            ConfigurationSection ingredients = recipeConfig.getConfigurationSection("ingredients");
            for (String key : ingredients.getKeys(false)) {
                recipe.setIngredient(key.charAt(0), Material.valueOf(ingredients.getString(key)));
            }

            plugin.getServer().addRecipe(recipe);
        }
    }

    private void registerReviveRecipe(ConfigurationSection recipeConfig, ItemStack result) {
        if (!recipeConfig.getBoolean("shaped", true)) {
            ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(plugin, "revive_item"), result);
            
            List<String> ingredients = recipeConfig.getStringList("ingredients");
            for (String ingredient : ingredients) {
                recipe.addIngredient(Material.valueOf(ingredient));
            }

            plugin.getServer().addRecipe(recipe);
        }
    }

    public ItemStack getCustomItem(String name) {
        return customItems.get(name) != null ? customItems.get(name).clone() : null;
    }
}