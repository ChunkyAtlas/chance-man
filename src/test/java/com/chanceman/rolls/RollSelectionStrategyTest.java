package com.chanceman.rolls;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RollSelectionStrategyTest
{
    @Test
    public void uniformStrategyReturnsOnlyLockedEligibleItems()
    {
        UniformRollSelectionStrategy strategy = new UniformRollSelectionStrategy();
        RollSelection selection = strategy.select(
                new LinkedHashSet<>(Arrays.asList(1, 2, 3)),
                Collections.singleton(2));

        assertEquals(RollSelection.Type.NORMAL, selection.getType());
        assertEquals(3, selection.randomItem(new FixedRandom(1)));
    }

    @Test
    public void emptyEligibleSetProducesEmptySelection()
    {
        RollSelection selection = new UniformRollSelectionStrategy().select(
                Collections.emptySet(), Collections.emptySet());

        assertTrue(selection.isEmpty());
    }

    private static final class FixedRandom extends java.util.Random
    {
        private final int value;

        private FixedRandom(int value)
        {
            this.value = value;
        }

        @Override
        public int nextInt(int bound)
        {
            return Math.min(value, bound - 1);
        }
    }
}
