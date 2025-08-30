package com.chanceman.managers;

import com.chanceman.account.AccountManager;
import com.chanceman.persist.ConfigPersistence;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

/**
 * Manages the set of unlocked items with robust, atomic persistence and 10-file backup rotation.
 * Also mirrors the set into RuneLite's ConfigManager (via ConfigPersistence) for cross-machine sync
 * when RuneLite profile/cloud sync is enabled.
 */
@Slf4j
@Singleton
public class UnlockedItemsManager
{
    private static final int MAX_BACKUPS = 10;
    private static final String CFG_KEY = "unlocked";
    private static final Type SET_TYPE = new TypeToken<Set<Integer>>(){}.getType();

    private final Set<Integer> unlockedItems = Collections.synchronizedSet(new LinkedHashSet<>());

    @Inject private AccountManager accountManager;
    @Inject private Gson gson;                      // reuse injected singleton; do NOT new Gson()
    @Inject private ConfigPersistence configPersistence;
    @Setter private ExecutorService executor;

    // Debounce config writes + warn-once
    private volatile long lastConfigWriteMs = 0L;
    private volatile boolean configWriteWarned = false;

    public boolean ready()
    {
        return accountManager.getPlayerName() != null;
    }

    /**
     * Atomically moves source→target, but if ATOMIC_MOVE fails retries a normal move with REPLACE_EXISTING.
     */
    private void safeMove(Path source, Path target, CopyOption... opts) throws IOException
    {
        try
        {
            Files.move(source, target, opts);
        }
        catch (AtomicMoveNotSupportedException | AccessDeniedException ex)
        {
            // retry without ATOMIC_MOVE but with REPLACE_EXISTING
            Set<CopyOption> fallback = new HashSet<>(Arrays.asList(opts));
            fallback.remove(StandardCopyOption.ATOMIC_MOVE);
            fallback.add(StandardCopyOption.REPLACE_EXISTING);
            Files.move(source, target, fallback.toArray(new CopyOption[0]));
        }
    }

    private Path getFilePath() throws IOException
    {
        String name = accountManager.getPlayerName();
        if (name == null)
        {
            throw new IOException("Player name is null");
        }
        Path dir = RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(name);
        Files.createDirectories(dir);
        return dir.resolve("chanceman_unlocked.json");
    }

    public boolean isUnlocked(int itemId)
    {
        return unlockedItems.contains(itemId);
    }

    public void unlockItem(int itemId)
    {
        if (unlockedItems.add(itemId))
        {
            saveUnlockedItems();
        }
    }

    /**
     * Load unlocked items from disk, then union with any config-stored set.
     * Avoids writing an empty file unless it's truly first-time usage.
     */
    public void loadUnlockedItems()
    {
        if (!ready())
        {
            return;
        }

        unlockedItems.clear();
        Path file;
        try
        {
            file = getFilePath();
        }
        catch (IOException ioe)
        {
            return;
        }

        final boolean fileExisted = Files.exists(file);

        // Load local JSON if present
        if (fileExisted)
        {
            try (Reader r = Files.newBufferedReader(file))
            {
                Set<Integer> loaded = gson.fromJson(r, SET_TYPE);
                if (loaded != null)
                {
                    unlockedItems.addAll(loaded);
                }
            }
            catch (IOException e)
            {
                log.error("Error loading unlocked items", e);
            }
        }

        // Merge from ConfigManager
        String player = accountManager.getPlayerName();
        int beforeMerge = unlockedItems.size();
        Set<Integer> fromCfg = configPersistence.readSet(player, CFG_KEY);
        unlockedItems.addAll(fromCfg);
        int afterMerge = unlockedItems.size();

        if (fileExisted)
        {
            if (afterMerge > beforeMerge)
            {
                saveUnlockedItems();
            }
        }
        else
        {
            if (afterMerge > 0)
            {
                saveUnlockedItems();
            }
            else
            {
                // truly first-time user
                saveUnlockedItems();
            }
        }
    }

    /**
     * Save to disk (atomic + backups), then mirror to ConfigManager (debounced).
     */
    public void saveUnlockedItems()
    {
        executor.submit(() ->
        {
            Path file;
            try
            {
                file = getFilePath();
            }
            catch (IOException ioe)
            {
                log.error("Could not resolve file path", ioe);
                return;
            }

            try
            {
                // 1) rotate .json → .bak
                if (Files.exists(file))
                {
                    Path backups = file.getParent().resolve("backups");
                    Files.createDirectories(backups);
                    String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                    Path bak = backups.resolve(file.getFileName() + "." + ts + ".bak");
                    safeMove(file, bak,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                    // prune older backups…
                    Files.list(backups)
                            .filter(p -> p.getFileName().toString().startsWith(file.getFileName() + "."))
                            .sorted(Comparator.comparing(Path::getFileName).reversed())
                            .skip(MAX_BACKUPS)
                            .forEach(p -> p.toFile().delete());
                }

                // 2) write new JSON to .tmp
                Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
                try (BufferedWriter w = Files.newBufferedWriter(tmp))
                {
                    gson.toJson(unlockedItems, w);
                }

                // 3) atomically replace .json with .tmp
                safeMove(tmp, file, StandardCopyOption.ATOMIC_MOVE);

                // 4) mirror to ConfigManager (debounced)
                mirrorToConfigDebounced();
            }
            catch (IOException e)
            {
                log.error("Error saving unlocked items", e);
            }
        });
    }

    private void mirrorToConfigDebounced()
    {
        long now = System.currentTimeMillis();
        if (now - lastConfigWriteMs < 3000) return; // ~3s debounce
        lastConfigWriteMs = now;

        String player = accountManager.getPlayerName();
        if (player == null || player.isEmpty()) return;

        Set<Integer> snapshot = new LinkedHashSet<>(unlockedItems);
        executor.submit(() -> {
            try
            {
                configPersistence.writeSet(player, CFG_KEY, snapshot);
            }
            catch (Exception e)
            {
                if (!configWriteWarned)
                {
                    configWriteWarned = true;
                    log.warn("ChanceMan: failed to mirror unlocked set to ConfigManager (local saves are intact).", e);
                }
            }
        });
    }

    /**
     * Retrieves an unmodifiable set of unlocked item IDs.
     *
     * @return An unmodifiable set of unlocked item IDs.
     */
    public Set<Integer> getUnlockedItems()
    {
        return Collections.unmodifiableSet(unlockedItems);
    }
}
