package com.chanceman;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Manages the set of unlocked items.
 * This class persists the unlocked item IDs to a JSON file so that items remain unlocked across sessions.
 * It uses an asynchronous executor for saving the data.
 */
@Slf4j
public class UnlockedItemsManager
{
    private final Set<Integer> unlockedItems = Collections.synchronizedSet(new HashSet<>());
    private final String filePath;
    private final Gson gson;
    private final ExecutorService executor;

    /**
     * Constructs an UnlockedItemsManager for a given player.
     *
     * @param playerName The player's name.
     */
    public UnlockedItemsManager(String playerName, Gson gson, ExecutorService executor)
    {
        this.gson = gson;
        this.executor = executor;
        filePath = RuneLite.RUNELITE_DIR + File.separator +
                "chanceman" + File.separator +
                playerName + File.separator +
                "chanceman_unlocked.json";
    }

    /**
     * Checks if an item is unlocked.
     *
     * @param itemId The item ID.
     * @return true if the item is unlocked, false otherwise.
     */
    public boolean isUnlocked(int itemId)
    {
        return unlockedItems.contains(itemId);
    }

    /**
     * Unlocks an item and saves the state.
     *
     * @param itemId The item ID to unlock.
     */
    public void unlockItem(int itemId)
    {
        unlockedItems.add(itemId);
        saveUnlockedItems();
    }

    /**
     * Loads the set of unlocked items from the JSON file.
     */
    public void loadUnlockedItems()
    {
        executor.submit(() -> {
            File file = new File(filePath);
            if (!file.exists())
            {
                file.getParentFile().mkdirs();
                return;
            }
            try (FileReader reader = new FileReader(file))
            {
                Type setType = new TypeToken<Set<Integer>>() {}.getType();
                Set<Integer> loaded = gson.fromJson(reader, setType);
                if (loaded != null)
                {
                    unlockedItems.addAll(loaded);
                }
            }
            catch (IOException e)
            {
                log.error("Error loading unlocked items", e);
            }
        });
    }

    /**
     * Saves the current set of unlocked items asynchronously to the JSON file.
     */
    public synchronized void saveUnlockedItems()
    {
        executor.submit(() -> {
            File file = new File(filePath);
            file.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(file))
            {
                gson.toJson(unlockedItems, writer);
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
}
