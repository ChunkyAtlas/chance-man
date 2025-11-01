package com.chanceman.managers;

import com.chanceman.ChanceManOverlay;
import com.chanceman.ChanceManPanel;
import com.chanceman.ChanceManConfig;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ColorUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages the roll animation for unlocking items.
 * It processes roll requests asynchronously and handles the roll animation through the overlay.
 */
@Singleton
public class RollAnimationManager
{
    @Inject private ItemManager itemManager;
    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private UnlockedItemsManager unlockedManager;
    @Inject private ChanceManOverlay overlay;
    @Inject private ChanceManConfig config;
    @Setter private ChanceManPanel chanceManPanel;

    @Setter private HashSet<Integer> allTradeableItems;
    private final Queue<Integer> rollQueue = new ConcurrentLinkedQueue<>();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean isRolling = false;
    private static final int SNAP_WINDOW_MS = 350;
    private final Random random = new Random();

    @Getter
    @Setter
    private volatile boolean manualRoll = false;

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
            int queuedItemId = rollQueue.poll();
            isRolling = true;
            executor.submit(() -> performRoll(queuedItemId));
        }
    }

    /**
     * Performs the roll animation.
     * Now announces/unlocks as soon as the item is selected (after the snap),
     * while still letting the highlight finish visually before accepting another roll.
     */
    private void performRoll(int queuedItemId)
    {
        // Duration of the continuous spin phase (ms)
        int rollDuration = 3000;
        overlay.startRollAnimation(0, rollDuration, this::getRandomLockedItem);

        try {
            Thread.sleep(rollDuration + SNAP_WINDOW_MS);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
        int finalRolledItem = overlay.getFinalItem();
        unlockedManager.unlockItem(finalRolledItem);
        final boolean wasManualRoll = isManualRoll();
        clientThread.invoke(() -> {
            String unlockedTag = ColorUtil.wrapWithColorTag(getItemName(finalRolledItem), config.unlockedItemColor());
            String message;
            if (wasManualRoll)
            {
                String pressTag = ColorUtil.wrapWithColorTag("pressing a button", config.rolledItemColor());
                message = "Unlocked " + unlockedTag + " by " + pressTag;
            }
            else
            {
                String rolledTag = ColorUtil.wrapWithColorTag(getItemName(queuedItemId), config.rolledItemColor());
                message = "Unlocked " + unlockedTag + " by rolling " + rolledTag;
            }
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", message, null);
            if (chanceManPanel != null) {
                SwingUtilities.invokeLater(() -> chanceManPanel.updatePanel());
            }
        });

        int remainingHighlight = Math.max(0, overlay.getHighlightDurationMs() - SNAP_WINDOW_MS);
        if (remainingHighlight > 0)
        {
            try
            {
                Thread.sleep(remainingHighlight);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }
        }

        setManualRoll(false);
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
            // Fallback: keep showing the current center item
            return overlay.getFinalItem();
        }
        return locked.get(random.nextInt(locked.size()));
    }

    public String getItemName(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp.getName();
    }

    public void startUp() {
        if (executor == null || executor.isShutdown() || executor.isTerminated()) {
            executor = Executors.newSingleThreadExecutor();
        }
    }

    /**
     * Shuts down the roll animation executor service.
     */
    public void shutdown()
    {
        executor.shutdownNow();
    }
}
