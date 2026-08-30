package com.chanceman.relational;

import org.junit.Test;
import com.google.gson.Gson;
import com.chanceman.rolls.RollSelection;

import java.util.Collections;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RelationalRollSelectorTest
{
    private static RollSelection pool(RollSelection.Type type, int id)
    {
        return new RollSelection(type, Collections.singleton(id));
    }

    @Test
    public void stateTracksCadenceAndQuestMisses()
    {
        RelationalRollState state = new RelationalRollState();
        state = state.afterRoll(false, false);
        state = state.afterRoll(false, false);
        assertEquals(2L, state.getTotalRolls());
        assertEquals(2, state.getRollsSinceToolRoll());
        assertEquals(2, state.getQuestMissStreak());

        state = state.afterRoll(true, true);
        assertEquals(3L, state.getTotalRolls());
        assertEquals(0, state.getRollsSinceToolRoll());
        assertEquals(0, state.getQuestMissStreak());
    }

    @Test
    public void toolPoolIsGuaranteedAtCadence()
    {
        RelationalRollState state = new RelationalRollState(4, 4, 4);
        RollSelection chosen = RelationalRollSelector.choosePool(
                state,
                pool(RollSelection.Type.NORMAL, 1),
                pool(RollSelection.Type.TOOLS, 2),
                pool(RollSelection.Type.QUEST, 3),
                5, 0.0, 0.0, 0.0,
                new FixedRandom(0.99));
        assertEquals(RollSelection.Type.TOOLS, chosen.getType());
    }

    @Test
    public void questWeightGrowsAfterMisses()
    {
        RollSelection normal = pool(RollSelection.Type.NORMAL, 1);
        RollSelection tools = new RollSelection(RollSelection.Type.TOOLS, Collections.emptySet());
        RollSelection quest = pool(RollSelection.Type.QUEST, 3);

        RollSelection low = RelationalRollSelector.choosePool(
                new RelationalRollState(0, 0, 0), normal, tools, quest,
                5, 0.1, 0.1, 1.0, new FixedRandom(0.15));
        RollSelection high = RelationalRollSelector.choosePool(
                new RelationalRollState(0, 0, 4), normal, tools, quest,
                5, 0.1, 0.1, 1.0, new FixedRandom(0.15));

        assertEquals(RollSelection.Type.NORMAL, low.getType());
        assertEquals(RollSelection.Type.QUEST, high.getType());
    }

    @Test
    public void emptySpecialPoolFallsBack()
    {
        RollSelection chosen = RelationalRollSelector.choosePool(
                new RelationalRollState(4, 4, 4),
                pool(RollSelection.Type.NORMAL, 1),
                new RollSelection(RollSelection.Type.TOOLS, Collections.emptySet()),
                new RollSelection(RollSelection.Type.QUEST, Collections.emptySet()),
                5, 10.0, 10.0, 0.0,
                new FixedRandom(0.0));
        assertEquals(RollSelection.Type.NORMAL, chosen.getType());
    }

    @Test
    public void bundledPoolsUseTradeableQuestRequirements()
    {
        RelationalItemPools pools = new RelationalItemPools(new Gson());

        org.junit.Assert.assertTrue(pools.isQuestItem(1933)); // Pot of flour
        org.junit.Assert.assertTrue(pools.isQuestItem(8778)); // Oak plank
        org.junit.Assert.assertFalse(pools.isQuestItem(35)); // Excalibur is a quest reward
        org.junit.Assert.assertFalse(pools.isQuestItem(23733)); // Divine ranging potion
        org.junit.Assert.assertFalse(pools.isQuestItem(28810)); // Zombie axe is a post-quest drop
        org.junit.Assert.assertFalse(pools.isQuestItem(29458)); // Adamant seeds are zombie pirate loot
        org.junit.Assert.assertFalse(pools.isQuestItem(29796)); // Noxious halberd is an Araxxor product
        org.junit.Assert.assertTrue(pools.isToolItem(311)); // Regular harpoon
        org.junit.Assert.assertTrue(pools.isToolItem(1265)); // Bronze pickaxe
        org.junit.Assert.assertTrue(pools.isToolItem(1351)); // Bronze axe
        org.junit.Assert.assertFalse(pools.isToolItem(23762)); // Crystal harpoon
        org.junit.Assert.assertFalse(pools.isToolItem(11920)); // Dragon pickaxe
        org.junit.Assert.assertTrue(pools.isToolItem(10008)); // Box trap
        org.junit.Assert.assertTrue(pools.isToolItem(5341)); // Farming rake
        org.junit.Assert.assertTrue(pools.isToolItem(5343)); // Seed dibber
        org.junit.Assert.assertFalse(pools.isToolItem(5418)); // Empty sack
        org.junit.Assert.assertTrue(pools.isToolItem(10025)); // Magic box
        org.junit.Assert.assertFalse(pools.isToolItem(15259)); // Legacy/non-OSRS ID
    }

    private static final class FixedRandom extends Random
    {
        private final double value;

        private FixedRandom(double value)
        {
            this.value = value;
        }

        @Override
        public double nextDouble()
        {
            return value;
        }
    }
}
