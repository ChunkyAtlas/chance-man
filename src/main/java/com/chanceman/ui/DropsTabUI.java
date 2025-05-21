package com.chanceman.ui;

import com.chanceman.drops.DropCache;
import com.chanceman.drops.NpcDropData;
import com.chanceman.drops.DropItem;
import com.chanceman.managers.RolledItemsManager;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

import net.runelite.api.*;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;

@Slf4j
@Singleton
public class DropsTabUI
{
    private static final int MUSIC_GROUP = 239;
    private final Client client;
    private final ClientThread clientThread;
    private final EventBus eventBus;
    private final DropCache dropCache;
    private boolean overrideActive = false;
    private NpcDropData currentDrops = null;
    private List<Widget> backupJukeboxKids = null;
    private List<Widget> backupScrollKids  = null;

    @Inject private RolledItemsManager rolledItemsManager;

    @Inject
    public DropsTabUI(
            Client client,
            ClientThread clientThread,
            EventBus eventBus,
            DropCache dropCache,
            RolledItemsManager rolledItemsManager

    )
    {
        this.client       = client;
        this.clientThread = clientThread;
        this.eventBus     = eventBus;
        this.dropCache    = dropCache;
        this.rolledItemsManager = rolledItemsManager;
    }

    public void startUp()
    {
        eventBus.register(this);
    }

    public void shutDown()
    {
        eventBus.unregister(this);
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        List<MenuEntry> list = new ArrayList<>(Arrays.asList(event.getMenuEntries()));
        NPC target = null;
        int idx = -1;

        for (int i = 0; i < list.size(); i++)
        {
            MenuEntry e = list.get(i);
            if (e.getType() == MenuAction.NPC_SECOND_OPTION
                    && "Attack".equals(e.getOption()))
            {
                try
                {
                    target = client.getTopLevelWorldView()
                            .npcs()
                            .byIndex(e.getIdentifier());
                }
                catch (ArrayIndexOutOfBoundsException ex)
                {
                    continue;
                }
                if (target != null && target.getCombatLevel() > 0)
                {
                    idx = i;
                    break;
                }
            }
        }
        if (idx < 0) return;

        MenuEntry attack = list.get(idx);
        String name    = target.getName();
        int    id      = target.getId();
        int    lvl     = target.getCombatLevel();

        MenuEntry showDrops = client.getMenu()
                .createMenuEntry(idx - 1)
                .setOption("Show Drops")
                .setTarget(attack.getTarget())
                .setIdentifier(attack.getIdentifier())
                .setParam0(attack.getParam0())
                .setParam1(attack.getParam1())
                .setType(MenuAction.RUNELITE);

        showDrops.onClick(me ->
        {
            dropCache.get(id, name, lvl)
                    .thenAccept(dropData ->
                    {
                        clientThread.invokeLater(() ->
                        {
                            currentDrops   = dropData;
                            overrideActive = true;
                            overrideMusicWidget(dropData);
                        });
                    });
        });

        list.add(idx + 1, showDrops);
        event.setMenuEntries(list.toArray(new MenuEntry[0]));
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        int group = event.getGroupId();

        if (overrideActive && group != MUSIC_GROUP) overrideActive = false;

        if (group == MUSIC_GROUP)
        {
            if (overrideActive) overrideMusicWidget(currentDrops);
            else restoreMusicWidget();
        }
    }
    /**
     * Returns only the first occurrence of each non-zero itemId, in itemId order.
     */
    private List<DropItem> dedupeAndSortByItemId(List<DropItem> drops)
    {
        return drops.stream()
                // ignore zeros, collect first occurrence in insertion order
                .filter(d -> d.getItemId() > 0)
                .collect(Collectors.toMap(
                        DropItem::getItemId,
                        d -> d,
                        (first, second) -> first,
                        LinkedHashMap::new
                ))
                .values().stream()
                // now sort by the key (itemId)
                .sorted(Comparator.comparingInt(DropItem::getItemId))
                .collect(Collectors.toList());
    }

    /**
     * Hides all static and dynamic children of the given widget, if any exist.
     */
    private void hideAllChildrenSafely(Widget widget)
    {
        if (widget == null)
        {
            return;
        }
        Widget[] staticKids = widget.getChildren();
        if (staticKids != null)
        {
            for (Widget w : staticKids)
            {
                w.setHidden(true);
            }
        }
        Widget[] dynamicKids = widget.getDynamicChildren();
        if (dynamicKids != null)
        {
            for (Widget w : dynamicKids)
            {
                w.setHidden(true);
            }
        }
    }

    private void overrideMusicWidget(NpcDropData dropData)
    {
        final int ICON_SIZE = 32;
        final int PADDING   = 4;
        final int COLUMNS   = 4;
        final int MARGIN_X  = 8;
        final int MARGIN_Y  = 8;

        Set<Integer> rolledIds = rolledItemsManager.getRolledItems();

        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        Widget jukebox    = client.getWidget(MUSIC_GROUP, 6);
        Widget overlay    = client.getWidget(MUSIC_GROUP, 5);
        Widget scrollbar  = client.getWidget(MUSIC_GROUP, 7);

        if (overlay != null)
        {
            overlay.setHidden(true);
        }
        if (jukebox == null || scrollable == null || scrollbar == null)
        {
            return;
        }

        // backup originals
        if (backupJukeboxKids == null)
        {
            Widget[] kids = jukebox.getChildren();
            backupJukeboxKids = (kids != null)
                    ? new ArrayList<>(Arrays.asList(kids))
                    : new ArrayList<>();
        }
        if (backupScrollKids == null)
        {
            Widget[] kids = scrollable.getChildren();
            backupScrollKids = (kids != null)
                    ? new ArrayList<>(Arrays.asList(kids))
                    : new ArrayList<>();
        }

        // hide everything safely
        hideAllChildrenSafely(jukebox);
        hideAllChildrenSafely(scrollable);

        List<DropItem> allDrops = dropData.getDropTableSections().stream()
                .flatMap(sec -> sec.getItems().stream())
                .collect(Collectors.toList());
        List<DropItem> drops = dedupeAndSortByItemId(allDrops);

        int displayIndex = 0;
        for (DropItem d : drops)
        {
            int itemId = d.getItemId();
            int col = displayIndex % COLUMNS;
            int row = displayIndex / COLUMNS;
            int x   = MARGIN_X + col * (ICON_SIZE + PADDING);
            int y   = MARGIN_Y + row * (ICON_SIZE + PADDING);

            Widget icon = scrollable.createChild(-1);
            icon.setHidden(false);
            icon.setType(WidgetType.GRAPHIC);
            icon.setItemId(itemId);
            icon.setItemQuantityMode(ItemQuantityMode.NEVER);
            icon.setOriginalX(x);
            icon.setOriginalY(y);
            icon.setOriginalWidth(ICON_SIZE);
            icon.setOriginalHeight(ICON_SIZE);
            icon.setBorderType(1);
            icon.setOpacity(rolledIds.contains(itemId) ? 0 : 150);
            icon.revalidate();

            displayIndex++;
        }

        int rows = (displayIndex + COLUMNS - 1) / COLUMNS;
        scrollable.setScrollHeight(MARGIN_Y * 2 + rows * (ICON_SIZE + PADDING));
        scrollbar.revalidateScroll();
    }

    private void restoreMusicWidget()
    {
        Widget overlay = client.getWidget(MUSIC_GROUP, 5);
        if (overlay != null) overlay.setHidden(false);

        Widget jukebox = client.getWidget(MUSIC_GROUP, 6);
        if (jukebox != null && backupJukeboxKids != null) for (Widget w : backupJukeboxKids) w.setHidden(false);

        Widget scrollable = client.getWidget(MUSIC_GROUP, 4);
        if (scrollable != null && backupScrollKids != null)
        {
            for (Widget w : backupScrollKids) w.setHidden(false);
            scrollable.deleteAllChildren();
        }

        backupJukeboxKids = null;
        backupScrollKids  = null;
        overrideActive    = false;
    }
}