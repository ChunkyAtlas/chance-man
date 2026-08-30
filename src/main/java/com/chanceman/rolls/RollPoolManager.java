package com.chanceman.rolls;

import com.chanceman.ChanceManConfig;
import com.chanceman.account.AccountManager;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
public final class RollPoolManager
{
    private static final String CONFIG_GROUP = "chanceman";
    private static final String TOOL_COUNTER_KEY = "toolRollsSinceLastTool.";
    private static final String QUEST_MISS_KEY = "questItemMissStreak.";

    private final ChanceManConfig config;
    private final ConfigManager configManager;
    private final AccountManager accountManager;

    private int rollsSinceTool;
    private int questMissStreak;

    @Inject
    public RollPoolManager(
            ChanceManConfig config,
            ConfigManager configManager,
            AccountManager accountManager)
    {
        this.config = config;
        this.configManager = configManager;
        this.accountManager = accountManager;
    }

    public synchronized List<Integer> selectPool(
            Set<Integer> allTradeableItems,
            Set<Integer> rolledItems,
            boolean toolGuaranteeEnabled,
            boolean questItemRollsEnabled)
    {
        if (allTradeableItems == null || allTradeableItems.isEmpty())
        {
            return Collections.emptyList();
        }

        List<Integer> available = new ArrayList<>(allTradeableItems.size());
        for (int itemId : allTradeableItems)
        {
            if (rolledItems == null || !rolledItems.contains(itemId))
            {
                available.add(itemId);
            }
        }

        if (available.isEmpty())
        {
            return Collections.emptyList();
        }

        if (toolGuaranteeEnabled
                && rollsSinceTool >= getToolGuaranteeInterval() - 1)
        {
            List<Integer> tools = new ArrayList<>();
            for (int itemId : available)
            {
                if (RollItemPools.isToolItem(itemId))
                {
                    tools.add(itemId);
                }
            }

            if (!tools.isEmpty())
            {
                return tools;
            }
        }

        if (!questItemRollsEnabled)
        {
            return available;
        }

        List<Integer> questItems = new ArrayList<>();
        List<Integer> normalItems = new ArrayList<>();
        for (int itemId : available)
        {
            if (RollItemPools.isQuestItem(itemId))
            {
                questItems.add(itemId);
            }
            else
            {
                normalItems.add(itemId);
            }
        }

        if (!questItems.isEmpty()
                && ThreadLocalRandom.current().nextInt(100) < getQuestItemChance())
        {
            return questItems;
        }

        return normalItems.isEmpty() ? questItems : normalItems;
    }

    public synchronized void recordCompletedRoll(
            int itemId,
            boolean toolGuaranteeEnabled,
            boolean questItemRollsEnabled)
    {
        if (!toolGuaranteeEnabled && !questItemRollsEnabled)
        {
            return;
        }

        if (toolGuaranteeEnabled)
        {
            rollsSinceTool = RollItemPools.isToolItem(itemId)
                    ? 0
                    : increment(rollsSinceTool);
        }

        if (questItemRollsEnabled)
        {
            questMissStreak = RollItemPools.isQuestItem(itemId)
                    ? 0
                    : increment(questMissStreak);
        }

        saveState(toolGuaranteeEnabled, questItemRollsEnabled);
    }

    public synchronized void loadState()
    {
        rollsSinceTool = 0;
        questMissStreak = 0;

        String player = accountManager.getPlayerName();
        if (player == null || player.isEmpty())
        {
            return;
        }

        rollsSinceTool = readCounter(TOOL_COUNTER_KEY + player);
        questMissStreak = readCounter(QUEST_MISS_KEY + player);
    }

    private int getToolGuaranteeInterval()
    {
        return Math.max(1, Math.min(1000, config.toolRollGuaranteeInterval()));
    }

    private int getQuestItemChance()
    {
        int startingChance = clampPercent(config.questItemStartingChance());
        int chanceIncrease = clampPercent(config.questItemChanceIncrease());
        int maximumChance = clampPercent(config.questItemMaximumChance());
        long chance = (long) startingChance + (long) questMissStreak * chanceIncrease;
        return (int) Math.min(maximumChance, chance);
    }

    private int readCounter(String key)
    {
        String value = configManager.getConfiguration(CONFIG_GROUP, key);
        if (value == null || value.isEmpty())
        {
            return 0;
        }

        try
        {
            return Math.max(0, Integer.parseInt(value.trim()));
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }

    private void saveState(boolean toolGuaranteeEnabled, boolean questItemRollsEnabled)
    {
        String player = accountManager.getPlayerName();
        if (player == null || player.isEmpty())
        {
            return;
        }

        if (toolGuaranteeEnabled)
        {
            configManager.setConfiguration(
                    CONFIG_GROUP,
                    TOOL_COUNTER_KEY + player,
                    String.valueOf(rollsSinceTool)
            );
        }

        if (questItemRollsEnabled)
        {
            configManager.setConfiguration(
                    CONFIG_GROUP,
                    QUEST_MISS_KEY + player,
                    String.valueOf(questMissStreak)
            );
        }
    }

    private static int clampPercent(int value)
    {
        return Math.max(0, Math.min(100, value));
    }

    private static int increment(int value)
    {
        return value == Integer.MAX_VALUE ? value : value + 1;
    }
}
