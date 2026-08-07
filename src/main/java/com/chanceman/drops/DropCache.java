package com.chanceman.drops;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

import com.chanceman.account.AccountManager;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Persistent drop table cache backed by JSON files in the user's RuneLite
 * directory. Exact NPC identities are cached by NPC ID so same-name/same-level
 * variants can never overwrite or satisfy one another.
 */
@Slf4j
@Singleton
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class DropCache {
    private static final Duration MAX_AGE = Duration.ofDays(7);
    private static final Duration TEMP_FILE_MAX_AGE = Duration.ofDays(1);
    private static final String CACHE_DIRECTORY = "drops";
    private static final String EXACT_FILE_PREFIX = "npc_";
    private static final Pattern EXACT_CACHE_FILE = Pattern.compile("^npc_(\\d+)\\.json$");

    private final Gson gson;
    private final AccountManager accountManager;
    private final DropFetcher dropFetcher;

    private final Map<Path, NpcDropData> cache = new ConcurrentHashMap<>();
    private final Object indexLock = new Object();

    private volatile String loadedPlayer;
    private volatile boolean indexLoaded;
    private ExecutorService ioExecutor;

    private static int npcIdFromFilename(String filename) {
        Matcher matcher = EXACT_CACHE_FILE.matcher(Objects.toString(filename, ""));
        if (!matcher.matches()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    /**
     * Preload the current account's on-disk index and prune stale entries.
     */
    public void startUp() {
        ensureExecutor();
        String player = currentPlayer();
        if (player == null) {
            return;
        }

        ensureIndexForPlayer(player);
        pruneOldCaches();
    }

    /**
     * Fetch one NPC. Positive NPC IDs are exact identities and are cached by ID.
     * Name only lookups are intentionally not persisted because a name/level pair
     * is not guaranteed to identify one NPC variant.
     */
    public CompletableFuture<NpcDropData> get(int npcId, String name, int level) {
        String player = currentPlayer();
        CompletableFuture<NpcDropData> raw = npcId > 0 && player != null
                ? getOrFetchExactRaw(npcId, name, level, player)
                : dropFetcher.fetch(npcId, name, level);

        return raw.thenCompose(dropFetcher::resolveForDisplay)
                .exceptionally(ex -> {
                    log.error("Error fetching drop data for NPC {}", npcId, ex);
                    return null;
                });
    }

    /**
     * Fetch every distinct Wiki monster variant matching a page name and optional
     * combat level. Each result carries an exact NPC ID whenever the Wiki exposes
     * one, so two variants such as same-level armed/unarmed NPCs remain separate.
     */
    public CompletableFuture<List<NpcDropData>> searchNpcVariants(String name, int level) {
        return dropFetcher.fetchVariants(name, level)
                .thenCompose(this::resolveAllForDisplay)
                .exceptionally(ex ->
                {
                    log.error("Error fetching NPC variants for {} (lvl {})", name, level, ex);
                    return new ArrayList<>();
                });
    }

    /**
     * Persist exactly one NPC chosen from search results. Search results are
     * display copies, so use the same exact-ID raw-cache path as get(...) instead
     * of serializing the display copy. That preserves the raw Wiki rows on disk
     * and also reuses any fresh exact cache that already exists.
     */
    public CompletableFuture<Void> cacheSelected(NpcDropData selected) {
        if (selected == null || selected.getNpcId() <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        String player = currentPlayer();
        if (player == null) {
            log.warn("Cannot cache selected NPC {} because player identity is unavailable", selected.getNpcId());
            return CompletableFuture.completedFuture(null);
        }

        int npcId = selected.getNpcId();
        return getOrFetchExactRaw(npcId, selected.getName(), selected.getLevel(), player)
                .thenApply(ignored -> (Void) null)
                .exceptionally(ex -> {
                    log.warn("Failed to persist selected NPC {}", npcId, ex);
                    return null;
                });
    }

    private CompletableFuture<NpcDropData> getOrFetchExactRaw(
            int npcId, String name, int level, String player) {
        ExecutorService executor = ensureExecutor();
        ensureIndexForPlayer(player);
        Path exactFile = getCacheFile(player, npcId);

        return CompletableFuture.supplyAsync(
                () -> readFreshCache(exactFile, player, npcId, level),
                executor
        ).thenComposeAsync(cached -> cached != null
                ? CompletableFuture.completedFuture(cached)
                : fetchAndCacheExact(npcId, name, level, player, exactFile, executor), executor);
    }

    private CompletableFuture<NpcDropData> fetchAndCacheExact(
            int npcId,
            String name,
            int level,
            String player,
            Path exactFile,
            ExecutorService executor) {
        return dropFetcher.fetch(npcId, name, level).thenApplyAsync(data ->
        {
            if (!isCacheable(data) || data.getNpcId() != npcId) {
                return null;
            }

            try {
                writeCacheFile(exactFile, data);
                if (player.equals(loadedPlayer) && indexLoaded) {
                    indexData(exactFile, data);
                }
            } catch (Exception ex) {
                log.error("Failed to write exact drop cache for NPC {}", npcId, ex);
            }
            return data;
        }, executor);
    }

    private CompletableFuture<List<NpcDropData>> resolveAllForDisplay(List<NpcDropData> raw) {
        if (raw == null || raw.isEmpty()) {
            return CompletableFuture.completedFuture(new ArrayList<>());
        }

        List<CompletableFuture<NpcDropData>> futures = raw.stream()
                .filter(Objects::nonNull)
                .map(dropFetcher::resolveForDisplay)
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .filter(data -> data.getDropTableSections() != null
                                && !data.getDropTableSections().isEmpty())
                        .collect(Collectors.toList()));
    }

    /**
     * @return a snapshot of all exact cached NPC drop data for the current account.
     */
    public Collection<NpcDropData> getAllNpcData() {
        String player = currentPlayer();
        if (player == null) {
            return new ArrayList<>();
        }

        ensureIndexForPlayer(player);
        return new ArrayList<>(cache.values());
    }

    /**
     * Return NPC names containing the supplied query. Local exact-cache matches
     * are combined with Wiki search results.
     */
    public CompletableFuture<List<String>> searchNpcNames(String query) {
        ExecutorService executor = ensureExecutor();
        return CompletableFuture.supplyAsync(() ->
        {
            String search = Objects.toString(query, "").trim();
            String lowerSearch = search.toLowerCase(Locale.ROOT);

            String player = currentPlayer();
            if (player != null) {
                ensureIndexForPlayer(player);
            }

            Set<String> names = cache.values().stream()
                    .map(NpcDropData::getName)
                    .filter(Objects::nonNull)
                    .filter(candidate -> candidate.toLowerCase(Locale.ROOT).contains(lowerSearch))
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            try {
                names.addAll(dropFetcher.searchNpcNames(search));
            } catch (Exception ex) {
                log.warn("Wiki search failed for {}", search, ex);
            }

            return new ArrayList<>(names);
        }, executor);
    }

    /**
     * Delete stale cache, legacy cache JSON, and abandoned temporary files.
     */
    public void pruneOldCaches() {
        String player = currentPlayer();
        if (player == null) {
            return;
        }

        ensureIndexForPlayer(player);
        Path directory = getCacheDir(player);
        if (!Files.isDirectory(directory)) {
            return;
        }

        Instant cacheCutoff = Instant.now().minus(MAX_AGE);
        Instant tempCutoff = Instant.now().minus(TEMP_FILE_MAX_AGE);

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile).forEach(path ->
            {
                try {
                    String filename = path.getFileName().toString();
                    Instant modified = Files.getLastModifiedTime(path).toInstant();

                    boolean legacyJson = filename.endsWith(".json") && npcIdFromFilename(filename) < 0;
                    boolean staleJson = filename.endsWith(".json") && modified.isBefore(cacheCutoff);
                    boolean staleTemp = filename.endsWith(".tmp") && modified.isBefore(tempCutoff);
                    if (legacyJson || staleJson || staleTemp) {
                        Files.deleteIfExists(path);
                        removeIndex(path);
                    }
                } catch (IOException ex) {
                    log.warn("Failed to prune drop cache {}", path, ex);
                }
            });
        } catch (IOException ex) {
            log.warn("Error pruning drop cache directory {}", directory, ex);
        }
    }

    /**
     * Delete all cached drop tables for the current player.
     */
    public void clearAllCaches() {
        String player = currentPlayer();
        if (player == null) {
            return;
        }

        synchronized (indexLock) {
            Path directory = getCacheDir(player);
            if (Files.isDirectory(directory)) {
                try (Stream<Path> files = Files.list(directory)) {
                    files.filter(Files::isRegularFile).forEach(path ->
                    {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            log.warn("Failed to delete drop cache {}", path, ex);
                        }
                    });
                } catch (IOException ex) {
                    log.warn("Error clearing drop cache directory {}", directory, ex);
                }
            }

            cache.clear();
            loadedPlayer = player;
            indexLoaded = true;
        }
    }

    /**
     * Gracefully stop cache IO and discard the in-memory account index.
     */
    public synchronized void shutdown() {
        ExecutorService executor = ioExecutor;
        if (executor != null) {
            executor.shutdownNow();
            ioExecutor = null;
        }

        synchronized (indexLock) {
            cache.clear();
            loadedPlayer = null;
            indexLoaded = false;
        }
    }

    private NpcDropData readFreshCache(Path file, String expectedPlayer, int expectedNpcId, int expectedLevel) {
        if (file == null || !Files.isRegularFile(file)) {
            if (file != null) {
                removeIndex(file);
            }
            return null;
        }

        if (!isFresh(file)) {
            deleteCacheFile(file);
            return null;
        }

        NpcDropData inMemory = cache.get(file);
        if (isValidExactCache(inMemory, expectedNpcId, expectedLevel)) {
            return inMemory;
        }

        try {
            NpcDropData data = readCacheFile(file);
            if (!isValidExactCache(data, expectedNpcId, expectedLevel)) {
                deleteCacheFile(file);
                return null;
            }

            if (expectedPlayer.equals(loadedPlayer) && indexLoaded) {
                indexData(file, data);
            }
            return data;
        } catch (Exception ex) {
            log.warn("Skipping bad cache file {}", file, ex);
            deleteCacheFile(file);
            return null;
        }
    }

    private boolean isValidExactCache(NpcDropData data, int expectedNpcId, int expectedLevel) {
        if (!isCacheable(data) || data.getNpcId() != expectedNpcId) {
            return false;
        }
        return expectedLevel <= 0 || data.getLevel() <= 0 || data.getLevel() == expectedLevel;
    }

    private void writeCacheFile(Path output, NpcDropData data) throws IOException {
        Files.createDirectories(output.getParent());
        Path temporary = Files.createTempFile(
                output.getParent(),
                output.getFileName().toString() + ".",
                ".tmp"
        );

        try {
            Files.writeString(
                    temporary,
                    gson.toJson(data),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );

            try {
                Files.move(
                        temporary,
                        output,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (AtomicMoveNotSupportedException | UnsupportedOperationException ex) {
                Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private NpcDropData readCacheFile(Path path) throws IOException {
        String json = Files.readString(path, StandardCharsets.UTF_8);
        return gson.fromJson(json, NpcDropData.class);
    }

    private boolean isCacheable(NpcDropData data) {
        return data != null
                && data.getDropTableSections() != null
                && !data.getDropTableSections().isEmpty();
    }

    private boolean isFresh(Path file) {
        try {
            Instant cutoff = Instant.now().minus(MAX_AGE);
            return !Files.getLastModifiedTime(file).toInstant().isBefore(cutoff);
        } catch (IOException ex) {
            return false;
        }
    }

    private void ensureIndexForPlayer(String player) {
        if (indexLoaded && player.equals(loadedPlayer)) {
            return;
        }

        synchronized (indexLock) {
            if (indexLoaded && player.equals(loadedPlayer)) {
                return;
            }

            if (!player.equals(loadedPlayer)) {
                cache.clear();
                loadedPlayer = player;
                indexLoaded = false;
            }

            Path directory = getCacheDir(player);
            if (!Files.exists(directory)) {
                indexLoaded = true;
                return;
            }

            try (Stream<Path> files = Files.list(directory)) {
                for (Path path : files
                        .filter(Files::isRegularFile)
                        .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
                        .collect(Collectors.toList())) {
                    String filename = path.getFileName().toString();
                    int expectedNpcId = npcIdFromFilename(filename);
                    if (expectedNpcId < 0 || !isFresh(path)) {
                        deleteCacheFile(path);
                        continue;
                    }
                    try {
                        NpcDropData data = readCacheFile(path);
                        if (isValidExactCache(data, expectedNpcId, 0)) {
                            indexData(path, data);
                        } else {
                            deleteCacheFile(path);
                        }
                    } catch (Exception ex) {
                        log.warn("Skipping bad cache file {}", path, ex);
                        deleteCacheFile(path);
                    }
                }
                indexLoaded = true;
            } catch (IOException ex) {
                log.warn("Error loading cache index for {}", player, ex);
            }
        }
    }

    private void indexData(Path path, NpcDropData data) {
        cache.put(path, data);
    }

    private void removeIndex(Path path) {
        cache.remove(path);
    }

    private void deleteCacheFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Failed to delete drop cache {}", path, ex);
        }
        removeIndex(path);
    }

    private Path getCacheDir(String player) {
        return RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve(CACHE_DIRECTORY);
    }

    private Path getCacheFile(String player, int npcId) {
        return getCacheDir(player).resolve(EXACT_FILE_PREFIX + npcId + ".json");
    }

    private String currentPlayer() {
        String player = Objects.toString(accountManager.getPlayerName(), "").trim();
        return player.isEmpty() ? null : player;
    }

    private synchronized ExecutorService ensureExecutor() {
        if (ioExecutor == null || ioExecutor.isShutdown() || ioExecutor.isTerminated()) {
            ioExecutor = Executors.newFixedThreadPool(
                    2,
                    new ThreadFactoryBuilder().setNameFormat("dropcache-io-%d").build()
            );
        }
        return ioExecutor;
    }
}