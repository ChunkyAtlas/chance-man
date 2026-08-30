package com.chanceman.rolls;

import com.chanceman.ChanceManConfig;
import com.chanceman.relational.RelationalItemPools;
import com.chanceman.relational.RelationalRollSelector;
import com.chanceman.relational.RelationalRollStateManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Random;
import java.util.Set;

@Singleton
public final class RelationalRollSelectionStrategy implements RollSelectionStrategy
{
    @Inject private ChanceManConfig config;
    @Inject private RelationalItemPools itemPools;
    @Inject private RelationalRollStateManager stateManager;

    private final Random random = new Random();

    @Override
    public RollSelection select(Set<Integer> eligibleItems, Set<Integer> rolledItems)
    {
        RollSelection normal = new RollSelection(
                RollSelection.Type.NORMAL,
                RelationalRollSelector.locked(
                        itemPools.filterNormalItems(eligibleItems), rolledItems));
        RollSelection tools = new RollSelection(
                RollSelection.Type.TOOLS,
                RelationalRollSelector.locked(
                        itemPools.filterTools(eligibleItems), rolledItems));
        RollSelection quest = new RollSelection(
                RollSelection.Type.QUEST,
                RelationalRollSelector.locked(
                        itemPools.filterQuestItems(eligibleItems), rolledItems));

        return RelationalRollSelector.choosePool(
                stateManager.getState(),
                normal,
                tools,
                quest,
                config.toolRollInterval(),
                config.questBaseWeight(),
                config.questWeightGrowth(),
                config.questWeightCap(),
                random);
    }
}
