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

@Slf4j
@Singleton
public class UnlockedItemsManager
{
    private static final int MAX_BACKUPS = 10;
    private static final String CFG_KEY = "unlocked";
    private static final String BACKUP_TS_PATTERN = "yyyyMMddHHmmss";
    private static final long CONFIG_DEBOUNCE_MS = 3000L;
    private static final long SELF_WRITE_GRACE_MS = 1500L;
    private static final long FS_DEBOUNCE_MS = 200L;
    private static final String FILE_NAME = "chanceman_unlocked.json";
    private static final Type SET_TYPE = new TypeToken<Set<Integer>>(){}.getType();
    private final Set<Integer> unlockedItems = Collections.synchronizedSet(new LinkedHashSet<>());
    @Inject private AccountManager accountManager;
    @Inject private Gson gson;
    @Inject private ConfigPersistence configPersistence;
    @Setter private ExecutorService executor; // file writes & cloud mirror
    @Setter private Runnable onChange;
    private volatile long lastConfigWriteMs = 0L;
    private volatile boolean configWriteWarned = false;
    private volatile boolean dirty = false;
    private WatchService watchService;
    private volatile boolean watcherRunning = false;
    private volatile long lastSelfWriteMs = 0L;
    private Thread watcherThread;

    public boolean ready() { return accountManager.getPlayerName() != null; }
    public boolean isUnlocked(int itemId) { return unlockedItems.contains(itemId); }
    public Set<Integer> getUnlockedItems() { return Collections.unmodifiableSet(unlockedItems); }

    public void unlockItem(int itemId)
    {
        if (unlockedItems.add(itemId))
        {
            dirty = true;
            saveUnlockedItems();
            safeNotifyChange();
        }
    }

    public void loadUnlockedItems()
    {
        reconcileWithCloud(false);
        safeNotifyChange();
    }

    /** Normal save: disk + debounced cloud with current time. */
    public void saveUnlockedItems()
    {
        saveInternal(System.currentTimeMillis(), true);
    }

    /** Live-reload: start watching the JSON for CREATE/MODIFY/DELETE. */
    public void startWatching()
    {
        if (watcherRunning) return;
        Path file = safeGetFilePathOrNull();
        if (file == null) return;

        try
        {
            watchService = FileSystems.getDefault().newWatchService();
            file.getParent().register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE
            );
        }
        catch (IOException e)
        {
            closeWatchServiceQuietly();
            log.error("Unlocked watcher: could not register", e);
            return;
        }

        watcherRunning = true;
        final String target = file.getFileName().toString();

        watcherThread = new Thread(() -> runWatcherLoop(target), "ChanceMan-Unlocked-Watcher");
        watcherThread.setDaemon(true);
        watcherThread.start();
    }

    public void stopWatching()
    {
        watcherRunning = false;
        if (watcherThread != null) watcherThread.interrupt();
        closeWatchServiceQuietly();
        watcherThread = null;
    }

    /** Flush synchronously on shutdown if dirty. */
    public void flushIfDirtyOnExit()
    {
        if (!dirty) return;
        Path file = safeGetFilePathOrNull();
        if (file == null) return;

        try
        {
            rotateBackupIfExists(file);
            writeJsonAtomic(file, unlockedItems);
            mirrorToCloud(System.currentTimeMillis(), false);
            dirty = false;
        }
        catch (IOException e)
        {
            log.error("Shutdown flush failed for unlocked items (local saves may be stale).", e);
        }
        catch (Exception e)
        {
            log.error("Shutdown flush: failed to mirror unlocked set to ConfigManager.", e);
        }
    }

    private void reconcileWithCloud(boolean runtime)
    {
        String player = accountManager.getPlayerName();
        if (player == null) return;

        Path file = safeGetFilePathOrNull();
        if (file == null) return;

        boolean fileExisted = Files.exists(file);
        Set<Integer> local = readLocalJson(file);
        long localMtime = fileExisted ? safeLastModified(file) : 0L;

        ConfigPersistence.StampedSet cloudStamped = readCloud(player);
        Set<Integer> cloud = new LinkedHashSet<>(cloudStamped.data);
        long cloudTs = cloudStamped.ts;

        // LWW
        Set<Integer> winner;
        Long winnerStamp = null;
        boolean needPersist;

        if (localMtime > cloudTs) { winner = local;  winnerStamp = localMtime; needPersist = true; } // push to cloud
        else if (cloudTs > localMtime) { winner = cloud;  winnerStamp = cloudTs;    needPersist = true; } // pull to disk
        else { winner = local;  needPersist = !fileExisted; } // create disk if missing

        unlockedItems.clear();
        unlockedItems.addAll(winner);

        if (needPersist)
        {
            long stamp = (winnerStamp != null) ? winnerStamp : System.currentTimeMillis();
            saveInternal(stamp, false); // bypass debounce during reconcile
        }
        dirty = false;
    }

    /** Disk write + cloud mirror (debounced or immediate). */
    private void saveInternal(long stampMillis, boolean debounced)
    {
        if (!isExecutorAvailable())
        {
            log.error("UnlockedItemsManager: executor unavailable; skipping save");
            return;
        }

        executor.submit(() -> {
            Path file = safeGetFilePathOrNull();
            if (file == null)
            {
                log.error("UnlockedItemsManager: file path unavailable; skipping save");
                return;
            }
            try
            {
                rotateBackupIfExists(file);
                writeJsonAtomic(file, unlockedItems);
                mirrorToCloud(stampMillis, debounced);
                dirty = false;
            }
            catch (IOException e)
            {
                log.error("Error saving unlocked items", e);
            }
        });
    }

    private void mirrorToCloud(long stampMillis, boolean debounced)
    {
        long now = System.currentTimeMillis();
        if (debounced && (now - lastConfigWriteMs < CONFIG_DEBOUNCE_MS)) return;
        lastConfigWriteMs = now;

        String player = accountManager.getPlayerName();
        if (player == null || player.isEmpty() || executor == null) return;

        Set<Integer> snapshot = new LinkedHashSet<>(unlockedItems);
        executor.submit(() -> {
            try
            {
                // Guard against out-of-order writes across machines/threads
                configPersistence.writeStampedSetIfNewer(player, CFG_KEY, snapshot, stampMillis);
            }
            catch (Exception e)
            {
                if (!configWriteWarned)
                {
                    configWriteWarned = true;
                    log.error("ChanceMan: failed to mirror unlocked set to ConfigManager (local saves intact).", e);
                }
            }
        });
    }

    private boolean isExecutorAvailable()
    {
        if (executor == null) return false;
        if (executor instanceof java.util.concurrent.ThreadPoolExecutor) {
            java.util.concurrent.ThreadPoolExecutor tpe =
                    (java.util.concurrent.ThreadPoolExecutor) executor;
            return !tpe.isShutdown() && !tpe.isTerminated();
        }
        return true;
    }

    private void safeNotifyChange()
    {
        Runnable cb = onChange;
        if (cb != null) { try { cb.run(); } catch (Throwable t) { log.error("onChange threw", t); } }
    }

    private Path getFilePath() throws IOException
    {
        String name = accountManager.getPlayerName();
        if (name == null) throw new IOException("Player name is null");
        Path dir = RUNELITE_DIR.toPath().resolve("chanceman").resolve(name);
        Files.createDirectories(dir);
        return dir.resolve(FILE_NAME);
    }

    private Path safeGetFilePathOrNull()
    {
        try { return getFilePath(); } catch (IOException ioe) { return null; }
    }

    /** Windows-safe: COPY current file to a timestamped backup with small retries; then prune. */
    private void rotateBackupIfExists(Path file) throws IOException
    {
        if (!Files.exists(file)) return;

        Path backups = file.getParent().resolve("backups");
        Files.createDirectories(backups);

        String ts = new SimpleDateFormat(BACKUP_TS_PATTERN).format(new Date());
        Path bak = backups.resolve(file.getFileName() + "." + ts + ".bak");

        final int maxAttempts = 5;
        for (int attempt = 1; ; attempt++)
        {
            try
            {
                Files.copy(file, bak, StandardCopyOption.REPLACE_EXISTING);
                break;
            }
            catch (FileSystemException fse)
            {
                if (attempt >= maxAttempts)
                {
                    log.error("Backup copy failed after {} attempts for {}", attempt, file, fse);
                    break; // give up on backup, continue save
                }
                try { Thread.sleep(50L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            }
        }

        try (java.util.stream.Stream<Path> stream = Files.list(backups))
        {
            stream
                    .filter(p -> p.getFileName().toString().startsWith(file.getFileName() + "."))
                    .sorted(Comparator.comparing(Path::getFileName).reversed())
                    .skip(MAX_BACKUPS)
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }
    }

    /** Write JSON to .tmp and atomically replace the main file; mark self-write for watcher echo suppression. */
    private void writeJsonAtomic(Path file, Set<Integer> data) throws IOException
    {
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try (BufferedWriter w = Files.newBufferedWriter(tmp))
        {
            gson.toJson(data, w);
        }
        safeMove(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        lastSelfWriteMs = System.currentTimeMillis(); // mark AFTER swap
    }

    private long safeLastModified(Path file)
    {
        try { return Files.getLastModifiedTime(file).toMillis(); } catch (IOException e) { return 0L; }
    }

    private Set<Integer> readLocalJson(Path file)
    {
        Set<Integer> local = new LinkedHashSet<>();
        if (!Files.exists(file)) return local;
        try (Reader r = Files.newBufferedReader(file))
        {
            Set<Integer> loaded = gson.fromJson(r, SET_TYPE);
            if (loaded != null) local.addAll(loaded);
        }
        catch (IOException e)
        {
            log.error("Error reading unlocked items JSON", e);
        }
        return local;
    }

    private ConfigPersistence.StampedSet readCloud(String player)
    {
        try { return configPersistence.readStampedSet(player, CFG_KEY); }
        catch (Exception e) { return new ConfigPersistence.StampedSet(new LinkedHashSet<>(), 0L); }
    }

    private void closeWatchServiceQuietly()
    {
        try { if (watchService != null) watchService.close(); } catch (IOException ignored) {}
        watchService = null;
    }

    private void runWatcherLoop(String target)
    {
        long lastHandled = 0L;
        try
        {
            while (watcherRunning)
            {
                WatchKey key;
                try { key = watchService.take(); }
                catch (InterruptedException | ClosedWatchServiceException ie) { break; }

                boolean relevant = key.pollEvents().stream().anyMatch(ev -> {
                    Object ctx = ev.context();
                    return (ctx instanceof Path) && ((Path) ctx).getFileName().toString().equals(target);
                });
                key.reset();
                if (!relevant) continue;

                long now = System.currentTimeMillis();
                if (now - lastSelfWriteMs <= SELF_WRITE_GRACE_MS) continue; // our own write
                if (now - lastHandled < FS_DEBOUNCE_MS) continue;
                lastHandled = now;

                try
                {
                    reconcileWithCloud(true);
                    safeNotifyChange();
                }
                catch (Throwable t)
                {
                    log.error("Unlocked watcher reconcile failed", t);
                }
            }
        }
        finally
        {
            closeWatchServiceQuietly();
            watcherRunning = false;
        }
    }

    /** Move with fallback when ATOMIC_MOVE not supported. */
    private void safeMove(Path source, Path target, CopyOption... opts) throws IOException
    {
        try
        {
            Files.move(source, target, opts);
        }
        catch (AtomicMoveNotSupportedException | AccessDeniedException ex)
        {
            Set<CopyOption> fallback = new HashSet<>(Arrays.asList(opts));
            fallback.remove(StandardCopyOption.ATOMIC_MOVE);
            fallback.add(StandardCopyOption.REPLACE_EXISTING);
            Files.move(source, target, fallback.toArray(new CopyOption[0]));
        }
    }
}
