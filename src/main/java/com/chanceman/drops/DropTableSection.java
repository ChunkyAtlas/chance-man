package com.chanceman.drops;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class DropTableSection
{
    private String header;
    private List<DropItem> items;

    public DropTableSection(String header, List<DropItem> items)
    {
        this.header = header;
        this.items = items;
    }

}