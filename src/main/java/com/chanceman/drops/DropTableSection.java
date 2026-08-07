package com.chanceman.drops;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@AllArgsConstructor
public class DropTableSection {
    private String header;
    private List<DropItem> items;
}
