package com.lifesteal.utils;

import org.bukkit.ChatColor;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ColorUtils {
    private static final char COLOR_CHAR = '&';
    private static final String EMPTY_STRING = "";

    public static String colorize(String text) {
        if (text == null || text.isEmpty()) {
            return EMPTY_STRING;
        }
        try {
            return ChatColor.translateAlternateColorCodes(COLOR_CHAR, text);
        } catch (Exception e) {
            return text;
        }
    }

    public static String[] colorize(String[] texts) {
        if (texts == null || texts.length == 0) {
            return new String[0];
        }
        return Arrays.stream(texts)
            .map(ColorUtils::colorize)
            .toArray(String[]::new);
    }

    public static List<String> colorize(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        return texts.stream()
            .map(ColorUtils::colorize)
            .collect(Collectors.toList());
    }

    public static String stripColors(String text) {
        if (text == null || text.isEmpty()) {
            return EMPTY_STRING;
        }
        try {
            return ChatColor.stripColor(colorize(text));
        } catch (Exception e) {
            return text;
        }
    }
}