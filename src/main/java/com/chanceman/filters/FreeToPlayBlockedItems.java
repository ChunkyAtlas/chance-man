package com.chanceman.filters;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum FreeToPlayBlockedItems {

    // Random
    Charcoal(ItemID.CHARCOAL),
    RawBoarMeat(ItemID.RAW_BOAR_MEAT),
    PureEssence(ItemID.BLANKRUNE_HIGH),
    CastleWarsBracelet3(ItemID.JEWL_CASTLEWARS_BRACELET3),
    CursedAmuletOfMagic(ItemID.AMULET_OF_MAGIC_CURSED),
    Dust(ItemID.DUST),

    // Dyed boots and gloves
    GreyBoots(ItemID.WOLFENBOOTS_GREY),
    RedBoots(ItemID.WOLFENBOOTS_CRIMSON),
    YellowBoots(ItemID.WOLFENBOOTS_TANGERINE),
    TealBoots(ItemID.WOLFENBOOTS_OCEAN),
    PurpleBoots(ItemID.WOLFENBOOTS_PURPLE),
    GreyGloves(ItemID.WOLFENGLOVES_GREY),
    RedGloves(ItemID.WOLFENGLOVES_CRIMSON),
    YellowGloves(ItemID.WOLFENGLOVES_TANGERINE),
    TealGloves(ItemID.WOLFENGLOVES_OCEAN),
    PurpleGloves(ItemID.WOLFENGLOVES_PURPLE),

    // Shade
    ShadeRobeTop(ItemID.BLACKROBETOP),
    ShadeRobe(ItemID.BLACKROBEBOTTOM),

    // Elven wear
    ElvenTop(ItemID.PRIF_ELVEN_CLOTHES_TOP_1),
    ElvenSkirt(ItemID.PRIF_ELVEN_CLOTHES_BOTTOMS_1),
    ElvenTop2(ItemID.PRIF_ELVEN_CLOTHES_TOP_2),
    ElvenSkirt2(ItemID.PRIF_ELVEN_CLOTHES_BOTTOMS_2),
    ElvenTop3(ItemID.PRIF_ELVEN_CLOTHES_TOP_3),
    ElvenLegwear(ItemID.PRIF_ELVEN_CLOTHES_BOTTOMS_3),
    ElvenTop4(ItemID.PRIF_ELVEN_CLOTHES_TOP_4),

    // Dragonstone
    DragonstoneFullHelm(ItemID.DRAGONSTONE_HELMET),
    DragonstonePlatebody(ItemID.DRAGONSTONE_PLATEBODY),
    DragonstonePlatelegs(ItemID.DRAGONSTONE_PLATELEGS),

    // Wilderness capes (the rest are accessible on f2p)
    Team4Cape(ItemID.WILDERNESS_CAPE_4),
    Team5Cape(ItemID.WILDERNESS_CAPE_5),
    Team14Cape(ItemID.WILDERNESS_CAPE_14),
    Team15Cape(ItemID.WILDERNESS_CAPE_15),
    Team24Cape(ItemID.WILDERNESS_CAPE_24),
    Team25Cape(ItemID.WILDERNESS_CAPE_25),
    Team34Cape(ItemID.WILDERNESS_CAPE_34),
    Team35Cape(ItemID.WILDERNESS_CAPE_35),
    Team44Cape(ItemID.WILDERNESS_CAPE_44),
    Team45Cape(ItemID.WILDERNESS_CAPE_45),

    // Clue Scroll - Random
    WillowCompBow(ItemID.TRAIL_COMPOSITE_BOW_WILLOW),
    HamJoint(ItemID.JOINT_OF_HAM),
    WoodenShieldG(ItemID.WOODEN_SHIELD_GOLD),

    // Clue Scroll - Random Gilded
    GildedScimitar(ItemID.RUNE_SCIMITAR_GOLD),
    Gilded2HSword(ItemID.RUNE_2H_SWORD_GOLD),
    GoldenChefsHat(ItemID.CHEFS_HAT_GOLD),
    GoldenApron(ItemID.GOLDEN_APRON),
    GildedPickaxe(ItemID.TRAIL_GILDED_PICKAXE),
    GildedAxe(ItemID.TRAIL_GILDED_AXE),
    GildedSpade(ItemID.TRAIL_GILDED_SPADE),

    // Clue Scroll - Cape
    TeamCapeZero(ItemID.WILDERNESS_CAPE_ZERO),
    TeamCapeX(ItemID.WILDERNESS_CAPE_X),
    TeamCapeI(ItemID.WILDERNESS_CAPE_I),
    WolfCloak(ItemID.WOLF_CLOAK),

    // Clue Scroll - Amulet
    StrengthAmuletT(ItemID.TRAIL_AMULET_OF_STRENGTH),
    AmuletOfMagicT(ItemID.TRAIL_AMULET_OF_MAGIC),
    AmuletOfPowerT(ItemID.TRAIL_POWER_AMMY),

    // Clue Scroll - Ring
    RingOfNature(ItemID.RING_OF_NATURE),
    RingOfCoins(ItemID.RING_OF_COINS),
    RingOf3RdAge(ItemID.RING_OF_3RD_AGE),

    // Clue Scroll - Bronze Armor
    BronzePlatebodyG(ItemID.BRONZE_PLATEBODY_GOLD),
    BronzePlatelegsG(ItemID.BRONZE_PLATELEGS_GOLD),
    BronzePlateskirtG(ItemID.BRONZE_PLATESKIRT_GOLD),
    BronzeFullHelmG(ItemID.BRONZE_FULL_HELM_GOLD),
    BronzeKiteshieldG(ItemID.BRONZE_KITESHIELD_GOLD),
    BronzePlatebodyT(ItemID.BRONZE_PLATEBODY_TRIM),
    BronzePlatelegsT(ItemID.BRONZE_PLATELEGS_TRIM),
    BronzePlateskirtT(ItemID.BRONZE_PLATESKIRT_TRIM),
    BronzeFullHelmT(ItemID.BRONZE_FULL_HELM_TRIM),
    BronzeKiteshieldT(ItemID.BRONZE_KITESHIELD_TRIM),

    // Clue Scroll - Iron Armor
    IronPlatebodyT(ItemID.IRON_PLATEBODY_TRIM),
    IronPlatelegsT(ItemID.IRON_PLATELEGS_TRIM),
    IronPlateskirtT(ItemID.IRON_PLATESKIRT_TRIM),
    IronFullHelmT(ItemID.IRON_FULL_HELM_TRIM),
    IronKiteshieldT(ItemID.IRON_KITESHIELD_TRIM),
    IronPlatebodyG(ItemID.IRON_PLATEBODY_GOLD),
    IronPlatelegsG(ItemID.IRON_PLATELEGS_GOLD),
    IronPlateskirtG(ItemID.IRON_PLATESKIRT_GOLD),
    IronFullHelmG(ItemID.IRON_FULL_HELM_GOLD),
    IronKiteshieldG(ItemID.IRON_KITESHIELD_GOLD),

    // Clue Scroll - Steel Armor
    SteelPlatebodyG(ItemID.STEEL_PLATEBODY_GOLD),
    SteelPlatelegsG(ItemID.STEEL_PLATELEGS_GOLD),
    SteelPlateskirtG(ItemID.STEEL_PLATESKIRT_GOLD),
    SteelFullHelmG(ItemID.STEEL_FULL_HELM_GOLD),
    SteelKiteshieldG(ItemID.STEEL_KITESHIELD_GOLD),
    SteelPlatebodyT(ItemID.STEEL_PLATEBODY_TRIM),
    SteelPlatelegsT(ItemID.STEEL_PLATELEGS_TRIM),
    SteelPlateskirtT(ItemID.STEEL_PLATESKIRT_TRIM),
    SteelFullHelmT(ItemID.STEEL_FULL_HELM_TRIM),
    SteelKiteshieldT(ItemID.STEEL_KITESHIELD_TRIM),

    // Clue Scroll - Mithril Armor
    MithrilPlatebodyG(ItemID.MITHRIL_PLATEBODY_GOLD),
    MithrilPlatelegsG(ItemID.MITHRIL_PLATELEGS_GOLD),
    MithrilKiteshieldG(ItemID.MITHRIL_KITESHIELD_GOLD),
    MithrilFullHelmG(ItemID.MITHRIL_FULL_HELM_GOLD),
    MithrilPlateskirtG(ItemID.MITHRIL_PLATESKIRT_GOLD),
    MithrilPlatebodyT(ItemID.MITHRIL_PLATEBODY_TRIM),
    MithrilPlatelegsT(ItemID.MITHRIL_PLATELEGS_TRIM),
    MithrilKiteshieldT(ItemID.MITHRIL_KITESHIELD_TRIM),
    MithrilFullHelmT(ItemID.MITHRIL_FULL_HELM_TRIM),
    MithrilPlateskirtT(ItemID.MITHRIL_PLATESKIRT_TRIM),

    // Clue Scroll - Black Armor
    BlackPlatebodyT(ItemID.BLACK_PLATEBODY_TRIM),
    BlackPlatelegsT(ItemID.BLACK_PLATELEGS_TRIM),
    BlackFullHelmT(ItemID.BLACK_FULL_HELM_TRIM),
    BlackKiteshieldT(ItemID.BLACK_KITESHIELD_TRIM),
    BlackPlateskirtT(ItemID.BLACK_PLATESKIRT_TRIM),
    BlackPlatebodyG(ItemID.BLACK_PLATEBODY_GOLD),
    BlackPlatelegsG(ItemID.BLACK_PLATELEGS_GOLD),
    BlackFullHelmG(ItemID.BLACK_FULL_HELM_GOLD),
    BlackKiteshieldG(ItemID.BLACK_KITESHIELD_GOLD),
    BlackPlateskirtG(ItemID.BLACK_PLATESKIRT_GOLD),
    BlackShieldH1(ItemID.BLACK_HERALDIC_KITESHIELD1),
    BlackShieldH2(ItemID.BLACK_HERALDIC_KITESHIELD2),
    BlackShieldH3(ItemID.BLACK_HERALDIC_KITESHIELD3),
    BlackShieldH4(ItemID.BLACK_HERALDIC_KITESHIELD4),
    BlackShieldH5(ItemID.BLACK_HERALDIC_KITESHIELD5),
    BlackHelmH1(ItemID.TRAIL_HERALDIC_HELM_1_BLACK),
    BlackHelmH2(ItemID.TRAIL_HERALDIC_HELM_2_BLACK),
    BlackHelmH3(ItemID.TRAIL_HERALDIC_HELM_3_BLACK),
    BlackHelmH4(ItemID.TRAIL_HERALDIC_HELM_4_BLACK),
    BlackHelmH5(ItemID.TRAIL_HERALDIC_HELM_5_BLACK),
    BlackPlatebodyH1(ItemID.BLACK_PLATEBODY_H1),
    BlackPlatebodyH2(ItemID.BLACK_PLATEBODY_H2),
    BlackPlatebodyH3(ItemID.BLACK_PLATEBODY_H3),
    BlackPlatebodyH4(ItemID.BLACK_PLATEBODY_H4),
    BlackPlatebodyH5(ItemID.BLACK_PLATEBODY_H5),

    // Clue Scroll - Adamant Armor
    AdamantPlatebodyT(ItemID.ADAMANT_PLATEBODY_TRIM),
    AdamantPlatelegsT(ItemID.ADAMANT_PLATELEGS_TRIM),
    AdamantKiteshieldT(ItemID.ADAMANT_KITESHIELD_TRIM),
    AdamantFullHelmT(ItemID.ADAMANT_FULL_HELM_TRIM),
    AdamantPlateskirtT(ItemID.ADAMANT_PLATESKIRT_TRIM),
    AdamantPlatebodyG(ItemID.ADAMANT_PLATEBODY_GOLD),
    AdamantPlatelegsG(ItemID.ADAMANT_PLATELEGS_GOLD),
    AdamantKiteshieldG(ItemID.ADAMANT_KITESHIELD_GOLD),
    AdamantFullHelmG(ItemID.ADAMANT_FULL_HELM_GOLD),
    AdamantPlateskirtG(ItemID.ADAMANT_PLATESKIRT_GOLD),
    AdamantShieldH1(ItemID.ADAMANT_HERALDIC_KITESHIELD1),
    AdamantShieldH2(ItemID.ADAMANT_HERALDIC_KITESHIELD2),
    AdamantShieldH3(ItemID.ADAMANT_HERALDIC_KITESHIELD3),
    AdamantShieldH4(ItemID.ADAMANT_HERALDIC_KITESHIELD4),
    AdamantShieldH5(ItemID.ADAMANT_HERALDIC_KITESHIELD5),
    AdamantHelmH1(ItemID.TRAIL_HERALDIC_HELM_1_ADAMANT),
    AdamantHelmH2(ItemID.TRAIL_HERALDIC_HELM_2_ADAMANT),
    AdamantHelmH3(ItemID.TRAIL_HERALDIC_HELM_3_ADAMANT),
    AdamantHelmH4(ItemID.TRAIL_HERALDIC_HELM_4_ADAMANT),
    AdamantHelmH5(ItemID.TRAIL_HERALDIC_HELM_5_ADAMANT),
    AdamantPlatebodyH1(ItemID.ADAMANT_PLATEBODY_H1),
    AdamantPlatebodyH2(ItemID.ADAMANT_PLATEBODY_H2),
    AdamantPlatebodyH3(ItemID.ADAMANT_PLATEBODY_H3),
    AdamantPlatebodyH4(ItemID.ADAMANT_PLATEBODY_H4),
    AdamantPlatebodyH5(ItemID.ADAMANT_PLATEBODY_H5),

    // Clue Scroll - Rune Armor
    RunePlatebodyT(ItemID.RUNE_PLATEBODY_TRIM),
    RunePlatelegsT(ItemID.RUNE_PLATELEGS_TRIM),
    RuneFullHelmT(ItemID.RUNE_FULL_HELM_TRIM),
    RuneKiteshieldT(ItemID.RUNE_KITESHIELD_TRIM),
    RunePlateskirtT(ItemID.RUNE_PLATESKIRT_TRIM),
    RunePlatebodyG(ItemID.RUNE_PLATEBODY_GOLD),
    RunePlatelegsG(ItemID.RUNE_PLATELEGS_GOLD),
    RuneFullHelmG(ItemID.RUNE_FULL_HELM_GOLD),
    RuneKiteshieldG(ItemID.RUNE_KITESHIELD_GOLD),
    RunePlateskirtG(ItemID.RUNE_PLATESKIRT_GOLD),
    RuneShieldH1(ItemID.RUNE_HERALDIC_KITESHIELD1),
    RuneShieldH2(ItemID.RUNE_HERALDIC_KITESHIELD2),
    RuneShieldH3(ItemID.RUNE_HERALDIC_KITESHIELD3),
    RuneShieldH4(ItemID.RUNE_HERALDIC_KITESHIELD4),
    RuneShieldH5(ItemID.RUNE_HERALDIC_KITESHIELD5),
    RuneHelmH1(ItemID.TRAIL_HERALDIC_HELM_1_RUNE),
    RuneHelmH2(ItemID.TRAIL_HERALDIC_HELM_2_RUNE),
    RuneHelmH3(ItemID.TRAIL_HERALDIC_HELM_3_RUNE),
    RuneHelmH4(ItemID.TRAIL_HERALDIC_HELM_4_RUNE),
    RuneHelmH5(ItemID.TRAIL_HERALDIC_HELM_5_RUNE),
    RunePlatebodyH1(ItemID.RUNE_PLATEBODY_H1),
    RunePlatebodyH2(ItemID.RUNE_PLATEBODY_H2),
    RunePlatebodyH3(ItemID.RUNE_PLATEBODY_H3),
    RunePlatebodyH4(ItemID.RUNE_PLATEBODY_H4),
    RunePlatebodyH5(ItemID.RUNE_PLATEBODY_H5),

    // Clue Scroll - God Armor
    ZamorakPlatebody(ItemID.RUNE_PLATEBODY_ZAMORAK),
    ZamorakPlatelegs(ItemID.RUNE_PLATELEGS_ZAMORAK),
    ZamorakFullHelm(ItemID.RUNE_FULL_HELM_ZAMORAK),
    ZamorakKiteshield(ItemID.RUNE_KITESHIELD_ZAMORAK),
    ZamorakPlateskirt(ItemID.RUNE_PLATESKIRT_ZAMORAK),
    SaradominPlatebody(ItemID.RUNE_PLATEBODY_SARADOMIN),
    SaradominPlatelegs(ItemID.RUNE_PLATELEGS_SARADOMIN),
    SaradominFullHelm(ItemID.RUNE_FULL_HELM_SARADOMIN),
    SaradominKiteshield(ItemID.RUNE_KITESHIELD_SARADOMIN),
    SaradominPlateskirt(ItemID.RUNE_PLATESKIRT_SARADOMIN),
    GuthixPlatebody(ItemID.RUNE_PLATEBODY_GUTHIX),
    GuthixPlatelegs(ItemID.RUNE_PLATELEGS_GUTHIX),
    GuthixFullHelm(ItemID.RUNE_FULL_HELM_GUTHIX),
    GuthixKiteshield(ItemID.RUNE_KITESHIELD_GUTHIX),
    GuthixPlateskirt(ItemID.RUNE_PLATESKIRT_GUTHIX),
    AncientPlatebody(ItemID.RUNE_PLATEBODY_ANCIENT),
    AncientPlatelegs(ItemID.RUNE_PLATELEGS_ANCIENT),
    AncientPlateskirt(ItemID.RUNE_PLATESKIRT_ANCIENT),
    AncientFullHelm(ItemID.RUNE_FULL_HELM_ANCIENT),
    AncientKiteshield(ItemID.RUNE_KITESHIELD_ANCIENT),
    ArmadylPlatebody(ItemID.RUNE_PLATEBODY_ARMADYL),
    ArmadylPlatelegs(ItemID.RUNE_PLATELEGS_ARMADYL),
    ArmadylPlateskirt(ItemID.RUNE_PLATESKIRT_ARMADYL),
    ArmadylFullHelm(ItemID.RUNE_FULL_HELM_ARMADYL),
    ArmadylKiteshield(ItemID.RUNE_KITESHIELD_ARMADYL),
    BandosPlatebody(ItemID.RUNE_PLATEBODY_BANDOS),
    BandosPlatelegs(ItemID.RUNE_PLATELEGS_BANDOS),
    BandosPlateskirt(ItemID.RUNE_PLATESKIRT_BANDOS),
    BandosFullHelm(ItemID.RUNE_FULL_HELM_BANDOS),
    BandosKiteshield(ItemID.RUNE_KITESHIELD_BANDOS),

    // Clue Scroll - Gilded Armor
    GildedPlatebody(ItemID.RUNE_PLATEBODY_GOLDPLATE),
    GildedPlatelegs(ItemID.RUNE_PLATELEGS_GOLDPLATE),
    GildedPlateskirt(ItemID.RUNE_PLATESKIRT_GOLDPLATE),
    GildedFullHelm(ItemID.RUNE_FULL_HELM_GOLDPLATE),
    GildedKiteshield(ItemID.RUNE_KITESHIELD_GOLDPLATE),
    GildedMedHelm(ItemID.RUNE_MED_HELM_GOLD),
    GildedChainbody(ItemID.RUNE_CHAINBODY_GOLD),
    GildedSqShield(ItemID.RUNE_SQ_SHIELD_GOLD),
    GildedDhideVambraces(ItemID.TRAIL_GILDED_DHIDE_VAMBRACES),
    GildedDhideBody(ItemID.TRAIL_GILDED_DHIDE_TOP),
    GildedDhideChaps(ItemID.TRAIL_GILDED_DHIDE_CHAPS),

    // Clue Scroll - Ranged Armor
    StuddedBodyG(ItemID.STUDDED_BODY_TRIM_GOLD),
    StuddedBodyT(ItemID.STUDDED_BODY_TRIM_FUR),
    StuddedChapsG(ItemID.STUDDED_CHAPS_TRIM_GOLD),
    StuddedChapsT(ItemID.STUDDED_CHAPS_TRIM_FUR),
    GreenDhideBodyG(ItemID.DRAGONHIDE_BODY_TRIM_GOLD),
    GreenDhideBodyT(ItemID.DRAGONHIDE_BODY_TRIM),
    GreenDhideChapsG(ItemID.DRAGONHIDE_CHAPS_TRIM_GOLD),
    GreenDhideChapsT(ItemID.DRAGONHIDE_CHAPS_TRIM),
    LeatherBodyG(ItemID.LEATHER_ARMOUR_TRIM_GOLD),
    LeatherChapsG(ItemID.LEATHER_CHAPS_TRIM_GOLD),

    // Clue Scroll - Magic Armor
    BlueSkirtG(ItemID.BLUE_SKIRT_TRIM_GOLD),
    BlueSkirtT(ItemID.BLUE_SKIRT_TRIM),
    BlueWizardRobeG(ItemID.WIZARDS_ROBE_TRIM_GOLD),
    BlueWizardRobeT(ItemID.WIZARDS_ROBE_TRIM),
    BlueWizardHatG(ItemID.BLUEWIZHAT_TRIM_GOLD),
    BlueWizardHatT(ItemID.BLUEWIZHAT_TRIM),
    BlackSkirtG(ItemID.BLACK_SKIRT_GOLD),
    BlackSkirtT(ItemID.BLACK_SKIRT_TRIM),
    BlackWizardRobeG(ItemID.BLACK_WIZARDS_ROBE_GOLD),
    BlackWizardRobeT(ItemID.BLACK_WIZARDS_ROBE_TRIM),
    BlackWizardHatG(ItemID.BLACKWIZHAT_GOLD),
    BlackWizardHatT(ItemID.BLACKWIZHAT_TRIM),

    // Clue Scroll - Prayer Armor
    MonksRobeTopG(ItemID.MONK_ROBETOP_GOLD),
    MonksRobeG(ItemID.MONK_ROBEBOTTOM_GOLD),

    // Item Sets
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
    GildedArmourLegs(ItemID.SET_RUNE_LEGS_GOLDPLATE),
    GildedArmourSkirt(ItemID.SET_RUNE_SKIRT_GOLDPLATE),
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
    GildedDragonhide(ItemID.SET_DHIDE_GILDED),
;

    @Getter
    private final int id;
    private static final Set<Integer> ids;
    static {
        Set<Integer> idsBuilder = new HashSet<>();
        for (FreeToPlayBlockedItems as : FreeToPlayBlockedItems.values()) {
            idsBuilder.add(as.getId());
        }
        ids = Collections.unmodifiableSet(idsBuilder);
    }

    FreeToPlayBlockedItems(int id) {
        this.id = id;
    }

    /**
     * Returns an unmodifiable set of all item set item IDs.
     */
    public static Set<Integer> getFreeToPlayTradeOnlyItemIds() {
        return ids;
    }

    /**
     * Checks if the given item ID is one of the items that cannot be self-sufficiently
     * obtained on a free to play server.
     *
     * @param id the item id to check.
     * @return true if the id cannot be self-sufficiently obtained, false otherwise.
     */
    public static boolean isFreeToPlayTradeOnlyItem(int id) {
        return getFreeToPlayTradeOnlyItemIds().contains(id);
    }
}
