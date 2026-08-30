package com.chanceman;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

import java.awt.Color;

@ConfigGroup("chanceman")
public interface ChanceManConfig extends Config
{
    @ConfigSection(
            name = "Roll Pool",
            description = "Configure which items can be included in random rolls.",
            position = 1
    )
    String rollPoolSection = "rollPoolSection";

    @ConfigItem(
            keyName = "freeToPlay",
            name = "Free To Play Mode",
            description = "Only allow free-to-play items",
            position = 0,
            section = rollPoolSection
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
            position = 1,
            section = rollPoolSection
    )
    default boolean includeF2PTradeOnlyItems()
    {
        return false;
    }

    @ConfigItem(
            keyName = "enableItemSets",
            name = "Roll Item Sets",
            description = "Include item set items in the rollable items list. Disabling this will exclude any" +
                    " item set items from random rolls.",
            position = 2,
            section = rollPoolSection
    )
    default boolean enableItemSets()
    {
        return false;
    }

    @ConfigItem(
            keyName = "enableFlatpacks",
            name = "Roll Flatpacks",
            description = "Include flatpacks in the rollable items list. Disabling this will prevent" +
                    " flatpacks from being rolled.",
            position = 3,
            section = rollPoolSection
    )
    default boolean enableFlatpacks()
    {
        return false;
    }

    @ConfigItem(
            keyName = "requireWeaponPoison",
            name = "Weapon Poison Requirements",
            description = "Force poison variants to roll only if both the base weapon and the corresponding" +
                    " weapon poison are unlocked. (Disabling this will allow poisoned variants to roll even if " +
                    "the poison is locked.)",
            position = 4,
            section = rollPoolSection
    )
    default boolean requireWeaponPoison()
    {
        return true;
    }

    @ConfigSection(
            name = "Roll Display & Sound",
            description = "Configure roll sounds and the rolling overlay.",
            position = 6
    )
    String rollDisplaySection = "rollDisplaySection";

    @ConfigItem(
            keyName = "enableRollSounds",
            name = "Enable Roll Sounds",
            description = "Toggle Roll Sound",
            position = 0,
            section = rollDisplaySection
    )
    default boolean enableRollSounds()
    {
        return true;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            keyName = "rollSoundVolume",
            name = "Roll Sound Volume",
            description = "Volume of the roll sound (0–100%).",
            position = 1,
            section = rollDisplaySection
    )
    default int rollSoundVolume()
    {
        return 50;
    }

    @ConfigItem(
            keyName = "rollOverlayScale",
            name = "Rolling Overlay Scale",
            description = "Controls the size of the rolling overlay. 100% matches the current size.",
            position = 2,
            section = rollDisplaySection
    )
    default RollOverlayScale rollOverlayScale()
    {
        return RollOverlayScale.SCALE_100;
    }

    @ConfigSection(
            name = "Show Drops",
            description = "Configure how NPC drop tables are displayed.",
            position = 10
    )
    String showDropsSection = "showDropsSection";

    @ConfigItem(
            keyName = "sortDropsByRarity",
            name = "Sort Drops by Rarity",
            description = "Order drops in the Show Drops menu by rarity instead of item ID.",
            position = 0,
            section = showDropsSection
    )
    default boolean sortDropsByRarity()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showRareDropTable",
            name = "Show Rare Drop Table",
            description = "Include rare drop table items in the Show Drops menu.",
            position = 1,
            section = showDropsSection
    )
    default boolean showRareDropTable()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showGemDropTable",
            name = "Show Gem Drop Table",
            description = "Include gem drop table items in the Show Drops menu.",
            position = 2,
            section = showDropsSection
    )
    default boolean showGemDropTable()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showDropsAlwaysOpen",
            name = "Show Drops Always Open",
            description = "Keep the Show Drops view active when switching away from the Music tab. Use the close button to exit.",
            position = 3,
            section = showDropsSection
    )
    default boolean showDropsAlwaysOpen()
    {
        return false;
    }

    @ConfigSection(
            name = "Chat Message Colors",
            description = "Configure the item colors used in roll chat messages.",
            position = 15
    )
    String chatMessageColorsSection = "chatMessageColorsSection";

    @ConfigItem(
            keyName = "unlockedItemColor",
            name = "Unlocked Item Color",
            description = "Color of the unlocked item name in chat messages.",
            position = 0,
            section = chatMessageColorsSection
    )
    default Color unlockedItemColor()
    {
        return Color.decode("#267567");
    }

    @ConfigItem(
            keyName = "rolledItemColor",
            name = "Rolled Item Color",
            description = "Color of the item used to unlock another item.",
            position = 1,
            section = chatMessageColorsSection
    )
    default Color rolledItemColor()
    {
        return Color.decode("#ff0000");
    }

    @ConfigSection(
            name = "Locked Item Behavior",
            description = "Configure how locked items behave in menus, interfaces, and the Grand Exchange.",
            position = 17
    )
    String lockedItemBehaviorSection = "lockedItemBehaviorSection";

    @ConfigItem(
            keyName = "requireRolledUnlockedForGe",
            name = "GE Requires Obtained & Rolled",
            description = "Only Allow Grand Exchange results for items that have been both obtained and rolled.",
            position = 0,
            section = lockedItemBehaviorSection
    )
    default boolean requireRolledUnlockedForGe()
    {
        return true;
    }

    @ConfigItem(
            keyName = "deprioritizeLockedOptions",
            name = "Deprioritize Locked Options",
            description = "Sorts locked menu options below the Walk Here option.",
            position = 1,
            section = lockedItemBehaviorSection
    )
    default boolean deprioritizeLockedOptions()
    {
        return true;
    }

    @ConfigItem(
            keyName = "dimLockedItemsEnabled",
            name = "Dim Locked Items",
            description = "Dim any item icons that have not been unlocked.",
            position = 2,
            section = lockedItemBehaviorSection
    )
    default boolean dimLockedItemsEnabled()
    {
        return true;
    }

    @Range(min = 0, max = 255)
    @ConfigItem(
            keyName = "dimLockedItemsOpacity",
            name = "Dim Opacity",
            description = "0 = no dim (fully visible), 255 = fully transparent.",
            position = 3,
            section = lockedItemBehaviorSection
    )
    default int dimLockedItemsOpacity()
    {
        return 150;
    }

    @ConfigSection(
            name = "Tool Roll Guarantee",
            description = "Guarantee a tool after too many rolls without one.",
            position = 19
    )
    String toolRollGuaranteeSection = "toolRollGuaranteeSection";

    @ConfigItem(
            keyName = "enableToolRollGuarantee",
            name = "Enable Guarantee",
            description = "Allows tools to roll normally, but guarantees a tool if one has not been rolled within the" +
                    " configured number of rolls.",
            position = 0,
            section = toolRollGuaranteeSection
    )
    default boolean enableToolRollGuarantee()
    {
        return false;
    }

    @Range(min = 1, max = 1000)
    @ConfigItem(
            keyName = "toolRollGuaranteeInterval",
            name = "Max Rolls Between Tools",
            description = "Guarantees at least one tool within this many completed rolls.",
            position = 1,
            section = toolRollGuaranteeSection
    )
    default int toolRollGuaranteeInterval()
    {
        return 100;
    }

    @ConfigSection(
            name = "Quest Item Rolls",
            description = "Roll quest items from a separate chance based pool.",
            position = 20
    )
    String questItemRollsSection = "questItemRollsSection";

    @ConfigItem(
            keyName = "enableQuestItemRolls",
            name = "Enable Quest Rolls",
            description = "Places quest items in a separate pool that becomes more likely after each non-quest roll.",
            position = 0,
            section = questItemRollsSection
    )
    default boolean enableQuestItemRolls()
    {
        return false;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            keyName = "questItemStartingChance",
            name = "Starting Chance (%)",
            description = "Starting chance for a roll to come from the quest-item pool.",
            position = 1,
            section = questItemRollsSection
    )
    default int questItemStartingChance()
    {
        return 2;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            keyName = "questItemChanceIncrease",
            name = "Chance Increase (%)",
            description = "Percentage points added after each completed roll that does not roll a quest item.",
            position = 2,
            section = questItemRollsSection
    )
    default int questItemChanceIncrease()
    {
        return 1;
    }

    @Range(min = 0, max = 100)
    @ConfigItem(
            keyName = "questItemMaximumChance",
            name = "Maximum Chance (%)",
            description = "Highest chance the quest-item pool can reach.",
            position = 3,
            section = questItemRollsSection
    )
    default int questItemMaximumChance()
    {
        return 25;
    }
}