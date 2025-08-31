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
 * Manages the set of rolled items with atomic persistence and 10-file backup rotation.
 * Uses last-writer-wins (LWW) stamped cloud sync:
 *  - On load: compare local file mtime vs cloud ts and choose the newer.
 *  - On save: write to disk atomically, then mirror stamped data to cloud (debounced for normal saves).
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
    // Track if there are unsaved in-memory changes
    private volatile boolean dirty = false;

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
     * Marks an item as rolled and triggers an asynchronous save (debounced cloud mirror).
     *
     * @param itemId The item ID to mark as rolled.
     */
    public void markRolled(int itemId)
    {
        if (rolledItems.add(itemId))
        {
            dirty = true;
            saveRolledItems();
        }
    }

    /**
     * Loads the set of rolled items using stamped LWW reconciliation:
     *  - Read local file (+mtime) and cloud stamped set (+ts).
     *  - If local mtime > cloud ts → local wins (push local to cloud).
     *  - If cloud ts > local mtime → cloud wins (write to local).
     *  - If equal/unknown → prefer local; create file if missing.
     */
    public void loadRolledItems()
    {
        if (accountManager.getPlayerName() == null)
        {
            return;
        }

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
        Set<Integer> local = new LinkedHashSet<>();
        if (fileExisted)
        {
            try (Reader r = Files.newBufferedReader(file))
            {
                Set<Integer> loaded = gson.fromJson(r, SET_TYPE);
                if (loaded != null)
                {
                    local.addAll(loaded);
                }
            }
            catch (IOException e)
            {
                log.error("Error loading rolled items", e);
            }
        }

        long localMtime = 0L;
        if (fileExisted)
        {
            try
            {
                localMtime = Files.getLastModifiedTime(file).toMillis();
            }
            catch (IOException ignored)
            {
                localMtime = 0L;
            }
        }

        // Load cloud (stamped)
        String player = accountManager.getPlayerName();
        ConfigPersistence.StampedSet cloudStamped = configPersistence.readStampedSet(player, CFG_KEY);
        Set<Integer> cloud = new LinkedHashSet<>(cloudStamped.data);
        long cloudTs = cloudStamped.ts;

        // Decide winner (LWW)
        Set<Integer> winner;
        Long winnerStamp = null; // stamp to write to cloud if we persist
        boolean needPersist = false;

        if (localMtime > cloudTs)
        {
            // Local modified later (manual edits welcome) → push local to cloud
            winner = local;
            winnerStamp = localMtime;
            needPersist = true;
        }
        else if (cloudTs > localMtime)
        {
            // Cloud newer → adopt cloud locally
            winner = cloud;
            winnerStamp = cloudTs;
            needPersist = true; // write to disk to reflect cloud
        }
        else
        {
            // Equal or unknown (ts=0). Prefer local state; if no file existed, create it.
            winner = local;
            needPersist = !fileExisted;
            if (!fileExisted && winner.isEmpty())
            {
                needPersist = true; // create an empty file once
            }
        }

        // Apply to memory
        rolledItems.clear();
        rolledItems.addAll(winner);

        // Persist if needed (bypass debounce during reconciliation)
        if (needPersist)
        {
            long stampToUse = (winnerStamp != null) ? winnerStamp : System.currentTimeMillis();
            saveRolledItemsWithExplicitStamp(stampToUse);
            dirty = false; // reconciled and persisted
        }
        else
        {
            dirty = false; // clean after no-op load
        }
    }

    /**
     * Saves the current set of rolled items to disk.
     * Uses a temporary file and backups for atomicity and data safety.
     * Mirrors into stamped ConfigManager (debounced).
     */
    public void saveRolledItems()
    {
        final long now = System.currentTimeMillis();
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

                // 4) mirror to ConfigManager (debounced) with current time
                mirrorToConfigDebounced(now);

                dirty = false;
            }
            catch (IOException e)
            {
                log.error("Error saving rolled items", e);
            }
        });
    }

    /**
     * Save + mirror immediately with an explicit stamp (bypasses debounce).
     * Used during load-time reconciliation to push/pull authoritative state right away.
     */
    private void saveRolledItemsWithExplicitStamp(long stampMillis)
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

                // 4) mirror to ConfigManager immediately with explicit stamp
                String player = accountManager.getPlayerName();
                if (player != null && !player.isEmpty())
                {
                    try
                    {
                        Set<Integer> snapshot = new LinkedHashSet<>(rolledItems);
                        configPersistence.writeStampedSet(player, CFG_KEY, snapshot, stampMillis);
                    }
                    catch (Exception e)
                    {
                        if (!configWriteWarned)
                        {
                            configWriteWarned = true;
                            log.warn("ChanceMan: failed to mirror rolled set to ConfigManager (local saves are intact).", e);
                        }
                    }
                }

                dirty = false; // explicit flush completed
            }
            catch (IOException e)
            {
                log.error("Error saving rolled items", e);
            }
        });
    }

    private void mirrorToConfigDebounced(long stampMillis)
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
                configPersistence.writeStampedSet(player, CFG_KEY, snapshot, stampMillis);
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
     * Flush synchronously on shutdown if there are unsaved changes.
     * Bypasses executor and debounce; writes disk + cloud with current time.
     */
    public void flushIfDirtyOnExit()
    {
        if (!dirty) return;

        Path file;
        try
        {
            file = getFilePath();
        }
        catch (IOException ioe)
        {
            log.error("Could not resolve rolled-items path during shutdown flush", ioe);
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

            // 4) mirror immediately with current time (no debounce)
            String player = accountManager.getPlayerName();
            if (player != null && !player.isEmpty())
            {
                Set<Integer> snapshot = new LinkedHashSet<>(rolledItems);
                configPersistence.writeStampedSet(player, CFG_KEY, snapshot, System.currentTimeMillis());
            }

            dirty = false;
        }
        catch (IOException e)
        {
            log.error("Shutdown flush failed for rolled items (local saves may be stale).", e);
        }
        catch (Exception e)
        {
            log.warn("Shutdown flush: failed to mirror rolled set to ConfigManager.", e);
        }
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