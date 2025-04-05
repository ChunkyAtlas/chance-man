package com.chanceman.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolActionMapping
{
    private static final Map<String, List<String>> toolActionMap = new HashMap<>();

    static
    {
        toolActionMap.put("mine", List.of("pickaxe"));
        toolActionMap.put("chop down", List.of("axe"));
        toolActionMap.put("shear", List.of("shears"));
        toolActionMap.put("light", List.of("tinderbox"));
        toolActionMap.put("cut", List.of("knife"));
        toolActionMap.put("grind", List.of("pestle & mortar"));
        toolActionMap.put("fish", List.of("fishing rod", "small fishing net", "lobster pot", "big fishing net"));
        toolActionMap.put("net", List.of("fishing rod", "small fishing net", "lobster pot", "big fishing net"));
        toolActionMap.put("bait", List.of("fishing rod", "fishing net", "lobster pot"));
        toolActionMap.put("harpoon", List.of("harpoon", "barb-tail harpoon", "dragon harpoon"));
        toolActionMap.put("rake", List.of("rake"));
        toolActionMap.put("clear", List.of("rake"));
        toolActionMap.put("plant", List.of("seed dibber"));
        toolActionMap.put("dig", List.of("spade"));
        toolActionMap.put("prune", List.of("secateurs"));
        toolActionMap.put("cure", List.of("secateurs"));
    }

    public static Map<String, List<String>> getToolActionMap()
    {
        return toolActionMap;
    }
}
