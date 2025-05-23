package com.chanceman.ui;

import com.chanceman.drops.DropItem;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.MenuAction;
import net.runelite.api.widgets.Widget;

import java.util.*;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

public final class WidgetUtils
{
    /**
     * Creates a "Show Drops" menu entry identical to the original Attack entry.
     *
     * @param client      The RuneLite client.
     * @param insertIndex The index at which to insert the new entry.
     * @param attackEntry The original "Attack" menu entry to clone.
     * @return A configured MenuEntry that runs a custom drop display.
     */
    public static MenuEntry createShowDropsEntry(
            Client client,
            int insertIndex,
            MenuEntry attackEntry
    )
    {
        return client.getMenu()
                .createMenuEntry(insertIndex)
                .setOption("Show Drops")
                .setTarget(attackEntry.getTarget())
                .setIdentifier(attackEntry.getIdentifier())
                .setParam0(attackEntry.getParam0())
                .setParam1(attackEntry.getParam1())
                .setType(MenuAction.RUNELITE);
    }

    /**
     * Hides all static and dynamic children of the given widget, if any exist.
     *
     * @param widget The widget whose children should be hidden.
     */
    public static void hideAllChildrenSafely(Widget widget)
    {
        if (widget == null)
            return;

        Widget[] staticKids = widget.getChildren();
        if (staticKids != null)
        {
            for (Widget child : staticKids)
            {
                if (child != null) child.setHidden(true);
            }
        }

        Widget[] dynamicKids = widget.getDynamicChildren();
        if (dynamicKids != null)
        {
            for (Widget child : dynamicKids)
            {
                if (child != null) child.setHidden(true);
            }
        }
    }

    /**
     * Deduplicates a list of DropItems by item ID and sorts them ascending.
     *
     * @param drops The list of DropItems to process.
     * @return A new sorted, deduplicated list.
     */
    public static List<DropItem> dedupeAndSort(List<DropItem> drops)
    {
        return drops.stream()
                .filter(d -> d.getItemId() > 0)
                .collect(Collectors.toMap(
                        DropItem::getItemId,
                        d -> d,
                        (first, second) -> first,
                        LinkedHashMap::new
                ))
                .values().stream()
                .sorted(Comparator.comparingInt(DropItem::getItemId))
                .collect(Collectors.toList());
    }
}
