package com.chanceman.filters;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Enum representing all POH flatpack items.
 */
public enum Flatpacks {
    // Chair variants
    Crudechair(ItemID.POH_FLATPACK_ARMCHAIR1),
    Woodenchair(ItemID.POH_FLATPACK_ARMCHAIR2),
    Rockingchair(ItemID.POH_FLATPACK_ARMCHAIR3),
    Oakchair(ItemID.POH_FLATPACK_ARMCHAIR4),
    Oakarmchair(ItemID.POH_FLATPACK_ARMCHAIR5),
    Teakarmchair(ItemID.POH_FLATPACK_ARMCHAIR6),
    Mahoganyarmchair(ItemID.POH_FLATPACK_ARMCHAIR7),

    // Bookcase variants
    Bookcase(ItemID.POH_FLATPACK_BOOKCASE1),
    Oakbookcase(ItemID.POH_FLATPACK_BOOKCASE2),
    Mahoganybookcase(ItemID.POH_FLATPACK_BOOKCASE3),

    // Beer barrel variants
    Beerbarrel(ItemID.POH_FLATPACK_BEERBARREL1),
    Ciderbarrel(ItemID.POH_FLATPACK_BEERBARREL2),
    Asgarnianale(ItemID.POH_FLATPACK_BEERBARREL3),
    Greensmansale(ItemID.POH_FLATPACK_BEERBARREL4),
    Dragonbitter(ItemID.POH_FLATPACK_BEERBARREL5),
    Chefsdelight(ItemID.POH_FLATPACK_BEERBARREL6),

    // Kitchen table variants
    Kitchentable(ItemID.POH_FLATPACK_KITCHENTABLE1),
    Oakkitchentable(ItemID.POH_FLATPACK_KITCHENTABLE2),
    Teakkitchentable(ItemID.POH_FLATPACK_KITCHENTABLE3),

    // Dining table variants
    Wooddiningtable(ItemID.POH_FLATPACK_DININGTABLE1),
    Oakdiningtable(ItemID.POH_FLATPACK_DININGTABLE2),
    Carvedoaktable(ItemID.POH_FLATPACK_DININGTABLE3),
    Teaktable(ItemID.POH_FLATPACK_DININGTABLE4),
    Carvedteaktable(ItemID.POH_FLATPACK_DININGTABLE5),
    Mahoganytable(ItemID.POH_FLATPACK_DININGTABLE6),
    OpulentsTable(ItemID.POH_FLATPACK_DININGTABLE7),

    // Dining chair (bench) variants
    Woodenbench(ItemID.POH_FLATPACK_DININGCHAIR1),
    Oakbench(ItemID.POH_FLATPACK_DININGCHAIR2),
    Carvedoakbench(ItemID.POH_FLATPACK_DININGCHAIR3),
    Teakdiningbench(ItemID.POH_FLATPACK_DININGCHAIR4),
    Carvedteakbench(ItemID.POH_FLATPACK_DININGCHAIR5),
    Mahoganybench(ItemID.POH_FLATPACK_DININGCHAIR6),
    Gildedbench(ItemID.POH_FLATPACK_DININGCHAIR7),

    // Bed variants
    Woodenbed(ItemID.POH_FLATPACK_BED1),
    Oakbed(ItemID.POH_FLATPACK_BED2),
    Largeoakbed(ItemID.POH_FLATPACK_BED3),
    Teakbed(ItemID.POH_FLATPACK_BED4),
    Largeteakbed(ItemID.POH_FLATPACK_BED5),
    Fourposterbed(ItemID.POH_FLATPACK_BED6),
    Gildedfourposter(ItemID.POH_FLATPACK_BED7),

    // Clock variants
    Oakclock(ItemID.POH_FLATPACK_CLOCK1),
    Teakclock(ItemID.POH_FLATPACK_CLOCK2),
    Gildedclock(ItemID.POH_FLATPACK_CLOCK3),

    // Dresser variants
    Shavingstand(ItemID.POH_FLATPACK_DRESSER1),
    Oakshavingstand(ItemID.POH_FLATPACK_DRESSER2),
    Oakdresser(ItemID.POH_FLATPACK_DRESSER3),
    Teakdresser(ItemID.POH_FLATPACK_DRESSER4),
    Fancyteakdresser(ItemID.POH_FLATPACK_DRESSER5),
    Mahoganydresser(ItemID.POH_FLATPACK_DRESSER6),
    Gildeddresser(ItemID.POH_FLATPACK_DRESSER7),

    // Wardrobe variants
    Shoebox(ItemID.POH_FLATPACK_WARDROBE1),
    Oakdrawers(ItemID.POH_FLATPACK_WARDROBE2),
    Oakwardrobe(ItemID.POH_FLATPACK_WARDROBE3),
    Teakdrawers(ItemID.POH_FLATPACK_WARDROBE4),
    Teakwardrobe(ItemID.POH_FLATPACK_WARDROBE5),
    Mahoganywardrobe(ItemID.POH_FLATPACK_WARDROBE6),
    Gildedwardrobe(ItemID.POH_FLATPACK_WARDROBE7),

    // Cape rack variants
    Oakcaperack(ItemID.POH_FLATPACK_CAPE_RACK),
    Teakcaperack(ItemID.POH_FLATPACK_CAPE_RACK2),
    Mahoganycaperack(ItemID.POH_FLATPACK_CAPE_RACK3),
    Gildedcaperack(ItemID.POH_FLATPACK_CAPE_RACK4),
    Marblecaperack(ItemID.POH_FLATPACK_CAPE_RACK5),
    Magiccaperack(ItemID.POH_FLATPACK_CAPE_RACK6),

    // Toy box variants
    Oaktoybox(ItemID.POH_FLATPACK_TOY_BOX),
    Teaktoybox(ItemID.POH_FLATPACK_TOY_BOX2),
    Mahoganytoybox(ItemID.POH_FLATPACK_TOY_BOX3),

    // Magic wardrobe variants
    Oakmagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE),
    Carvedoakmagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE2),
    Teakmagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE3),
    Carvedteakmagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE4),
    Mahoganymagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE5),
    Gildedmagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE6),
    Marblemagicwardrobe(ItemID.POH_FLATPACK_MAGIC_WARDROBE7),

    // Armour case variants
    Oakarmourcase(ItemID.POH_FLATPACK_ARMOUR_CASE),
    Teakarmourcase(ItemID.POH_FLATPACK_ARMOUR_CASE2),
    Mahoganyarmourcase(ItemID.POH_FLATPACK_ARMOUR_CASE3),

    // Treasure chest variants
    Oaktreasurechest(ItemID.POH_FLATPACK_TREASURE_CHEST),
    Teaktreasurechest(ItemID.POH_FLATPACK_TREASURE_CHEST2),
    Magictreasurechest(ItemID.POH_FLATPACK_TREASURE_CHEST3),

    // Fancy dress box variants
    Oakfancydressbox(ItemID.POH_FLATPACK_FANCY_DRESS_BOX),
    Teakfancydressbox(ItemID.POH_FLATPACK_FANCY_DRESS_BOX2),
    Mahoganyfancydressbox(ItemID.POH_FLATPACK_FANCY_DRESS_BOX3);

    private static final Set<Integer> IDS;

    static {
        Set<Integer> ids = new HashSet<>();
        for (Flatpacks fp : Flatpacks.values()) {
            ids.add(fp.getId());
        }
        IDS = Collections.unmodifiableSet(ids);
    }

    @Getter
    private final int id;

    Flatpacks(int id) {
        this.id = id;
    }

    /**
     * Returns an unmodifiable set of all flatpack item IDs.
     */
    public static Set<Integer> getAllFlatpackIds() {
        return IDS;
    }

    /**
     * Checks if the given item ID is a flatpack.
     *
     * @param id the item id to check.
     * @return true if the id is a flatpack, false otherwise.
     */
    public static boolean isFlatpack(int id) {
        return getAllFlatpackIds().contains(id);
    }
}
