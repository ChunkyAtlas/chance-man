package com.chanceman.relational;

import com.chanceman.rolls.RollSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RelationalRollSelector
{
    private RelationalRollSelector() { }

    public static RollSelection choosePool(
            RelationalRollState state,
            RollSelection normal,
            RollSelection tools,
            RollSelection quest,
            int toolInterval,
            double questBaseWeight,
            double questWeightGrowth,
            double questWeightCap,
            Random random)
    {
        if (!tools.isEmpty() && toolInterval > 0
                && state.getRollsSinceToolRoll() + 1 >= toolInterval)
        {
            return tools;
        }

        if (normal.isEmpty()) return quest.isEmpty() ? tools : quest;
        if (quest.isEmpty()) return normal;

        double questWeight = Math.max(0.0, questBaseWeight)
                + Math.max(0, state.getQuestMissStreak()) * Math.max(0.0, questWeightGrowth);
        if (questWeightCap > 0.0) questWeight = Math.min(questWeight, questWeightCap);

        double total = 1.0 + questWeight;
        if (total <= 0.0) return normal;
        return random.nextDouble() * total < questWeight ? quest : normal;
    }

    public static List<Integer> locked(Iterable<Integer> candidates, java.util.Set<Integer> rolledItems)
    {
        List<Integer> locked = new ArrayList<>();
        if (candidates == null) return locked;
        for (Integer itemId : candidates)
        {
            if (itemId != null && !rolledItems.contains(itemId)) locked.add(itemId);
        }
        return locked;
    }

}
