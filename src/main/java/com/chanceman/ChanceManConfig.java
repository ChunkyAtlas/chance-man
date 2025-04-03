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
            keyName = "gudiRuleSet",
            name = "Gudi Rule Set",
            description = "Blocks more items (Flatpacks and Sets and Poisoned weapons are locked until equivalent weapon poison and base weapon is unlocked)"
    )
    default boolean freeToPlay()
    {
        return false;
    }
    default boolean gudiRuleSet()
    {
        return false;
    }
}
