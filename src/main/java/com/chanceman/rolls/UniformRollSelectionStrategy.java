package com.chanceman.rolls;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Singleton
public final class UniformRollSelectionStrategy implements RollSelectionStrategy
{
    @Override
    public RollSelection select(Set<Integer> eligibleItems, Set<Integer> rolledItems)
    {
        Set<Integer> lockedItems = eligibleItems == null
                ? new LinkedHashSet<>()
                : new LinkedHashSet<>(eligibleItems);
        if (rolledItems != null)
        {
            lockedItems.removeAll(rolledItems);
        }
        return new RollSelection(RollSelection.Type.NORMAL, lockedItems);
    }
}
