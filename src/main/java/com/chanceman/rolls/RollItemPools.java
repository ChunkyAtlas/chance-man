package com.chanceman.rolls;

import lombok.experimental.UtilityClass;
import net.runelite.api.gameval.ItemID;

import java.util.Set;

@UtilityClass
public class RollItemPools
{
    private static final Set<Integer> TOOL_ITEMS = Set.of(
            ItemID.PESTLE_AND_MORTAR, // Pestle and mortar
            ItemID.LOBSTER_POT, // Lobster pot
            ItemID.BIG_NET, // Big fishing net
            ItemID.FISHING_ROD, // Fishing rod
            ItemID.FLY_FISHING_ROD, // Fly fishing rod
            ItemID.HARPOON, // Harpoon
            ItemID.FISHING_BAIT, // Fishing bait
            ItemID.FEATHER, // Feather
            ItemID.TINDERBOX, // Tinderbox
            ItemID.KNIFE, // Knife
            ItemID.SPADE, // Spade
            ItemID.ROPE, // Rope
            ItemID.MACHETTE, // Machete
            ItemID.BRONZE_PICKAXE, // Bronze pickaxe
            ItemID.BRONZE_AXE, // Bronze axe
            ItemID.LOCKPICK, // Lockpick
            ItemID.RING_MOULD, // Ring mould
            ItemID.AMULET_MOULD, // Amulet mould
            ItemID.NECKLACE_MOULD, // Necklace mould
            ItemID.HOLY_SYMBOL_MOULD, // Holy mould
            ItemID.NEEDLE, // Needle
            ItemID.THREAD, // Thread
            ItemID.SHEARS, // Shears
            ItemID.CHISEL, // Chisel
            ItemID.GLASSBLOWINGPIPE, // Glassblowing pipe
            ItemID.BUCKET_EMPTY, // Bucket
            ItemID.HAMMER, // Hammer
            ItemID.SILVER_SICKLE, // Silver sickle
            ItemID.SECATEURS, // Secateurs
            ItemID.RAKE, // Rake
            ItemID.GARDENING_TROWEL, // Gardening trowel
            ItemID.PLANTPOT_COMPOST, // Filled plant pot
            ItemID.TIARA_MOULD, // Tiara mould
            ItemID.POH_SAW, // Saw
            ItemID.XBOWS_SILVER_BOLT_MOULD, // Bolt mould
            ItemID.HUNTING_OJIBWAY_BIRD_SNARE, // Bird snare
            ItemID.HUNTING_BOX_TRAP, // Box trap
            ItemID.HUNTING_BUTTERFLY_NET, // Butterfly net
            ItemID.BUTTERFLY_JAR, // Butterfly jar
            ItemID.HUNTING_TEASING_STICK, // Teasing stick
            ItemID.HUNTING_SNARE, // Rabbit snare
            ItemID.NOOSE_WAND, // Noose wand
            ItemID.JEWL_BRACELET_MOULD, // Bracelet mould
            ItemID.II_IMPLING_JAR, // Impling jar
            ItemID.BOTTOMLESS_COMPOST_BUCKET, // Bottomless compost bucket (empty)
            ItemID.NARWHAL_HORN // Narwhal horn
    );

    private static final Set<Integer> QUEST_ITEMS = Set.of(
            ItemID.BUCKET_WAX, // Bucket of wax
            ItemID.UNLIT_CANDLE, // Candle
            ItemID.EYE_OF_NEWT, // Eye of newt
            ItemID.RED_SPIDERS_EGGS, // Red spiders' eggs
            ItemID.LIMPWURT_ROOT, // Limpwurt root
            ItemID.VIAL_WATER, // Vial of water
            ItemID.VIAL_EMPTY, // Vial
            ItemID.SNAPE_GRASS, // Snape grass
            ItemID.PESTLE_AND_MORTAR, // Pestle and mortar
            ItemID.UNICORN_HORN_DUST, // Unicorn horn dust
            ItemID.WHITE_BERRIES, // White berries
            ItemID.WINE_OF_ZAMORAK, // Wine of Zamorak
            ItemID.JANGERBERRIES, // Jangerberries
            ItemID.GUAM_LEAF, // Guam leaf
            ItemID.MARENTILL, // Marrentill
            ItemID.HARRALANDER, // Harralander
            ItemID.KWUARM, // Kwuarm
            ItemID.FISH_FOOD, // Fish food
            ItemID.POISON, // Poison
            ItemID.GOBLIN_ARMOUR, // Goblin mail
            ItemID.LOBSTER_POT, // Lobster pot
            ItemID.NET, // Small fishing net
            ItemID.BIG_NET, // Big fishing net
            ItemID.FISHING_ROD, // Fishing rod
            ItemID.FLY_FISHING_ROD, // Fly fishing rod
            ItemID.HARPOON, // Harpoon
            ItemID.FISHING_BAIT, // Fishing bait
            ItemID.FEATHER, // Feather
            ItemID.SHRIMP, // Shrimps
            ItemID.RAW_SARDINE, // Raw sardine
            ItemID.SALMON, // Salmon
            ItemID.TROUT, // Trout
            ItemID.RAW_COD, // Raw cod
            ItemID.RAW_MACKEREL, // Raw mackerel
            ItemID.RAW_TUNA, // Raw tuna
            ItemID.TUNA, // Tuna
            ItemID.BASS, // Bass
            ItemID.SWORDFISH, // Swordfish
            ItemID.RAW_SHARK, // Raw shark
            ItemID.SHARK, // Shark
            ItemID.RAW_MANTARAY, // Raw manta ray
            ItemID.RAW_SEATURTLE, // Raw sea turtle
            ItemID.SEAWEED, // Seaweed
            ItemID.CLAY, // Clay
            ItemID.COPPER_ORE, // Copper ore
            ItemID.TIN_ORE, // Tin ore
            ItemID.IRON_ORE, // Iron ore
            ItemID.MITHRIL_ORE, // Mithril ore
            ItemID.COAL, // Coal
            ItemID.BONES, // Bones
            ItemID.BAT_BONES, // Bat bones
            ItemID.DRAGON_BONES, // Dragon bones
            ItemID.FIRERUNE, // Fire rune
            ItemID.WATERRUNE, // Water rune
            ItemID.AIRRUNE, // Air rune
            ItemID.EARTHRUNE, // Earth rune
            ItemID.MINDRUNE, // Mind rune
            ItemID.DEATHRUNE, // Death rune
            ItemID.NATURERUNE, // Nature rune
            ItemID.CHAOSRUNE, // Chaos rune
            ItemID.LAWRUNE, // Law rune
            ItemID.COSMICRUNE, // Cosmic rune
            ItemID.BLOODRUNE, // Blood rune
            ItemID.SOULRUNE, // Soul rune
            ItemID.STAFFORB, // Battlestaff
            ItemID.TINDERBOX, // Tinderbox
            ItemID.ASHES, // Ashes
            ItemID.CADAVABERRIES, // Cadava berries
            ItemID.RUNE_THROWNAXE, // Rune thrownaxe
            ItemID.CROSSBOW, // Crossbow
            ItemID.OAK_LONGBOW, // Oak longbow
            ItemID.WILLOW_LONGBOW, // Willow longbow
            ItemID.BLACK_KNIFE, // Black knife
            ItemID.KNIFE, // Knife
            ItemID.FUR, // Bear fur
            ItemID.SILK, // Silk
            ItemID.SPADE, // Spade
            ItemID.ROPE, // Rope
            ItemID.GREY_WOLF_FUR, // Grey wolf fur
            ItemID.WOODPLANK, // Plank
            ItemID.PAPYRUS, // Papyrus
            ItemID.CHARCOAL, // Charcoal
            ItemID.MACHETTE, // Machete
            ItemID.WHITE_APRON, // White apron
            ItemID.PINK_SKIRT, // Pink skirt
            ItemID.LEATHER_GLOVES, // Leather gloves
            ItemID.STEEL_PLATELEGS, // Steel platelegs
            ItemID.BLACK_PLATELEGS, // Black platelegs
            ItemID.IRON_CHAINBODY, // Iron chainbody
            ItemID.STEEL_CHAINBODY, // Steel chainbody
            ItemID.ADAMANT_CHAINBODY, // Adamant chainbody
            ItemID.STEEL_PLATEBODY, // Steel platebody
            ItemID.BLACK_PLATEBODY, // Black platebody
            ItemID.BRONZE_MED_HELM, // Bronze med helm
            ItemID.STEEL_MED_HELM, // Steel med helm
            ItemID.STEEL_FULL_HELM, // Steel full helm
            ItemID.BLACK_FULL_HELM, // Black full helm
            ItemID.STEEL_DAGGER, // Steel dagger
            ItemID.BLACK_DAGGER, // Black dagger
            ItemID.IRON_SPEAR, // Iron spear
            ItemID.STEEL_SPEAR, // Steel spear
            ItemID.MITHRIL_SPEAR, // Mithril spear
            ItemID.ADAMANT_SPEAR, // Adamant spear
            ItemID.RUNE_SPEAR, // Rune spear
            ItemID.DRAGON_SPEAR, // Dragon spear
            ItemID.BRONZE_PICKAXE, // Bronze pickaxe
            ItemID.ADAMANT_PICKAXE, // Adamant pickaxe
            ItemID.RUNE_PICKAXE, // Rune pickaxe
            ItemID.STEEL_SWORD, // Steel sword
            ItemID.STEEL_LONGSWORD, // Steel longsword
            ItemID.MITHRIL_2H_SWORD, // Mithril 2h sword
            ItemID.STEEL_WARHAMMER, // Steel warhammer
            ItemID.BRONZE_AXE, // Bronze axe
            ItemID.MITHRIL_AXE, // Mithril axe
            ItemID.RUNE_AXE, // Rune axe
            ItemID.PLAINSTAFF, // Staff
            ItemID.STEEL_MACE, // Steel mace
            ItemID.BLANKRUNE, // Rune essence
            ItemID.AIR_TALISMAN, // Air talisman
            ItemID.EARTH_TALISMAN, // Earth talisman
            ItemID.FIRE_TALISMAN, // Fire talisman
            ItemID.WATER_TALISMAN, // Water talisman
            ItemID.MIND_TALISMAN, // Mind talisman
            ItemID.DEATH_TALISMAN, // Death talisman
            ItemID.RED_BEAD, // Red bead
            ItemID.YELLOW_BEAD, // Yellow bead
            ItemID.BLACK_BEAD, // Black bead
            ItemID.WHITE_BEAD, // White bead
            ItemID.LOGS, // Logs
            ItemID.MAGIC_LOGS, // Magic logs
            ItemID.YEW_LOGS, // Yew logs
            ItemID.MAPLE_LOGS, // Maple logs
            ItemID.WILLOW_LOGS, // Willow logs
            ItemID.OAK_LOGS, // Oak logs
            ItemID.LOCKPICK, // Lockpick
            ItemID.NAILS, // Steel nails
            ItemID.ANTIDRAGONBREATHSHIELD, // Anti-dragon shield
            ItemID.GARLIC, // Garlic
            ItemID.SEASONED_SARDINE, // Seasoned sardine
            ItemID.DOOGLELEAVES, // Doogle leaves
            ItemID.RING_MOULD, // Ring mould
            ItemID.NECKLACE_MOULD, // Necklace mould
            ItemID.DIAMOND, // Diamond
            ItemID.RUBY, // Ruby
            ItemID.EMERALD, // Emerald
            ItemID.SAPPHIRE, // Sapphire
            ItemID.OPAL, // Opal
            ItemID.JADE, // Jade
            ItemID.RED_TOPAZ, // Red topaz
            ItemID.DRAGONSTONE, // Dragonstone
            ItemID.STRUNG_SAPPHIRE_AMULET, // Sapphire amulet
            ItemID.BLESSEDSTAR, // Holy symbol
            ItemID.NEEDLE, // Needle
            ItemID.THREAD, // Thread
            ItemID.SHEARS, // Shears
            ItemID.WOOL, // Wool
            ItemID.LEATHER, // Leather
            ItemID.HARD_LEATHER, // Hard leather
            ItemID.CHISEL, // Chisel
            ItemID.BROWN_APRON, // Brown apron
            ItemID.BALL_OF_WOOL, // Ball of wool
            ItemID.SOFTCLAY, // Soft clay
            ItemID.REDDYE, // Red dye
            ItemID.YELLOWDYE, // Yellow dye
            ItemID.BLUEDYE, // Blue dye
            ItemID.ORANGEDYE, // Orange dye
            ItemID.GREENDYE, // Green dye
            ItemID.PURPLEDYE, // Purple dye
            ItemID.MOLTEN_GLASS, // Molten glass
            ItemID.BOW_STRING, // Bow string
            ItemID.BUCKET_SAND, // Bucket of sand
            ItemID.GLASSBLOWINGPIPE, // Glassblowing pipe
            ItemID.BOWL_UNFIRED, // Unfired bowl
            ItemID.BRONZECRAFTWIRE, // Bronze wire
            ItemID.DESERT_SHIRT, // Desert shirt
            ItemID.DESERT_ROBE, // Desert robe
            ItemID.DESERT_BOOTS, // Desert boots
            ItemID.SHANTAY_PASS, // Shantay pass
            ItemID.CAKE_TIN, // Cake tin
            ItemID.CAKE, // Cake
            ItemID.CHOCOLATE_CAKE, // Chocolate cake
            ItemID.ASGARNIAN_ALE, // Asgarnian ale
            ItemID.WIZARDS_MIND_BOMB, // Wizard's mind bomb
            ItemID.GREENMANS_ALE, // Greenman's ale
            ItemID.DWARVEN_STOUT, // Dwarven stout
            ItemID.BEER, // Beer
            ItemID.BEER_GLASS, // Beer glass
            ItemID.BOWL_WATER, // Bowl of water
            ItemID.BOWL_EMPTY, // Bowl
            ItemID.BUCKET_EMPTY, // Bucket
            ItemID.BUCKET_WATER, // Bucket of water
            ItemID.POT_EMPTY, // Pot
            ItemID.POT_FLOUR, // Pot of flour
            ItemID.JUG_WATER, // Jug of water
            ItemID.SWAMP_TAR, // Swamp tar
            ItemID.SWAMPPASTE, // Swamp paste
            ItemID.POTATO, // Potato
            ItemID.EGG, // Egg
            ItemID.GRAIN, // Grain
            ItemID.REDBERRIES, // Redberries
            ItemID.ONION, // Onion
            ItemID.BANANA, // Banana
            ItemID.CABBAGE, // Cabbage
            ItemID.KEBAB, // Kebab
            ItemID.CHOCOLATE_BAR, // Chocolate bar
            ItemID.CHOCOLATE_DUST, // Chocolate dust
            ItemID.CUP_OF_TEA, // Cup of tea
            ItemID.TOMATO, // Tomato
            ItemID.CHEESE, // Cheese
            ItemID.GRAPES, // Grapes
            ItemID.STEW, // Stew
            ItemID.SPICESPOT, // Spice
            ItemID.VODKA, // Vodka
            ItemID.PREMADE_FRUIT_BLAST, // Premade fr' blast
            ItemID.FRUIT_BLAST, // Fruit blast
            ItemID.LEMON, // Lemon
            ItemID.ORANGE, // Orange
            ItemID.ORANGE_SLICES, // Orange slices
            ItemID.PINEAPPLE_CHUNKS, // Pineapple chunks
            ItemID.DWELLBERRIES, // Dwellberries
            ItemID.EQUA_LEAVES, // Equa leaves
            ItemID.POT_OF_CREAM, // Pot of cream
            ItemID.RAW_BEEF, // Raw beef
            ItemID.RAW_RAT_MEAT, // Raw rat meat
            ItemID.RAW_BEAR_MEAT, // Raw bear meat
            ItemID.RAW_CHICKEN, // Raw chicken
            ItemID.COOKED_MEAT, // Cooked meat
            ItemID.GNOME_SPICE, // Gnome spice
            ItemID.TOAD_CRUNCHIES, // Toad crunchies
            ItemID.PREMADE_TOAD_CRUNCHIES, // Premade t'd crunch
            ItemID.BREAD, // Bread
            ItemID.REDBERRY_PIE, // Redberry pie
            ItemID.HAMMER, // Hammer
            ItemID.BRONZE_BAR, // Bronze bar
            ItemID.IRON_BAR, // Iron bar
            ItemID.STEEL_BAR, // Steel bar
            ItemID.SILVER_BAR, // Silver bar
            ItemID.GOLD_BAR, // Gold bar
            ItemID.MITHRIL_BAR, // Mithril bar
            ItemID.RUNITE_BAR, // Runite bar
            ItemID.RING_OF_LIFE, // Ring of life
            ItemID.WOLF_BONES, // Wolf bones
            ItemID.RAW_CHOMPY, // Raw chompy
            ItemID.SILVER_SICKLE, // Silver sickle
            ItemID.MORTMYREMUSHROOM, // Mort myre fungus
            ItemID.DEATH_CLIMBINGBOOTS, // Climbing boots
            ItemID.DEATH_SPIKEDBOOTS, // Spiked boots
            ItemID.TBWT_JOGRE_BONES, // Jogre bones
            ItemID.TBWT_SLICED_BANANA, // Sliced banana
            ItemID.MM_NORMAL_MONKEY_BONES, // Monkey bones
            ItemID.LIMESTONE, // Limestone
            ItemID.COOKED_RABBIT, // Cooked rabbit
            ItemID.MORT_SLIMEY_EEL, // Raw slimy eel
            ItemID.LIMESTONEBRICK, // Limestone brick
            ItemID.FLAMTAER_HAMMER, // Flamtaer hammer
            ItemID.VIKING_TANKARD_FULL, // Beer tankard
            ItemID.MM_MONKEY_NUTS, // Monkey nuts
            ItemID.SLAYER_MIRROR_SHIELD, // Mirror shield
            ItemID.SLAYER_BAG_OF_SALT, // Bag of salt
            ItemID.SLAYER_FACEMASK, // Face mask
            ItemID.FAVOUR_AIRTIGHT_POT, // Airtight pot
            ItemID.POTLID, // Pot lid
            ItemID.CUP_HOT_WATER, // Cup of hot water
            ItemID.BULLSEYE_LANTERN_LENS, // Lantern lens
            ItemID.BULLSEYE_LANTERN_UNLIT, // Bullseye lantern (unlit)
            ItemID.FEUD_KARIDIAN_TURBAN, // Karidian headpiece
            ItemID.FEUD_KARIDIAN_FAKEBEARD, // Fake beard
            ItemID.FD_CRUSHED_GARLIC, // Garlic powder
            ItemID.ICS_LITTLE_LINEN, // Linen
            ItemID.ICS_LITTLE_SAP_BUCKET, // Bucket of sap
            ItemID.MISTRUNE, // Mist rune
            ItemID.MUDRUNE, // Mud rune
            ItemID.LAVARUNE, // Lava rune
            ItemID.ZOGRE_BOW, // Comp ogre bow
            ItemID.MARIGOLD_SEED, // Marigold seed
            ItemID.SNAPDRAGON_SEED, // Snapdragon seed
            ItemID.CADANTINE_SEED, // Cadantine seed
            ItemID.POTATO_SEED, // Potato seed
            ItemID.ONION_SEED, // Onion seed
            ItemID.CABBAGE_SEED, // Cabbage seed
            ItemID.GARDENING_TROWEL, // Gardening trowel
            ItemID.SECATEURS, // Secateurs
            ItemID.WATERING_CAN_0, // Watering can
            ItemID.RAKE, // Rake
            ItemID.DIBBER, // Seed dibber
            ItemID.PLANTPOT_COMPOST, // Filled plant pot
            ItemID.SACK_EMPTY, // Empty sack
            ItemID.ELEMENTAL_TALISMAN, // Elemental talisman
            ItemID.TIARA, // Tiara
            ItemID.TIARA_MIND, // Mind tiara
            ItemID.TIARA_DEATH, // Death tiara
            ItemID.ALE_YEAST, // Ale yeast
            ItemID.WILLOW_BRANCH, // Willow branch
            ItemID.SWEETCORN, // Sweetcorn
            ItemID.BARLEY_MALT, // Barley malt
            ItemID.BUCKET_COMPOST, // Compost
            ItemID.BUCKET_SUPERCOMPOST, // Supercompost
            ItemID.PLANT_CURE, // Plant cure
            ItemID.POTATO_BAKED, // Baked potato
            ItemID.DRAGON_AXE, // Dragon axe
            ItemID.AGRITH_DESERT_SHIRT_DYED, // Black desert shirt
            ItemID.AGRITH_DESERT_ROBE_DYED, // Black desert robe
            ItemID.WEREWOLVE_FUR, // Werewolf fur
            ItemID.BOWL_SWEETCORN, // Bowl of sweetcorn
            ItemID.SLAYER_BOOTS, // Insulated boots
            ItemID.SPIT_IRON, // Iron spit
            ItemID.CHICKENQUEST_POT_CORNFLOUR, // Pot of cornflour
            ItemID.BLANKRUNE_HIGH, // Pure essence
            ItemID.PLANK_OAK, // Oak plank
            ItemID.PLANK_MAHOGANY, // Mahogany plank
            ItemID.CLOTH, // Bolt of cloth
            ItemID.POH_CLOCKWORK_MECHANISM, // Clockwork
            ItemID.POH_SAW, // Saw
            ItemID.ASTRALRUNE, // Astral rune
            ItemID.XBOWS_GRAPPLE_TIP_BOLT_MITHRIL, // Mith grapple
            ItemID.XBOWS_GRAPPLE_TIP_BOLT_MITHRIL_ROPE, // Mith grapple
            ItemID.HUNTING_BOX_TRAP, // Box trap
            ItemID.ANMA_P_BUTTONS, // Buttons
            ItemID.ARCTIC_PINE_SPLIT, // Split log
            ItemID.YAK_HIDE_ARMOUR_BODY, // Yak-hide armour
            ItemID.YAK_HIDE_ARMOUR_GREAVES, // Yak-hide armour
            ItemID.FREMMENIK_ROUND_SHIELD, // Fremennik shield
            ItemID.BRAIN_INV_WOODEN_CAT, // Wooden cat
            ItemID.BRUT_ROE, // Roe
            ItemID.BUCKET_ULTRACOMPOST, // Ultracompost
            ItemID.CATALYTIC_TALISMAN, // Catalytic talisman
            ItemID.TIARA_CATALYTIC, // Catalytic tiara
            ItemID.WATER_SKIN4, // Waterskin(4)
            ItemID.GUAMVIAL, // Guam potion (unf)
            ItemID.POH_TOY_MOUSE_WOUND, // Toy mouse (wound)
            ItemID.ACHEY_TREE_LOGS, // Achey tree logs
            ItemID.OGRE_ARROW_SHAFT, // Ogre arrow shaft
            ItemID.OGRE_HEADLESS_ARROW, // Flighted ogre arrow
            ItemID.WOLFBONE_ARROWHEADS, // Wolfbone arrowtips
            ItemID.OGRE_ARROW, // Ogre arrow
            ItemID.PRIEST_GOWN, // Priest gown
            ItemID.PRIEST_ROBE, // Priest gown
            ItemID.MARRENTILLVIAL, // Marrentill potion (unf)
            ItemID.HAM_BOOTS, // Ham boots
            ItemID.HAM_CLOAK, // Ham cloak
            ItemID.HAM_GLOVES, // Ham gloves
            ItemID.HAM_HOOD, // Ham hood
            ItemID.HAM_BADGE, // Ham logo
            ItemID.HAM_ROBE, // Ham robe
            ItemID.HAM_SHIRT, // Ham shirt
            ItemID.RANARR_WEED, // Ranarr weed
            ItemID.RANARRVIAL, // Ranarr potion (unf)
            ItemID.ENAKH_GRANITE_MEDIUM, // Granite (5kg)
            ItemID.SACK_POTATO_10, // Potatoes(10)
            ItemID.TARROMIN, // Tarromin
            ItemID.TARROMINVIAL, // Tarromin potion (unf)
            ItemID.HARRALANDERVIAL, // Harralander potion (unf)
            ItemID.TOADFLAX, // Toadflax
            ItemID.TOADFLAXVIAL, // Toadflax potion (unf)
            ItemID._4DOSESTATRESTORE, // Restore potion(4)
            ItemID.KEG_OF_BEER, // Keg of beer
            ItemID.TBWT_KARAMBWAN_VESSEL, // Karambwan vessel
            ItemID.TBWT_KARAMBWAN_VESSEL_LOADED_WITH_KARAMBWANJI, // Karambwan vessel
            ItemID.TBWT_RAW_KARAMBWAN, // Raw karambwan
            ItemID._4DOSE1AGILITY, // Agility potion(4)
            ItemID.RAW_MONKFISH, // Raw monkfish
            ItemID.SHADE_BONES1, // Loar remains
            ItemID.MORT_SERUM3, // Serum 207 (3)
            ItemID.TORCH_UNLIT, // Unlit torch
            ItemID.CUP_EMPTY, // Empty cup
            ItemID._100_JUBBLY_MEAT_RAW, // Raw jubbly
            ItemID.FISHBOWL_EMPTY, // Empty fishbowl
            ItemID.WEEDS, // Weeds
            ItemID.CUP_GUTHIX_REST_3, // Guthix rest(3)
            ItemID.SICKLE_MOULD, // Sickle mould
            ItemID.REGICIDE_BARREL_EMPTY, // Barrel
            ItemID.POTLID_UNFIRED, // Unfired pot lid
            ItemID.XBOWS_CROSSBOW_BOLTS_MITHRIL, // Mithril bolts
            ItemID.HUNTING_JERBOA_TAIL // Jerboa tail
    );

    public static boolean isToolItem(int itemId)
    {
        return TOOL_ITEMS.contains(itemId);
    }

    public static boolean isQuestItem(int itemId)
    {
        return QUEST_ITEMS.contains(itemId) && !TOOL_ITEMS.contains(itemId);
    }
}