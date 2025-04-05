package com.chanceman;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("chanceman")
public interface ChanceManConfig extends Config
{
    @ConfigItem(
            keyName = "freeToPlay",
            name = "Free To Play Mode",
            description = "Only allow free-to-play items"
    )
    @ConfigItem(
            keyName = "noFlatpacks",
            name = "No Flatpacks",
            description = "Blocks flatpacks from being rollable"
    )
    @ConfigItem(
            keyName = "noSets",
            name = "No Armour Sets",
            description = "Blocks armour sets from being rollable"
    )
    @ConfigItem(
            keyName = "NoPoisoned",
            name = "No Poisoned Weapons",
            description = "Blocks poisoned weapons from being rollable"
    )
    default boolean freeToPlay()
    {
        return false;
    }
    default boolean noFlatpacks()
    {
        return false;
    }
    default boolean noSets()
    {
        return false;
    }
    default boolean noPoisoned()
    {
        return false;
    }
}
