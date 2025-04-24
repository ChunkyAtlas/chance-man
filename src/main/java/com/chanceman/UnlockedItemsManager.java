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
 * Manages the set of unlocked items with robust, atomic persistence and 10‑file backup rotation.
 */
@Slf4j
@Singleton
public class UnlockedItemsManager
{
    private static final int MAX_BACKUPS = 10;

    private final Set<Integer> unlockedItems = Collections.synchronizedSet(new LinkedHashSet<>());

    @Inject private AccountManager accountManager;
    @Inject private Gson gson;
    @Setter private ExecutorService executor;

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

        if (!Files.exists(file))
        {
            // first‑run: empty set → write an empty JSON file
            saveUnlockedItems();
            return;
        }

        try (Reader r = Files.newBufferedReader(file))
        {
            Set<Integer> loaded = gson.fromJson(r,
                    new com.google.gson.reflect.TypeToken<Set<Integer>>() {}.getType());
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
            }
            catch (IOException e)
            {
                log.error("Error saving unlocked items", e);
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

    /**
     * @return The number of unlocked items
     */
    public int getUnlockCount() { return unlockedItems.size(); }
}
