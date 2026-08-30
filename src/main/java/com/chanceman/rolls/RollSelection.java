package com.chanceman.rolls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class RollSelection
{
    public enum Type
    {
        NORMAL,
        TOOLS,
        QUEST
    }

    private final Type type;
    private final List<Integer> itemIds;

    public RollSelection(Type type, Iterable<Integer> itemIds)
    {
        this.type = type;
        List<Integer> copy = new ArrayList<>();
        if (itemIds != null)
        {
            for (Integer itemId : itemIds)
            {
                if (itemId != null && itemId > 0)
                {
                    copy.add(itemId);
                }
            }
        }
        this.itemIds = Collections.unmodifiableList(copy);
    }

    public Type getType()
    {
        return type;
    }

    public boolean isEmpty()
    {
        return itemIds.isEmpty();
    }

    public int randomItem(Random random)
    {
        if (itemIds.isEmpty()) return -1;
        return itemIds.get(random.nextInt(itemIds.size()));
    }
}
