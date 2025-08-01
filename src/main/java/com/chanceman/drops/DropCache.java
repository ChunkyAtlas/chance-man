package com.chanceman.drops;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

import com.chanceman.account.AccountManager;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class DropCache
{
    private final Gson gson;
    private final AccountManager accountManager;
    private final DropFetcher dropFetcher;
    private static final Duration MAX_AGE = Duration.ofDays(7);

    @Inject
    public DropCache(Gson gson, AccountManager accountManager, DropFetcher dropFetcher)
    {
        this.gson = gson;
        this.accountManager = accountManager;
        this.dropFetcher = dropFetcher;
    }

    /**
     * Load from disk if possible; otherwise fetch from the wiki,
     * write out the JSON, and return the data.
     */
    public CompletableFuture<NpcDropData> get(int npcId, String name, int level)
    {
        final Path file;
        try
        {
            file = getCacheFile(npcId, name, level);
        }
        catch (IOException ex)
        {
            log.error("Could not resolve cache file for {} ({}, lvl {})", npcId, name, level, ex);
            return CompletableFuture.failedFuture(ex);
        }

        return CompletableFuture.supplyAsync(() ->
                {
                    if (Files.exists(file))
                    {
                        try
                        {
                            String json = Files.readString(file, StandardCharsets.UTF_8);
                            return gson.fromJson(json, NpcDropData.class);
                        }
                        catch (Exception e)
                        {
                            log.warn("Failed to read cache {}, will re-fetch", file, e);
                            try
                            {
                                Files.deleteIfExists(file);
                            }
                            catch (IOException ignored) { }
                        }
                    }
                    return null;
                })
                // If we had a valid cache, return it; otherwise fetch & write
                .thenCompose(cached ->
                {
                    if (cached != null)
                    {
                        return CompletableFuture.completedFuture(cached);
                    }

                    return dropFetcher.fetch(npcId, name, level)
                            .thenApply(data ->
                            {
                                try
                                {
                                    String json = gson.toJson(data);
                                    Files.createDirectories(file.getParent());
                                    Files.writeString(
                                            file,
                                            json,
                                            StandardCharsets.UTF_8,
                                            StandardOpenOption.CREATE,
                                            StandardOpenOption.TRUNCATE_EXISTING
                                    );
                                }
                                catch (Exception e)
                                {
                                    log.error("Failed to write cache file: {}", file, e);
                                }
                                return data;
                            })
                            .exceptionally(ex ->
                            {
                                log.error("Error fetching drop data for NPC {}", npcId, ex);
                                return null;
                            });
                });
    }

    private Path getCacheFile(int npcId, String name, int level) throws IOException
    {
        String player = accountManager.getPlayerName();
        if (player == null)
        {
            throw new IOException("Player name is not available");
        }

        String safeName = name.replaceAll("[^A-Za-z0-9]", "_");
        Path dir = RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve("drops");
        Files.createDirectories(dir);

        String fn = npcId + "_" + safeName + "_" + level + ".json";
        return dir.resolve(fn);
    }

    /**
     * Deletes cached drop table files older than {@link #MAX_AGE}.
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
     * Deletes all cached drop table files for the current player.
     */
    public void clearAllCaches()
    {
        String player = accountManager.getPlayerName();
        if (player == null) return;

        Path dir = RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve("drops");

        if (!Files.exists(dir)) return;

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
}
