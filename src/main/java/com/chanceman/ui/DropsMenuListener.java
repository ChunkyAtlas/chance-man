package com.chanceman.ui;

import com.chanceman.drops.DropCache;
import lombok.extern.slf4j.Slf4j;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.events.MenuOpened;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

@Slf4j
@Singleton
public class DropsMenuListener
{
    private final Client client;
    private final ClientThread clientThread;
    private final DropCache dropCache;
    private final MusicWidgetController widgetController;

    @Inject
    public DropsMenuListener(
            Client client,
            ClientThread clientThread,
            DropCache dropCache,
            MusicWidgetController widgetController
    )
    {
        this.client = client;
        this.clientThread = clientThread;
        this.dropCache = dropCache;
        this.widgetController = widgetController;
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event)
    {
        List<MenuEntry> entries = new ArrayList<>(Arrays.asList(event.getMenuEntries()));
        NPC target = null;
        MenuEntry attackEntry = null;
        int attackIdx = -1;

        for (int i = 0; i < entries.size(); i++)
        {
            MenuEntry e = entries.get(i);
            if (e.getType() == MenuAction.NPC_SECOND_OPTION && "Attack".equals(e.getOption()))
            {
                try
                {
                    NPC possible = client.getTopLevelWorldView()
                            .npcs()
                            .byIndex(e.getIdentifier());
                    if (possible != null && possible.getCombatLevel() > 0)
                    {
                        target = possible;
                        attackEntry = e;
                        attackIdx = i;
                        break;
                    }
                }
                catch (ArrayIndexOutOfBoundsException ex)
                {
                    // ignore invalid indices
                }
            }
        }
        if (attackEntry == null || target == null)
        {
            return;
        }

        int id = target.getId();
        String name = target.getName();
        int level = target.getCombatLevel();

        MenuEntry showDrops = WidgetUtils.createShowDropsEntry(
                client,
                attackIdx - 1,
                attackEntry
        );
        showDrops.onClick(me ->
                dropCache.get(id, name, level)
                        .thenAccept(dropData ->
                                clientThread.invokeLater(() -> widgetController.override(dropData))
                        )
        );

        entries.add(attackIdx + 1, showDrops);
        event.setMenuEntries(entries.toArray(new MenuEntry[0]));
    }
}