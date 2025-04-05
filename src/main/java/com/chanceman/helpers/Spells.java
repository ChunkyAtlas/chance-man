package com.chanceman.helpers;

import java.util.Map;

public class Spells
{
    private int spellId;
    private String name;
    private Map<String, Integer> runes;

    public int getSpellId()
    {
        return spellId;
    }

    public String getName()
    {
        return name;
    }

    public Map<String, Integer> getRunes()
    {
        return runes;
    }
}
