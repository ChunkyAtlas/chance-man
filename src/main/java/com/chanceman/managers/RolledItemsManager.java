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
 * Manages the set of rolled items with atomic persistence.
 * Provides thread-safe operations for marking items as rolled,
 * loading from and saving to disk with backups and atomic moves.
 * Also mirrors state into RuneLite's ConfigManager (via ConfigPersistence) so, if the user
 * has profile/cloud sync enabled, progress follows them across machines.
 */
@Slf4j
@Singleton
public class RolledItemsManager
{
    private static final int MAX_BACKUPS = 10;
    private static final String CFG_KEY = "rolled";
    private static final Type SET_TYPE = new TypeToken<Set<Integer>>(){}.getType();

    private final Set<Integer> rolledItems = Collections.synchronizedSet(new LinkedHashSet<>());

    @Inject private AccountManager accountManager;
    @Inject private Gson gson;
    @Inject private ConfigPersistence configPersistence;
    @Setter private ExecutorService executor;

    // Debounce ConfigManager writes to avoid churn
    private volatile long lastConfigWriteMs = 0L;
    // Only warn once if config mirroring fails
    private volatile boolean configWriteWarned = false;

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
            // remove ATOMIC_MOVE, add REPLACE_EXISTING
            Set<CopyOption> fallback = new HashSet<>(Arrays.asList(opts));
            fallback.remove(StandardCopyOption.ATOMIC_MOVE);
            fallback.add(StandardCopyOption.REPLACE_EXISTING);
            Files.move(source, target, fallback.toArray(new CopyOption[0]));
        }
    }

    /**
     * Builds the file path for the current account's rolled-items JSON file.
     *
     * @return path to the rolled items JSON file
     */
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
        return dir.resolve("chanceman_rolled.json");
    }

    /**
     * Checks if an item has been rolled.
     *
     * @param itemId The item ID.
     * @return true if the item has been rolled, false otherwise.
     */
    public boolean isRolled(int itemId)
    {
        return rolledItems.contains(itemId);
    }

    /**
     * Marks an item as rolled and triggers an asynchronous save.
     *
     * @param itemId The item ID to mark as rolled.
     */
    public void markRolled(int itemId)
    {
        if (rolledItems.add(itemId))
        {
            saveRolledItems();
        }
    }

    /**
     * Loads the set of rolled items from disk into memory.
     * If the file does not exist, avoid writing an empty file up-front:
     *  - read local if present
     *  - merge from config
     *  - save only if the merged set is non-empty, OR if it's truly first-time (both sources empty).
     */
    public void loadRolledItems()
    {
        if (accountManager.getPlayerName() == null)
        {
            return;
        }

        rolledItems.clear();
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
                    rolledItems.addAll(loaded);
                }
            }
            catch (IOException e)
            {
                log.error("Error loading rolled items", e);
            }
        }

        // Merge from ConfigManager (always)
        String player = accountManager.getPlayerName();
        int beforeMerge = rolledItems.size();
        Set<Integer> fromCfg = configPersistence.readSet(player, CFG_KEY);
        rolledItems.addAll(fromCfg);
        int afterMerge = rolledItems.size();

        if (fileExisted)
        {
            if (afterMerge > beforeMerge)
            {
                saveRolledItems();
            }
        }
        else
        {
            if (afterMerge > 0)
            {
                // New machine but had cloud/config data → create local file with merged contents
                saveRolledItems();
            }
            else
            {
                // Truly first-time user: create an empty local JSON once
                saveRolledItems();
            }
        }
    }

    /**
     * Saves the current set of rolled items to disk.
     * Uses a temporary file and backups for atomicity and data safety.
     * Also mirrors into ConfigManager (debounced).
     */
    public void saveRolledItems()
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
                log.error("Could not resolve rolled-items path", ioe);
                return;
            }

            try
            {
                // 1) backup current .json
                if (Files.exists(file))
                {
                    Path backups = file.getParent().resolve("backups");
                    Files.createDirectories(backups);
                    String ts = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                    Path bak = backups.resolve(file.getFileName() + "." + ts + ".bak");
                    safeMove(file, bak,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                    // prune old backups…
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
                    gson.toJson(rolledItems, w);
                }

                // 3) atomically replace .json
                safeMove(tmp, file, StandardCopyOption.ATOMIC_MOVE);

                // 4) mirror to ConfigManager (debounced)
                mirrorToConfigDebounced();
            }
            catch (IOException e)
            {
                log.error("Error saving rolled items", e);
            }
        });
    }

    private void mirrorToConfigDebounced()
    {
        long now = System.currentTimeMillis();
        if (now - lastConfigWriteMs < 3000) return; // 3s debounce
        lastConfigWriteMs = now;

        String player = accountManager.getPlayerName();
        if (player == null || player.isEmpty()) return;

        // snapshot to avoid concurrent modification on the synchronized set
        Set<Integer> snapshot = new LinkedHashSet<>(rolledItems);
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
                    log.warn("ChanceMan: failed to mirror rolled set to ConfigManager (local saves are intact).", e);
                }
            }
        });
    }

    /**
     * Retrieves an unmodifiable set of rolled item IDs.
     *
     * @return An unmodifiable set of rolled item IDs.
     */
    public Set<Integer> getRolledItems()
    {
        return Collections.unmodifiableSet(rolledItems);
    }
}