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
 * Uses last-writer-wins (LWW) with stamped cloud storage:
 *  - On load: compare local file mtime vs cloud ts; pick the newer as authoritative.
 *  - On save: write disk atomically, then mirror to stamped cloud (debounced for regular saves).
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

    // Debounce config writes for normal in-session updates
    private volatile long lastConfigWriteMs = 0L;
    private volatile boolean configWriteWarned = false;
    // Track if there are unsaved in-memory changes
    private volatile boolean dirty = false;

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
            dirty = true;
            saveUnlockedItems(); // normal path (debounced cloud mirror)
        }
    }

    /**
     * Load unlocked items using stamped LWW:
     *  - Read local file (+mtime) and cloud stamped set (+ts).
     *  - If local mtime > cloud ts → local wins (push local to cloud).
     *  - If cloud ts > local mtime → cloud wins (write to local).
     *  - If equal/unknown → prefer local; create file if missing.
     */
    public void loadUnlockedItems()
    {
        if (!ready())
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
                log.error("Error loading unlocked items", e);
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
            // Local was modified later (manual edits welcome) → push local to cloud
            winner = local;
            winnerStamp = localMtime;
            needPersist = true;
        }
        else if (cloudTs > localMtime)
        {
            // Cloud is newer → adopt cloud locally
            winner = cloud;
            winnerStamp = cloudTs;
            needPersist = true; // write to disk to reflect cloud
        }
        else
        {
            // Equal or unknown (ts=0). Prefer local state; if no file existed, create it.
            winner = local;
            needPersist = !fileExisted; // create local file if missing
            // If both sides are empty and no file existed, still create an empty file once
            if (!fileExisted && winner.isEmpty())
            {
                needPersist = true;
            }
        }

        // Apply to memory
        unlockedItems.clear();
        unlockedItems.addAll(winner);

        // Persist if needed
        if (needPersist)
        {
            // When reconciling at load time, bypass debounce to ensure immediate cloud accuracy.
            long stampToUse = (winnerStamp != null) ? winnerStamp : System.currentTimeMillis();
            saveUnlockedItemsWithExplicitStamp(stampToUse);
            dirty = false; // reconciled and persisted
        }
        else
        {
            dirty = false; // clean after no-op load
        }
    }

    /**
     * Save to disk (atomic + backups), then mirror to stamped ConfigManager (debounced).
     * Normal path for in-session changes (e.g., unlockItem).
     */
    public void saveUnlockedItems()
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

                // 4) mirror to ConfigManager (debounced) with current time
                mirrorToConfigDebounced(now);

                dirty = false;
            }
            catch (IOException e)
            {
                log.error("Error saving unlocked items", e);
            }
        });
    }

    /**
     * Save + mirror immediately with an explicit stamp (bypasses debounce).
     * Used during load-time reconciliation to push/pull authoritative state right away.
     */
    private void saveUnlockedItemsWithExplicitStamp(long stampMillis)
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

                // 4) mirror to ConfigManager immediately with stamp
                String player = accountManager.getPlayerName();
                if (player != null && !player.isEmpty())
                {
                    try
                    {
                        Set<Integer> snapshot = new LinkedHashSet<>(unlockedItems);
                        configPersistence.writeStampedSet(player, CFG_KEY, snapshot, stampMillis);
                    }
                    catch (Exception e)
                    {
                        if (!configWriteWarned)
                        {
                            configWriteWarned = true;
                            log.warn("ChanceMan: failed to mirror unlocked set to ConfigManager (local saves are intact).", e);
                        }
                    }
                }

                dirty = false;
            }
            catch (IOException e)
            {
                log.error("Error saving unlocked items", e);
            }
        });
    }

    private void mirrorToConfigDebounced(long stampMillis)
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
                configPersistence.writeStampedSet(player, CFG_KEY, snapshot, stampMillis);
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
            log.error("Could not resolve file path during shutdown flush", ioe);
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

            // 4) mirror immediately with current time (no debounce)
            String player = accountManager.getPlayerName();
            if (player != null && !player.isEmpty())
            {
                Set<Integer> snapshot = new LinkedHashSet<>(unlockedItems);
                configPersistence.writeStampedSet(player, CFG_KEY, snapshot, System.currentTimeMillis());
            }

            dirty = false;
        }
        catch (IOException e)
        {
            log.error("Shutdown flush failed for unlocked items (local saves may be stale).", e);
        }
        catch (Exception e)
        {
            log.warn("Shutdown flush: failed to mirror unlocked set to ConfigManager.", e);
        }
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
