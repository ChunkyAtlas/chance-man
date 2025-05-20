package com.chanceman.drops;

import java.util.List;

public class DropTableSection
{
    private String header;
    private List<DropItem> items;

    public DropTableSection(String header, List<DropItem> items)
    {
        this.header = header;
        this.items = items;
    }

    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }
    public List<DropItem> getItems() { return items; }
    public void setItems(List<DropItem> items) { this.items = items; }
}