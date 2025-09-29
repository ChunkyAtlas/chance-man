package com.chanceman.drops;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

import com.chanceman.account.AccountManager;
import com.google.gson.Gson;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persistent drop-table cache backed by JSON files in the user's RuneLite
 * directory. The cache is mirrored in memory to make name-based lookups and
 * searches effectively instantaneous.
 */
@Slf4j
@Singleton
public class DropCache
{
    private final Gson gson;
    private final AccountManager accountManager;
    private final DropFetcher dropFetcher;
    private static final Duration MAX_AGE = Duration.ofDays(7);
    private final Map<Path, Object> writeLocks = new ConcurrentHashMap<>();
    private final Map<Path, NpcDropData> cache = new ConcurrentHashMap<>();
    private final Map<String, Path> nameIndex = new ConcurrentHashMap<>();
    private volatile boolean indexLoaded = false;

    // Dedicated IO executor so we dont block the common ForkJoinPool with file ops
    private ExecutorService ioExecutor;

    @Inject
    public DropCache(Gson gson, AccountManager accountManager, DropFetcher dropFetcher)
    {
        this.gson = gson;
        this.accountManager = accountManager;
        this.dropFetcher = dropFetcher;
    }

    /** Preload on-disk index and prune stale cache entries. */
    public void startUp()
    {
        ensureExecutor();
        String player = accountManager.getPlayerName();
        if (player == null || player.isEmpty()) { return; }
        loadIndex();
        pruneOldCaches();
    }

    /**
     * Load from disk if possible; otherwise fetch from the wiki, write the
     * JSON, and return the data. Results without drop-table sections are
     * discarded and never cached.
     */
    public CompletableFuture<NpcDropData> get(int npcId, String name, int level)
    {
        loadIndex();
        final String safeName = name.replaceAll("[^A-Za-z0-9]", "_");
        final Path file;
        try
        {
            file = npcId == 0
                    ? findExistingCacheFile(safeName, level)
                    : getCacheFile(npcId, name, level);
        }
        catch (IOException ex)
        {
            log.error("Could not resolve cache file for {} ({}, lvl {})", npcId, name, level, ex);
            return CompletableFuture.failedFuture(ex);
        }

        ExecutorService executor = ensureExecutor();
        return CompletableFuture.supplyAsync(() ->
        {
            if (file != null)
            {
                NpcDropData cached = cache.get(file);
                if (cached != null && Files.exists(file) && isFresh(file))
                {
                    return cached;
                }

                // stale or missing entry, clean up
                try
                {
                    Files.deleteIfExists(file);
                }
                catch (IOException ignore) { }
                removeIndex(file);
            }
            return null;
        }, executor).thenComposeAsync(cached ->
        {
            if (cached != null)
            {
                return CompletableFuture.completedFuture(cached);
            }

            return dropFetcher.fetch(npcId, name, level)
                    .thenApplyAsync(data ->
                    {
                        try
                        {
                            if (data == null || data.getDropTableSections().isEmpty())
                            {
                                return null;
                            }

                            Path out = getCacheFile(data.getNpcId(), data.getName(), data.getLevel());
                            Files.createDirectories(out.getParent());
                            String json = gson.toJson(data);

                            Object lock = writeLocks.computeIfAbsent(out, p -> new Object());
                            synchronized (lock)
                            {
                                Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
                                try
                                {
                                    Files.writeString(
                                            tmp,
                                            json,
                                            StandardCharsets.UTF_8,
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.TRUNCATE_EXISTING
                                    );
                                    Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                                }
                                finally
                                {
                                    writeLocks.remove(out);
                                    try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
                                }
                            }

                            cache.put(out, data);
                            nameIndex.put(buildNameKey(data.getName(), data.getLevel()), out);

                            if (npcId == 0 && data.getNpcId() != 0)
                            {
                                // Remove old 0_id placeholder if present
                                Path old = findExistingCacheFile(safeName, data.getLevel());
                                if (old != null && !old.equals(out))
                                {
                                    Files.deleteIfExists(old);
                                    removeIndex(old);
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("Failed to write cache file for {}", name, e);
                        }
                        return data;
                    }, executor)
                    .exceptionally(ex ->
                    {
                        log.error("Error fetching drop data for NPC {}", npcId, ex);
                        return null;
                    });
        }, executor);
    }

    /**
     * @return a collection of all cached NPC drop data in memory
     */
    public Collection<NpcDropData> getAllNpcData()
    {
        loadIndex();
        return new ArrayList<>(cache.values());
    }

    /**
     * Return a list of NPC names containing the supplied query. Matches from
     * the local cache are combined with wiki search results to ensure partial
     * lookups surface all relevant NPCs.
     */
    public CompletableFuture<List<String>> searchNpcNames(String query)
    {
        ExecutorService executor = ensureExecutor();
        return CompletableFuture.supplyAsync(() ->
        {
            String lc = query.toLowerCase(Locale.ROOT).trim();
            loadIndex();

            // Preserve insertion order while de-duplicating names
            Set<String> names = cache.values().stream()
                    .map(NpcDropData::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase(Locale.ROOT).contains(lc))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            try
            {
                names.addAll(dropFetcher.searchNpcNames(query));
            }
            catch (Exception ex)
            {
                log.debug("Wiki search failed for {}", query, ex);
            }

            return new ArrayList<>(names);
        }, executor);
    }

    private boolean isFresh(Path file)
    {
        try
        {
            Instant cutoff = Instant.now().minus(MAX_AGE);
            return Files.getLastModifiedTime(file).toInstant().isAfter(cutoff);
        }
        catch (IOException e)
        {
            return false;
        }
    }

    /**
     * Locate an existing cache file by name and level regardless of stored ID.
     */
    private Path findExistingCacheFile(String safeName, int level) throws IOException
    {
        String key = safeName + "_" + level;
        Path p = nameIndex.get(key);
        if (p != null && Files.exists(p))
        {
            if (isFresh(p))
            {
                return p;
            }
            Files.deleteIfExists(p);
            removeIndex(p);
        }
        return null;
    }

    private Path getCacheDir() throws IOException
    {
        String player = accountManager.getPlayerName();
        if (player == null)
        {
            throw new IOException("Player name is not available");
        }
        return RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve("drops");
    }

    /** Resolve the on-disk cache path for a specific NPC. */
    private Path getCacheFile(int npcId, String name, int level) throws IOException
    {
        String safeName = name.replaceAll("[^A-Za-z0-9]", "_");
        Path dir = getCacheDir();
        Files.createDirectories(dir);
        String fn = npcId + "_" + safeName + "_" + level + ".json";
        return dir.resolve(fn);
    }

    /**
     * Deletes cached drop table files older than {@link #MAX_AGE} and purges
     * them from the in-memory index.
     */
    public void pruneOldCaches()
    {
        String player = accountManager.getPlayerName();
        if (player == null)
        {
            return;
        }

        Path dir = RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve("drops");

        if (!Files.exists(dir))
        {
            return;
        }

        Instant cutoff = Instant.now().minus(MAX_AGE);
        try (Stream<Path> files = Files.list(dir))
        {
            files.filter(Files::isRegularFile)
                    .forEach(p ->
                    {
                        try
                        {
                            Instant mod = Files.getLastModifiedTime(p).toInstant();
                            if (mod.isBefore(cutoff))
                            {
                                Files.deleteIfExists(p);
                                removeIndex(p);
                            }
                        }
                        catch (IOException ex)
                        {
                            log.debug("Failed to delete old drop cache {}", p, ex);
                        }
                    });
        }
        catch (IOException ex)
        {
            log.debug("Error pruning drop cache directory {}", dir, ex);
        }
    }

    /**
     * Deletes all cached drop table files for the current player and clears the
     * in-memory index.
     */
    public void clearAllCaches()
    {
        String player = accountManager.getPlayerName();
        if (player == null) return;

        Path dir = RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve("drops");

        if (Files.exists(dir))
        {
            try (Stream<Path> files = Files.list(dir))
            {
                files.filter(Files::isRegularFile)
                        .forEach(p ->
                        {
                            try
                            {
                                Files.deleteIfExists(p);
                            }
                            catch (IOException ex)
                            {
                                log.debug("Failed to delete drop cache {}", p, ex);
                            }
                        });
            }
            catch (IOException ex)
            {
                log.debug("Error clearing drop cache directory {}", dir, ex);
            }
        }

        cache.clear();
        nameIndex.clear();
        indexLoaded = true;
    }

    /** Remove the given file from the in-memory indices. */
    private void removeIndex(Path p)
    {
        NpcDropData data = cache.remove(p);
        if (data != null)
        {
            nameIndex.remove(buildNameKey(data.getName(), data.getLevel()));
        }
    }

    /** Lazily populate the in-memory indices from existing cache files. */
    private void loadIndex()
    {
        if (indexLoaded)
        {
            return;
        }
        synchronized (this)
        {
            if (indexLoaded)
            {
                return;
            }
            try
            {
                Path dir = getCacheDir();
                if (Files.exists(dir))
                {
                    try (Stream<Path> files = Files.list(dir))
                    {
                        for (Path p : files.filter(Files::isRegularFile).collect(Collectors.toList()))
                        {
                            if (!isFresh(p))
                            {
                                Files.deleteIfExists(p);
                                continue;
                            }
                            try
                            {
                                String json = Files.readString(p, StandardCharsets.UTF_8);
                                NpcDropData data = gson.fromJson(json, NpcDropData.class);
                                if (data != null && data.getDropTableSections() != null && !data.getDropTableSections().isEmpty())
                                {
                                    cache.put(p, data);
                                    nameIndex.put(buildNameKey(data.getName(), data.getLevel()), p);
                                }
                                else
                                {
                                    Files.deleteIfExists(p);
                                }
                            }
                            catch (Exception e)
                            {
                                log.warn("Skipping bad cache file {}", p, e);
                                Files.deleteIfExists(p);
                            }
                        }
                    }
                }
            }
            catch (IOException e)
            {
                log.debug("Error loading cache index", e);
            }
            indexLoaded = true;
        }
    }

    /** Gracefully shutdown IO executor. */
    public void shutdown() {
        ExecutorService executor = ioExecutor;
        if (executor == null) {
            return;
        }

        executor.shutdownNow();
        ioExecutor = null;
        cache.clear();
        nameIndex.clear();
        indexLoaded = false;
    }

    private synchronized ExecutorService ensureExecutor() {
        if (ioExecutor == null || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
            ioExecutor = Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors() / 2),
                    new ThreadFactoryBuilder().setNameFormat("dropcache-io-%d").build()
            );
        }
        return ioExecutor;
    }

    private String buildNameKey(String name, int level)
    {
        return name.replaceAll("[^A-Za-z0-9]", "_") + "_" + level;
    }
}