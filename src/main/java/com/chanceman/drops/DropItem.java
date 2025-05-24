package com.chanceman.drops;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DropItem
{
    private int itemId;
    private String name;

    public DropItem(int itemId, String name)
    {
        this.itemId = itemId;
        this.name = name;
    }
}