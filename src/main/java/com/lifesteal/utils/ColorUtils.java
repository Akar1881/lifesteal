package com.lifesteal.utils;

import org.bukkit.ChatColor;

public class ColorUtils {
    public static String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    public static String[] colorize(String[] texts) {
        String[] result = new String[texts.length];
        for (int i = 0; i < texts.length; i++) {
            result[i] = colorize(texts[i]);
        }
        return result;
    }
}