package com.chanceman.managers;

import com.chanceman.ChanceManOverlay;
import com.chanceman.ChanceManPanel;
import com.chanceman.ChanceManConfig;
import com.chanceman.relational.RelationalRollStateManager;
import com.chanceman.rolls.RelationalRollSelectionStrategy;
import com.chanceman.rolls.RollSelection;
import com.chanceman.rolls.RollSelectionStrategy;
import com.chanceman.rolls.UniformRollSelectionStrategy;
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

/** Manages roll animations and result announcements. */
@Singleton
public class RollAnimationManager
{
    @Inject private ItemManager itemManager;
    @Inject private Client client;
    @Inject private ClientThread clientThread;

    @Inject private ObtainedItemsManager obtainedManager;
    @Inject private RolledItemsManager rolledManager;
    @Inject private RelationalRollStateManager relationalStateManager;
    @Inject private UniformRollSelectionStrategy uniformRollSelectionStrategy;
    @Inject private RelationalRollSelectionStrategy relationalRollSelectionStrategy;

    @Inject private ChanceManOverlay overlay;
    @Inject private ChanceManConfig config;

    @Setter private ChanceManPanel chanceManPanel;

    private Set<Integer> allTradeableItems = Collections.emptySet();

    private final Queue<Integer> rollQueue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private volatile boolean isRolling = false;

    private volatile boolean tradeablesReady = false;

    private static final int SNAP_WINDOW_MS = 350;
    private final Random random = new Random();
    private volatile RollSelection activeSelection =
            new RollSelection(RollSelection.Type.NORMAL, Collections.emptySet());

    @Getter
    @Setter
    private volatile boolean manualRoll = false;

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
            return;
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
     * The selected item is marked as rolled during the snap window while the
     * animation finishes visually.
     */
    private void performRoll(int obtainedItemId)
    {
        if (!hasTradeablesReady())
        {
            finishRoll();
            return;
        }

        int rollDuration = 3000;
        activeSelection = selectSelection();
        if (activeSelection.isEmpty())
        {
            finishRoll();
            return;
        }
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
        if (rolledItemId <= 0)
        {
            finishRoll();
            return;
        }
        rolledManager.markRolled(rolledItemId);
        if (config.enableRelationalRolls())
        {
            relationalStateManager.recordRoll(
                    activeSelection.getType() == RollSelection.Type.TOOLS,
                    activeSelection.getType() == RollSelection.Type.QUEST
            );
        }

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
        int itemId = activeSelection.randomItem(random);
        return itemId > 0 ? itemId : overlay.getFinalItem();
    }

    private RollSelection selectSelection()
    {
        Set<Integer> eligible = new LinkedHashSet<>(allTradeableItems);
        Set<Integer> rolled = rolledManager.getRolledItems();
        RollSelectionStrategy strategy = config.enableRelationalRolls()
                ? relationalRollSelectionStrategy
                : uniformRollSelectionStrategy;
        return strategy.select(eligible, rolled);
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
