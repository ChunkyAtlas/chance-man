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
        blocked.add("blighted overload");
        blocked.add("blighted overload (1)");
        blocked.add("blighted overload (2)");
        blocked.add("blighted overload (3)");
        blocked.add("blighted overload (4)");
        blocked.add("chitin");
        blocked.add("corrupted armadyl godsword");
        blocked.add("corrupted dark bow");
        blocked.add("corrupted dragon claws");
        blocked.add("corrupted twisted bow");
        blocked.add("corrupted voidwaker");
        blocked.add("corrupted volatile nightmare staff");
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
        blocked.add("twisted relic hunter (t1)");
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
        blocked.add("trailblazer relic hunter (t1)");
        blocked.add("trailblazer relic hunter (t1) armour sset");
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
        blocked.add("shattered cannon ornament kit (pack of 4)");
        blocked.add("shattered relic hunter (t1)");
        blocked.add("shattered relic hunter (t1) armour set");
        blocked.add("shattered relic hunter (t2)");
        blocked.add("shattered relic hunter (t2) armour set");
        blocked.add("shattered relic hunter (t3)");
        blocked.add("shattered relic hunter (t3) armour set");
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
