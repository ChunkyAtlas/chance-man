package com.chanceman.filters;

import com.chanceman.ChanceManConfig;

import java.util.Set;

/**
 * Utility class for additional item filtering logic.
 */
public class ItemsFilter {

    /**
     * Checks if an item is blocked.
     * An item is blocked if it is in the blocked set,
     * or if it is a flatpack and flatpacks are disabled,
     * or if it is an item set and item sets are disabled.
     *
     * @param itemId the item id
     * @param config determines if item sets, flatpacks, and/or trade-only f2p items
     *               should be blocked
     * @return true if the item is blocked; false otherwise
     */
    public static boolean isBlocked(int itemId, ChanceManConfig config) {
        return (!config.enableFlatpacks() && Flatpacks.isFlatpack(itemId))
                || (!config.enableItemSets() && ItemSets.isItemSet(itemId))
                || BlockedItems.getBLOCKED_ITEMS().contains(itemId)
                || (config.freeToPlay() && isBlockedOnFreeToPlay(itemId, config));
    }

    private static boolean isBlockedOnFreeToPlay(int itemId, ChanceManConfig config) {
        return !config.enableFreeToPlayTradeOnlyItems() &&
                FreeToPlayBlockedItems.isFreeToPlayTradeOnlyItem(itemId);
    }

    /**
     * Returns the correct ensouled head ID for the given item name.
     * If not found, returns DEFAULT_ENSOULED_HEAD_ID.
     *
     * @param itemName the item name to check.
     * @return the ensouled head ID if found; otherwise DEFAULT_ENSOULED_HEAD_ID.
     */
    public static int getEnsouledHeadId(String itemName) {
        return (itemName == null)
                ? EnsouledHeadMapping.DEFAULT_ENSOULED_HEAD_ID
                : EnsouledHeadMapping.getENSOULED_HEAD_MAP().getOrDefault(
                itemName.toLowerCase(), EnsouledHeadMapping.DEFAULT_ENSOULED_HEAD_ID);
    }

    /**
     * Scans the provided text and returns the ensouled head ID if any known ensouled head key is found.
     * Returns DEFAULT_ENSOULED_HEAD_ID if no match is found.
     *
     * @param text the text to scan.
     * @return the ensouled head ID if a key is found; otherwise DEFAULT_ENSOULED_HEAD_ID.
     */
    public static int getEnsouledHeadIdFromText(String text) {
        if (text == null) {
            return EnsouledHeadMapping.DEFAULT_ENSOULED_HEAD_ID;
        }
        String lowerText = text.toLowerCase();
        for (String key : EnsouledHeadMapping.getENSOULED_HEAD_MAP().keySet()) {
            if (lowerText.contains(key)) {
                return EnsouledHeadMapping.getENSOULED_HEAD_MAP().get(key);
            }
        }
        return EnsouledHeadMapping.DEFAULT_ENSOULED_HEAD_ID;
    }

    /**
     * Checks if a poisonable weapon variant is eligible for rolling.
     * Base weapons are always eligible; for poisoned variants, if requireWeaponPoison is true,
     * the corresponding global weapon poison must also be unlocked.
     *
     * @param itemId the item id to check
     * @param requireWeaponPoison if true, the matching global poison must be unlocked
     * @param unlockedItems the set of unlocked item ids
     * @return true if eligible; false otherwise
     */
    public static boolean isPoisonEligible(int itemId, boolean requireWeaponPoison, Set<Integer> unlockedItems) {
        return PoisonWeapons.isPoisonVariantEligible(itemId, requireWeaponPoison, unlockedItems);
    }

    private boolean isGlobalWeaponPoison(int itemId) {
        return itemId == PoisonWeapons.WEAPON_POISON.getBaseId() ||
                itemId == PoisonWeapons.WEAPON_POISON_.getBaseId() ||
                itemId == PoisonWeapons.WEAPON_POISON__.getBaseId();
    }
}
