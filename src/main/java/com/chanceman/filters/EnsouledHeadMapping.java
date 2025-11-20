package com.chanceman.filters;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.runelite.api.gameval.ItemID;

/**
 * Maps ensouled head item IDs (including untradeable drop IDs) to their tradeable IDs.
 * Non-ensouled or unknown IDs are returned unchanged by {@link #toTradeableId(int)}.
 */
public final class EnsouledHeadMapping {

    @Getter
    public static final Map<Integer, Integer> ENSOULED_CANONICAL_ID;

    static {
        Map<Integer, Integer> idMap = new HashMap<>();

        java.util.function.BiConsumer<Integer, Integer> pair = (untradeable, tradeable) -> {
            idMap.put(untradeable, tradeable);
            idMap.put(tradeable, tradeable);
        };

        pair.accept(ItemID.ARCEUUS_CORPSE_GOBLIN_INITIAL, ItemID.ARCEUUS_CORPSE_GOBLIN); // Goblin
        pair.accept(ItemID.ARCEUUS_CORPSE_MONKEY_INITIAL, ItemID.ARCEUUS_CORPSE_MONKEY); // Monkey
        pair.accept(ItemID.ARCEUUS_CORPSE_IMP_INITIAL, ItemID.ARCEUUS_CORPSE_IMP); // Imp
        pair.accept(ItemID.ARCEUUS_CORPSE_MINOTAUR_INITIAL, ItemID.ARCEUUS_CORPSE_MINOTAUR); // Minotaur
        pair.accept(ItemID.ARCEUUS_CORPSE_SCORPION_INITIAL, ItemID.ARCEUUS_CORPSE_SCORPION); // Scorpion
        pair.accept(ItemID.ARCEUUS_CORPSE_BEAR_INITIAL, ItemID.ARCEUUS_CORPSE_BEAR); // Bear
        pair.accept(ItemID.ARCEUUS_CORPSE_UNICORN_INITIAL, ItemID.ARCEUUS_CORPSE_UNICORN ); // Unicorn
        pair.accept(ItemID.ARCEUUS_CORPSE_DOG_INITIAL, ItemID.ARCEUUS_CORPSE_DOG); // Dog
        pair.accept(ItemID.ARCEUUS_CORPSE_CHAOSDRUID_INITIAL, ItemID.ARCEUUS_CORPSE_CHAOSDRUID); // Chaos Druid
        pair.accept(ItemID.ARCEUUS_CORPSE_GIANT_INITIAL, ItemID.ARCEUUS_CORPSE_GIANT); // Giant
        pair.accept(ItemID.ARCEUUS_CORPSE_OGRE_INITIAL, ItemID.ARCEUUS_CORPSE_OGRE); // Ogre
        pair.accept(ItemID.ARCEUUS_CORPSE_ELF_INITIAL, ItemID.ARCEUUS_CORPSE_ELF); // Elf
        pair.accept(ItemID.ARCEUUS_CORPSE_TROLL_INITIAL, ItemID.ARCEUUS_CORPSE_TROLL); // Troll
        pair.accept(ItemID.ARCEUUS_CORPSE_HORROR_INITIAL, ItemID.ARCEUUS_CORPSE_HORROR); // Horror
        pair.accept(ItemID.ARCEUUS_CORPSE_KALPHITE_INITIAL, ItemID.ARCEUUS_CORPSE_KALPHITE); // Kalphite
        pair.accept(ItemID.ARCEUUS_CORPSE_DAGANNOTH_INITIAL, ItemID.ARCEUUS_CORPSE_DAGANNOTH); // Dagannoth
        pair.accept(ItemID.ARCEUUS_CORPSE_BLOODVELD_INITIAL, ItemID.ARCEUUS_CORPSE_BLOODVELD); // Bloodveld
        pair.accept(ItemID.ARCEUUS_CORPSE_TZHAAR_INITIAL, ItemID.ARCEUUS_CORPSE_TZHAAR); // TzHaar
        pair.accept(ItemID.ARCEUUS_CORPSE_DEMON_INITIAL, ItemID.ARCEUUS_CORPSE_DEMON); // Demon
        pair.accept(ItemID.ARCEUUS_CORPSE_AVIANSIE_INITIAL, ItemID.ARCEUUS_CORPSE_AVIANSIE); //Aviansie
        pair.accept(ItemID.ARCEUUS_CORPSE_ABYSSAL_INITIAL, ItemID.ARCEUUS_CORPSE_ABYSSAL); // Abyssal
        pair.accept(ItemID.ARCEUUS_CORPSE_DRAGON_INITIAL, ItemID.ARCEUUS_CORPSE_DRAGON); // Dragon
        pair.accept(ItemID.ARCEUUS_CORPSE_HELLHOUND_INITIAL, ItemID.ARCEUUS_CORPSE_HELLHOUND); // Hellhound

        ENSOULED_CANONICAL_ID = Collections.unmodifiableMap(idMap);
    }

    private EnsouledHeadMapping() { /* utility class, no instances */ }

    /**
     * Returns the tradeable ID for any ensouled head item ID.
     * If the ID is already tradeable or not recognized as an ensouled head, it's returned unchanged.
     */
    public static int toTradeableId(int itemId) {
        return ENSOULED_CANONICAL_ID.getOrDefault(itemId, itemId);
    }
}
