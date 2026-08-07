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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Manages roll animations and result announcements.
 *
 * New domain meanings:
 *  - ObtainedItemsManager = items you have obtained (legacy: Rolled)
 *  - RolledItemsManager   = items that have been rolled/unlocked (legacy: Unlocked)
 */
@Singleton
public class RollAnimationManager
{
    @Inject private ItemManager itemManager;
    @Inject private Client client;
    @Inject private ClientThread clientThread;

    @Inject private ObtainedItemsManager obtainedManager;
    @Inject private RolledItemsManager rolledManager;

    @Inject private ChanceManOverlay overlay;
    @Inject private ChanceManConfig config;

    @Setter private ChanceManPanel chanceManPanel;

    private Set<Integer> allTradeableItems = Collections.emptySet();

    private final Queue<Integer> rollQueue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean isRolling = false;

    // tradeables gating
    private volatile boolean tradeablesReady = false;

    private static final int SNAP_WINDOW_MS = 350;
    private final Random random = new Random();

    @Getter
    @Setter
    private volatile boolean manualRoll = false;

    /** Called by plugin after building tradeables. */
    public void setAllTradeableItems(Set<Integer> allTradeableItems)
    {
        this.allTradeableItems = (allTradeableItems != null) ? allTradeableItems : Collections.emptySet();
        this.tradeablesReady = !this.allTradeableItems.isEmpty();
    }

    public boolean hasTradeablesReady()
    {
        return tradeablesReady && allTradeableItems != null && !allTradeableItems.isEmpty();
    }

    /**
     * Enqueue an obtained item that should trigger a roll.
     *
     * @param obtainedItemId item that caused the roll
     */
    public void enqueueRoll(int obtainedItemId)
    {
        rollQueue.offer(obtainedItemId);
    }

    /**
     * Process pending rolls if idle.
     */
    public void process()
    {
        if (!hasTradeablesReady())
        {
            return; // queue stays intact until tradeables are built
        }

        if (!isRolling && !rollQueue.isEmpty())
        {
            int obtainedItemId = rollQueue.poll();
            isRolling = true;
            executor.submit(() -> performRoll(obtainedItemId));
        }
    }

    /**
     * Perform the roll animation and announce the result.
     * The rolled item is selected during the snap window and immediately
     * marked as ROLLED (legacy: unlocked), while the animation finishes visually.
     */
    private void performRoll(int obtainedItemId)
    {
        if (!hasTradeablesReady())
        {
            finishRoll();
            return;
        }

        int rollDuration = 3000;
        overlay.startRollAnimation(0, rollDuration, this::getRandomLockedItem);

        executor.schedule(
                () -> completeRoll(obtainedItemId),
                rollDuration + SNAP_WINDOW_MS,
                TimeUnit.MILLISECONDS
        );

        executor.schedule(
                this::finishRoll,
                rollDuration + overlay.getHighlightDurationMs(),
                TimeUnit.MILLISECONDS
        );
    }

    private void completeRoll(int obtainedItemId)
    {
        int rolledItemId = overlay.getFinalItem();
        rolledManager.markRolled(rolledItemId);

        final boolean wasManual = manualRoll;

        clientThread.invoke(() ->
        {
            String rolledTag = ColorUtil.wrapWithColorTag(
                    getItemName(rolledItemId),
                    config.unlockedItemColor()
            );

            String message;
            if (wasManual)
            {
                String pressTag = ColorUtil.wrapWithColorTag(
                        "pressing a button",
                        config.rolledItemColor()
                );
                message = "Rolled " + rolledTag + " by " + pressTag;
            }
            else
            {
                String obtainedTag = ColorUtil.wrapWithColorTag(
                        getItemName(obtainedItemId),
                        config.rolledItemColor()
                );
                message = "Rolled " + rolledTag + " by obtaining " + obtainedTag;
            }

            client.addChatMessage(
                    ChatMessageType.GAMEMESSAGE,
                    "",
                    message,
                    null
            );

            if (chanceManPanel != null)
            {
                SwingUtilities.invokeLater(chanceManPanel::updatePanel);
            }
        });
    }

    private void finishRoll()
    {
        manualRoll = false;
        isRolling = false;
    }

    public boolean isRolling()
    {
        return isRolling;
    }

    /**
     * Pick a random locked item to display during the roll.
     */
    private int getRandomLockedItem()
    {
        if (!hasTradeablesReady())
        {
            return overlay.getFinalItem();
        }

        List<Integer> locked = new ArrayList<>();
        for (int id : allTradeableItems)
        {
            if (!rolledManager.isRolled(id))
            {
                locked.add(id);
            }
        }

        if (locked.isEmpty())
        {
            return overlay.getFinalItem();
        }

        return locked.get(random.nextInt(locked.size()));
    }

    private String getItemName(int itemId)
    {
        ItemComposition comp = itemManager.getItemComposition(itemId);
        return comp.getName();
    }

    public void startUp()
    {
        if (executor == null || executor.isShutdown() || executor.isTerminated())
        {
            executor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    public void shutdown()
    {
        executor.shutdownNow();
    }
}
