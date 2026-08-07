package com.chanceman.drops;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class NpcDropData {
    private int npcId;
    private String name;
    private int level;
    private List<DropTableSection> dropTableSections;
}
