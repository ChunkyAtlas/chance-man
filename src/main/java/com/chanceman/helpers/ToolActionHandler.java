package com.chanceman.helpers;

import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.ItemComposition;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.ItemContainer;
import net.runelite.api.InventoryID;
import net.runelite.client.game.ItemManager;
import com.chanceman.UnlockedItemsManager;

public class ToolActionHandler
{
    /**
     * Checks if the event corresponds to a tool/skilling action and if so,
     * verifies that the player has an unlocked tool.
     * Returns true if the action is allowed, false if it should be blocked.
     */
    public static boolean handleToolAction(Client client, ItemManager itemManager,
                                           UnlockedItemsManager unlockedItemsManager, Map<String,
                    List<String>> toolActionMap, MenuOptionClicked event)
    {
        String option = event.getMenuEntry().getOption().toLowerCase();
        for (Map.Entry<String, List<String>> entry : toolActionMap.entrySet())
        {
            if (option.contains(entry.getKey()))
            {
                List<String> requiredToolKeywords = entry.getValue();
                boolean foundUnlockedTool = false;
                ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
                if (inventory != null)
                {
                    for (net.runelite.api.Item item : inventory.getItems())
                    {
                        int canonicalId = itemManager.canonicalize(item.getId());
                        ItemComposition comp = itemManager.getItemComposition(canonicalId);
                        if (comp != null)
                        {
                            String itemName = comp.getName().toLowerCase();
                            for (String toolKeyword : requiredToolKeywords)
                            {
                                if (itemName.contains(toolKeyword))
                                {
                                    if (unlockedItemsManager != null && unlockedItemsManager.isUnlocked(canonicalId))
                                    {
                                        foundUnlockedTool = true;
                                        break;
                                    }
                                }
                            }
                            if (foundUnlockedTool)
                            {
                                break;
                            }
                        }
                    }
                }
                if (!foundUnlockedTool)
                {
                    event.consume();
                    return false;
                }
            }
        }
        return true;
    }
}
