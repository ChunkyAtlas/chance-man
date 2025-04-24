package com.chanceman;

import com.chanceman.account.AccountManager;
import com.google.gson.Gson;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;

import static net.runelite.client.RuneLite.RUNELITE_DIR;

/**
 * Manages the set of rolled items with atomic persistence.
 * Provides thread-safe operations for marking items as rolled,
 * loading from and saving to disk with backups and atomic moves.
 */
@Slf4j
@Singleton
public class RolledItemsManager
{
    private static final int MAX_BACKUPS = 10;
    private final Set<Integer> rolledItems = Collections.synchronizedSet(new LinkedHashSet<>());

    @Inject private AccountManager accountManager;
    @Inject private Gson gson;
    @Setter private ExecutorService executor;

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
     * If the file does not exist or is empty, initializes an empty set.
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

        if (!Files.exists(file))
        {
            // first run: write an empty file
            saveRolledItems();
            return;
        }

        try (Reader r = Files.newBufferedReader(file))
        {
            Set<Integer> loaded = gson.fromJson(r,
                    new com.google.gson.reflect.TypeToken<Set<Integer>>() {}.getType());
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

    /**
     * Saves the current set of rolled items to disk.
     * Uses a temporary file and backups for atomicity and data safety.
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
                log.error("Could not resolve rolled‑items path", ioe);
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
            }
            catch (IOException e)
            {
                log.error("Error saving rolled items", e);
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

    /**
     * @return The number of rolled items
     */
    public int getRollCount() { return rolledItems.size(); }
}