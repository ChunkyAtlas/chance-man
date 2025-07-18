package com.chanceman.drops;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DropItem
{
    private int itemId;
    private String name;
    private String rarity;

    public DropItem(int itemId, String name, String rarity)
    {
        this.itemId = itemId;
        this.name = name;
        this.rarity = rarity;
    }

    /**
     * Convert a raw rarity string like "2/128" to a simplified
     * one-over form, e.g. "1/64". If no fraction is detected,
     * return the original string.
     */
    public String getOneOverRarity()
    {
        if (rarity == null)
        {
            return "";
        }

        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)")
                .matcher(rarity);

        if (m.find())
        {
            double a = Double.parseDouble(m.group(1));
            double b = Double.parseDouble(m.group(2));
            if (a != 0)
            {
                double val = b / a;
                if (Math.abs(val - Math.round(val)) < 0.01)
                {
                    return "1/" + Math.round(val);
                }
                return String.format("1/%.2f", val);
            }
        }

        return rarity;
    }
}