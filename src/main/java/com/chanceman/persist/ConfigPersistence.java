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
 * Uses stamped read/write: each set is stored with a last-write timestamp
 * to support last-writer-wins (LWW) reconciliation across machines.
 */
@Singleton
public final class ConfigPersistence
{
    private static final String GROUP = "chanceman";
    private static final Type SET_TYPE = new TypeToken<Set<Integer>>(){}.getType();

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public ConfigPersistence(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }

    private static String dataKey(String key, String player)
    {
        return key + "." + player + ".data";
    }

    private static String tsKey(String key, String player)
    {
        return key + "." + player + ".ts";
    }

    /** Value class for stamped set reads. */
    public static final class StampedSet
    {
        public final Set<Integer> data;
        public final long ts; // epoch millis; 0 means "unknown/not set"

        public StampedSet(Set<Integer> data, long ts)
        {
            this.data = (data != null) ? data : new LinkedHashSet<>();
            this.ts = ts;
        }
    }

    /**
     * Read a stamped set from ConfigManager.
     * Returns empty set + ts=0 if absent or malformed.
     */
    public StampedSet readStampedSet(String player, String key)
    {
        if (player == null || player.isEmpty())
        {
            return new StampedSet(new LinkedHashSet<>(), 0L);
        }

        String rawData = configManager.getConfiguration(GROUP, dataKey(key, player));
        String rawTs = configManager.getConfiguration(GROUP, tsKey(key, player));
        if (rawData == null || rawData.isEmpty() || rawTs == null || rawTs.isEmpty())
        {
            return new StampedSet(new LinkedHashSet<>(), 0L);
        }

        try
        {
            Set<Integer> data = gson.fromJson(rawData, SET_TYPE);
            long ts = Long.parseLong(rawTs);
            return new StampedSet((data != null) ? new LinkedHashSet<>(data) : new LinkedHashSet<>(), ts);
        }
        catch (Exception e)
        {
            return new StampedSet(new LinkedHashSet<>(), 0L);
        }
    }

    /**
     * Write a stamped set to ConfigManager.
     *
     * @param timestampMillis epoch millis representing the authoritative write time
     */
    public void writeStampedSet(String player, String key, Set<Integer> data, long timestampMillis)
    {
        if (player == null || player.isEmpty())
        {
            return;
        }

        String dataJson = gson.toJson((data != null) ? data : new LinkedHashSet<>());
        String tsStr = String.valueOf(Math.max(0L, timestampMillis));

        configManager.setConfiguration(GROUP, dataKey(key, player), dataJson);
        configManager.setConfiguration(GROUP, tsKey(key, player), tsStr);
    }
}
