package com.chanceman.filters;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum ItemSets {

    // Items are sorted in the same order as displayed on the Grand Exchange interface
    DwarfCannon(ItemID.SET_CANNON),

    // Dragonhide sets
    GreenDragonhide(ItemID.SET_DHIDE_GREEN),
    BlueDragonhide(ItemID.SET_DHIDE_BLUE),
    RedDragonhide(ItemID.SET_DHIDE_RED),
    BlackDragonhide(ItemID.SET_DHIDE_BLACK),

    // Barrows armour sets
    Guthans(ItemID.SET_BARROWS_GUTHAN),
    Veracs(ItemID.SET_BARROWS_VERAC),
    Dharoks(ItemID.SET_BARROWS_DHAROK),
    Torags(ItemID.SET_BARROWS_TORAG),
    Ahrims(ItemID.SET_BARROWS_AHRIM),
    Karils(ItemID.SET_BARROWS_KARIL),

    // Potion sets
    CombatPotion(ItemID.SET_COMBAT_POTION),
    SuperPotion(ItemID.SET_SUPERCOMBAT_POTION),

    // Base metal armour sets
    BronzeLegs(ItemID.SET_BRONZE_LEGS),
    BronzeSkirt(ItemID.SET_BRONZE_SKIRT),

    IronLegs(ItemID.SET_IRON_LEGS),
    IronSkirt(ItemID.SET_IRON_SKIRT),

    SteelLegs(ItemID.SET_STEEL_LEGS),
    SteelSkirt(ItemID.SET_STEEL_SKIRT),

    BlackLegs(ItemID.SET_BLACK_LEGS),
    BlackSkirt(ItemID.SET_BLACK_SKIRT),

    MithrilLegs(ItemID.SET_MITHRIL_LEGS),
    MithrilSkirt(ItemID.SET_MITHRIL_SKIRT),

    AdamantLegs(ItemID.SET_ADAMANT_LEGS),
    AdamantSkirt(ItemID.SET_ADAMANT_SKIRT),

    RuneLegs(ItemID.SET_RUNE_LEGS),
    RuneSkirt(ItemID.SET_RUNE_SKIRT),

    // Trimmed and gilded base metal armour sets

    BronzeLegsTrim(ItemID.SET_BRONZE_LEGS_TRIM),
    BronzeSkirtTrim(ItemID.SET_BRONZE_SKIRT_TRIM),
    BronzeLegsGold(ItemID.SET_BRONZE_LEGS_GOLD),
    BronzeSkirtGold(ItemID.SET_BRONZE_SKIRT_GOLD),

    IronLegsTrim(ItemID.SET_IRON_LEGS_TRIM),
    IronSkirtTrim(ItemID.SET_IRON_SKIRT_TRIM),
    IronLegsGold(ItemID.SET_IRON_LEGS_GOLD),
    IronSkirtGold(ItemID.SET_IRON_SKIRT_GOLD),

    SteelLegsTrim(ItemID.SET_STEEL_LEGS_TRIM),
    SteelSkirtTrim(ItemID.SET_STEEL_SKIRT_TRIM),
    SteelLegsGold(ItemID.SET_STEEL_LEGS_GOLD),
    SteelSkirtGold(ItemID.SET_STEEL_SKIRT_GOLD),

    BlackLegsTrim(ItemID.SET_BLACK_LEGS_TRIM),
    BlackSkirtTrim(ItemID.SET_BLACK_SKIRT_TRIM),
    BlackLegsGold(ItemID.SET_BLACK_LEGS_GOLD),
    BlackSkirtGold(ItemID.SET_BLACK_SKIRT_GOLD),

    MithrilLegsTrim(ItemID.SET_MITHRIL_LEGS_TRIM),
    MithrilSkirtTrim(ItemID.SET_MITHRIL_SKIRT_TRIM),
    MithrilLegsGold(ItemID.SET_MITHRIL_LEGS_GOLD),
    MithrilSkirtGold(ItemID.SET_MITHRIL_SKIRT_GOLD),

    AdamantLegsTrim(ItemID.SET_ADAMANT_LEGS_TRIM),
    AdamantSkirtTrim(ItemID.SET_ADAMANT_SKIRT_TRIM),
    AdamantLegsGold(ItemID.SET_ADAMANT_LEGS_GOLD),
    AdamantSkirtGold(ItemID.SET_ADAMANT_SKIRT_GOLD),

    RuneLegsTrim(ItemID.SET_RUNE_LEGS_TRIM),
    RuneSkirtTrim(ItemID.SET_RUNE_SKIRT_TRIM),
    RuneLegsGold(ItemID.SET_RUNE_LEGS_GOLD),
    RuneSkirtGold(ItemID.SET_RUNE_SKIRT_GOLD),

    // Dragon armour sets
    DragonLegs(ItemID.SET_DRAGON_LEGS),
    DragonSkirt(ItemID.SET_DRAGON_SKIRT),

    // God rune armour sets
    SaradominLegs(ItemID.SET_RUNE_LEGS_SARADOMIN),
    SaradominSkirt(ItemID.SET_RUNE_SKIRT_SARADOMIN),

    ZamorakLegs(ItemID.SET_RUNE_LEGS_ZAMORAK),
    ZamorakSkirt(ItemID.SET_RUNE_SKIRT_ZAMORAK),

    GuthixLegs(ItemID.SET_RUNE_LEGS_GUTHIX),
    GuthixSkirt(ItemID.SET_RUNE_SKIRT_GUTHIX),

    ArmadylLegs(ItemID.SET_RUNE_LEGS_ARMADYL),
    ArmadylSkirt(ItemID.SET_RUNE_SKIRT_ARMADYL),

    BandosLegs(ItemID.SET_RUNE_LEGS_BANDOS),
    BandosSkirt(ItemID.SET_RUNE_SKIRT_BANDOS),

    AncientLegs(ItemID.SET_RUNE_LEGS_ANCIENT),
    AncientSkirt(ItemID.SET_RUNE_SKIRT_ANCIENT),

    // Gilded armour sets
    GildedArmourLegs(ItemID.SET_RUNE_LEGS_GOLDPLATE),
    GildedArmourSkirt(ItemID.SET_RUNE_SKIRT_GOLDPLATE),

    // God book page sets
    HolyBookPage(ItemID.SET_HOLY_BOOK),
    UnholyBookPage(ItemID.SET_UNHOLY_BOOK),
    BookBalancePage(ItemID.SET_BOOK_BALANCE),
    BookWarPage(ItemID.SET_BOOK_WAR),
    BookLawPage(ItemID.SET_BOOK_LAW),
    BookDarknessPage(ItemID.SET_BOOK_DARKNESS),

    // Dragonhide sets
    GildedDragonhide(ItemID.SET_DHIDE_GILDED),
    ZamorakDragonhide(ItemID.SET_DHIDE_ZAMORAK),
    SaradominDragonhide(ItemID.SET_DHIDE_SARADOMIN),
    GuthixDragonhide(ItemID.SET_DHIDE_GUTHIX),
    BandosDragonhide(ItemID.SET_DHIDE_BANDOS),
    ArmadylDragonhide(ItemID.SET_DHIDE_ARMADYL),
    AncientDragonhide(ItemID.SET_DHIDE_ANCIENT),

    // Miscellaneous sets
    OathplateSet(ItemID.SET_OATHPLATE_ARMOUR),
    PartyhatSet(ItemID.SET_PARTYHAT),
    HalloweenMaskSet(ItemID.SET_HALLOWEEN_MASK),
    AncestralRobes(ItemID.SET_ANCESTRAL_ROBES),
    ObsidianArmour(ItemID.SET_OBSIDIAN_ARMOUR),
    JusticiarArmour(ItemID.SET_JUSTICIAR_ARMOUR),
    MysticLight(ItemID.SET_MYSTIC_LIGHT),
    MysticBlue(ItemID.SET_MYSTIC_BLUE),
    MysticDark(ItemID.SET_MYSTIC_DARK),
    MysticDusk(ItemID.SET_MYSTIC_DUSK),
    DragonstoneArmour(ItemID.SET_DRAGONSTONE_ARMOUR),
    DagonhaiRobes(ItemID.SET_DAGONHAI_ROBES),
    InquisitorsArmour(ItemID.SET_INQUISITORS_ARMOUR),
    MasoriFortified(ItemID.SET_MASORI_FORTIFIED),
    // Leagues item sets are omitted as they are globally blocked
    SunfireFanatic(ItemID.SET_SUNFIRE_FANATIC);

    @Getter
    private final int id;

    ItemSets(int id) {
        this.id = id;
    }

    /**
     * Returns an unmodifiable set of all item set item IDs.
     */
    public static Set<Integer> getAllItemSetIds() {
        Set<Integer> ids = new HashSet<>();
        for (ItemSets as : ItemSets.values()) {
            ids.add(as.getId());
        }
        return Collections.unmodifiableSet(ids);
    }

    /**
     * Checks if the given item ID is one of the item set items.
     *
     * @param id the item id to check.
     * @return true if the id belongs to an item set, false otherwise.
     */
    public static boolean isItemSet(int id) {
        return getAllItemSetIds().contains(id);
    }
}
