package com.chanceman.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.VarClientIntChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import com.chanceman.ChanceManConfig;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TabListener
{
    private final Client client;
    private final ClientThread clientThread;
    private final MusicWidgetController widgetController;
    private final ChanceManConfig config;

    @Inject
    public TabListener(
            Client client,
            ClientThread clientThread,
            MusicWidgetController widgetController,
            ChanceManConfig config
    )
    {
        this.client = client;
        this.clientThread = clientThread;
        this.widgetController = widgetController;
        this.config = config;
    }

    @Subscribe
    public void onVarClientIntChanged(VarClientIntChanged ev)
    {
        if (ev.getIndex() != 171) return;

        int newTab = client.getVarcIntValue(171);
        if (widgetController.isOverrideActive() && newTab != 13)
        {
            if (!config.showDropsAlwaysOpen())
            {
                clientThread.invokeLater(widgetController::restore);
            }
            return;
        }

        // If returning to the music tab and we have cached drops data, re-apply
        if (!widgetController.isOverrideActive() && newTab == 13 && widgetController.hasData())
        {
            clientThread.invokeLater(() ->
                    widgetController.override(widgetController.getCurrentData())
            );
        }
    }
}
