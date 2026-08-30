package com.chanceman.relational;

import com.chanceman.account.AccountManager;
import com.chanceman.persist.ConfigPersistence;
import com.google.gson.Gson;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;

/** Account-scoped persistence for relational roll counters. */
@Slf4j
@Singleton
public final class RelationalRollStateManager
{
    private static final String CLOUD_KEY = "relationalState";

    @Inject private AccountManager accountManager;
    @Inject private Gson gson;
    @Inject private ConfigPersistence configPersistence;
    @Setter private ExecutorService executor;
    @Setter private Runnable onChange;

    private RelationalRollState state = new RelationalRollState();
    private volatile boolean dirty;

    public synchronized RelationalRollState getState()
    {
        return state;
    }

    public synchronized void recordRoll(boolean toolRoll, boolean questRoll)
    {
        state = state.afterRoll(toolRoll, questRoll);
        dirty = true;
        notifyChanged();
        saveState();
    }

    public synchronized void resetState()
    {
        state = new RelationalRollState();
        dirty = false;
        notifyChanged();
    }

    public synchronized void loadState()
    {
        String player = accountManager.getPlayerName();
        if (player == null)
        {
            resetState();
            return;
        }

        ConfigPersistence.StampedValue cloud = readCloud(player);
        state = readJson(cloud.data);
        dirty = false;
        notifyChanged();
    }

    public synchronized void saveState()
    {
        saveInternal(System.currentTimeMillis());
    }

    public synchronized void flushIfDirtyOnExit()
    {
        if (!dirty) return;
        String player = accountManager.getPlayerName();
        if (player == null) return;
        writeCloud(player, state, System.currentTimeMillis());
        dirty = false;
    }

    private void saveInternal(long timestamp)
    {
        if (executor == null)
        {
            String player = accountManager.getPlayerName();
            if (player != null) writeCloud(player, state, timestamp);
            dirty = false;
            return;
        }

        final RelationalRollState snapshot = state;
        final String player = accountManager.getPlayerName();
        if (player == null) return;
        executor.submit(() -> {
            writeCloud(player, snapshot, timestamp);
            dirty = false;
        });
    }

    private RelationalRollState readJson(String json)
    {
        if (json == null || json.isEmpty()) return new RelationalRollState();
        try
        {
            RelationalRollState loaded = gson.fromJson(json, RelationalRollState.class);
            return loaded != null ? loaded : new RelationalRollState();
        }
        catch (Exception ignored) { return new RelationalRollState(); }
    }

    private ConfigPersistence.StampedValue readCloud(String player)
    {
        try { return configPersistence.readStampedValue(player, CLOUD_KEY); }
        catch (Exception ignored) { return new ConfigPersistence.StampedValue("", 0L); }
    }

    private void writeCloud(String player, RelationalRollState value, long timestamp)
    {
        try
        {
            configPersistence.writeStampedValueIfNewer(player, CLOUD_KEY, gson.toJson(value), timestamp);
        }
        catch (Exception e)
        {
            log.warn("Could not mirror relational roll state", e);
        }
    }

    private void notifyChanged()
    {
        if (onChange != null) onChange.run();
    }
}
