package com.chanceman;

import net.runelite.api.Client;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.ItemManager;
import net.runelite.client.callback.ClientThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the roll animation for unlocking items.
 * It processes roll requests asynchronously and handles the roll animation through the overlay.
 */
public class RollAnimationManager
{
    private final Queue<Integer> rollQueue = new ConcurrentLinkedQueue<>();
    private final UnlockedItemsManager unlockedManager;
    private final ChanceManOverlay overlay;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isRolling = false;
    private final int rollDuration = 8000; // Continuous phase duration (ms)
    private final int highlightDuration = 2000; // Highlight phase (ms)
    private final List<Integer> allTradeableItems;
    private final Random random = new Random();
    private final ItemManager itemManager;
    private final Client client;
    private final ChatMessageManager chatMessageManager;
    private final ChanceManPlugin plugin;
    private final ClientThread clientThread;

    /**
     * Constructs a RollAnimationManager.
     *
     * @param unlockedManager The manager for unlocked items.
     * @param overlay The overlay displaying the roll animation.
     * @param allTradeableItems List of all tradeable item IDs.
     * @param itemManager The item manager.
     * @param client The game client.
     * @param chatMessageManager The chat message manager.
     * @param plugin The parent plugin.
     * @param clientThread The client thread for scheduling tasks.
     */
    public RollAnimationManager(UnlockedItemsManager unlockedManager, ChanceManOverlay overlay,
                                List<Integer> allTradeableItems, ItemManager itemManager, Client client,
                                ChatMessageManager chatMessageManager, ChanceManPlugin plugin,
                                ClientThread clientThread)
    {
        this.unlockedManager = unlockedManager;
        this.overlay = overlay;
        this.allTradeableItems = allTradeableItems;
        this.itemManager = itemManager;
        this.client = client;
        this.chatMessageManager = chatMessageManager;
        this.plugin = plugin;
        this.clientThread = clientThread;
    }

    /**
     * Enqueues an item ID for the roll animation.
     *
     * @param itemId The item ID to be rolled.
     */
    public void enqueueRoll(int itemId)
    {
        rollQueue.offer(itemId);
    }

    /**
     * Processes the roll queue by initiating a roll animation if not already rolling.
     */
    public void process()
    {
        if (!isRolling && !rollQueue.isEmpty())
        {
            int dropItemId = rollQueue.poll();
            isRolling = true;
            executor.submit(this::performRoll);
        }
    }

    /**
     * Performs the roll animation, unlocking the final item and sending a chat message.
     */
    private void performRoll()
    {
        overlay.startRollAnimation(0, rollDuration, this::getRandomLockedItem);
        try {
            Thread.sleep(rollDuration + highlightDuration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int finalRolledItem = overlay.getFinalItem();
        unlockedManager.unlockItem(finalRolledItem);
        clientThread.invokeLater(() -> {
            String message = new ChatMessageBuilder()
                    .append(ChatColorType.HIGHLIGHT)
                    .append("Unlocked " + plugin.getItemName(finalRolledItem))
                    .build();
            chatMessageManager.queue(QueuedMessage.builder()
                    .type(net.runelite.api.ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage(message)
                    .build());
        });
        isRolling = false;
    }

    /**
     * Checks if a roll animation is currently in progress.
     *
     * @return true if a roll is in progress, false otherwise.
     */
    public boolean isRolling() {
        return isRolling;
    }

    /**
     * Retrieves a random locked item from the list of tradeable items.
     *
     * @return A random locked item ID, or a fallback if all items are unlocked.
     */
    public int getRandomLockedItem()
    {
        List<Integer> locked = new ArrayList<>();
        for (int id : allTradeableItems)
        {
            if (!unlockedManager.isUnlocked(id))
            {
                locked.add(id);
            }
        }
        if (locked.isEmpty())
        {
            int fallback = overlay.getFinalItem();
            return fallback;
        }
        int selected = locked.get(random.nextInt(locked.size()));
        return selected;
    }

    /**
     * Shuts down the roll animation executor service.
     */
    public void shutdown()
    {
        executor.shutdownNow();
    }
}
