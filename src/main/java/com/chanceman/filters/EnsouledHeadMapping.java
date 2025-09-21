package com.chanceman.filters;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

        pair.accept(13447, 13448); // Goblin
        pair.accept(13450, 13451); // Monkey
        pair.accept(13453, 13454); // Imp
        pair.accept(13456, 13457); // Minotaur
        pair.accept(13459, 13460); // Scorpion
        pair.accept(13462, 13463); // Bear
        pair.accept(13465, 13466); // Unicorn
        pair.accept(13468, 13469); // Dog
        pair.accept(13471, 13472); // Chaos Druid
        pair.accept(13477, 13478); // Ogre
        pair.accept(13480, 13481); // Elf
        pair.accept(13483, 13484); // Troll
        pair.accept(13486, 13487); // Horror
        pair.accept(13489, 13490); // Kalphite
        pair.accept(13492, 13493); // Dagannoth
        pair.accept(13495, 13496); // Bloodveld
        pair.accept(13498, 13499); // TzHaar
        pair.accept(13501, 13502); // Demon
        pair.accept(26996, 26997); // Hellhound
        pair.accept(13504, 13505); // Aviansie
        pair.accept(13507, 13508); // Abyssal
        pair.accept(13510, 13511); // Dragon

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
