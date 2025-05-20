package com.chanceman.ui;

import com.chanceman.drops.DropCache;
import com.chanceman.drops.DropFetcher;
import com.chanceman.drops.NpcDropData;
import com.chanceman.drops.DropItem;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import net.runelite.api.*;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;

@Slf4j
@Singleton
public class DropsTabUI
{
    private static final int MUSIC_GROUP = 239;
    @Inject private ItemManager itemManager;
    private final Client       client;
    private final ClientThread clientThread;
    private final EventBus     eventBus;
    private final DropCache    dropCache;
    private final DropFetcher  dropFetcher;

    // these get set when someone clicks “Show Drops”
    private List<Widget> backupJukeboxKids;
    private List<Widget> backupScrollKids;
    private boolean overrideActive = false;
    private NpcDropData currentDrops     = null;


    @Inject
    public DropsTabUI(
            Client client,
            ClientThread clientThread,
            EventBus eventBus,
            DropCache dropCache,
            DropFetcher dropFetcher
    )
    {
        this.client       = client;
        this.clientThread = clientThread;
        this.eventBus     = eventBus;
        this.dropCache    = dropCache;
        this.dropFetcher  = dropFetcher;
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
        if (idx < 0)
        {
            return;
        }

        MenuEntry attack = list.get(idx);
        String name    = target.getName();
        int    id      = target.getId();
        int    lvl     = target.getCombatLevel();

        // build our new entry right after “Attack”
        MenuEntry showDrops = client.getMenu()
                .createMenuEntry(idx + 1)
                .setOption("Show Drops")
                .setTarget(attack.getTarget())
                .setIdentifier(attack.getIdentifier())
                .setParam0(attack.getParam0())
                .setParam1(attack.getParam1())
                .setType(MenuAction.RUNELITE);

        showDrops.onClick(me ->
        {
            dropCache.get(id, name, lvl)
                    .thenCompose(cached ->
                    {
                        if (cached != null && !cached.getDropTableSections().isEmpty())
                        {
                            return CompletableFuture.completedFuture(cached);
                        }
                        return dropFetcher.fetch(id, name, lvl);
                    })
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
        log.info("[MenuOpened] Inserted Show Drops at {}", idx + 1);
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event)
    {
        if (overrideActive && event.getGroupId() == MUSIC_GROUP)
        {
            log.info("[WidgetLoaded] Music tab opened, applying override");
            overrideMusicWidget(currentDrops);
        }
    }

    @Subscribe
    public void onWidgetClosed(WidgetClosed event)
    {
        if (overrideActive && event.getGroupId() == MUSIC_GROUP)
        {
            log.info("[WidgetClosed] Music tab closed, restoring originals");
            clientThread.invokeLater(this::restoreMusicWidget);
        }
    }

    private void overrideMusicWidget(NpcDropData dropData)
    {

        Widget jukebox    = client.getWidget(239, 6);
        Widget scrollable = client.getWidget(239, 4);
        Widget scrollbar  = client.getWidget(239, 7);

        if (jukebox == null || scrollable == null || scrollbar == null) return;

        for (Widget w : jukebox.getChildren())
            w.setHidden(true);
        for (Widget w : jukebox.getDynamicChildren())
            w.setHidden(true);

        List<DropItem> drops = dropData.getDropTableSections().stream()
                .flatMap(sec -> sec.getItems().stream())
                .collect(Collectors.toList());

        final int ICON_SIZE = 32;
        final int PADDING   = 4;
        final int COLUMNS   = 4;
        final int MARGIN_X  = 8;
        final int MARGIN_Y  = 8;

        for (int i = 0; i < drops.size(); i++)
        {
            DropItem d  = drops.get(i);
            String name = d.getName();

            int resolvedId = itemManager.search(name).stream()
                    .map(ItemPrice::getId)
                    .filter(id -> {
                        ItemComposition comp = itemManager.getItemComposition(id);
                        return comp != null && comp.getName().equalsIgnoreCase(name);
                    })
                    .findFirst()
                    .orElse(-1);

            if (resolvedId <= 0) continue;

            int col  = i % COLUMNS;
            int row  = i / COLUMNS;
            int x    = MARGIN_X + col * (ICON_SIZE + PADDING);
            int y    = MARGIN_Y + row * (ICON_SIZE + PADDING);

            Widget icon = jukebox.createChild(-1);
            icon.setType(WidgetType.GRAPHIC);
            icon.setItemId(resolvedId);
            icon.setItemQuantityMode(ItemQuantityMode.NEVER);
            icon.setOriginalX(x);
            icon.setOriginalY(y);
            icon.setOriginalWidth(ICON_SIZE);
            icon.setOriginalHeight(ICON_SIZE);
            icon.revalidate();
        }

        int rows = (drops.size() + COLUMNS - 1) / COLUMNS;
        scrollable.setScrollHeight(MARGIN_Y * 2 + rows * (ICON_SIZE + PADDING));
        scrollbar.revalidateScroll();
    }


    private void restoreMusicWidget()
    {
        Widget jukebox = client.getWidget(239, 6);
        Widget scrollable = client.getWidget(239, 4);
        Widget overlay = client.getWidget(239, 5);

        if (overlay != null)
            overlay.setHidden(false);
        if (jukebox != null)
            jukebox.setHidden(false);

        if (scrollable != null)
        {
            for (Widget w : scrollable.getDynamicChildren())
                w.setHidden(true);
        }

        overrideActive = false;
    }
}