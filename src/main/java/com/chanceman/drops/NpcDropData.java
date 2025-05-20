package com.chanceman.drops;

import java.util.List;

public class NpcDropData
{
    private int npcId;
    private String name;
    private int level;
    private List<DropTableSection> dropTableSections;

    public NpcDropData(int npcId, String name, int level, List<DropTableSection> dropTableSections)
    {
        this.npcId = npcId;
        this.name = name;
        this.level = level;
        this.dropTableSections = dropTableSections;
    }

    public int getNpcId() { return npcId; }
    public void setNpcId(int npcId) { this.npcId = npcId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public List<DropTableSection> getDropTableSections() { return dropTableSections; }
    public void setDropTableSections(List<DropTableSection> dropTableSections) { this.dropTableSections = dropTableSections; }
}
