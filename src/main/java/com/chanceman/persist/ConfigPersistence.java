package com.chanceman.persist;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.client.config.ConfigManager;


import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;


/**
 * Mirrors ChanceMan state into RuneLite ConfigManager so it can sync across machines
 * via RuneLite's profile/cloud sync.
 */
@Singleton
public final class ConfigPersistence {
    private static final String GROUP = "chanceman";
    private static final Type SET_TYPE = new TypeToken<Set<Integer>>(){}.getType();


    private final ConfigManager configManager;
    private final Gson gson;


    @Inject
    public ConfigPersistence(ConfigManager configManager, Gson gson) {
        this.configManager = configManager;
        this.gson = gson;
    }


    /** Read a set from ConfigManager (returns empty set if absent/bad). */
    public Set<Integer> readSet(String player, String key) {
        if (player == null || player.isEmpty()) return new LinkedHashSet<>();
        String raw = configManager.getConfiguration(GROUP, key + "." + player);
        if (raw == null || raw.isEmpty()) return new LinkedHashSet<>();
        try {
            Set<Integer> data = gson.fromJson(raw, SET_TYPE);
            return (data != null) ? new LinkedHashSet<>(data) : new LinkedHashSet<>();
        } catch (Exception e) {
            return new LinkedHashSet<>();
        }
    }


    /** Write a set to ConfigManager. */
    public void writeSet(String player, String key, Set<Integer> data) {
        if (player == null || player.isEmpty()) return;
        configManager.setConfiguration(GROUP, key + "." + player, gson.toJson(data));
    }
}