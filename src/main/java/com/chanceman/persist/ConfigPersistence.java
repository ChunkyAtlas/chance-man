package com.chanceman.persist;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public final class ConfigPersistence
{
    private static final String GROUP = "chanceman";
    private static final String DATA_SUFFIX = ".data";
    private static final String TS_SUFFIX = ".ts";
    private static final Type SET_TYPE = new TypeToken<Set<Integer>>(){}.getType();

    private final ConfigManager configManager;
    private final Gson gson;

    @Inject
    public ConfigPersistence(ConfigManager configManager, Gson gson)
    {
        this.configManager = configManager;
        this.gson = gson;
    }
    private static String dataKey(String key, String player) { return key + "." + player + DATA_SUFFIX; }
    private static String tsKey(String key, String player)   { return key + "." + player + TS_SUFFIX; }
    /** Value class for stamped set reads. */
    public static final class StampedSet
    {
        public final Set<Integer> data;
        public final long ts; // epoch millis; 0 means "unknown/not set"

        public StampedSet(Set<Integer> data, long ts)
        {
            this.data = (data != null) ? new LinkedHashSet<>(data) : new LinkedHashSet<>();
            this.ts = Math.max(0L, ts);
        }
    }

    /** Value class for stamped JSON reads used by non-set plugin state. */
    public static final class StampedValue
    {
        public final String data;
        public final long ts;

        public StampedValue(String data, long ts)
        {
            this.data = (data != null) ? data : "";
            this.ts = Math.max(0L, ts);
        }
    }

    public StampedValue readStampedValue(String player, String key)
    {
        if (isBlank(player) || isBlank(key)) return new StampedValue("", 0L);

        String rawData = configManager.getConfiguration(GROUP, dataKey(key, player));
        String rawTs = configManager.getConfiguration(GROUP, tsKey(key, player));
        if (isBlank(rawData) || isBlank(rawTs)) return new StampedValue("", 0L);
        return new StampedValue(rawData, parseLongSafe(rawTs));
    }

    public void writeStampedValue(String player, String key, String data, long timestampMillis)
    {
        if (isBlank(player) || isBlank(key)) return;
        configManager.setConfiguration(GROUP, dataKey(key, player), data != null ? data : "");
        configManager.setConfiguration(GROUP, tsKey(key, player), String.valueOf(Math.max(0L, timestampMillis)));
    }

    public boolean writeStampedValueIfNewer(String player, String key, String data, long timestampMillis)
    {
        if (isBlank(player) || isBlank(key)) return false;
        long existingTs = parseLongSafe(configManager.getConfiguration(GROUP, tsKey(key, player)));
        if (timestampMillis < existingTs) return false;
        writeStampedValue(player, key, data, timestampMillis);
        return true;
    }

    /**
     * Read a stamped set from ConfigManager.
     * Returns empty set + ts=0 if absent or malformed.
     */
    public StampedSet readStampedSet(String player, String key)
    {
        StampedValue stamped = readStampedValue(player, key);
        if (isBlank(stamped.data)) return new StampedSet(new LinkedHashSet<>(), 0L);
        try
        {
            Set<Integer> parsed = gson.fromJson(stamped.data, SET_TYPE);
            return new StampedSet((parsed != null) ? parsed : new LinkedHashSet<>(), stamped.ts);
        }
        catch (Exception ignored)
        {
            return new StampedSet(new LinkedHashSet<>(), 0L);
        }
    }

    /**
     * Write a stamped set to ConfigManager (unconditional).
     *
     * @param timestampMillis epoch millis representing the authoritative write time
     */
    public void writeStampedSet(String player, String key, Set<Integer> data, long timestampMillis)
    {
        if (isBlank(player) || isBlank(key)) return;

        writeStampedValue(player, key, gson.toJson((data != null) ? data : new LinkedHashSet<>()), timestampMillis);
    }

    /**
     * Write only if the provided timestamp is >= the currently stored timestamp.
     * This is a simple guard against out-of-order writes when multiple machines
     * or threads may be updating the cloud state.
     *
     * @return true if a write occurred, false if skipped
     */
    public boolean writeStampedSetIfNewer(String player, String key, Set<Integer> data, long timestampMillis)
    {
        if (isBlank(player) || isBlank(key)) return false;

        return writeStampedValueIfNewer(
                player,
                key,
                gson.toJson((data != null) ? data : new LinkedHashSet<>()),
                timestampMillis);
    }

    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }

    private static long parseLongSafe(String s)
    {
        if (isBlank(s)) return 0L;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return 0L; }
    }
}
