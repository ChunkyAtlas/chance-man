package com.chanceman.helpers;

import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.game.ItemManager;
import com.chanceman.UnlockedItemsManager;
import com.chanceman.ChanceManPlugin;

public class InventoryActionHandler
{
    /**
     * Handles direct inventory actions including ground item actions.
     * For locked inventory items, only "examine" and "drop" actions are allowed.
     * Also, for ground items, if the item is locked, the action is consumed.
     *
     * @param itemManager         The item manager.
     * @param unlockedItemsManager The unlocked items manager.
     * @param event               The menu option clicked event.
     * @param plugin              Reference to the main plugin for helper methods.
     */
    public static void handleInventoryAction(ItemManager itemManager, UnlockedItemsManager unlockedItemsManager,
                                             MenuOptionClicked event, ChanceManPlugin plugin)
    {
        String option = event.getMenuEntry().getOption().toLowerCase();

        // Handle ground item actions.
        if (event.getMenuAction() != null &&
                (event.getMenuAction().toString().contains("GROUND_ITEM") ||
                        option.contains("take") || option.contains("pick-up") || option.contains("pickup")))
        {
            int rawItemId = event.getId() != -1 ? event.getId() : event.getMenuEntry().getItemId();
            int canonicalGroundId = itemManager.canonicalize(rawItemId);
            if (plugin.isTradeable(canonicalGroundId)
                    && !plugin.isNotTracked(canonicalGroundId)
                    && unlockedItemsManager != null && !unlockedItemsManager.isUnlocked(canonicalGroundId))
            {
                event.consume();
                return;
            }
        }

        // Handle direct inventory item actions.
        if (event.getMenuEntry().getItemId() != -1)
        {
            int rawItemId = event.getMenuEntry().getItemId();
            int canonicalId = itemManager.canonicalize(rawItemId);
            String itemName = plugin.getItemName(canonicalId).toLowerCase();
            // Skip non-tradeable items and special cases.
            if (!plugin.isTradeable(canonicalId)
                    || plugin.isNotTracked(canonicalId)
                    || itemName.contains("coin pouch")
                    || itemName.contains("clue scroll"))
            {
                return;
            }
            // For locked inventory items (when directly clicked), allow only "examine" and "drop" actions.
            if (unlockedItemsManager != null && !unlockedItemsManager.isUnlocked(canonicalId))
            {
                if (!option.equals("examine") && !option.equals("drop"))
                {
                    event.consume();
                }
            }
        }
    }
}
