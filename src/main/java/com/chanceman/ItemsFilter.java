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

    // Removes Flatpacks, only active if "No Flatpacks" is toggled on in config (non-rollable items); stored in lower-case.
    private static final Set<String> FLATPACK_BLOCKED_ITEMS;

    // Removes Armour Sets, only active if "No Armour Sets" is toggled on in config (non-rollable items); stored in lower-case.
    private static final Set<String> SET_BLOCKED_ITEMS;

    // Removes Poisoned Weapons, only active if "No Poisoned Weapons" is toggled on in config (non-rollable items); stored in lower-case.
    private static final Set<String> POISONED_BLOCKED_ITEMS;

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
        
        BLOCKED_ITEMS = Collections.unmodifiableSet(blocked);
        
        //flatpacks
        Set<String> fpblocked = new HashSet<>();
         // Additional blocked items (Flatpacks and sets NOTE: not all flatpaks were added, some share exact names with their drink counterpart and therefore would block both items. only ways to fix this is to hardcode it or change blocklist format to IDs or Runelites NameIDS)
        //Flatpacks
        //fpblocked.add("armillary sphere");
        //fpblocked.add("asgarnian ale");
        fpblocked.add("beer barrel");
        fpblocked.add("bookcase");
        fpblocked.add("carved oak bench");
        fpblocked.add("carved oak magic wardrobe");
        fpblocked.add("carved oak table");
        fpblocked.add("carved teak bench");
        fpblocked.add("carved teak magic wardrobe");
        fpblocked.add("carved teak table");
        //fpblocked.add("celestial globe");
        //fpblocked.add("chef's delight");
        //fpblocked.add("cider barrel");
        fpblocked.add("crude chair");
        //fpblocked.add("crystal ball");
        //fpblocked.add("crystal of power");
        //fpblocked.add("demon lectern");
        //fpblocked.add("dragon bitter");
        //fpblocked.add("eagle lectern");
        //fpblocked.add("elemental sphere");
        fpblocked.add("fancy teak dresser");
        fpblocked.add("four-poster bed");
        fpblocked.add("gilded bench");
        fpblocked.add("gilded cape rack");
        fpblocked.add("gilded clock");
        fpblocked.add("gilded dresser");
        fpblocked.add("gilded four-poster");
        fpblocked.add("gilded magic wardrobe");
        fpblocked.add("gilded wardrobe");
        //fpblocked.add("globe");
        //fpblocked.add("greenman's ale");
        fpblocked.add("kitchen table");
        fpblocked.add("large oak bed");
        //fpblocked.add("large orrery");
        fpblocked.add("large teak bed");
        //fpblocked.add("lunar globe");
        fpblocked.add("m. treasure chest");
        fpblocked.add("magic cape rack");
        fpblocked.add("mahogany armchair");
        fpblocked.add("mahogany armour case");
        fpblocked.add("mahogany bench");
        fpblocked.add("mahogany bookcase");
        fpblocked.add("mahogany cape rack");
        //fpblocked.add("mahogany demon");
        fpblocked.add("mahogany dresser");
        //fpblocked.add("mahogany eagle");
        fpblocked.add("mahogany fancy dress box");
        fpblocked.add("mahogany magic wardrobe");
        fpblocked.add("mahogany table");
        //fpblocked.add("mahogany telescope");
        fpblocked.add("mahogany toy box");
        fpblocked.add("mahogany wardrobe");
        fpblocked.add("marble cape rack");
        fpblocked.add("marble magic wardrobe");
        fpblocked.add("oak armchair");
        fpblocked.add("oak armour case");
        fpblocked.add("oak bed");
        fpblocked.add("oak bench");
        fpblocked.add("oak bookcase");
        fpblocked.add("oak cape rack");
        fpblocked.add("oak chair");
        fpblocked.add("oak clock");
        fpblocked.add("oak dining table");
        fpblocked.add("oak drawers");
        fpblocked.add("oak dresser");
        fpblocked.add("oak fancy dress box");
        fpblocked.add("oak kitchen table");
        //fpblocked.add("oak lectern");
        fpblocked.add("oak magic wardrobe");
        fpblocked.add("oak shaving stand");
        //fpblocked.add("oak telescope");
        fpblocked.add("oak toy box");
        fpblocked.add("oak treasure chest");
        fpblocked.add("oak wardrobe");
        fpblocked.add("opulent table");
        //fpblocked.add("ornamental globe");
        fpblocked.add("rocking chair");
        fpblocked.add("shaving stand");
        fpblocked.add("shoe box");
        //fpblocked.add("small orrery");
        fpblocked.add("teak armchair");
        fpblocked.add("teak armour case");
        fpblocked.add("teak bed");
        fpblocked.add("teak cape rack");
        fpblocked.add("teak clock");
        //fpblocked.add("teak demon lectern");
        fpblocked.add("teak dining bench");
        fpblocked.add("teak drawers");
        fpblocked.add("teak dresser");
        //fpblocked.add("teak eagle lectern");
        fpblocked.add("teak fancy dress box");
        fpblocked.add("teak kitchen table");
        fpblocked.add("teak magic wardrobe");
        fpblocked.add("teak table");
        //fpblocked.add("teak telescope");
        fpblocked.add("teak toy box");
        fpblocked.add("teak treasure chest");
        fpblocked.add("teak wardrobe");
        fpblocked.add("wood dining table");
        fpblocked.add("wooden bed");
        fpblocked.add("wooden bench");
        fpblocked.add("wooden chair");
        
        FLATPACK_BLOCKED_ITEMS = Collections.unmodifiableSet(fpblocked);
        
        //GE Sets
        Set<String> setblocked = new HashSet<>();
        setblocked.add("bronze set (lg)");
        setblocked.add("iron set (lg)");
        setblocked.add("steel set (lg)");
        setblocked.add("black set (lg)");
        setblocked.add("mithril set (lg)");
        setblocked.add("adamant set (lg)");
        setblocked.add("rune armour set (lg)");
        setblocked.add("dragon armour set (lg)");
        setblocked.add("bronze gold-trimmed set (lg)");
        setblocked.add("iron gold-trimmed set (lg)");
        setblocked.add("steel gold-trimmed set (lg)");
        setblocked.add("black gold-trimmed set (lg)");
        setblocked.add("mithril gold-trimmed set (lg)");
        setblocked.add("adamant gold-trimmed set (lg)");
        setblocked.add("rune gold-trimmed set (lg)");
        setblocked.add("gilded armour set (lg)");
        setblocked.add("bronze trimmed set (lg)");
        setblocked.add("iron trimmed set (lg)");
        setblocked.add("steel trimmed set (lg)");
        setblocked.add("black trimmed set (lg)");
        setblocked.add("mithril trimmed set (lg)");
        setblocked.add("adamant trimmed set (lg)");
        setblocked.add("rune trimmed set (lg)");
        setblocked.add("guthix armour set (lg)");
        setblocked.add("saradomin armour set (lg)");
        setblocked.add("zamorak armour set (lg)");
        setblocked.add("ancient rune armour set (lg)");
        setblocked.add("armadyl rune armour set (lg)");
        setblocked.add("bandos rune armour set (lg)");
        setblocked.add("bronze set (sk)");
        setblocked.add("iron set (sk)");
        setblocked.add("steel set (sk)");
        setblocked.add("black set (sk)");
        setblocked.add("mithril set (sk)");
        setblocked.add("adamant set (sk)");
        setblocked.add("rune armour set (sk)");
        setblocked.add("dragon armour set (sk)");
        setblocked.add("bronze gold-trimmed set (sk)");
        setblocked.add("iron gold-trimmed set (sk)");
        setblocked.add("steel gold-trimmed set (sk)");
        setblocked.add("black gold-trimmed set (sk)");
        setblocked.add("mithril gold-trimmed set (sk)");
        setblocked.add("adamant gold-trimmed set (sk)");
        setblocked.add("rune gold-trimmed set (sk)");
        setblocked.add("gilded armour set (sk)");
        setblocked.add("bronze trimmed set (sk)");
        setblocked.add("iron trimmed set (sk)");
        setblocked.add("steel trimmed set (sk)");
        setblocked.add("black trimmed set (sk)");
        setblocked.add("mithril trimmed set (sk)");
        setblocked.add("adamant trimmed set (sk)");
        setblocked.add("rune trimmed set (sk)");
        setblocked.add("guthix armour set (sk)");
        setblocked.add("saradomin armour set (sk)");
        setblocked.add("zamorak armour set (sk)");
        setblocked.add("ancient rune armour set (sk)");
        setblocked.add("armadyl rune armour set (sk)");
        setblocked.add("bandos rune armour set (sk)");
        setblocked.add("initiate harness m");
        setblocked.add("proselyte harness m");
        setblocked.add("proselyte harness f");
        setblocked.add("green dragonhide set");
        setblocked.add("gilded dragonhide set");
        setblocked.add("blue dragonhide set");
        setblocked.add("red dragonhide set");
        setblocked.add("black dragonhide set");
        setblocked.add("guthix dragonhide set");
        setblocked.add("saradomin dragonhide set");
        setblocked.add("zamorak dragonhide set");
        setblocked.add("ancient dragonhide set");
        setblocked.add("armadyl dragonhide set");
        setblocked.add("bandos dragonhide set");
        setblocked.add("ahrim's armour set");
        setblocked.add("dharok's armour set");
        setblocked.add("guthan's armour set");
        setblocked.add("karil's armour set");
        setblocked.add("torag's armour set");
        setblocked.add("verac's armour set");
        setblocked.add("mystic set (blue)");
        setblocked.add("mystic set (light)");
        setblocked.add("mystic set (dark)");
        setblocked.add("mystic set (dusk)");
        setblocked.add("book of balance page set");
        setblocked.add("holy book page set");
        setblocked.add("unholy book page set");
        setblocked.add("book of darkness page set");
        setblocked.add("book of law page set");
        setblocked.add("book of war page set");
        setblocked.add("dwarf cannon set");
        setblocked.add("combat potion set");
        setblocked.add("super potion set");
        setblocked.add("partyhat set");
        setblocked.add("halloween mask set");
        setblocked.add("ancestral robes set");
        setblocked.add("inquisitor's armour set");
        setblocked.add("dagon'hai robes set");
        setblocked.add("justiciar armour set");
        setblocked.add("obsidian armour set");
        setblocked.add("sunfire fanatic armour set");
        setblocked.add("dragonstone armour set");
        setblocked.add("masori armour set (f)");

        SET_BLOCKED_ITEMS = Collections.unmodifiableSet(setblocked);

        //Poison weapons
        Set<String> poisonedblocked = new HashSet<>();
        //all (p) items
        poisonedblocked.add("abyssaldagger(p)");
        poisonedblocked.add("adamantarrow(p)");
        poisonedblocked.add("adamantbolts(p)");
        poisonedblocked.add("adamantdagger(p)");
        poisonedblocked.add("adamantdart(p)");
        poisonedblocked.add("adamanthasta(p)");
        poisonedblocked.add("adamantjavelin(p)");
        poisonedblocked.add("adamantknife(p)");
        poisonedblocked.add("adamantspear(p)");
        poisonedblocked.add("amethystarrow(p)");
        poisonedblocked.add("amethystdart(p)");
        poisonedblocked.add("amethystjavelin(p)");
        poisonedblocked.add("blackdagger(p)");
        poisonedblocked.add("blackdart(p)");
        poisonedblocked.add("blackknife(p)");
        poisonedblocked.add("blackspear(p)");
        poisonedblocked.add("bonedagger(p)");
        poisonedblocked.add("bronzearrow(p)");
        poisonedblocked.add("bronzebolts(p)");
        poisonedblocked.add("bronzedagger(p)");
        poisonedblocked.add("bronzedart(p)");
        poisonedblocked.add("bronzehasta(p)");
        poisonedblocked.add("bronzejavelin(p)");
        poisonedblocked.add("bronzeknife(p)");
        poisonedblocked.add("bronzespear(p)");
        poisonedblocked.add("dragonarrow(p)");
        poisonedblocked.add("dragonbolts(p)");
        poisonedblocked.add("dragondagger(p)");
        poisonedblocked.add("dragondart(p)");
        poisonedblocked.add("dragonhasta(p)");
        poisonedblocked.add("dragonjavelin(p)");
        poisonedblocked.add("dragonknife(p)");
        poisonedblocked.add("dragonspear(p)(cr)");
        poisonedblocked.add("dragonspear(p)");
        poisonedblocked.add("ironarrow(p)");
        poisonedblocked.add("ironbolts(p)");
        poisonedblocked.add("irondagger(p)");
        poisonedblocked.add("irondart(p)");
        poisonedblocked.add("ironhasta(p)");
        poisonedblocked.add("ironjavelin(p)");
        poisonedblocked.add("ironknife(p)");
        poisonedblocked.add("ironspear(p)");
        poisonedblocked.add("mithrilarrow(p)");
        poisonedblocked.add("mithrilbolts(p)");
        poisonedblocked.add("mithrildagger(p)");
        poisonedblocked.add("mithrildart(p)");
        poisonedblocked.add("mithrilhasta(p)");
        poisonedblocked.add("mithriljavelin(p)");
        poisonedblocked.add("mithrilknife(p)");
        poisonedblocked.add("mithrilspear(p)");
        poisonedblocked.add("runearrow(p)");
        poisonedblocked.add("runedagger(p)");
        poisonedblocked.add("runedart(p)");
        poisonedblocked.add("runehasta(p)");
        poisonedblocked.add("runejavelin(p)");
        poisonedblocked.add("runeknife(p)");
        poisonedblocked.add("runespear(p)");
        poisonedblocked.add("runitebolts(p)");
        poisonedblocked.add("steelarrow(p)");
        poisonedblocked.add("steelbolts(p)");
        poisonedblocked.add("steeldagger(p)");
        poisonedblocked.add("steeldart(p)");
        poisonedblocked.add("steelhasta(p)");
        poisonedblocked.add("steeljavelin(p)");
        poisonedblocked.add("steelknife(p)");
        poisonedblocked.add("steelspear(p)");
        poisonedblocked.add("whitedagger(p)");
        //all(p+)items
        poisonedblocked.add("abyssaldagger(p+)");
        poisonedblocked.add("adamantarrow(p+)");
        poisonedblocked.add("adamantbolts(p+)");
        poisonedblocked.add("adamantdagger(p+)");
        poisonedblocked.add("adamantdart(p+)");
        poisonedblocked.add("adamanthasta(p+)");
        poisonedblocked.add("adamantjavelin(p+)");
        poisonedblocked.add("adamantknife(p+)");
        poisonedblocked.add("adamantspear(p+)");
        poisonedblocked.add("amethystarrow(p+)");
        poisonedblocked.add("amethystdart(p+)");
        poisonedblocked.add("amethystjavelin(p+)");
        poisonedblocked.add("blackdagger(p+)");
        poisonedblocked.add("blackdart(p+)");
        poisonedblocked.add("blackknife(p+)");
        poisonedblocked.add("blackspear(p+)");
        poisonedblocked.add("bonedagger(p+)");
        poisonedblocked.add("bronzearrow(p+)");
        poisonedblocked.add("bronzebolts(p+)");
        poisonedblocked.add("bronzedagger(p+)");
        poisonedblocked.add("bronzedart(p+)");
        poisonedblocked.add("bronzehasta(p+)");
        poisonedblocked.add("bronzejavelin(p+)");
        poisonedblocked.add("bronzeknife(p+)");
        poisonedblocked.add("bronzespear(p+)");
        poisonedblocked.add("dragonarrow(p+)");
        poisonedblocked.add("dragonbolts(p+)");
        poisonedblocked.add("dragondagger(p+)");
        poisonedblocked.add("dragondart(p+)");
        poisonedblocked.add("dragonhasta(p+)");
        poisonedblocked.add("dragonjavelin(p+)");
        poisonedblocked.add("dragonknife(p+)");
        poisonedblocked.add("dragonspear(p+)(cr)");
        poisonedblocked.add("dragonspear(p+)");
        poisonedblocked.add("ironarrow(p+)");
        poisonedblocked.add("ironbolts(p+)");
        poisonedblocked.add("irondagger(p+)");
        poisonedblocked.add("irondart(p+)");
        poisonedblocked.add("ironhasta(p+)");
        poisonedblocked.add("ironjavelin(p+)");
        poisonedblocked.add("ironknife(p+)");
        poisonedblocked.add("ironspear(p+)");
        poisonedblocked.add("mithrilarrow(p+)");
        poisonedblocked.add("mithrilbolts(p+)");
        poisonedblocked.add("mithrildagger(p+)");
        poisonedblocked.add("mithrildart(p+)");
        poisonedblocked.add("mithrilhasta(p+)");
        poisonedblocked.add("mithriljavelin(p+)");
        poisonedblocked.add("mithrilknife(p+)");
        poisonedblocked.add("mithrilspear(p+)");
        poisonedblocked.add("runearrow(p+)");
        poisonedblocked.add("runedagger(p+)");
        poisonedblocked.add("runedart(p+)");
        poisonedblocked.add("runehasta(p+)");
        poisonedblocked.add("runejavelin(p+)");
        poisonedblocked.add("runeknife(p+)");
        poisonedblocked.add("runespear(p+)");
        poisonedblocked.add("runitebolts(p+)");
        poisonedblocked.add("steelarrow(p+)");
        poisonedblocked.add("steelbolts(p+)");
        poisonedblocked.add("steeldagger(p+)");
        poisonedblocked.add("steeldart(p+)");
        poisonedblocked.add("steelhasta(p+)");
        poisonedblocked.add("steeljavelin(p+)");
        poisonedblocked.add("steelknife(p+)");
        poisonedblocked.add("steelspear(p+)");
        poisonedblocked.add("whitedagger(p+)");
        //allp++items
        poisonedblocked.add("abyssaldagger(p++)");
        poisonedblocked.add("adamantarrow(p++)");
        poisonedblocked.add("adamantbolts(p++)");
        poisonedblocked.add("adamantdagger(p++)");
        poisonedblocked.add("adamantdart(p++)");
        poisonedblocked.add("adamanthasta(p++)");
        poisonedblocked.add("adamantjavelin(p++)");
        poisonedblocked.add("adamantknife(p++)");
        poisonedblocked.add("adamantspear(p++)");
        poisonedblocked.add("amethystarrow(p++)");
        poisonedblocked.add("amethystdart(p++)");
        poisonedblocked.add("amethystjavelin(p++)");
        poisonedblocked.add("blackdagger(p++)");
        poisonedblocked.add("blackdart(p++)");
        poisonedblocked.add("blackknife(p++)");
        poisonedblocked.add("blackspear(p++)");
        poisonedblocked.add("bonedagger(p++)");
        poisonedblocked.add("bronzearrow(p++)");
        poisonedblocked.add("bronzebolts(p++)");
        poisonedblocked.add("bronzedagger(p++)");
        poisonedblocked.add("bronzedart(p++)");
        poisonedblocked.add("bronzehasta(p++)");
        poisonedblocked.add("bronzejavelin(p++)");
        poisonedblocked.add("bronzeknife(p++)");
        poisonedblocked.add("bronzespear(p++)");
        poisonedblocked.add("dragonarrow(p++)");
        poisonedblocked.add("dragonbolts(p++)");
        poisonedblocked.add("dragondagger(p++)");
        poisonedblocked.add("dragondart(p++)");
        poisonedblocked.add("dragonhasta(p++)");
        poisonedblocked.add("dragonjavelin(p++)");
        poisonedblocked.add("dragonknife(p++)");
        poisonedblocked.add("dragonspear(p++)(cr)");
        poisonedblocked.add("dragonspear(p++)");
        poisonedblocked.add("ironarrow(p++)");
        poisonedblocked.add("ironbolts(p++)");
        poisonedblocked.add("irondagger(p++)");
        poisonedblocked.add("irondart(p++)");
        poisonedblocked.add("ironhasta(p++)");
        poisonedblocked.add("ironjavelin(p++)");
        poisonedblocked.add("ironknife(p++)");
        poisonedblocked.add("ironspear(p++)");
        poisonedblocked.add("mithrilarrow(p++)");
        poisonedblocked.add("mithrilbolts(p++)");
        poisonedblocked.add("mithrildagger(p++)");
        poisonedblocked.add("mithrildart(p++)");
        poisonedblocked.add("mithrilhasta(p++)");
        poisonedblocked.add("mithriljavelin(p++)");
        poisonedblocked.add("mithrilknife(p++)");
        poisonedblocked.add("mithrilspear(p++)");
        poisonedblocked.add("runearrow(p++)");
        poisonedblocked.add("runedagger(p++)");
        poisonedblocked.add("runedart(p++)");
        poisonedblocked.add("runehasta(p++)");
        poisonedblocked.add("runejavelin(p++)");
        poisonedblocked.add("runeknife(p++)");
        poisonedblocked.add("runespear(p++)");
        poisonedblocked.add("runitebolts(p++)");
        poisonedblocked.add("steelarrow(p++)");
        poisonedblocked.add("steelbolts(p++)");
        poisonedblocked.add("steeldagger(p++)");
        poisonedblocked.add("steeldart(p++)");
        poisonedblocked.add("steelhasta(p++)");
        poisonedblocked.add("steeljavelin(p++)");
        poisonedblocked.add("steelknife(p++)");
        poisonedblocked.add("steelspear(p++)");
        poisonedblocked.add("whitedagger(p++)");

        
        POISONED_BLOCKED_ITEMS = Collections.unmodifiableSet(poisonedblocked);
        

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

    public static boolean isFpBlocked(String itemName) {
        return itemName != null && FLATPACK_BLOCKED_ITEMS.contains(itemName.toLowerCase());
    }
    
    public static boolean isSetBlocked(String itemName) {
        return itemName != null && SET_BLOCKED_ITEMS.contains(itemName.toLowerCase());
    }
    
    public static boolean isPoisonedBlocked(String itemName) {
        return itemName != null && POISONED_BLOCKED_ITEMS.contains(itemName.toLowerCase());
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
