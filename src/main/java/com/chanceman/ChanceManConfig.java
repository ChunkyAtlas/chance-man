package com.chanceman;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import java.awt.Color;

@ConfigGroup("chanceman")
public interface ChanceManConfig extends Config
{
    @ConfigItem(
            keyName = "freeToPlay",
            name = "Free To Play Mode",
            description = "Only allow free-to-play items",
            position = 1
    )
    default boolean freeToPlay()
    {
        return false;
    }

    @ConfigItem(
            keyName = "includeF2PTradeOnlyItems",
            name = "Include F2P trade-only items",
            description = "When Free-to-Play mode is enabled, also roll items that can only " +
                    "be obtained via trading or the Grand Exchange.",
            position = 2
    )
    default boolean includeF2PTradeOnlyItems() { return false; }

    @ConfigItem(
            keyName = "enableItemSets",
            name = "Roll Item Sets",
            description = "Include item set items in the rollable items list. Disabling this will exclude any" +
                    " item set items from random rolls.",
            position = 3
    )
    default boolean enableItemSets() { return true; }

    @ConfigItem(
            keyName = "enableFlatpacks",
            name = "Roll Flatpacks",
            description = "Include flatpacks in the rollable items list. Disabling this will prevent" +
                    " flatpacks from being rolled.",
            position = 4
    )
    default boolean enableFlatpacks() { return true; }

    @ConfigItem(
            keyName = "requireWeaponPoison",
            name = "Weapon Poison Unlock Requirements",
            description = "Force poison variants to roll only if both the base weapon and the corresponding" +
                    " weapon poison are unlocked. (Disabling this will allow poisoned variants to roll even if " +
                    "the poison is locked.)",
            position = 5
    )
    default boolean requireWeaponPoison() { return true; }

    @ConfigItem(
            keyName = "enableRollSounds",
            name = "Enable Roll Sounds",
            description = "Toggle Roll Sound",
            position = 6
    )
    default boolean enableRollSounds() { return true; }

    @ConfigItem(
            keyName = "requireRolledUnlockedForGe",
            name = "GE Requires Rolled and Unlocked",
            description = "Only allow Grand Exchange purchases once items are both rolled and unlocked.",
            position = 7
    )
    default boolean requireRolledUnlockedForGe() { return true; }


    @ConfigItem(
            keyName = "sortDropsByRarity",
            name = "Sort Drops by Rarity",
            description = "Order drops in the Show Drops menu by rarity instead of item ID.",
            position = 8
    )
    default boolean sortDropsByRarity()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showRareDropTable",
            name = "Show Rare Drop Table",
            description = "Include rare drop table items in the Show Drops menu.",
            position = 9
    )
    default boolean showRareDropTable() { return true; }

    @ConfigItem(
            keyName = "showGemDropTable",
            name = "Show Gem Drop Table",
            description = "Include gem drop table items in the Show Drops menu.",
            position = 10
    )
    default boolean showGemDropTable() { return true; }

    @ConfigItem(
            keyName = "unlockedItemColor",
            name = "Unlocked Item Color",
            description = "Color of the unlocked item name in chat messages.",
            position = 11
    )
    default Color unlockedItemColor()
    {
        return Color.decode("#267567");
    }

    @ConfigItem(
            keyName = "rolledItemColor",
            name = "Rolled Item Color",
            description = "Color of the item used to unlock another item.",
            position = 12
    )
    default Color rolledItemColor()
    {
        return Color.decode("#ff0000");
    }
}
