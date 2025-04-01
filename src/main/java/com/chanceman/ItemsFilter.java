package com.chanceman;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ItemsFilter {
    // Default ensouled head ID value when no mapping exists.
    public static final int DEFAULT_ENSOULED_HEAD_ID = -1;

    // Set of blocked item names (non-rollable items); stored in lower-case.
    private static final Set<String> BLOCKED_ITEMS;

    // Mapping of ensouled head names (in lower-case) to their correct item IDs.
    private static final Map<String, Integer> ENSOULED_HEAD_MAP;

    static {
        // Initialize the blocked items set.
        Set<String> blocked = new HashSet<>();
        blocked.add("armageddon teleport scroll");
        blocked.add("armageddon cape fabric");
        blocked.add("armageddon weapon scroll");
        blocked.add("armageddon rug");
        blocked.add("blighted overload");
        blocked.add("blighted overload (1)");
        blocked.add("blighted overload (2)");
        blocked.add("blighted overload (3)");
        blocked.add("blighted overload (4)");
        blocked.add("chitin");
        blocked.add("crystal ball (flatpack)");
        blocked.add("cornflour");
        blocked.add("corrupted armadyl godsword");
        blocked.add("corrupted dark bow");
        blocked.add("corrupted dragon claws");
        blocked.add("corrupted twisted bow");
        blocked.add("corrupted voidwaker");
        blocked.add("corrupted volatile nightmare staff");
        blocked.add("corrupted tumeken's shadow");
        blocked.add("corrupted tumeken's shadow (uncharged)");
        blocked.add("morrigan's coif (deadman mode)");
        blocked.add("morrigan's javelin (deadman mode)");
        blocked.add("morrigan's leather body (deadman mode)");
        blocked.add("morrigan's leather chaps (deadman mode)");
        blocked.add("morrigan's throwing axe (deadman mode)");
        blocked.add("sigil of adroit");
        blocked.add("sigil of aggression");
        blocked.add("sigil of agile fortune");
        blocked.add("sigil of arcane swiftness");
        blocked.add("sigil of barrows");
        blocked.add("sigil of binding");
        blocked.add("sigil of consistency");
        blocked.add("sigil of deception");
        blocked.add("sigil of deft strikes");
        blocked.add("sigil of devotion");
        blocked.add("sigil of enhanced harvest");
        blocked.add("sigil of escaping");
        blocked.add("sigil of exaggeration");
        blocked.add("sigil of faith");
        blocked.add("sigil of finality");
        blocked.add("sigil of fortification");
        blocked.add("sigil of freedom");
        blocked.add("sigil of garments");
        blocked.add("sigil of gunslinger");
        blocked.add("sigil of hoarding");
        blocked.add("sigil of last recall");
        blocked.add("sigil of lithe");
        blocked.add("sigil of meticulousness");
        blocked.add("sigil of mobility");
        blocked.add("sigil of nature");
        blocked.add("sigil of onslaught");
        blocked.add("sigil of pious protection");
        blocked.add("sigil of precision");
        blocked.add("sigil of preservation");
        blocked.add("sigil of prosperity");
        blocked.add("sigil of rampage");
        blocked.add("sigil of rampart");
        blocked.add("sigil of remote storage");
        blocked.add("sigil of resilience");
        blocked.add("sigil of resistance");
        blocked.add("sigil of restoration");
        blocked.add("sigil of revoked limitation");
        blocked.add("sigil of slaughter");
        blocked.add("sigil of specialised strikes");
        blocked.add("sigil of stamina");
        blocked.add("sigil of storage");
        blocked.add("sigil of supreme stamina");
        blocked.add("sigil of sustenance");
        blocked.add("sigil of swashbuckler");
        blocked.add("sigil of the abyss");
        blocked.add("sigil of the alchemaniac");
        blocked.add("sigil of the alchemist");
        blocked.add("sigil of the augmented thrall");
        blocked.add("sigil of the barbarians");
        blocked.add("sigil of the bloodhound");
        blocked.add("sigil of the chef");
        blocked.add("sigil of the craftsman");
        blocked.add("sigil of the dwarves");
        blocked.add("sigil of the elves");
        blocked.add("sigil of the eternal jeweller");
        blocked.add("sigil of the feral fighter");
        blocked.add("sigil of the fletcher");
        blocked.add("sigil of the food master");
        blocked.add("sigil of the forager");
        blocked.add("sigil of the formidable fighter");
        blocked.add("sigil of the fortune farmer");
        blocked.add("sigil of the gnomes");
        blocked.add("sigil of the guardian angel");
        blocked.add("sigil of the hunter");
        blocked.add("sigil of the infernal chef");
        blocked.add("sigil of the infernal smith");
        blocked.add("sigil of the lightbearer");
        blocked.add("sigil of the menacing mage");
        blocked.add("sigil of the meticulous mage");
        blocked.add("sigil of the ninja");
        blocked.add("sigil of the porcupine");
        blocked.add("sigil of the potion master");
        blocked.add("sigil of the rigorous ranger");
        blocked.add("sigil of the ruthless ranger");
        blocked.add("sigil of the serpent");
        blocked.add("sigil of the skiller");
        blocked.add("sigil of the smith");
        blocked.add("sigil of the treasure hunter");
        blocked.add("sigil of the well-fed");
        blocked.add("sigil of titanium");
        blocked.add("sigil of versatility");
        blocked.add("sigil of woodcraft");
        blocked.add("starter bow");
        blocked.add("starter staff");
        blocked.add("starter sword");
        blocked.add("statius's full helm (deadman mode)");
        blocked.add("statius's platebody (deadman mode)");
        blocked.add("statius's platelegs (deadman mode)");
        blocked.add("statius's warhammer (deadman mode)");
        blocked.add("trinket of advanced weaponry");
        blocked.add("trinket of fairies");
        blocked.add("trinket of undead");
        blocked.add("trinket of vengeance");
        blocked.add("vesta's chainbody (deadman mode)");
        blocked.add("vesta's longsword (deadman mode)");
        blocked.add("vesta's plateskirt (deadman mode)");
        blocked.add("vesta's spear (deadman mode)");
        blocked.add("zuriel's hood (deadman mode)");
        blocked.add("zuriel's robe bottom (deadman mode)");
        blocked.add("zuriel's robe top (deadman mode)");
        blocked.add("zuriel's staff (deadman mode)");
        blocked.add("osman's report");

        // Additional blocked items (twisted, trailblazer, shattered, etc.)
        blocked.add("twisted banner");
        blocked.add("twisted teleport scroll");
        blocked.add("twisted blueprints");
        blocked.add("twisted horns");
        blocked.add("twisted coat (t1)");
        blocked.add("twisted coat (t2)");
        blocked.add("twisted coat (t3)");
        blocked.add("twisted trousers (t1)");
        blocked.add("twisted trousers (t2)");
        blocked.add("twisted trousers (t3)");
        blocked.add("twisted relic hunter (t1) armour set");
        blocked.add("twisted relic hunter (t2)");
        blocked.add("twisted relic hunter (t2) armour set");
        blocked.add("twisted relic hunter (t3)");
        blocked.add("twisted relic hunter (t3) armour set");
        blocked.add("trailblazer banner");
        blocked.add("trailblazer teleport scroll");
        blocked.add("trailblazer tool ornament kit");
        blocked.add("trailblazer globe");
        blocked.add("trailblazer rug");
        blocked.add("trailblazer graceful dye (pack of 6)");
        blocked.add("trailblazer relic hunter (t1) armour set");
        blocked.add("trailblazer relic hunter (t2)");
        blocked.add("trailblazer relic hunter (t2) armour set");
        blocked.add("trailblazer relic hunter (t3)");
        blocked.add("trailblazer relic hunter (t3) armour set");
        blocked.add("trailblazer hood (t1)");
        blocked.add("trailblazer hood (t2)");
        blocked.add("trailblazer hood (t3)");
        blocked.add("trailblazer top (t1)");
        blocked.add("trailblazer top (t2)");
        blocked.add("trailblazer top (t3)");
        blocked.add("trailblazer trousers (t1)");
        blocked.add("trailblazer trousers (t2)");
        blocked.add("trailblazer trousers (t3)");
        blocked.add("trailblazer boots (t1)");
        blocked.add("trailblazer boots (t2)");
        blocked.add("trailblazer boots (t3)");
        blocked.add("shattered banner");
        blocked.add("shattered teleport scroll");
        blocked.add("shattered relics variety ornament kit");
        blocked.add("shattered relics void ornament kit (pack of 6)");
        blocked.add("shattered relics mystic ornament kit (pack of 5)");
        blocked.add("shattered relics mystic ornament kit");
        blocked.add("shattered cannon ornament kit (pack of 4)");
        blocked.add("shattered relic hunter (t1)");
        blocked.add("shattered relic hunter (t1) armour set");
        blocked.add("shattered relic hunter (t2)");
        blocked.add("shattered relic hunter (t2) armour set");
        blocked.add("shattered relic hunter (t3)");
        blocked.add("shattered relic hunter (t3) armour set");
        blocked.add("shattered top (t1)");
        blocked.add("shattered top (t2)");
        blocked.add("shattered top (t3)");
        blocked.add("shattered hood (t1)");
        blocked.add("shattered hood (t2)");
        blocked.add("shattered hood (t3)");
        blocked.add("shattered trousers (t1)");
        blocked.add("shattered trousers (t2)");
        blocked.add("shattered trousers (t3)");
        blocked.add("shattered boots (t1)");
        blocked.add("shattered boots (t2)");
        blocked.add("shattered boots (t3)");
        blocked.add("trailblazer reloaded banner");
        blocked.add("trailblazer reloaded home teleport scroll");
        blocked.add("trailblazer reloaded death scroll");
        blocked.add("trailblazer reloaded alchemy scroll");
        blocked.add("trailblazer reloaded vengeance scroll");
        blocked.add("trailblazer reloaded rejuvenation pool scroll");
        blocked.add("trailblazer reloaded blowpipe orn kit");
        blocked.add("trailblazer reloaded bulwark orn kit");
        blocked.add("trailblazer reloaded relic hunter (t1)");
        blocked.add("trailblazer reloaded relic hunter (t1) armour set");
        blocked.add("trailblazer reloaded relic hunter (t2)");
        blocked.add("trailblazer reloaded relic hunter (t2) armour set");
        blocked.add("trailblazer reloaded relic hunter (t3)");
        blocked.add("trailblazer reloaded relic hunter (t3) armour set");
        blocked.add("trailblazer reloaded headband (t1)");
        blocked.add("trailblazer reloaded headband (t2)");
        blocked.add("trailblazer reloaded headband (t3)");
        blocked.add("trailblazer reloaded top (t1)");
        blocked.add("trailblazer reloaded top (t2)");
        blocked.add("trailblazer reloaded top (t3)");
        blocked.add("trailblazer reloaded trousers (t1)");
        blocked.add("trailblazer reloaded trousers (t2)");
        blocked.add("trailblazer reloaded trousers (t3)");
        blocked.add("trailblazer reloaded boots (t1)");
        blocked.add("trailblazer reloaded boots (t2)");
        blocked.add("trailblazer reloaded boots (t3)");
        blocked.add("raging echoes banner");
        blocked.add("raging echoes relic hunter (t1)");
        blocked.add("raging echoes relic hunter (t1) armour set");
        blocked.add("raging echoes relic hunter (t2)");
        blocked.add("raging echoes relic hunter (t2) armour set");
        blocked.add("raging echoes relic hunter (t3)");
        blocked.add("raging echoes relic hunter (t3) armour set");
        blocked.add("raging echoes spirit tree");
        blocked.add("raging echoes portal nexus");
        blocked.add("raging echoes rug");
        blocked.add("raging echoes curtains");
        blocked.add("raging echoes scrying pool");
        blocked.add("raging echoes portal");
        blocked.add("raging echoes home teleport");
        blocked.add("raging echoes death");
        blocked.add("raging echoes npc contact");
        blocked.add("echo ahrim's robes orn kit (pack of 4)");
        blocked.add("echo virtus robes orn kit (pack of 3)");
        blocked.add("echo venator bow orn kit");
        
        // Additional blocked items (Flatpacks and sets NOTE: not all flatpaks were added, some share exact names with their drink counterpart and therefore would block both items. only ways to fix this is to hardcode it or change blocklist format to IDs or Runelites NameIDS)
        //Flatpacks
        //blocked.add("armillary sphere"); exists only in game cache
        //blocked.add("asgarnian ale"); //flatpack shares name with drink
        blocked.add("beer barrel");
        blocked.add("bookcase");
        blocked.add("carved oak bench");
        blocked.add("carved oak magic wardrobe");
        blocked.add("carved oak table");
        blocked.add("carved teak bench");
        blocked.add("carved teak magic wardrobe");
        blocked.add("carved teak table");
        //blocked.add("celestial globe"); exists only in game cache
        //blocked.add("chef's delight"); //flatpack shares name with drink
        //blocked.add("cider barrel"); //flatpack shares name with drink
        blocked.add("crude chair");
        //blocked.add("crystal ball"); exists only in game cache
        //blocked.add("crystal of power"); exists only in game cache
        //blocked.add("demon lectern"); exists only in game cache
        //blocked.add("dragon bitter"); //flatpack shares name with drink
        //blocked.add("eagle lectern"); //exists only in game cache
        //blocked.add("elemental sphere"); //exists only in game cache
        blocked.add("fancy teak dresser");
        blocked.add("four-poster bed");
        blocked.add("gilded bench");
        blocked.add("gilded cape rack");
        blocked.add("gilded clock");
        blocked.add("gilded dresser");
        blocked.add("gilded four-poster");
        blocked.add("gilded magic wardrobe");
        blocked.add("gilded wardrobe");
        //blocked.add("globe"); //exists only in game cache
        //blocked.add("greenman's ale"); //flatpack shares name with drink
        blocked.add("kitchen table");
        blocked.add("large oak bed");
        //blocked.add("large orrery"); //exists only in game cache
        blocked.add("large teak bed");
        //blocked.add("lunar globe"); //exists only in game cache
        blocked.add("m. treasure chest");
        blocked.add("magic cape rack");
        blocked.add("mahogany armchair");
        blocked.add("mahogany armour case");
        blocked.add("mahogany bench");
        blocked.add("mahogany bookcase");
        blocked.add("mahogany cape rack");
        //blocked.add("mahogany demon"); //exists only in game cache
        blocked.add("mahogany dresser");
        //blocked.add("mahogany eagle"); //exists only in game cache
        blocked.add("mahogany fancy dress box");
        blocked.add("mahogany magic wardrobe");
        blocked.add("mahogany table");
        //blocked.add("mahogany telescope"); //exists only in game cache
        blocked.add("mahogany toy box");
        blocked.add("mahogany wardrobe");
        blocked.add("marble cape rack");
        blocked.add("marble magic wardrobe");
        blocked.add("oak armchair");
        blocked.add("oak armour case");
        blocked.add("oak bed");
        blocked.add("oak bench");
        blocked.add("oak bookcase");
        blocked.add("oak cape rack");
        blocked.add("oak chair");
        blocked.add("oak clock");
        blocked.add("oak dining table");
        blocked.add("oak drawers");
        blocked.add("oak dresser");
        blocked.add("oak fancy dress box");
        blocked.add("oak kitchen table");
        //blocked.add("oak lectern"); //exists only in game cache
        blocked.add("oak magic wardrobe");
        blocked.add("oak shaving stand");
        //blocked.add("oak telescope"); //exists only in game cache
        blocked.add("oak toy box");
        blocked.add("oak treasure chest");
        blocked.add("oak wardrobe");
        blocked.add("opulent table");
        //blocked.add("ornamental globe"); //exists only in game cache
        blocked.add("rocking chair");
        blocked.add("shaving stand");
        blocked.add("shoe box");
        //blocked.add("small orrery"); //exists only in game cache
        blocked.add("teak armchair");
        blocked.add("teak armour case");
        blocked.add("teak bed");
        blocked.add("teak cape rack");
        blocked.add("teak clock");
        //blocked.add("teak demon lectern"); //exists only in game cache
        blocked.add("teak dining bench");
        blocked.add("teak drawers");
        blocked.add("teak dresser");
        //blocked.add("teak eagle lectern"); //exists only in game cache
        blocked.add("teak fancy dress box");
        blocked.add("teak kitchen table");
        blocked.add("teak magic wardrobe");
        blocked.add("teak table");
        //blocked.add("teak telescope"); //exists only in game cache
        blocked.add("teak toy box");
        blocked.add("teak treasure chest");
        blocked.add("teak wardrobe");
        blocked.add("wood dining table");
        blocked.add("wooden bed");
        blocked.add("wooden bench");
        blocked.add("wooden chair");
        //GE Sets
        blocked.add("bronze set (lg)");
        blocked.add("iron set (lg)");
        blocked.add("steel set (lg)");
        blocked.add("black set (lg)");
        blocked.add("mithril set (lg)");
        blocked.add("adamant set (lg)");
        blocked.add("rune armour set (lg)");
        blocked.add("dragon armour set (lg)");
        blocked.add("bronze gold-trimmed set (lg)");
        blocked.add("iron gold-trimmed set (lg)");
        blocked.add("steel gold-trimmed set (lg)");
        blocked.add("black gold-trimmed set (lg)");
        blocked.add("mithril gold-trimmed set (lg)");
        blocked.add("adamant gold-trimmed set (lg)");
        blocked.add("rune gold-trimmed set (lg)");
        blocked.add("gilded armour set (lg)");
        blocked.add("bronze trimmed set (lg)");
        blocked.add("iron trimmed set (lg)");
        blocked.add("steel trimmed set (lg)");
        blocked.add("black trimmed set (lg)");
        blocked.add("mithril trimmed set (lg)");
        blocked.add("adamant trimmed set (lg)");
        blocked.add("rune trimmed set (lg)");
        blocked.add("guthix armour set (lg)");
        blocked.add("saradomin armour set (lg)");
        blocked.add("zamorak armour set (lg)");
        blocked.add("ancient rune armour set (lg)");
        blocked.add("armadyl rune armour set (lg)");
        blocked.add("bandos rune armour set (lg)");
        blocked.add("bronze set (sk)");
        blocked.add("iron set (sk)");
        blocked.add("steel set (sk)");
        blocked.add("black set (sk)");
        blocked.add("mithril set (sk)");
        blocked.add("adamant set (sk)");
        blocked.add("rune armour set (sk)");
        blocked.add("dragon armour set (sk)");
        blocked.add("bronze gold-trimmed set (sk)");
        blocked.add("iron gold-trimmed set (sk)");
        blocked.add("steel gold-trimmed set (sk)");
        blocked.add("black gold-trimmed set (sk)");
        blocked.add("mithril gold-trimmed set (sk)");
        blocked.add("adamant gold-trimmed set (sk)");
        blocked.add("rune gold-trimmed set (sk)");
        blocked.add("gilded armour set (sk)");
        blocked.add("bronze trimmed set (sk)");
        blocked.add("iron trimmed set (sk)");
        blocked.add("steel trimmed set (sk)");
        blocked.add("black trimmed set (sk)");
        blocked.add("mithril trimmed set (sk)");
        blocked.add("adamant trimmed set (sk)");
        blocked.add("rune trimmed set (sk)");
        blocked.add("guthix armour set (sk)");
        blocked.add("saradomin armour set (sk)");
        blocked.add("zamorak armour set (sk)");
        blocked.add("ancient rune armour set (sk)");
        blocked.add("armadyl rune armour set (sk)");
        blocked.add("bandos rune armour set (sk)");
        blocked.add("initiate harness m");
        blocked.add("proselyte harness m");
        blocked.add("proselyte harness f");
        blocked.add("green dragonhide set");
        blocked.add("gilded dragonhide set");
        blocked.add("blue dragonhide set");
        blocked.add("red dragonhide set");
        blocked.add("black dragonhide set");
        blocked.add("guthix dragonhide set");
        blocked.add("saradomin dragonhide set");
        blocked.add("zamorak dragonhide set");
        blocked.add("ancient dragonhide set");
        blocked.add("armadyl dragonhide set");
        blocked.add("bandos dragonhide set");
        blocked.add("ahrim's armour set");
        blocked.add("dharok's armour set");
        blocked.add("guthan's armour set");
        blocked.add("karil's armour set");
        blocked.add("torag's armour set");
        blocked.add("verac's armour set");
        blocked.add("mystic set (blue)");
        blocked.add("mystic set (light)");
        blocked.add("mystic set (dark)");
        blocked.add("mystic set (dusk)");
        blocked.add("book of balance page set");
        blocked.add("holy book page set");
        blocked.add("unholy book page set");
        blocked.add("book of darkness page set");
        blocked.add("book of law page set");
        blocked.add("book of war page set");
        blocked.add("dwarf cannon set");
        blocked.add("combat potion set");
        blocked.add("super potion set");
        blocked.add("partyhat set");
        blocked.add("halloween mask set");
        blocked.add("ancestral robes set");
        blocked.add("inquisitor's armour set");
        blocked.add("dagon'hai robes set");
        blocked.add("justiciar armour set");
        blocked.add("obsidian armour set");
        blocked.add("sunfire fanatic armour set");
        blocked.add("dragonstone armour set");
        blocked.add("masori armour set (f)");
        
        BLOCKED_ITEMS = Collections.unmodifiableSet(blocked);

        // Initialize the ensouled head mapping.
        Map<String, Integer> ensouledMap = new HashMap<>();
        ensouledMap.put("ensouled abyssal head", 13508);
        ensouledMap.put("ensouled aviansie head", 13505);
        ensouledMap.put("ensouled bear head", 13463);
        ensouledMap.put("ensouled bloodveld head", 13496);
        ensouledMap.put("ensouled chaos druid head", 13472);
        ensouledMap.put("ensouled dagannoth head", 13493);
        ensouledMap.put("ensouled demon head", 13502);
        ensouledMap.put("ensouled dog head", 13469);
        ensouledMap.put("ensouled dragon head", 13511);
        ensouledMap.put("ensouled elf head", 13481);
        ensouledMap.put("ensouled giant head", 13475);
        ensouledMap.put("ensouled goblin head", 13448);
        ensouledMap.put("ensouled hellhound head", 26997);
        ensouledMap.put("ensouled horror head", 13487);
        ensouledMap.put("ensouled imp head", 13454);
        ensouledMap.put("ensouled kalphite head", 13490);
        ensouledMap.put("ensouled minotaur head", 13457);
        ensouledMap.put("ensouled monkey head", 13451);
        ensouledMap.put("ensouled ogre head", 13478);
        ensouledMap.put("ensouled scorpion head", 13460);
        ensouledMap.put("ensouled troll head", 13484);
        ensouledMap.put("ensouled tzhaar head", 13499);
        ensouledMap.put("ensouled unicorn head", 13466);
        ENSOULED_HEAD_MAP = Collections.unmodifiableMap(ensouledMap);
    }

    /**
     * Returns true if the provided item name is in the blocked list.
     * The check is case-insensitive.
     */
    public static boolean isBlocked(String itemName) {
        return itemName != null && BLOCKED_ITEMS.contains(itemName.toLowerCase());
    }

    /**
     * Returns the correct ensouled head ID for the given item name.
     * If not found, returns DEFAULT_ENSOULED_HEAD_ID.
     */
    public static int getEnsouledHeadId(String itemName) {
        return itemName == null ? DEFAULT_ENSOULED_HEAD_ID
                : ENSOULED_HEAD_MAP.getOrDefault(itemName.toLowerCase(), DEFAULT_ENSOULED_HEAD_ID);
    }

    /**
     * Scans the provided text and returns the ensouled head ID if any known ensouled head key is found.
     * Returns DEFAULT_ENSOULED_HEAD_ID if no match is found.
     */
    public static int getEnsouledHeadIdFromText(String text) {
        if (text == null) {
            return DEFAULT_ENSOULED_HEAD_ID;
        }
        String lower = text.toLowerCase();
        for (String key : ENSOULED_HEAD_MAP.keySet()) {
            if (lower.contains(key)) {
                return ENSOULED_HEAD_MAP.get(key);
            }
        }
        return DEFAULT_ENSOULED_HEAD_ID;
    }
}
