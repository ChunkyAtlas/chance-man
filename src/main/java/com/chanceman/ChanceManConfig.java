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
    default boolean enableItemSets() { return false; }

    @ConfigItem(
            keyName = "enableFlatpacks",
            name = "Roll Flatpacks",
            description = "Include flatpacks in the rollable items list. Disabling this will prevent" +
                    " flatpacks from being rolled.",
            position = 4
    )
    default boolean enableFlatpacks() { return false; }

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

    @net.runelite.client.config.Range(min = 0, max = 100)
    @ConfigItem(
            keyName = "rollSoundVolume",
            name = "Roll Sound Volume",
            description = "Volume of the roll sound (0–100%).",
            position = 7
    )
    default int rollSoundVolume() { return 50; }

    @ConfigItem(
            keyName = "rollOverlayScale",
            name = "Rolling Overlay Scale",
            description = "Controls the size of the rolling overlay. 100% matches the current size.",
            position = 8
    )
    default RollOverlayScale rollOverlayScale() { return RollOverlayScale.SCALE_100; }

    @ConfigItem(
            keyName = "requireRolledUnlockedForGe",
            name = "GE Requires Obtained and Rolled",
            description = "Only Allow Grand Exchange results for items that have been both obtained and rolled.",
            position = 9
    )
    default boolean requireRolledUnlockedForGe() { return true; }

    @ConfigItem(
            keyName = "sortDropsByRarity",
            name = "Sort Drops by Rarity",
            description = "Order drops in the Show Drops menu by rarity instead of item ID.",
            position = 10
    )
    default boolean sortDropsByRarity() { return true; }

    @ConfigItem(
            keyName = "showRareDropTable",
            name = "Show Rare Drop Table",
            description = "Include rare drop table items in the Show Drops menu.",
            position = 11
    )
    default boolean showRareDropTable() { return true; }

    @ConfigItem(
            keyName = "showGemDropTable",
            name = "Show Gem Drop Table",
            description = "Include gem drop table items in the Show Drops menu.",
            position = 12
    )
    default boolean showGemDropTable() { return true; }

    @ConfigItem(
            keyName = "showDropsAlwaysOpen",
            name = "Show Drops Always Open",
            description = "Keep the Show Drops view active when switching away from the Music tab. Use the close button to exit.",
            position = 13
    )
    default boolean showDropsAlwaysOpen()
    {
        return false;
    }

    @ConfigItem(
            keyName = "deprioritizeLockedOptions",
            name = "Deprioritize Locked Menu Options",
            description = "Sorts locked menu options below the Walk Here option.",
            position = 14
    )
    default boolean deprioritizeLockedOptions() { return true; }

    @ConfigItem(
            keyName = "unlockedItemColor",
            name = "Unlocked Item Color",
            description = "Color of the unlocked item name in chat messages.",
            position = 15
    )
    default Color unlockedItemColor()
    {
        return Color.decode("#267567");
    }

    @ConfigItem(
            keyName = "rolledItemColor",
            name = "Rolled Item Color",
            description = "Color of the item used to unlock another item.",
            position = 16
    )
    default Color rolledItemColor()
    {
        return Color.decode("#ff0000");
    }

    @ConfigItem(
            keyName = "dimLockedItemsEnabled",
            name = "Dim locked items",
            description = "Dim any item icons that have not been unlocked.",
            position = 17
    )
    default boolean dimLockedItemsEnabled()
    {
        return true;
    }

    @net.runelite.client.config.Range(min = 0, max = 255)
    @ConfigItem(
            keyName = "dimLockedItemsOpacity",
            name = "Dim opacity",
            description = "0 = no dim (fully visible), 255 = fully transparent.",
            position = 18
    )
    default int dimLockedItemsOpacity()
    {
        return 150;
    }

    @ConfigItem(
            keyName = "enableRelationalRolls",
            name = "Enable Relational Rolls",
            description = "Use tool cadence and gradually increasing quest-item weighting.",
            position = 19
    )
    default boolean enableRelationalRolls() { return false; }

    @net.runelite.client.config.Range(min = 1, max = 100)
    @ConfigItem(
            keyName = "toolRollInterval",
            name = "Tool Roll Every X Rolls",
            description = "Guarantee a tool-pool roll after this many completed rolls.",
            position = 20
    )
    default int toolRollInterval() { return 100; }

    @ConfigItem(
            keyName = "questBaseWeight",
            name = "Quest Base Weight",
            description = "Starting relative chance for the quest pool versus the normal pool.",
            position = 22
    )
    default double questBaseWeight() { return 0.02; }

    @ConfigItem(
            keyName = "questWeightGrowth",
            name = "Quest Weight per Miss",
            description = "Additional quest-pool weight added after each non-quest roll.",
            position = 23
    )
    default double questWeightGrowth() { return 0.01; }

    @ConfigItem(
            keyName = "questWeightCap",
            name = "Quest Weight Cap",
            description = "Maximum quest-pool weight; set to 0 for no cap.",
            position = 24
    )
    default double questWeightCap() { return 0.25; }
}
