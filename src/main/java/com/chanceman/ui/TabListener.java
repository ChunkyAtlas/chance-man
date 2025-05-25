package com.chanceman.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.VarClientInt;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TabListener
{
    private final Client client;
    private final ClientThread clientThread;
    private final MusicWidgetController widgetController;

    @Inject
    public TabListener(
            Client client,
            ClientThread clientThread,
            MusicWidgetController widgetController
    )
    {
        this.client = client;
        this.clientThread = clientThread;
        this.widgetController = widgetController;
    }

    @Subscribe
    public void onVarClientIntChanged(VarClientIntChanged ev)
    {
        if (ev.getIndex() != VarClientInt.INVENTORY_TAB) return;

        int newTab = client.getVarcIntValue(VarClientInt.INVENTORY_TAB);
        if (widgetController.isOverrideActive() && newTab != 13)
        {
            clientThread.invokeLater(widgetController::restore);
        }
        else if (!widgetController.isOverrideActive() && newTab == 13 && widgetController.hasData())
        {
            clientThread.invokeLater(() ->
                    widgetController.override(widgetController.getCurrentData())
            );
        }
    }
}
