package com.chanceman.rolls;

import java.util.Set;

public interface RollSelectionStrategy
{
    RollSelection select(Set<Integer> eligibleItems, Set<Integer> rolledItems);
}
