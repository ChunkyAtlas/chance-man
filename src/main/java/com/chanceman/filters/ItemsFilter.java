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
        return !config.includeF2PTradeOnlyItems() &&
                FreeToPlayBlockedItems.isFreeToPlayTradeOnlyItem(itemId);
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

    private static boolean isGlobalWeaponPoison(int itemId) {
        return itemId == PoisonWeapons.WEAPON_POISON.getBaseId() ||
                itemId == PoisonWeapons.WEAPON_POISON_.getBaseId() ||
                itemId == PoisonWeapons.WEAPON_POISON__.getBaseId();
    }
}
