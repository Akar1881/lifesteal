package com.lifesteal.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PvPCycleChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final boolean pvpEnabled;

    public PvPCycleChangeEvent(boolean pvpEnabled) {
        this.pvpEnabled = pvpEnabled;
    }

    public boolean isPvPEnabled() {
        return pvpEnabled;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
} 